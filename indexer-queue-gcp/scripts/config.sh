#!/bin/bash

if [ "$PROJECT_ID" = "" ]
then
    export PROJECT_ID=$(gcloud config get-value project)
fi
