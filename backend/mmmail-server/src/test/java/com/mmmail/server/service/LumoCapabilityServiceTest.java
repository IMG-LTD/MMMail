package com.mmmail.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.server.model.entity.LumoProjectKnowledge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LumoCapabilityServiceTest {

    private final LumoCapabilityService lumoCapabilityService = new LumoCapabilityService(new ObjectMapper());

    @Test
    void composeAssistantReplyPersistsCapabilityPayloadAndCitations() {
        LumoProjectKnowledge knowledge = newKnowledge(9L, "Business suite rollout", "Need translated parity summary");

        LumoCapabilityService.AssistantReply reply = lumoCapabilityService.composeAssistantReply(
                "Compare Proton business suite pricing and Lumo translate privacy",
                "LUMO-PLUS",
                List.of(knowledge),
                true,
                true,
                "zh-CN"
        );
        LumoCapabilityService.MessageCapabilityView view = lumoCapabilityService.resolveView(reply.capabilityPayloadJson());

        assertThat(reply.content()).contains("公共资料源");
        assertThat(view.capabilityMode()).isEqualTo("SEARCH_TRANSLATE");
        assertThat(view.responseLocale()).isEqualTo("zh-CN");
        assertThat(view.webSearchEnabled()).isTrue();
        assertThat(view.citationsEnabled()).isTrue();
        assertThat(view.citations()).extracting(LumoCapabilityService.CitationView::sourceType)
                .contains("PUBLIC_SOURCE", "PROJECT_KNOWLEDGE");
        assertThat(view.citations()).extracting(LumoCapabilityService.CitationView::url)
                .contains("knowledge://9");
    }

    @Test
    void composeAssistantReplyFallsBackToStandardModeWithoutCapabilities() {
        LumoCapabilityService.AssistantReply reply = lumoCapabilityService.composeAssistantReply(
                "Summarize the current draft",
                "LUMO-BASE",
                List.of(),
                false,
                false,
                null
        );
        LumoCapabilityService.MessageCapabilityView view = lumoCapabilityService.resolveView(reply.capabilityPayloadJson());

        assertThat(view.capabilityMode()).isEqualTo("STANDARD");
        assertThat(view.responseLocale()).isNull();
        assertThat(view.citations()).isEmpty();
        assertThat(reply.content()).contains("not live internet search");
    }

    @Test
    void rejectsUnsupportedTranslateLocale() {
        assertThatThrownBy(() -> lumoCapabilityService.composeAssistantReply(
                "Translate this",
                "LUMO-BASE",
                List.of(),
                true,
                true,
                "ja"
        )).isInstanceOf(BizException.class)
                .hasMessage("Unsupported Lumo translate locale");
    }

    private LumoProjectKnowledge newKnowledge(Long id, String title, String content) {
        LumoProjectKnowledge knowledge = new LumoProjectKnowledge();
        knowledge.setId(id);
        knowledge.setTitle(title);
        knowledge.setContent(content);
        return knowledge;
    }
}
