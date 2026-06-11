package com.ubs.wma.aat.rampuppack.service;

import java.util.Map;

/**
 * Resolves <code>{placeholder}</code> merge fields in template text by plain string replacement —
 * deliberately no template engine. Field names may contain spaces ({@code {FA Name}} works);
 * unknown placeholders are left as-is so they are visible in the stored merged output.
 */
public final class TemplateMerger {

    private TemplateMerger() {
    }

    public static String merge(String template, Map<String, String> fields) {
        if (template == null || template.isEmpty() || fields.isEmpty()) {
            return template;
        }
        String merged = template;
        for (Map.Entry<String, String> field : fields.entrySet()) {
            merged = merged.replace("{" + field.getKey() + "}", field.getValue());
        }
        return merged;
    }
}
