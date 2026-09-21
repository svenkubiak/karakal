#!/bin/bash
set -euo pipefail

# Secrets are written to .env, so no other local user may read what is created here
umask 077

# Set KARAKAL_VERSION to a released tag to pin the downloaded artifacts, for example
#   KARAKAL_VERSION=1.1.26 ./install.sh
if [ -n "${KARAKAL_VERSION:-}" ]; then
  KARAKAL_REF="refs/tags/${KARAKAL_VERSION}"
else
  KARAKAL_REF="refs/heads/main"
fi

BASE_URL="https://raw.githubusercontent.com/svenkubiak/karakal/${KARAKAL_REF}"
CONFIG_URL="${BASE_URL}/config.yaml"
COMPOSE_URL="${BASE_URL}/compose.yaml"

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

# Create an .env file and add configuration variables
if [ -f ".env" ]; then
  echo "1/6 [Skipping] .env already exists - not regenerating secrets."
  echo "    Remove it manually if you really want a fresh installation."
else
  echo "1/6 Creating .env file..."

  # Generated up front, so that a failure aborts the installation instead of writing empty secrets
  MONGODB_USERNAME=karakal
  MONGODB_PASSWORD=$(generate_secret)
  APPLICATION_SECRET=$(generate_secret)
  SESSION_COOKIE_SECRET=$(generate_secret)
  SESSION_COOKIE_KEY=$(generate_secret)
  AUTHENTICATION_COOKIE_SECRET=$(generate_secret)
  AUTHENTICATION_COOKIE_KEY=$(generate_secret)
  FLASH_COOKIE_SECRET=$(generate_secret)
  FLASH_COOKIE_KEY=$(generate_secret)

  cat > .env <<EOL
# Custom configuration
VERSION=latest
KARAKAL_URL=http://localhost

# Auto generated - Change at your own risk
MONGODB_INITDB_DATABASE=karakal
MONGODB_INITDB_ROOT_USERNAME=${MONGODB_USERNAME}
MONGODB_INITDB_ROOT_PASSWORD=${MONGODB_PASSWORD}
PERSISTENCE_MONGO_USERNAME=${MONGODB_USERNAME}
PERSISTENCE_MONGO_PASSWORD=${MONGODB_PASSWORD}
APPLICATION_SECRET=${APPLICATION_SECRET}
SESSION_COOKIE_SECRET=${SESSION_COOKIE_SECRET}
SESSION_COOKIE_KEY=${SESSION_COOKIE_KEY}
AUTHENTICATION_COOKIE_SECRET=${AUTHENTICATION_COOKIE_SECRET}
AUTHENTICATION_COOKIE_KEY=${AUTHENTICATION_COOKIE_KEY}
FLASH_COOKIE_SECRET=${FLASH_COOKIE_SECRET}
FLASH_COOKIE_KEY=${FLASH_COOKIE_KEY}
EOL

  chmod 600 .env
fi

if [ ! -d "logs" ]; then
  echo "2/6 Creating logs folder..."
  mkdir "logs"
else
  echo "[Skipping] Logs folder already exists."
fi

# Create config folder if it does not exist
if [ ! -d "config" ]; then
  echo "3/6 Creating config folder..."
  mkdir "config"
else
  echo "[Skipping] Config folder already exists."
fi

# Download the default config.yaml
echo "4/6 Downloading config.yaml..."
download "$CONFIG_URL" "config/config.yaml"
chmod 600 "config/config.yaml"

# Step 5: Download the compose.yaml
echo "5/6 Downloading compose.yaml..."
download "$COMPOSE_URL" "compose.yaml"

# Step 6: Installation complete
echo "6/6 Installation complete!"
echo ""
echo "Please configure your specific environment in your compose.yaml."
echo "Enjoy Karakal!"
