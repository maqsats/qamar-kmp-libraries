#!/bin/bash

# Script to publish libraries to Maven Central
# Usage: ./scripts/publish.sh [version] [snapshot]

set -e

VERSION=${1:-"1.0.0"}
SNAPSHOT=${2:-"false"}

if [ "$SNAPSHOT" == "true" ]; then
    VERSION="${VERSION}-SNAPSHOT"
fi

echo "Publishing version: $VERSION"

# Check if required properties are set
if [ -z "$OSSRH_USERNAME" ] || [ -z "$OSSRH_PASSWORD" ]; then
    echo "Error: OSSRH_USERNAME and OSSRH_PASSWORD must be set"
    exit 1
fi

# Update gradle.properties
cat >> gradle.properties << EOF
GROUP=io.github.maqsats
VERSION_NAME=$VERSION
OSSRH_USERNAME=$OSSRH_USERNAME
OSSRH_PASSWORD=$OSSRH_PASSWORD
SIGNING_KEY_ID=$SIGNING_KEY_ID
SIGNING_PASSWORD=$SIGNING_PASSWORD
SIGNING_KEY=$SIGNING_KEY
PUBLISHING_GITHUB_REPO=${PUBLISHING_GITHUB_REPO:-"maqsats/qamar-kmp-libraries"}
PUBLISHING_DEVELOPER_ID=${PUBLISHING_DEVELOPER_ID:-"maqsats"}
PUBLISHING_DEVELOPER_NAME=${PUBLISHING_DEVELOPER_NAME:-"Maksat Inkar"}
PUBLISHING_DEVELOPER_EMAIL=${PUBLISHING_DEVELOPER_EMAIL:-""}
EOF

# Publish
./gradlew publish --no-daemon

echo "Published successfully!"
echo "For non-snapshot releases, verify and manage your deployments at:"
echo "https://central.sonatype.com/publishing/deployments"
