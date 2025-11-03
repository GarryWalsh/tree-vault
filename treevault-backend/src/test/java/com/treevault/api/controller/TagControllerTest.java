package com.treevault.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treevault.api.dto.request.TagRequest;
import com.treevault.api.dto.response.TagResponse;
import com.treevault.api.mapper.ApiMapper;
import com.treevault.application.usecase.AddTagUseCase;
import com.treevault.application.usecase.RemoveTagUseCase;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.NodeName;
import com.treevault.domain.model.valueobject.NodeType;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import(TagControllerTest.MockConfig.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AddTagUseCase addTagUseCase;

    @Autowired
    private RemoveTagUseCase removeTagUseCase;

    @Autowired
    private ApiMapper apiMapper;

    @Test
    @DisplayName("Should add tag successfully")
    void shouldAddTagSuccessfully() throws Exception {
        UUID nodeId = UUID.randomUUID();
        TagRequest request = new TagRequest();
        request.setKey("priority");
        request.setValue("high");

        Node node = Node.reconstruct(
            NodeId.of(nodeId),
            NodeName.of("Folder"),
            NodeType.FOLDER,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            1L,
            null,
            null
        );
        node.addTag(TagKey.of("priority"), TagValue.of("high"));
        Tag tag = node.getTags().get(TagKey.of("priority"));

        TagResponse response = new TagResponse();
        response.setKey("priority");
        response.setValue("high");

        when(addTagUseCase.execute(any(AddTagUseCase.AddTagCommand.class))).thenReturn(tag);
        when(apiMapper.toTagResponse(tag)).thenReturn(response);

        mockMvc.perform(post("/api/v1/nodes/{id}/tags", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("priority"))
                .andExpect(jsonPath("$.value").value("high"));

        verify(addTagUseCase).execute(any(AddTagUseCase.AddTagCommand.class));
        verify(apiMapper).toTagResponse(tag);
    }

    @Test
    @DisplayName("Should remove tag successfully")
    void shouldRemoveTagSuccessfully() throws Exception {
        UUID nodeId = UUID.randomUUID();
        String tagKey = "priority";

        doNothing().when(removeTagUseCase).execute(any(NodeId.class), any(TagKey.class));

        mockMvc.perform(delete("/api/v1/nodes/{id}/tags/{key}", nodeId, tagKey))
                .andExpect(status().isNoContent());

        verify(removeTagUseCase).execute(eq(NodeId.of(nodeId)), eq(TagKey.of(tagKey)));
    }

    @TestConfiguration
    static class MockConfig {

        @Bean
        AddTagUseCase addTagUseCase() {
            return mock(AddTagUseCase.class);
        }

        @Bean
        RemoveTagUseCase removeTagUseCase() {
            return mock(RemoveTagUseCase.class);
        }

        @Bean
        ApiMapper apiMapper() {
            return mock(ApiMapper.class);
        }
    }
}

