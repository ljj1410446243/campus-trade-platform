package com.campus.trade.file.util;

import com.campus.trade.file.exception.BusinessException;

public final class FileBizType {

    public static final String ITEM_IMAGE = "ITEM_IMAGE";
    public static final String AVATAR = "AVATAR";
    public static final String CHAT_IMAGE = "CHAT_IMAGE";

    private static final long FIVE_MB = 5L * 1024 * 1024;
    private static final long TWO_MB = 2L * 1024 * 1024;

    private FileBizType() {
    }

    public static String normalize(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException("bizType不能为空");
        }

        String normalized = bizType.trim().toUpperCase();
        if (!ITEM_IMAGE.equals(normalized) && !AVATAR.equals(normalized) && !CHAT_IMAGE.equals(normalized)) {
            throw new BusinessException("bizType不支持");
        }
        return normalized;
    }

    public static long maxFileSize(String bizType) {
        if (AVATAR.equals(bizType)) {
            return TWO_MB;
        }
        return FIVE_MB;
    }

    public static int maxFileCount(String bizType) {
        if (AVATAR.equals(bizType)) {
            return 1;
        }
        return 9;
    }
}
