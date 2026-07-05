package com.example.smartmessaging.util;

public class MaskingUtils {

    /**
     * 전화번호 마스킹 (예: 010-1234-5678 -> 010-****-5678)
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() >= 10) {
            int len = cleaned.length();
            String prefix = cleaned.substring(0, 3);
            String suffix = cleaned.substring(len - 4);
            String middle = "*".repeat(len - 7);
            return prefix + "-" + middle + "-" + suffix;
        } else if (cleaned.length() >= 7) {
            int len = cleaned.length();
            String prefix = cleaned.substring(0, 3);
            String suffix = cleaned.substring(len - 4);
            return prefix + "-***-" + suffix;
        }
        return phone;
    }

    /**
     * 이메일 마스킹 (예: gildong@naver.com -> gil***@naver.com)
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts.length > 1 ? parts[1] : "";

        if (local.length() <= 3) {
            return "*".repeat(local.length()) + "@" + domain;
        }
        String visible = local.substring(0, 3);
        String masked = "*".repeat(local.length() - 3);
        return visible + masked + "@" + domain;
    }
}
