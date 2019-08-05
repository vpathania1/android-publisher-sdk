#!/bin/bash -l

echo "Building and publishing artifacts to nexus"
ORIGINAL_VERSION=3.0.1
VERSION=$ORIGINAL_VERSION-criteo-$(date -u +%Y%m%d%H%M%S)
sh scripts/build-pre-submit.sh
sh gradlew publish -Pversion=$VERSION -PpublishUrl="http://nexus.criteo.prod/content/repositories/criteo.android.releases/"