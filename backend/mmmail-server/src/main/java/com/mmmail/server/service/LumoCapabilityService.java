package com.mmmail.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.LumoProjectKnowledge;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LumoCapabilityService {

    private static final String MODE_STANDARD = "STANDARD";
    private static final String MODE_SEARCH = "SEARCH";
    private static final String MODE_SEARCH_TRANSLATE = "SEARCH_TRANSLATE";
    private static final String SOURCE_PUBLIC = "PUBLIC_SOURCE";
    private static final String SOURCE_KNOWLEDGE = "PROJECT_KNOWLEDGE";

    private static final List<PublicSourceDefinition> PUBLIC_SOURCES = List.of(
            new PublicSourceDefinition(
                    "Proton plans explained",
                    "https://proton.me/support/proton-plans",
                    "Consumer plan lineup for Free, Mail Plus, Unlimited, Duo, Family, and Visionary.",
                    Set.of("plan", "pricing", "mail", "duo", "family", "visionary", "unlimited", "free")
            ),
            new PublicSourceDefinition(
                    "Proton business plans",
                    "https://proton.me/business/plans",
                    "Business tiers for Mail, full-suite bundles, enterprise rollout, and security add-ons.",
                    Set.of("business", "enterprise", "mail", "suite", "security", "vpn", "pass", "sentinel")
            ),
            new PublicSourceDefinition(
                    "Lumo getting started",
                    "https://proton.me/support/lumo-getting-started",
                    "Lumo privacy posture and visible capability narrative including web search and Translate.",
                    Set.of("lumo", "search", "translate", "citation", "privacy", "ai", "web")
            )
    );

    private final ObjectMapper objectMapper;

    public LumoCapabilityService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssistantReply composeAssistantReply(
            String prompt,
            String modelCode,
            List<LumoProjectKnowledge> referencedKnowledge,
            boolean webSearchEnabled,
            boolean citationsEnabled,
            String translateToLocale
    ) {
        String responseLocale = normalizeResponseLocale(translateToLocale);
        List<CitationPayload> citations = buildCitations(prompt, referencedKnowledge, webSearchEnabled, citationsEnabled);
        String capabilityMode = resolveCapabilityMode(webSearchEnabled, responseLocale != null);
        String content = buildContent(
                capabilityMode,
                responseLocale,
                prompt,
                modelCode,
                referencedKnowledge,
                citations,
                webSearchEnabled,
                citationsEnabled
        );
        MessageCapabilityPayload payload = new MessageCapabilityPayload(
                capabilityMode,
                responseLocale,
                webSearchEnabled,
                citationsEnabled,
                citations
        );
        return new AssistantReply(content, writePayload(payload));
    }

    public MessageCapabilityView resolveView(String capabilityPayloadJson) {
        if (!StringUtils.hasText(capabilityPayloadJson)) {
            return new MessageCapabilityView(MODE_STANDARD, null, false, false, List.of());
        }
        try {
            MessageCapabilityPayload payload = objectMapper.readValue(capabilityPayloadJson, MessageCapabilityPayload.class);
            List<CitationView> citations = payload.citations() == null
                    ? List.of()
                    : payload.citations().stream()
                    .map(item -> new CitationView(item.title(), item.url(), item.note(), item.sourceType()))
                    .toList();
            return new MessageCapabilityView(
                    payload.capabilityMode(),
                    payload.responseLocale(),
                    payload.webSearchEnabled(),
                    payload.citationsEnabled(),
                    citations
            );
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse Lumo capability payload");
        }
    }

    private String writePayload(MessageCapabilityPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to persist Lumo capability payload");
        }
    }

    private String normalizeResponseLocale(String rawLocale) {
        if (!StringUtils.hasText(rawLocale)) {
            return null;
        }
        return switch (rawLocale.trim()) {
            case "en" -> "en";
            case "zh-CN" -> "zh-CN";
            case "zh-TW" -> "zh-TW";
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported Lumo translate locale");
        };
    }

    private String resolveCapabilityMode(boolean webSearchEnabled, boolean translated) {
        if (webSearchEnabled && translated) {
            return MODE_SEARCH_TRANSLATE;
        }
        if (webSearchEnabled) {
            return MODE_SEARCH;
        }
        return MODE_STANDARD;
    }

    private List<CitationPayload> buildCitations(
            String prompt,
            List<LumoProjectKnowledge> referencedKnowledge,
            boolean webSearchEnabled,
            boolean citationsEnabled
    ) {
        if (!citationsEnabled && !webSearchEnabled) {
            return List.of();
        }

        List<CitationPayload> citations = new ArrayList<>();
        if (webSearchEnabled) {
            citations.addAll(matchPublicSources(prompt));
        }
        if (citationsEnabled) {
            citations.addAll(matchProjectKnowledge(referencedKnowledge, prompt));
        }
        return citations.stream().limit(4).toList();
    }

    private List<CitationPayload> matchPublicSources(String prompt) {
        Set<String> promptKeywords = tokenize(prompt);
        List<CitationPayload> citations = new ArrayList<>();
        for (PublicSourceDefinition source : PUBLIC_SOURCES) {
            long score = source.keywords().stream().filter(promptKeywords::contains).count();
            if (score == 0 && !prompt.toLowerCase(Locale.ROOT).contains(source.title().toLowerCase(Locale.ROOT))) {
                continue;
            }
            citations.add(new CitationPayload(source.title(), source.url(), source.note(), SOURCE_PUBLIC));
        }
        return citations;
    }

    private List<CitationPayload> matchProjectKnowledge(List<LumoProjectKnowledge> referencedKnowledge, String prompt) {
        Set<String> promptKeywords = tokenize(prompt);
        List<CitationPayload> citations = new ArrayList<>();
        for (LumoProjectKnowledge item : referencedKnowledge) {
            String combined = (item.getTitle() + ' ' + item.getContent()).toLowerCase(Locale.ROOT);
            boolean matched = promptKeywords.stream().anyMatch(combined::contains);
            if (!matched && !promptKeywords.isEmpty()) {
                continue;
            }
            citations.add(new CitationPayload(
                    item.getTitle(),
                    "knowledge://" + item.getId(),
                    "Project knowledge selected for this reply.",
                    SOURCE_KNOWLEDGE
            ));
        }
        return citations;
    }

    private Set<String> tokenize(String prompt) {
        Set<String> tokens = new LinkedHashSet<>();
        if (!StringUtils.hasText(prompt)) {
            return tokens;
        }
        for (String token : prompt.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5-]+")) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String buildContent(
            String capabilityMode,
            String responseLocale,
            String prompt,
            String modelCode,
            List<LumoProjectKnowledge> referencedKnowledge,
            List<CitationPayload> citations,
            boolean webSearchEnabled,
            boolean citationsEnabled
    ) {
        String promptSnippet = compactPrompt(prompt);
        String citationTitles = citations.isEmpty()
                ? noCitationLabel(responseLocale)
                : citations.stream().map(CitationPayload::title).distinct().reduce((left, right) -> left + " / " + right).orElse(noCitationLabel(responseLocale));
        String knowledgeTitles = referencedKnowledge.isEmpty()
                ? noKnowledgeLabel(responseLocale)
                : referencedKnowledge.stream().map(LumoProjectKnowledge::getTitle).limit(3).reduce((left, right) -> left + " / " + right).orElse(noKnowledgeLabel(responseLocale));

        if ("zh-CN".equals(responseLocale)) {
            return String.join("\n",
                    "Lumo " + modelCode + " 工作区回复",
                    "能力模式：" + capabilityModeLabel(capabilityMode, responseLocale, webSearchEnabled, citationsEnabled),
                    "请求焦点：" + promptSnippet,
                    "项目知识：" + knowledgeTitles,
                    "公共资料源：" + citationTitles,
                    "说明：当前工作区只扫描受控公共资料源与已选项目知识，不伪装成实时互联网搜索。"
            );
        }
        if ("zh-TW".equals(responseLocale)) {
            return String.join("\n",
                    "Lumo " + modelCode + " 工作區回覆",
                    "能力模式：" + capabilityModeLabel(capabilityMode, responseLocale, webSearchEnabled, citationsEnabled),
                    "請求焦點：" + promptSnippet,
                    "專案知識：" + knowledgeTitles,
                    "公共資料源：" + citationTitles,
                    "說明：目前工作區只掃描受控公共資料源與已選專案知識，不偽裝成即時網際網路搜尋。"
            );
        }
        return String.join("\n",
                "Lumo " + modelCode + " workspace reply",
                "Capability mode: " + capabilityModeLabel(capabilityMode, responseLocale, webSearchEnabled, citationsEnabled),
                "Request focus: " + promptSnippet,
                "Project knowledge: " + knowledgeTitles,
                "Public source scan: " + citationTitles,
                "Note: this workspace scans curated public references and selected project knowledge, not live internet search."
        );
    }

    private String capabilityModeLabel(
            String capabilityMode,
            String responseLocale,
            boolean webSearchEnabled,
            boolean citationsEnabled
    ) {
        if ("zh-CN".equals(responseLocale)) {
            if (MODE_SEARCH_TRANSLATE.equals(capabilityMode)) {
                return "公共资料源扫描 + 翻译输出" + (citationsEnabled ? " + 引用" : "");
            }
            if (MODE_SEARCH.equals(capabilityMode)) {
                return webSearchEnabled ? "公共资料源扫描" + (citationsEnabled ? " + 引用" : "") : "标准回复";
            }
            return citationsEnabled ? "标准回复 + 引用" : "标准回复";
        }
        if ("zh-TW".equals(responseLocale)) {
            if (MODE_SEARCH_TRANSLATE.equals(capabilityMode)) {
                return "公共資料源掃描 + 翻譯輸出" + (citationsEnabled ? " + 引用" : "");
            }
            if (MODE_SEARCH.equals(capabilityMode)) {
                return webSearchEnabled ? "公共資料源掃描" + (citationsEnabled ? " + 引用" : "") : "標準回覆";
            }
            return citationsEnabled ? "標準回覆 + 引用" : "標準回覆";
        }
        if (MODE_SEARCH_TRANSLATE.equals(capabilityMode)) {
            return "curated public-source scan + translated output" + (citationsEnabled ? " + citations" : "");
        }
        if (MODE_SEARCH.equals(capabilityMode)) {
            return webSearchEnabled ? "curated public-source scan" + (citationsEnabled ? " + citations" : "") : "standard reply";
        }
        return citationsEnabled ? "standard reply + citations" : "standard reply";
    }

    private String compactPrompt(String prompt) {
        String normalized = prompt.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180) + "...";
    }

    private String noCitationLabel(String responseLocale) {
        if ("zh-CN".equals(responseLocale)) {
            return "未命中受控资料源";
        }
        if ("zh-TW".equals(responseLocale)) {
            return "未命中受控資料源";
        }
        return "No curated source match";
    }

    private String noKnowledgeLabel(String responseLocale) {
        if ("zh-CN".equals(responseLocale)) {
            return "未选择项目知识";
        }
        if ("zh-TW".equals(responseLocale)) {
            return "未選取專案知識";
        }
        return "No project knowledge selected";
    }

    public record AssistantReply(
            String content,
            String capabilityPayloadJson
    ) {
    }

    public record CitationView(
            String title,
            String url,
            String note,
            String sourceType
    ) {
    }

    public record MessageCapabilityView(
            String capabilityMode,
            String responseLocale,
            boolean webSearchEnabled,
            boolean citationsEnabled,
            List<CitationView> citations
    ) {
    }

    private record MessageCapabilityPayload(
            String capabilityMode,
            String responseLocale,
            boolean webSearchEnabled,
            boolean citationsEnabled,
            List<CitationPayload> citations
    ) {
    }

    private record CitationPayload(
            String title,
            String url,
            String note,
            String sourceType
    ) {
    }

    private record PublicSourceDefinition(
            String title,
            String url,
            String note,
            Set<String> keywords
    ) {
    }
}
