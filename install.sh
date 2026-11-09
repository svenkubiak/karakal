#!/bin/bash
set -euo pipefail

# .env is mode 600. The secrets directory is mode 755 and the files are mode 644.
# compose mounts the whole directory at /run/secrets, so the container user (uid 33)
# must be able to traverse it. A mode 700 directory would hide the files from that user.
umask 077

# Set KARAKAL_VERSION to a released tag to pin the downloaded compose.yaml, for example
#   KARAKAL_VERSION=1.1.26 ./install.sh
if [ -n "${KARAKAL_VERSION:-}" ]; then
  KARAKAL_REF="refs/tags/${KARAKAL_VERSION}"
else
  KARAKAL_REF="refs/heads/main"
fi

BASE_URL="https://raw.githubusercontent.com/svenkubiak/karakal/${KARAKAL_REF}"
COMPOSE_URL="${BASE_URL}/compose.yaml"
SECRETS_DIR="secrets"

# The file name is the configuration key. Mangoo reads file{} from /run/secrets/<key>.
SECRET_KEYS=(
  "application.secret"
  "session.cookie.secret"
  "session.cookie.key"
  "authentication.cookie.secret"
  "authentication.cookie.key"
  "flash.cookie.secret"
  "flash.cookie.key"
  "persistence.mongo.password"
)

# LC_ALL=C, because tr fails with "Illegal byte sequence" on systems without a C.UTF-8 locale
# (macOS, BSD) and would then silently produce an empty secret.
# The pipeline is guarded, as head closes the pipe and tr exits with SIGPIPE, which would abort
# the script through pipefail.
generate_secret() {
  local secret
  secret="$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom 2>/dev/null | head -c 64)" || true

  if [ "${#secret}" -ne 64 ]; then
    echo "Failed to generate a random secret. Is /dev/urandom readable?" >&2
    exit 1
  fi

  printf '%s' "$secret"
}

# Fails on HTTP errors instead of silently writing the error page to disk
download() {
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 --output "$2" "$1"
}

read_env() {
  local key="$1"
  local file="$2"
  local line value

  line="$(grep -E "^${key}=" "$file" 2>/dev/null | tail -n 1 || true)"
  value="${line#"${key}"=}"
  value="${value%$'\r'}"
  printf '%s' "$value"
}

# An existing installation kept these values in .env. Reuse them so cookie and
# database secrets stay valid across the move to secret files.
migrated_secret() {
  local key="$1"

  if [ ! -f .env ]; then
    printf ''
    return
  fi

  case "$key" in
    application.secret) read_env APPLICATION_SECRET .env ;;
    session.cookie.secret) read_env SESSION_COOKIE_SECRET .env ;;
    session.cookie.key) read_env SESSION_COOKIE_KEY .env ;;
    authentication.cookie.secret) read_env AUTHENTICATION_COOKIE_SECRET .env ;;
    authentication.cookie.key) read_env AUTHENTICATION_COOKIE_KEY .env ;;
    flash.cookie.secret) read_env FLASH_COOKIE_SECRET .env ;;
    flash.cookie.key) read_env FLASH_COOKIE_KEY .env ;;
    persistence.mongo.password)
      local password
      password="$(read_env MONGODB_INITDB_ROOT_PASSWORD .env)"
      if [ -z "$password" ]; then
        password="$(read_env PERSISTENCE_MONGO_PASSWORD .env)"
      fi
      printf '%s' "$password"
      ;;
    *)
      echo "Unknown secret key: ${key}" >&2
      exit 1
      ;;
  esac
}

write_secret_file() {
  local path="$1"
  local value="$2"

  if [ -z "$value" ]; then
    echo "Refusing to write an empty secret to ${path}" >&2
    exit 1
  fi

  printf '%s' "$value" > "$path"
  chmod 644 "$path"
}

read_secret_file() {
  local path="$1"
  printf '%s' "$(cat "$path")"
}

echo "1/5 Creating secret files..."
mkdir -p "$SECRETS_DIR"
chmod 755 "$SECRETS_DIR"

created=0
migrated=0
for key in "${SECRET_KEYS[@]}"; do
  path="${SECRETS_DIR}/${key}"
  if [ -s "$path" ]; then
    chmod 644 "$path"
    continue
  fi

  existing="$(migrated_secret "$key")"
  if [ -n "$existing" ]; then
    write_secret_file "$path" "$existing"
    echo "    ${key} copied from .env"
    migrated=1
  else
    write_secret_file "$path" "$(generate_secret)"
    echo "    ${key} generated"
  fi
  created=1
done

if [ "$created" -eq 0 ]; then
  echo "    [Skipping] secret files already exist."
elif [ "$migrated" -eq 1 ]; then
  echo "    Existing values were copied from .env into ${SECRETS_DIR}/."
  echo "    The application reads those files. MONGODB_INITDB_ROOT_PASSWORD in .env is still used by the database container."
fi

if [ -f .env ] && [ -s "${SECRETS_DIR}/persistence.mongo.password" ]; then
  env_password="$(read_env MONGODB_INITDB_ROOT_PASSWORD .env)"
  file_password="$(read_secret_file "${SECRETS_DIR}/persistence.mongo.password")"
  if [ -n "$env_password" ] && [ "$env_password" != "$file_password" ]; then
    echo "    Warning: MONGODB_INITDB_ROOT_PASSWORD in .env differs from ${SECRETS_DIR}/persistence.mongo.password." >&2
    echo "    The database container uses .env and the application reads the secret file. They must match." >&2
  fi
fi

if [ -f ".env" ]; then
  echo "2/5 [Skipping] .env already exists - not regenerating it."
  echo "    Remove it manually if you really want a fresh installation."
else
  echo "2/5 Creating .env file..."

  mongo_password="$(read_secret_file "${SECRETS_DIR}/persistence.mongo.password")"
  if [ -z "$mongo_password" ]; then
    echo "Failed to read the MongoDB password from ${SECRETS_DIR}/persistence.mongo.password" >&2
    exit 1
  fi

  cat > .env <<EOL
# Custom configuration
VERSION=latest
KARAKAL_URL=http://localhost

# Auto generated - Change at your own risk
# The database container reads the MONGODB_INITDB_* variables. The application
# reads the same password from secrets/persistence.mongo.password and the
# username from PERSISTENCE_MONGO_USERNAME. Keep the two usernames identical.
MONGODB_INITDB_DATABASE=karakal
MONGODB_INITDB_ROOT_USERNAME=karakal
MONGODB_INITDB_ROOT_PASSWORD=${mongo_password}
PERSISTENCE_MONGO_USERNAME=karakal
EOL

  chmod 600 .env
fi

if [ ! -d "logs" ]; then
  echo "3/5 Creating logs folder..."
  mkdir "logs"
else
  echo "3/5 [Skipping] Logs folder already exists."
fi

echo "4/5 Downloading compose.yaml..."
download "$COMPOSE_URL" "compose.yaml"

if [ -f "config/config.yaml" ]; then
  echo "    Note: config/config.yaml is no longer used. The application ships its own config.yaml."
  echo "    You can remove the config directory."
fi

echo "5/5 Installation complete!"
echo ""
echo "Set KARAKAL_URL in .env, then adjust the published host and port in compose.yaml if needed."
echo "Enjoy Karakal!"
