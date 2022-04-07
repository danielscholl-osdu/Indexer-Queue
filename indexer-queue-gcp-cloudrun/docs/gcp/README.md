# Service Configuration for GCP

## Table of Contents <a name="TOC"></a>
* [Environment variables](#Environment-variables)
* [Common properties for all environments](#Common-properties-for-all-environments)
* [For Mappers to activate drivers](#For-Mappers-to-activate-drivers)
* [For Google Cloud only](#For-Google-Cloud-only)
* [Pubsub configuration](#Pubsub-configuration)
* [Google cloud service account configuration](#Google-cloud-service-account-configuration)

## Environment variables

### Must have

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `GOOGLE_CLOUD_PROJECT_REGION` | ex `us-central1` | Service deployment region | no | output of infrastructure deployment |
| `GOOGLE_CLOUD_PROJECT` | ex `opendes` | Google Cloud Project Id| no | output of infrastructure deployment |
| `SPRING_PROFILES_ACTIVE` | ex `gcp` | Spring profile that activate default configuration for GCP environment | false | - |

### Defined in default application property file but possible to override

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `OSDU_ENTITLEMENTS_URL` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `CLOUD_TASK_TARGET_HOST` | ex `http://indexer` | Indexer Service _public_ host, relative url will be used from incoming task, if no url present in task then will be used url from property `DEFAULT_RELATIVE_INDEXER_WORKER_URL` | no | output of infrastructure deployment |
| `INDEXER_HOST` | ex `http://indexer` | Should be provided when `INDEX_TASK_TYPE == rest` only. Indexer Service host, base url to send REST request to Indexer, combined with relative indexer worker url | no | output of infrastructure deployment |
| `DEFAULT_RELATIVE_INDEXER_WORKER_URL` | default `/api/indexer/v2/_dps/task-handlers/index-worker` | Indexer Service has two endpoints to process indexing tasks, `/index-worker` and `/reindex-worker` first serves to process requests with specific `storage-record-ids` and add those records to elasticsearch, second for reprocessing tasks it consumes `kind` and `cursor` to request more records from storage| no | output of infrastructure deployment |
| `PARTITION_API` | ex `http://localhost:8081/api/partition/v1` | Partition service endpoint | no | - |
| `INDEXER_QUEUE_IDENTIFIER` | ex `os-indexer-queue-osdu` | Config for cloud tasks queue, will be used combination of `data-partition-id` and `INDEXER_QUEUE_IDENTIFIER` | no | - |
| `RECORDS_CHANGE_TOPIC_NAME` | ex `records-changed` | Name of queue for receiving messages from Storage service about records changes | no | - |
| `DEFAULT_QUEUE_NAME` | ex `records` | Name of queue for sending messages with records info  to Indexer service | no | - |
| `OQMDRIVER` | `pubsub` OR `rabbitmq` | Oqm driver mode that defines which queue will be used | no | - |
| `INDEX_TASK_TYPE` | `cloud-task` OR `queue` OR `rest` | The property enables the support of Google Cloud Tasks / REST requests / Queue messaging. | no | - |

### For Google Cloud only

These variables define service behavior, and are used to switch between `anthos` or `gcp` environments, their overriding
and usage in mixed mode was not tested. Usage of spring profiles is preferred.

| name                         | value                                 | description                                                        | sensitive? | source                                            |
|------------------------------|---------------------------------------|--------------------------------------------------------------------|------------|---------------------------------------------------|
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |

## Pubsub configuration

At Pubsub should be created topic with name:

1. **name:** `records_changed`

It can be overridden by:

- through the Spring Boot property `records_changed_topic_name`
- environment variable `RECORDS_CHANGED_TOPIC_NAME`

2.  **name:** `records`

It can be overridden by:

- through the Spring Boot property `records_topic_name`
- environment variable `RECORDS_TOPIC_NAME`

## Google cloud service account configuration
TBD

| Required roles |
| ---    |
| Pub/Sub EditorModify topics and subscriptions, publish and consume messages. |