package com.connectsphere.search.util;

import java.util.regex.Pattern;

/**
 * Centralized constants for the Search Service.
 * <p>
 * Contains the regex pattern used to extract hashtags from raw post content.
 * </p>
 *
 * <h3>Constants Overview</h3>
 * <pre class="mermaid">
 * classDiagram
 *     class SearchConstants {
 *         +Pattern HASHTAG_PATTERN
 *     }
 * </pre>
 */
public final class SearchConstants {
    private SearchConstants() {}

    public static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");
}
