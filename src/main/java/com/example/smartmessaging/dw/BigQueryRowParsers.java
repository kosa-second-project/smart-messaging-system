package com.example.smartmessaging.dw;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BigQueryRowParsers {

    private BigQueryRowParsers() {
    }

    public static String string(FieldValueList row, String fieldName) {
        FieldValue value = value(row, fieldName);
        return value == null ? null : value.getStringValue();
    }

    public static Long longValue(FieldValueList row, String fieldName) {
        FieldValue value = value(row, fieldName);
        return value == null ? null : value.getLongValue();
    }

    public static Integer intValue(FieldValueList row, String fieldName) {
        Long value = longValue(row, fieldName);
        return value == null ? null : Math.toIntExact(value);
    }

    public static Double doubleValue(FieldValueList row, String fieldName) {
        FieldValue value = value(row, fieldName);
        return value == null ? null : value.getDoubleValue();
    }

    public static BigDecimal decimal(FieldValueList row, String fieldName) {
        FieldValue value = value(row, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return value.getNumericValue();
        } catch (UnsupportedOperationException exception) {
            return new BigDecimal(value.getStringValue());
        }
    }

    public static Boolean bool(FieldValueList row, String fieldName) {
        FieldValue value = value(row, fieldName);
        if (value == null) {
            return null;
        }
        try {
            return value.getBooleanValue();
        } catch (UnsupportedOperationException exception) {
            String raw = value.getStringValue();
            return "1".equals(raw) || "true".equalsIgnoreCase(raw);
        }
    }

    public static LocalDate date(FieldValueList row, String fieldName) {
        String value = string(row, fieldName);
        return value == null ? null : LocalDate.parse(value);
    }

    private static FieldValue value(FieldValueList row, String fieldName) {
        FieldValue value = row.get(fieldName);
        return value == null || value.isNull() ? null : value;
    }
}
