import { useCallback } from 'react';
import { nodeApi } from '../api/nodeApi';
import { NodeResponse } from '../api/types';
import { useTreeStore } from '../store/treeStore';

export interface NodeOperationsHandlers {
  loadTree: () => Promise<void>;
  handleCreateNode: (name: string, type: 'FOLDER' | 'FILE', parentId: string) => Promise<void>;
  handleRenameNode: (nodeId: string, newName: string) => Promise<void>;
  handleDeleteNode: (nodeId: string) => Promise<void>;
  handleAddTag: (nodeId: string, key: string, value: string) => Promise<void>;
  handleRemoveTag: (nodeId: string, key: string) => Promise<void>;
  handleMoveNode: (nodeId: string, newParentId: string, position: number) => Promise<void>;
  findNodeById: (nodeId: string) => NodeResponse | null;
  getSelectedNode: () => NodeResponse | null;
}

interface UseNodeOperationsOptions {
  onSuccess?: (message: string) => void;
  onError?: (message: string) => void;
}

export const useNodeOperations = (options?: UseNodeOperationsOptions): NodeOperationsHandlers => {
  const { 
    tree,
    selectedNodeId, 
    selectNode, 
    setLoading, 
    setError,
    setTree,
    updateNodeInTree,
    addNodeToTree,
    removeNodeFromTree
  } = useTreeStore();

  const { onSuccess, onError } = options || {};

  const loadTree = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await nodeApi.getTree();
      setTree(response.data);
    } catch (err: any) {
      const errorMessage = err.response?.data?.detail || 'Failed to load tree';
      setError(errorMessage);
      onError?.(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError, setTree, onError]);

  const handleCreateNode = useCallback(async (name: string, type: 'FOLDER' | 'FILE', parentId: string) => {
    try {
      console.log('Creating node:', { name, type, parentId });
      const response = await nodeApi.createNode({
        name,
        type,
        parentId,
      });
      // Add the new node to local state
      addNodeToTree(parentId, response.data);
      onSuccess?.(`${type === 'FOLDER' ? 'Folder' : 'File'} "${name}" created successfully`);
    } catch (err: any) {
      console.error('Create node error:', err);
      const errorDetail = err.response?.data?.detail || 'Failed to create node';
      onError?.(errorDetail);
      throw err;
    }
  }, [addNodeToTree, onSuccess, onError]);

  const handleRenameNode = useCallback(async (nodeId: string, newName: string) => {
    try {
      const response = await nodeApi.updateNode(nodeId, newName);
      // Update the node in local state
      updateNodeInTree(nodeId, (node) => ({ ...node, ...response.data }));
      onSuccess?.(`Renamed to "${newName}" successfully`);
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to rename node';
      onError?.(errorDetail);
      throw err;
    }
  }, [updateNodeInTree, onSuccess, onError]);

  const handleDeleteNode = useCallback(async (nodeId: string) => {
    try {
      await nodeApi.deleteNode(nodeId);
      // Remove the node from local state
      removeNodeFromTree(nodeId);
      if (selectedNodeId === nodeId) {
        selectNode(null);
      }
      onSuccess?.('Node deleted successfully');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to delete node';
      onError?.(errorDetail);
      throw err;
    }
  }, [removeNodeFromTree, selectedNodeId, selectNode, onSuccess, onError]);

  const handleAddTag = useCallback(async (nodeId: string, key: string, value: string) => {
    try {
      await nodeApi.addTag(nodeId, key, value);
      // Update the node's tags in local state
      updateNodeInTree(nodeId, (node) => ({
        ...node,
        tags: { ...(node.tags || {}), [key]: value }
      }));
      onSuccess?.('Tag added successfully');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to add tag';
      onError?.(errorDetail);
      throw err;
    }
  }, [updateNodeInTree, onSuccess, onError]);

  const handleRemoveTag = useCallback(async (nodeId: string, key: string) => {
    try {
      await nodeApi.removeTag(nodeId, key);
      // Remove the tag from local state
      updateNodeInTree(nodeId, (node) => {
        const { [key]: removed, ...remainingTags } = node.tags || {};
        return { ...node, tags: remainingTags };
      });
      onSuccess?.('Tag removed successfully');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to remove tag';
      onError?.(errorDetail);
      throw err;
    }
  }, [updateNodeInTree, onSuccess, onError]);

  const handleMoveNode = useCallback(async (nodeId: string, newParentId: string, position: number) => {
    try {
      console.log('Moving node:', { nodeId, newParentId, position });
      await nodeApi.moveNode(nodeId, newParentId, position);
      // For move operations, we need to refetch to get the updated tree structure
      // This is because moving involves removing from one location and adding to another
      // which is complex to do correctly with local state
      const treeResponse = await nodeApi.getTree();
      setTree(treeResponse.data);
      onSuccess?.('Node moved successfully');
    } catch (err: any) {
      console.error('Move node error:', err);
      const errorDetail = err.response?.data?.detail || 'Failed to move node';
      onError?.(errorDetail);
      throw err;
    }
  }, [setTree, onSuccess, onError]);

  const findNodeByIdRecursive = useCallback((node: NodeResponse, id: string): NodeResponse | null => {
    if (node.id === id) return node;
    if (node.children) {
      for (const child of node.children) {
        const found = findNodeByIdRecursive(child, id);
        if (found) return found;
      }
    }
    return null;
  }, []);

  const findNodeById = useCallback((nodeId: string): NodeResponse | null => {
    if (!tree) return null;
    return findNodeByIdRecursive(tree.root, nodeId);
  }, [tree, findNodeByIdRecursive]);

  const getSelectedNode = useCallback((): NodeResponse | null => {
    if (!tree || !selectedNodeId) return null;
    return findNodeByIdRecursive(tree.root, selectedNodeId);
  }, [tree, selectedNodeId, findNodeByIdRecursive]);

  return {
    loadTree,
    handleCreateNode,
    handleRenameNode,
    handleDeleteNode,
    handleAddTag,
    handleRemoveTag,
    handleMoveNode,
    findNodeById,
    getSelectedNode,
  };
};


