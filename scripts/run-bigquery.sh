#!/usr/bin/env bash
set -euo pipefail

missing=()

if [[ -z "${BIGQUERY_PROJECT_ID:-}" ]]; then
  missing+=("BIGQUERY_PROJECT_ID")
fi

if [[ -z "${BIGQUERY_DATASET:-}" ]]; then
  missing+=("BIGQUERY_DATASET")
fi

if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
  missing+=("GOOGLE_APPLICATION_CREDENTIALS")
elif [[ ! -f "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
  echo "GOOGLE_APPLICATION_CREDENTIALS file does not exist: ${GOOGLE_APPLICATION_CREDENTIALS}" >&2
  exit 1
fi

if (( ${#missing[@]} > 0 )); then
  echo "Missing required BigQuery environment variables: ${missing[*]}" >&2
  echo "Example:" >&2
  echo "  export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/service-account.json" >&2
  echo "  export BIGQUERY_PROJECT_ID=your-gcp-project" >&2
  echo "  export BIGQUERY_DATASET=smart_messaging_dw" >&2
  exit 1
fi

export BIGQUERY_DW_ENABLED=true

exec ./gradlew bootRun
