package com.campus.trade.file.util;

import java.util.Set;

public final class FileConstants {

    public static final String STORAGE_LOCAL = "LOCAL";

    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_BOUND = "BOUND";
    public static final String STATUS_DELETED = "DELETED";

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private FileConstants() {
    }
}
