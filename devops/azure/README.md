# Pipeline Support Commands

```bash
AZURE_SERVICE="indexer-queue"
REPO_BRANCH="master"
TAG="latest"
PARTIAL=${REPO_BRANCH/\//-}
BRANCH=${PARTIAL/./-}

echo "--set image.branch=$BRANCH --set image.tag=$TAG"

# Install the Service
helm upgrade -i osdu-gitlab-$AZURE_SERVICE chart --set image.branch=$BRANCH --set image.tag=$TAG
pod=$(kubectl get pod |grep $AZURE_SERVICE | tail -1 | awk '{print $1}')
status=$(kubectl wait --for=condition=Ready pod/$pod --timeout=60s)
if [[ "$status" != *"met"* ]]; then echo "POD didn't start correctly" ; exit 1 ; fi
```
