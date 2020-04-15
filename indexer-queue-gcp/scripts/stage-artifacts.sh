#!/bin/bash

# Exit as soon as a command fails
set -e

if [ ! $# = 1 ]; then
    echo "Usage: $0 dir"
    exit 1
fi

STAGE_DIR=$1

cd $BUILD_REPOSITORY_LOCALPATH/indexer-queue-gcp/scripts

source ./config.sh

#apply gomplate to deploy2gcp.sh.tmpl
gomplate -f ./deploy2gcp.sh.tmpl -o ./deploy2gcp.sh
chmod a+x ./*.sh
echo "Contents of deploy2gcp.sh:"
cat ./deploy2gcp.sh

cd ..
# Upload all build and deploy scripts as artifacts
cp -R ./scripts $STAGE_DIR

# set current directory back to build repo root
cd $BUILD_REPOSITORY_LOCALPATH

# Zip indexer-queue-gcp folder and upload it as artifact
zip -r indexer-queue-gcp.zip *
cp indexer-queue-gcp.zip $STAGE_DIR

