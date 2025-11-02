package com.treevault.api.mapper;

import com.treevault.api.dto.request.CreateNodeRequest;
import com.treevault.api.dto.request.TagRequest;
import com.treevault.api.dto.response.NodeResponse;
import com.treevault.api.dto.response.TagResponse;
import com.treevault.api.dto.response.TreeResponse;
import com.treevault.application.usecase.CreateNodeUseCase;
import com.treevault.application.usecase.MoveNodeUseCase;
import com.treevault.domain.model.entity.Node;
import com.treevault.domain.model.entity.Tag;
import com.treevault.domain.model.valueobject.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ApiMapper {
    
    public TreeResponse toTreeResponse(Node root) {
        return new TreeResponse(toNodeResponse(root, true));
    }
    
    public NodeResponse toNodeResponse(Node node) {
        return toNodeResponse(node, false);
    }
    
    public NodeResponse toNodeResponseWithChildren(Node node) {
        return toNodeResponse(node, true);
    }
    
    private NodeResponse toNodeResponse(Node node, boolean includeChildren) {
        NodeResponse response = new NodeResponse();
        response.setId(node.getId().toString());
        response.setName(node.getName().getValue());
        response.setType(node.getType());
        response.setParentId(node.getParent().map(p -> p.getId().toString()).orElse(null));
        response.setPath(node.getPath().toString());
        response.setPosition(node.getPosition() != null ? node.getPosition().getValue() : null);
        response.setVersion(node.getVersion());
        response.setCreatedAt(node.getCreatedAt());
        response.setUpdatedAt(node.getUpdatedAt());
        
        // Convert tags
        Map<String, String> tags = node.getTags().entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().getValue(),
                e -> e.getValue().getValue().toString()
            ));
        response.setTags(tags);
        
        // Convert children recursively if requested
        if (includeChildren) {
            List<NodeResponse> children = node.getChildren().stream()
                .map(child -> toNodeResponse(child, true))
                .collect(Collectors.toList());
            response.setChildren(children);
        }
        
        return response;
    }
    
    public CreateNodeUseCase.CreateNodeCommand toCreateCommand(CreateNodeRequest request) {
        NodeId parentId = request.getParentId() != null && !request.getParentId().isEmpty()
            ? NodeId.of(request.getParentId())
            : null;
        
        return new CreateNodeUseCase.CreateNodeCommand(
            request.getName(),
            request.getType(),
            parentId,
            request.getTags()
        );
    }
    
    public TagResponse toTagResponse(Tag tag) {
        return new TagResponse(
            tag.getKey().getValue(),
            tag.getValue().getValue()
        );
    }
}

