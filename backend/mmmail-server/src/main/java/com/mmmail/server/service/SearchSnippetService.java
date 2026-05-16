package com.mmmail.server.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

@Service
public class SearchSnippetService {
    private static final int HALF_WINDOW = 60;
    private static final int MAX_SNIPPET_LENGTH = 160;

    public String create(String title, String body, String keyword) {
        String source = chooseSource(title, body, keyword);
        if (source.isBlank()) {
            return "";
        }
        int match = source.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
        int start = match < 0 ? 0 : Math.max(0, match - HALF_WINDOW);
        int end = Math.min(source.length(), start + MAX_SNIPPET_LENGTH);
        return highlight(source.substring(start, end), keyword);
    }

    private String chooseSource(String title, String body, String keyword) {
        String safeTitle = title == null ? "" : title;
        String safeBody = body == null ? "" : body;
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        if (safeTitle.toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
            return safeTitle;
        }
        return safeBody.isBlank() ? safeTitle : safeBody;
    }

    private String highlight(String text, String keyword) {
        String escaped = HtmlUtils.htmlEscape(text);
        if (keyword.isBlank()) {
            return escaped;
        }
        return escaped.replaceAll("(?i)(" + java.util.regex.Pattern.quote(keyword) + ")", "<em>$1</em>");
    }
}
