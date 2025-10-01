#!/bin/bash
set -e

IMAGE_NAME="karakal"
GHCR_USERNAME="svenkubiak"
REPO_NAME="karakal"
GHCR_URL="ghcr.io"

MODE="$1"

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
  docker build --no-cache -t "$IMAGE_NAME:dev" .
  docker tag "$IMAGE_NAME:dev" "$IMAGE_DEV_PATH"
  docker push "$IMAGE_DEV_PATH"

  echo "✅ Dev images pushed successfully."
  exit 0
fi

# === REGULAR RELEASE MODE ===

mvn release:clean
mvn versions:set
STATUS=$?
mvn clean verify -DskipTests=true

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

if [ $STATUS -ne 0 ]; then
    echo "❌ Failed to set new version! Exiting..."
    exit 1
else
    echo "[Karakal] Building Version Docker image..."
    docker build --no-cache -t "$IMAGE_NAME:$IMAGE_VERSION" .

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
fi

rm -f pom.xml.versionsBackup