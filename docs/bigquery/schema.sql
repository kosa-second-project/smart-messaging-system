CREATE SCHEMA IF NOT EXISTS `smart_messaging_dw`;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.channel` (
  id INT64 NOT NULL,
  channel_type STRING,
  cost_per_msg NUMERIC,
  max_length INT64,
  is_active INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.message_stat` (
  id INT64,
  total_send_count INT64,
  total_success_count INT64,
  billing_cost NUMERIC,
  max_cost NUMERIC,
  stat_date DATE,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.message_stat_by_degree` (
  id INT64,
  degree INT64,
  send_count INT64,
  success_count INT64,
  stat_date DATE,
  cost NUMERIC,
  channel_id INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY channel_id, degree;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.channel_stat` (
  id INT64,
  click_target_count INT64,
  click_count INT64,
  conversion_target_count INT64,
  conversion_count INT64,
  consent_target_count INT64,
  consent_count INT64,
  channel_id INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY channel_id;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.customer_stat` (
  id INT64,
  stat_date DATE,
  total_customer_count INT64,
  normal_customer_count INT64,
  new_customer_count INT64,
  dormant_customer_count INT64,
  sms_consent_count INT64,
  kakao_consent_count INT64,
  email_consent_count INT64,
  joined_customer_count INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.customer_channel_consent` (
  id INT64,
  customer_id INT64,
  channel_id INT64,
  is_consented INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY channel_id;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.click_stat` (
  id INT64,
  stat_date DATE,
  channel_id INT64,
  hour_00 INT64,
  hour_01 INT64,
  hour_02 INT64,
  hour_03 INT64,
  hour_04 INT64,
  hour_05 INT64,
  hour_06 INT64,
  hour_07 INT64,
  hour_08 INT64,
  hour_09 INT64,
  hour_10 INT64,
  hour_11 INT64,
  hour_12 INT64,
  hour_13 INT64,
  hour_14 INT64,
  hour_15 INT64,
  hour_16 INT64,
  hour_17 INT64,
  hour_18 INT64,
  hour_19 INT64,
  hour_20 INT64,
  hour_21 INT64,
  hour_22 INT64,
  hour_23 INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY channel_id;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.template` (
  id INT64 NOT NULL,
  title STRING,
  is_ai_generated INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.template_stat` (
  id INT64,
  template_id INT64,
  stat_date DATE,
  click_target_count INT64,
  click_count INT64,
  conversion_target_count INT64,
  conversion_count INT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY template_id;

CREATE TABLE IF NOT EXISTS `smart_messaging_dw.ai_marketing_insight` (
  insight_id STRING,
  doc_id STRING,
  insight_type STRING,
  title STRING,
  content STRING,
  channel_type STRING,
  message_type STRING,
  category STRING,
  metric_name STRING,
  metric_value FLOAT64,
  is_deleted INT64,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
CLUSTER BY insight_type, message_type, category;
