# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
import boto3
import json
import os
import argparse

# Create the build-info.json
parser = argparse.ArgumentParser(description="")
parser.add_argument("--branch", type=str, help="")
parser.add_argument("--commitId", type=str,  help="")
parser.add_argument("--commitMessage", type=str,  help="")
parser.add_argument("--commitAuthor", type=str,  help="")
parser.add_argument("--commitDate", type=str,  help="")

# env - CODEBUILD_BUILD_ID
parser.add_argument("--buildid", type=str,  help="")

# env - CODEBUILD_BUILD_NUMBER
parser.add_argument("--buildnumber", type=str,  help="")

parser.add_argument("--reponame", type=str,  help="")

# env OUTPUT_DIR
parser.add_argument("--outdir", type=str,  help="")

# full ecr image and tag, and any other artifacts
parser.add_argument("--artifact", type=str, action="append", help="")



args = parser.parse_args()

branch = args.branch
commitId = args.commitId
commitMessage = args.commitMessage
commitAuthor = args.commitAuthor
commitDate = args.commitDate
buildId = args.buildid
buildNumber = args.buildnumber
repoName = args.reponame
outputDir = args.outdir
artifacts = args.artifact

buildInfoFilePath = os.path.join(".", outputDir, "build-info.json")

print(buildInfoFilePath)

buildInfo = {
    "branch": branch,
    "build-id": buildId,
    "build-number": buildNumber,
    "repo": repoName,
    "artifacts": artifacts,
    "commit-id": commitId,
    "commit-message": commitMessage,
    "commit-author": commitAuthor,
    "commit-date":commitDate
}


print(json.dumps(buildInfo, sort_keys=True, indent=4))
# write the build.json file to dist
f = open(buildInfoFilePath, "w")
f.write(json.dumps(buildInfo, sort_keys=True, indent=4))
f.close()
