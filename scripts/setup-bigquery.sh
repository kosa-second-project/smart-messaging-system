#!/usr/bin/env bash
set -euo pipefail

project_id="${BIGQUERY_PROJECT_ID:-}"
dataset="${BIGQUERY_DATASET:-smart_messaging_dw}"
location="${BIGQUERY_LOCATION:-asia-northeast3}"

if [[ -z "${project_id}" ]]; then
  echo "BIGQUERY_PROJECT_ID is required." >&2
  exit 1
fi

if ! command -v bq >/dev/null 2>&1; then
  echo "bq CLI is required. Install Google Cloud CLI first." >&2
  exit 1
fi

schema_file="docs/bigquery/schema.sql"
tmp_schema="$(mktemp)"
trap 'rm -f "${tmp_schema}"' EXIT

sed "s/\`smart_messaging_dw\`/\`${project_id}.${dataset}\`/g" "${schema_file}" > "${tmp_schema}"

bq mk --dataset --location="${location}" "${project_id}:${dataset}" 2>/dev/null || true
bq query --use_legacy_sql=false < "${tmp_schema}"

echo "BigQuery dataset and tables are ready: ${project_id}.${dataset}"
