package com.healthtracker.common;

import java.util.List;

public record PageResponse<T>(List<T> items, int page, int size, long total) {

    /** Slices an already sorted, in-memory list. Pages are zero-based. */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 500));
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(List.copyOf(all.subList(from, to)), safePage, safeSize, all.size());
    }
}
