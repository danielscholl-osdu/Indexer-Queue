#!/bin/bash

# deploy script

# Exit as soon as a command fails
set -e

SCRIPTS_DIR=$(dirname $0)
DROP_DIR=$(dirname $SCRIPTS_DIR)

# Go to drop directory
cd $DROP_DIR
# convert to full path
DROP_DIR=`pwd`
echo "Current working directory: $DROP_DIR"

DEPLOY_DIR=$DROP_DIR/deploy

if [ -s $DEPLOY_DIR ]; then
  rm -rf $DEPLOY_DIR/*
else
  mkdir $DEPLOY_DIR
fi

echo "Copy artifacts to folder: $DEPLOY_DIR"
unzip $DROP_DIR/indexer-queue-gcp.zip  -d $DEPLOY_DIR

DEPLOY_SCRIPTS_DIR=$DEPLOY_DIR/indexer-queue-gcp/scripts
chmod a+x $DEPLOY_SCRIPTS_DIR/*.sh
source $DEPLOY_SCRIPTS_DIR/config.sh

echo "Deploying to gcp"
$DEPLOY_SCRIPTS_DIR/deploy2gcp.sh
echo "Deployed to gcp"