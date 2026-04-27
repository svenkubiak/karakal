#!/bin/bash
set -e

MODE="$1"
IMAGE_NAME="karakal"
GHCR_USERNAME="svenkubiak"
REPO_NAME="karakal"
GHCR_URL="ghcr.io"
REPO_URL="https://github.com/$GHCR_USERNAME/$REPO_NAME"

# Check git state ONCE, at script start
check_clean_git() {
  git status
  if ! git diff-index --quiet HEAD --; then
    echo "There are uncommitted changes in the repository. Please commit or stash them before running this script."
    exit 1
  fi
}

# Official SemVer 2.0.0 regex, single line, bash-friendly
SEMVER_REGEX='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-((0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)(\.(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*))?(\+[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$'

check_clean_git

# === Always run Maven build ===
echo "🔧 Starting Maven build..."
mvn clean verify

if [ $? -ne 0 ]; then
  echo "❌ Maven build failed! Exiting..."
  exit 1
else
  echo "✅ Maven build succeeded."
fi

# === DEV MODE ===
if [[ "$MODE" == "dev" ]]; then
  echo "[Dev Mode] Skipping Maven release/version updates..."

  IMAGE_DEV_PATH="$GHCR_URL/$GHCR_USERNAME/$REPO_NAME/$IMAGE_NAME:dev"

  echo "[Karakal] Building :dev image..."
  docker build --no-cache \
    --label "org.opencontainers.image.source=$REPO_URL" \
    -t "$IMAGE_NAME:dev" .
  docker tag "$IMAGE_NAME:dev" "$IMAGE_DEV_PATH"
  docker push "$IMAGE_DEV_PATH"

  echo "✅ Dev images pushed successfully."
  exit 0
fi

# === REGULAR RELEASE MODE ===

# 1) Get current version and prompt for new version
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
read -rp "Enter new version (current: ${CURRENT_VERSION}): " NEW_VERSION

# 2) Validate against strict SemVer 2.0.0
if [[ ! "$NEW_VERSION" =~ $SEMVER_REGEX ]]; then
  echo "Invalid Semantic Version (must follow SemVer 2.0.0): $NEW_VERSION"
  exit 1
fi

# 3) Set version (this will make the repo dirty, which is OK now)
mvn versions:set -DnewVersion="$NEW_VERSION"
STATUS=$?

if [ $STATUS -ne 0 ]; then
    echo "❌ Failed to set new version! Exiting..."
    exit 1
fi

# 4) Rebuild JAR with the new version so the correct version tag ends up in the image  ← NEU
echo "🔧 Rebuilding JAR with new version $NEW_VERSION..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
  echo "❌ Maven rebuild failed! Exiting..."
  exit 1
else
  echo "✅ Maven rebuild succeeded."
fi

IMAGE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
IMAGE_FULL_PATH="$GHCR_URL/$GHCR_USERNAME/$REPO_NAME/$IMAGE_NAME:$IMAGE_VERSION"
IMAGE_LATEST_PATH="$GHCR_URL/$GHCR_USERNAME/$REPO_NAME/$IMAGE_NAME:latest"

is_stable_release() {
    if [[ "$IMAGE_VERSION" =~ [aA]lpha|[bB]eta|[rR][cC] ]]; then
        return 1
    else
        return 0
    fi
}

echo "[Karakal] Building Version Docker image..."
docker build --no-cache \
  --label "org.opencontainers.image.source=$REPO_URL" \
  -t "$IMAGE_NAME:$IMAGE_VERSION" .

if is_stable_release; then
    docker tag "$IMAGE_NAME:$IMAGE_VERSION" "$IMAGE_NAME:latest"
fi

docker tag "$IMAGE_NAME:$IMAGE_VERSION" "$IMAGE_FULL_PATH"
if is_stable_release; then
    docker tag "$IMAGE_NAME:latest" "$IMAGE_LATEST_PATH"
fi

docker push "$IMAGE_FULL_PATH"
if is_stable_release; then
    docker push "$IMAGE_LATEST_PATH"
fi

if [ $? -eq 0 ]; then
    git tag "$IMAGE_VERSION"
    mvn release:update-versions
    git commit -am "Updated version after release"
    git push --tags origin main
    echo "🎉 Released $IMAGE_VERSION!"
else
    echo "❌ Failed to push the image. Exiting..."
    exit 1
fi

rm -f pom.xml.versionsBackup