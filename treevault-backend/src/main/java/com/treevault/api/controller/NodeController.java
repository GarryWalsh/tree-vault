package com.treevault.api.controller;

import com.treevault.application.usecase.CreateNodeUseCase;
import com.treevault.application.usecase.DeleteNodeUseCase;
import com.treevault.application.usecase.GetTreeUseCase;
import com.treevault.application.usecase.MoveNodeUseCase;
import com.treevault.application.usecase.UpdateNodeUseCase;
import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.MoveNodeRequest;
import com.treevault.api.dto.request.UpdateNodeRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.api.mapper.ApiMapper;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Nodes", description = "API for managing hierarchical tree nodes")
public class NodeController {
    
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
    
    @GetMapping("/tree")
    @Operation(summary = "Get the entire tree", description = "Retrieves the root node with all children recursively")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tree retrieved successfully",
            content = @Content(schema = @Schema(implementation = TreeResponse.class)))
    })
    public TreeResponse getTree() {
        var tree = getTreeUseCase.execute();
        return apiMapper.toTreeResponse(tree);
    }
    
    @GetMapping("/nodes/{id}")
    @Operation(summary = "Get a node by ID", description = "Retrieves a specific node by its UUID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Node found",
            content = @Content(schema = @Schema(implementation = NodeResponse.class))),
        @ApiResponse(responseCode = "404", description = "Node not found")
    })
    public NodeResponse getNode(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id) {
        var node = getTreeUseCase.getNode(NodeId.of(id));
        return apiMapper.toNodeResponseWithChildren(node);
    }
    
    @PostMapping("/nodes")
    @Operation(summary = "Create a new node", description = "Creates a new folder or file node")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Node created successfully",
            content = @Content(schema = @Schema(implementation = NodeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Parent node not found"),
        @ApiResponse(responseCode = "409", description = "Node with same name already exists")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse createNode(@Valid @RequestBody CreateNodeRequest request) {
        var command = apiMapper.toCreateCommand(request);
        var node = createNodeUseCase.execute(command);
        return apiMapper.toNodeResponse(node);
    }
    
    @PutMapping("/nodes/{id}")
    @Operation(summary = "Update a node", description = "Updates the name of an existing node")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Node updated successfully",
            content = @Content(schema = @Schema(implementation = NodeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Node not found"),
        @ApiResponse(responseCode = "409", description = "Node with same name already exists")
    })
    public NodeResponse updateNode(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateNodeRequest request) {
        var command = new UpdateNodeUseCase.UpdateNodeCommand(NodeId.of(id), request.getName());
        var node = updateNodeUseCase.execute(command);
        return apiMapper.toNodeResponse(node);
    }
    
    @DeleteMapping("/nodes/{id}")
    @Operation(summary = "Delete a node", description = "Deletes a node and all its children recursively")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Node deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Node not found"),
        @ApiResponse(responseCode = "400", description = "Cannot delete root node with children")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id) {
        deleteNodeUseCase.execute(NodeId.of(id));
    }
    
    @PostMapping("/nodes/{id}/move")
    @Operation(summary = "Move a node", description = "Moves a node to a different parent at a specific position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Node moved successfully",
            content = @Content(schema = @Schema(implementation = NodeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Node or parent not found"),
        @ApiResponse(responseCode = "409", description = "Circular reference detected or name conflict")
    })
    public NodeResponse moveNode(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MoveNodeRequest request) {
        var command = new MoveNodeUseCase.MoveNodeCommand(
            NodeId.of(id),
            NodeId.of(request.getNewParentId()),
            Position.of(request.getPosition())
        );
        var node = moveNodeUseCase.execute(command);
        return apiMapper.toNodeResponse(node);
    }
}

