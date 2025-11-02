package com.treevault.api.controller;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.UpdateNodeRequest;
import com.treevault.api.dto.request.MoveNodeRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.application.usecase.*;
import com.treevault.api.mapper.ApiMapper;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.valueobject.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NodeController.class)
@Import(NodeControllerTest.MockConfig.class)
class NodeControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CreateNodeUseCase createNodeUseCase;
    
    @Autowired
    private UpdateNodeUseCase updateNodeUseCase;
    
    @Autowired
    private DeleteNodeUseCase deleteNodeUseCase;
    
    @Autowired
    private MoveNodeUseCase moveNodeUseCase;
    
    @Autowired
    private GetTreeUseCase getTreeUseCase;
    
    @Autowired
    private ApiMapper apiMapper;
    
    @AfterEach
    void resetMocks() {
        reset(createNodeUseCase, updateNodeUseCase, deleteNodeUseCase, moveNodeUseCase, getTreeUseCase, apiMapper);
    }

    @Test
    @DisplayName("Should create node successfully")
    void shouldCreateNodeSuccessfully() throws Exception {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test Folder");
        request.setType(NodeType.FOLDER);
        
        Node createdNode = Node.createFolder(NodeName.of("Test Folder"), null);
        NodeResponse response = new NodeResponse();
        response.setId(createdNode.getId().toString());
        response.setName("Test Folder");
        response.setType(NodeType.FOLDER);
        
        when(createNodeUseCase.execute(any())).thenReturn(createdNode);
        when(apiMapper.toNodeResponse(any())).thenReturn(response);
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Folder"))
                .andExpect(jsonPath("$.type").value("FOLDER"));
    }
    
    @Test
    @DisplayName("Should get tree successfully")
    void shouldGetTreeSuccessfully() throws Exception {
        // Given
        Node root = Node.createRoot();
        TreeResponse treeResponse = new TreeResponse();
        NodeResponse rootResponse = new NodeResponse();
        rootResponse.setId(root.getId().toString());
        rootResponse.setName("root");
        treeResponse.setRoot(rootResponse);
        
        when(getTreeUseCase.execute()).thenReturn(root);
        when(apiMapper.toTreeResponse(any())).thenReturn(treeResponse);
        
        // When/Then
        mockMvc.perform(get("/api/v1/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root").exists());
    }
    
    @Test
    @DisplayName("Should update node successfully")
    void shouldUpdateNodeSuccessfully() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName("Updated Name");
        
        Node updatedNode = Node.createFolder(NodeName.of("Updated Name"), null);
        NodeResponse response = new NodeResponse();
        response.setId(updatedNode.getId().toString());
        response.setName("Updated Name");
        
        when(updateNodeUseCase.execute(any())).thenReturn(updatedNode);
        when(apiMapper.toNodeResponse(any())).thenReturn(response);
        
        // When/Then
        mockMvc.perform(put("/api/v1/nodes/" + nodeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
    
    @Test
    @DisplayName("Should delete node successfully")
    void shouldDeleteNodeSuccessfully() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        doNothing().when(deleteNodeUseCase).execute(any());
        
        // When/Then
        mockMvc.perform(delete("/api/v1/nodes/" + nodeId))
                .andExpect(status().isNoContent());
        
        verify(deleteNodeUseCase).execute(any());
    }
    
    @Test
    @DisplayName("Should fail when invalid JSON is provided")
    void shouldFailWhenInvalidJsonIsProvided() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when required fields are missing")
    void shouldFailWhenRequiredFieldsAreMissing() throws Exception {
        // Given - Missing name field
        CreateNodeRequest request = new CreateNodeRequest();
        request.setType(NodeType.FOLDER);
        // name is not set
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when node type is null")
    void shouldFailWhenNodeTypeIsNull() throws Exception {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test");
        request.setType(null);
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when update request has empty name")
    void shouldFailWhenUpdateRequestHasEmptyName() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName("");
        
        // When/Then
        mockMvc.perform(put("/api/v1/nodes/" + nodeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when update request has null name")
    void shouldFailWhenUpdateRequestHasNullName() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        UpdateNodeRequest request = new UpdateNodeRequest();
        request.setName(null);
        
        // When/Then
        mockMvc.perform(put("/api/v1/nodes/" + nodeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when move request has invalid position")
    void shouldFailWhenMoveRequestHasInvalidPosition() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        MoveNodeRequest request = new MoveNodeRequest();
        request.setNewParentId(UUID.randomUUID().toString());
        request.setPosition(-1); // Invalid negative position
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes/" + nodeId + "/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when move request has null parent")
    void shouldFailWhenMoveRequestHasNullParent() throws Exception {
        // Given
        UUID nodeId = UUID.randomUUID();
        MoveNodeRequest request = new MoveNodeRequest();
        request.setNewParentId(null);
        request.setPosition(0);
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes/" + nodeId + "/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when node ID is invalid UUID format")
    void shouldFailWhenNodeIdIsInvalidUuidFormat() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/nodes/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when request body is empty")
    void shouldFailWhenRequestBodyIsEmpty() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should fail when request has extremely long name")
    void shouldFailWhenRequestHasExtremelyLongName() throws Exception {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("a".repeat(256)); // Exceeds max length
        request.setType(NodeType.FOLDER);
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        // Verify use case is never called when validation fails
        verify(createNodeUseCase, never()).execute(any());
    }
    
    @Test
    @DisplayName("Should fail when content type is missing")
    void shouldFailWhenContentTypeIsMissing() throws Exception {
        // Given
        CreateNodeRequest request = new CreateNodeRequest();
        request.setName("Test");
        request.setType(NodeType.FOLDER);
        
        // When/Then
        mockMvc.perform(post("/api/v1/nodes")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }
    
    @Test
    @DisplayName("Should fail when request has malformed JSON structure")
    void shouldFailWhenRequestHasMalformedJsonStructure() throws Exception {
        // When/Then - Truly malformed JSON
        mockMvc.perform(post("/api/v1/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class MockConfig {

        @Bean
        CreateNodeUseCase createNodeUseCase() {
            return mock(CreateNodeUseCase.class);
        }

        @Bean
        UpdateNodeUseCase updateNodeUseCase() {
            return mock(UpdateNodeUseCase.class);
        }

        @Bean
        DeleteNodeUseCase deleteNodeUseCase() {
            return mock(DeleteNodeUseCase.class);
        }

        @Bean
        MoveNodeUseCase moveNodeUseCase() {
            return mock(MoveNodeUseCase.class);
        }

        @Bean
        GetTreeUseCase getTreeUseCase() {
            return mock(GetTreeUseCase.class);
        }

        @Bean
        ApiMapper apiMapper() {
            return mock(ApiMapper.class);
        }
    }
}

