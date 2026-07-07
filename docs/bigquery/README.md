# BigQuery DW Setup

This app can read statistics and AI insight context from BigQuery when `dw.bigquery.enabled=true`.
BigQuery is not the primary transactional database. Oracle is still required for login, message history, batch metadata, and operational writes.

## Required Local/GCP Setup

Install Google Cloud CLI or provide Application Default Credentials.

```bash
gcloud auth application-default login
gcloud config set project <project-id>
```

Or use a service account key:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/service-account.json
```

The runtime identity needs:

- BigQuery Job User on the project
- BigQuery Data Viewer on the dataset

## Create Dataset And Tables

```bash
export BIGQUERY_PROJECT_ID=<project-id>
export BIGQUERY_DATASET=<dataset>
./scripts/setup-bigquery.sh
```

If you run the SQL in the BigQuery console, replace `smart_messaging_dw` with your dataset name.

## Run The App With BigQuery Enabled

```bash
export BIGQUERY_DW_ENABLED=true
export BIGQUERY_PROJECT_ID=<project-id>
export BIGQUERY_DATASET=<dataset>
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/service-account.json

./scripts/run-bigquery.sh
```

Optional table-name overrides:

```bash
export BIGQUERY_TABLE_MESSAGE_STAT=message_stat
export BIGQUERY_TABLE_AI_MARKETING_INSIGHT=ai_marketing_insight
```

## Data Responsibilities

Statistics and dashboard read these tables:

- `channel`
- `message_stat`
- `message_stat_by_degree`
- `channel_stat`
- `customer_stat`
- `customer_channel_consent`
- `click_stat`
- `template`
- `template_stat`

AI recommendation and AI review prompt context reads:

- `ai_marketing_insight`

If BigQuery is disabled, not configured, or a query fails, the app falls back to the existing Oracle/MyBatis statistics source.
