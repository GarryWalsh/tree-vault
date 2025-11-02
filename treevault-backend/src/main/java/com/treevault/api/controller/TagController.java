package com.treevault.api.controller;

import com.treevault.api.dto.request.TagRequest;
import com.treevault.api.dto.response.TagResponse;
import com.treevault.api.mapper.ApiMapper;
import com.treevault.application.usecase.AddTagUseCase;
import com.treevault.application.usecase.RemoveTagUseCase;
import com.treevault.domain.model.valueobject.NodeId;
import com.treevault.domain.model.valueobject.TagKey;
import com.treevault.domain.model.valueobject.TagValue;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tags", description = "API for managing node tags")
public class TagController {

    @Autowired
    private AddTagUseCase addTagUseCase;

    @Autowired
    private RemoveTagUseCase removeTagUseCase;

    @Autowired
    private ApiMapper apiMapper;

    @PostMapping("/nodes/{id}/tags")
    @Operation(summary = "Add a tag to a node", description = "Adds a key-value tag to a node")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tag added successfully",
            content = @Content(schema = @Schema(implementation = TagResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Node not found"),
        @ApiResponse(responseCode = "422", description = "Validation error or tag limit exceeded")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse addTag(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody TagRequest request) {
        var command = new AddTagUseCase.AddTagCommand(
            NodeId.of(id),
            TagKey.of(request.getKey()),
            TagValue.of(request.getValue())
        );
        var tag = addTagUseCase.execute(command);
        return apiMapper.toTagResponse(tag);
    }

    @DeleteMapping("/nodes/{id}/tags/{key}")
    @Operation(summary = "Remove a tag from a node", description = "Removes a tag by key from a node")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tag removed successfully"),
        @ApiResponse(responseCode = "404", description = "Node or tag not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTag(
            @Parameter(description = "Node UUID", required = true) @PathVariable UUID id,
            @Parameter(description = "Tag key", required = true) @PathVariable String key) {
        removeTagUseCase.execute(NodeId.of(id), TagKey.of(key));
    }
}

