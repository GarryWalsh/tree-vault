import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useNodeOperations } from '../../src/hooks/useNodeOperations';
import { nodeApi } from '../../src/api/nodeApi';
import { useTreeStore } from '../../src/store/treeStore';
import type { AxiosResponse } from 'axios';
import type { NodeResponse, TreeResponse } from '../../src/api/types';

// Helper to create mock AxiosResponse
const createMockAxiosResponse = <T>(data: T): AxiosResponse<T> => ({
  data,
  status: 200,
  statusText: 'OK',
  headers: {},
  config: {} as any,
});

// Mock the API
vi.mock('../../src/api/nodeApi', () => ({
  nodeApi: {
    getTree: vi.fn(),
    createNode: vi.fn(),
    updateNode: vi.fn(),
    deleteNode: vi.fn(),
    moveNode: vi.fn(),
    addTag: vi.fn(),
    removeTag: vi.fn(),
  },
}));

describe('useNodeOperations', () => {
  const mockNodeResponse: NodeResponse = {
    id: 'test-node',
    name: 'Test Node',
    type: 'FOLDER',
    path: '/Test',
    createdAt: '2025-01-01T00:00:00',
    updatedAt: '2025-01-01T00:00:00',
  };

  const mockTreeResponse = createMockAxiosResponse<TreeResponse>({
    root: {
        id: 'root-1',
        name: 'Root',
        type: 'FOLDER' as const,
        path: '/Root',
        createdAt: '2025-01-01T00:00:00',
        updatedAt: '2025-01-01T00:00:00',
        children: [
          {
            id: 'node-1',
            name: 'Node 1',
            type: 'FILE' as const,
            path: '/Root/Node 1',
            createdAt: '2025-01-01T00:00:00',
            updatedAt: '2025-01-01T00:00:00',
            parentId: 'root-1',
          },
          {
            id: 'folder-1',
            name: 'Folder 1',
            type: 'FOLDER' as const,
            path: '/Root/Folder 1',
            createdAt: '2025-01-01T00:00:00',
            updatedAt: '2025-01-01T00:00:00',
            parentId: 'root-1',
            children: [
              {
                id: 'nested-file',
                name: 'Nested File',
                type: 'FILE' as const,
                path: '/Root/Folder 1/Nested File',
                createdAt: '2025-01-01T00:00:00',
                updatedAt: '2025-01-01T00:00:00',
                parentId: 'folder-1',
              },
            ],
          },
        ],
      }
    });

  beforeEach(() => {
    vi.clearAllMocks();
    // Reset store state
    const store = useTreeStore.getState();
    store.setTree(null);
    store.selectNode(null);
    store.setLoading(false);
    store.setError(null);
    
    // Default mock for getTree
    vi.mocked(nodeApi.getTree).mockResolvedValue(mockTreeResponse);
  });

  describe('loadTree', () => {
    it('should load tree successfully', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      expect(nodeApi.getTree).toHaveBeenCalled();
      expect(useTreeStore.getState().tree).toEqual(mockTreeResponse.data);
      expect(useTreeStore.getState().loading).toBe(false);
      expect(useTreeStore.getState().error).toBeNull();
    });

    it('should set loading state during load', async () => {
      const { result } = renderHook(() => useNodeOperations());

      let loadingDuringCall = false;
      vi.mocked(nodeApi.getTree).mockImplementation(async () => {
        loadingDuringCall = useTreeStore.getState().loading;
        return Promise.resolve(mockTreeResponse);
      });

      await act(async () => {
        await result.current.loadTree();
      });

      expect(loadingDuringCall).toBe(true);
      expect(useTreeStore.getState().loading).toBe(false);
    });

    it('should handle load tree error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Network error occurred',
          },
        },
      };

      vi.mocked(nodeApi.getTree).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      expect(useTreeStore.getState().error).toBe('Network error occurred');
      expect(mockOnError).toHaveBeenCalledWith('Network error occurred');
      expect(useTreeStore.getState().loading).toBe(false);
    });

    it('should use default error message when no detail provided', async () => {
      const mockOnError = vi.fn();
      vi.mocked(nodeApi.getTree).mockRejectedValue(new Error('Unknown error'));

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      expect(useTreeStore.getState().error).toBe('Failed to load tree');
      expect(mockOnError).toHaveBeenCalledWith('Failed to load tree');
    });
  });

  describe('handleCreateNode', () => {
    it('should create a folder successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.createNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleCreateNode('New Folder', 'FOLDER', 'root-1');
      });

      expect(nodeApi.createNode).toHaveBeenCalledWith({
        name: 'New Folder',
        type: 'FOLDER',
        parentId: 'root-1',
      });
      expect(mockOnSuccess).toHaveBeenCalledWith('Folder "New Folder" created successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(1); // Only initial load, create updates local state
    });

    it('should create a file successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.createNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleCreateNode('New File', 'FILE', 'root-1');
      });

      expect(mockOnSuccess).toHaveBeenCalledWith('File "New File" created successfully');
    });

    it('should handle create node error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Node with this name already exists',
          },
        },
      };

      vi.mocked(nodeApi.createNode).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleCreateNode('Duplicate', 'FILE', 'root-1');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Node with this name already exists');
    });
  });

  describe('handleRenameNode', () => {
    it('should rename node successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.updateNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleRenameNode('node-1', 'Renamed Node');
      });

      expect(nodeApi.updateNode).toHaveBeenCalledWith('node-1', 'Renamed Node');
      expect(mockOnSuccess).toHaveBeenCalledWith('Renamed to "Renamed Node" successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(1); // Only initial load, rename updates local state
    });

    it('should handle rename error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Invalid node name',
          },
        },
      };

      vi.mocked(nodeApi.updateNode).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleRenameNode('node-1', '');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Invalid node name');
    });
  });

  describe('handleDeleteNode', () => {
    it('should delete node successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.deleteNode).mockResolvedValue(createMockAxiosResponse<void>(undefined));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleDeleteNode('node-1');
      });

      expect(nodeApi.deleteNode).toHaveBeenCalledWith('node-1');
      expect(mockOnSuccess).toHaveBeenCalledWith('Node deleted successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(1); // Only initial load, delete updates local state
    });

    it('should deselect node after deletion if it was selected', async () => {
      vi.mocked(nodeApi.deleteNode).mockResolvedValue(createMockAxiosResponse<void>(undefined));

      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      // Select the node
      act(() => {
        useTreeStore.getState().selectNode('node-1');
      });

      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');

      await act(async () => {
        await result.current.handleDeleteNode('node-1');
      });

      expect(useTreeStore.getState().selectedNodeId).toBeNull();
    });

    it('should not deselect other nodes when deleting', async () => {
      vi.mocked(nodeApi.deleteNode).mockResolvedValue(createMockAxiosResponse<void>(undefined));

      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      // Select a different node
      act(() => {
        useTreeStore.getState().selectNode('node-2');
      });

      await act(async () => {
        await result.current.handleDeleteNode('node-1');
      });

      expect(useTreeStore.getState().selectedNodeId).toBe('node-2');
    });

    it('should handle delete error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Cannot delete node with children',
          },
        },
      };

      vi.mocked(nodeApi.deleteNode).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleDeleteNode('folder-1');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Cannot delete node with children');
    });
  });

  describe('handleAddTag', () => {
    it('should add tag successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.addTag).mockResolvedValue(createMockAxiosResponse<any>({}));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleAddTag('node-1', 'priority', 'high');
      });

      expect(nodeApi.addTag).toHaveBeenCalledWith('node-1', 'priority', 'high');
      expect(mockOnSuccess).toHaveBeenCalledWith('Tag added successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(1); // Only initial load, addTag updates local state
    });

    it('should handle add tag error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Tag key already exists',
          },
        },
      };

      vi.mocked(nodeApi.addTag).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleAddTag('node-1', 'priority', 'high');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Tag key already exists');
    });
  });

  describe('handleRemoveTag', () => {
    it('should remove tag successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.removeTag).mockResolvedValue(createMockAxiosResponse<void>(undefined));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleRemoveTag('node-1', 'priority');
      });

      expect(nodeApi.removeTag).toHaveBeenCalledWith('node-1', 'priority');
      expect(mockOnSuccess).toHaveBeenCalledWith('Tag removed successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(1); // Only initial load, removeTag updates local state
    });

    it('should handle remove tag error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Tag not found',
          },
        },
      };

      vi.mocked(nodeApi.removeTag).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleRemoveTag('node-1', 'nonexistent');
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Tag not found');
    });
  });

  describe('handleMoveNode', () => {
    it('should move node successfully', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.moveNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleMoveNode('node-1', 'folder-1', 0);
      });

      expect(nodeApi.moveNode).toHaveBeenCalledWith('node-1', 'folder-1', 0);
      expect(mockOnSuccess).toHaveBeenCalledWith('Node moved successfully');
      expect(nodeApi.getTree).toHaveBeenCalledTimes(2);
    });

    it('should handle move node error', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Cannot move node to itself',
          },
        },
      };

      vi.mocked(nodeApi.moveNode).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleMoveNode('folder-1', 'folder-1', 0);
        } catch (err) {
          // Expected to throw
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Cannot move node to itself');
    });
  });

  describe('findNodeById', () => {
    it('should find root node', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      const found = result.current.findNodeById('root-1');
      expect(found).not.toBeNull();
      expect(found?.id).toBe('root-1');
      expect(found?.name).toBe('Root');
    });

    it('should find direct child node', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      const found = result.current.findNodeById('node-1');
      expect(found).not.toBeNull();
      expect(found?.id).toBe('node-1');
      expect(found?.name).toBe('Node 1');
    });

    it('should find nested node', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      const found = result.current.findNodeById('nested-file');
      expect(found).not.toBeNull();
      expect(found?.id).toBe('nested-file');
      expect(found?.name).toBe('Nested File');
    });

    it('should return null for non-existent node', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      const found = result.current.findNodeById('non-existent-id');
      expect(found).toBeNull();
    });

    it('should return null when tree is not loaded', () => {
      const { result } = renderHook(() => useNodeOperations());

      const found = result.current.findNodeById('node-1');
      expect(found).toBeNull();
    });
  });

  describe('getSelectedNode', () => {
    it('should return selected node', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      act(() => {
        useTreeStore.getState().selectNode('node-1');
      });

      const selected = result.current.getSelectedNode();
      expect(selected).not.toBeNull();
      expect(selected?.id).toBe('node-1');
    });

    it('should return null when no node is selected', async () => {
      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      const selected = result.current.getSelectedNode();
      expect(selected).toBeNull();
    });

    it('should return null when tree is not loaded', () => {
      const { result } = renderHook(() => useNodeOperations());

      act(() => {
        useTreeStore.getState().selectNode('node-1');
      });

      const selected = result.current.getSelectedNode();
      expect(selected).toBeNull();
    });
  });

  describe('callbacks', () => {
    it('should work without callbacks provided', async () => {
      vi.mocked(nodeApi.createNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations());

      await act(async () => {
        await result.current.loadTree();
      });

      // Should not throw
      await act(async () => {
        await result.current.handleCreateNode('Test', 'FILE', 'root-1');
      });

      expect(nodeApi.createNode).toHaveBeenCalled();
    });

    it('should call onSuccess callback when provided', async () => {
      const mockOnSuccess = vi.fn();
      vi.mocked(nodeApi.createNode).mockResolvedValue(createMockAxiosResponse(mockNodeResponse));

      const { result } = renderHook(() => useNodeOperations({ onSuccess: mockOnSuccess }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        await result.current.handleCreateNode('Test', 'FILE', 'root-1');
      });

      expect(mockOnSuccess).toHaveBeenCalled();
    });

    it('should call onError callback when provided', async () => {
      const mockOnError = vi.fn();
      const errorResponse = {
        response: {
          data: {
            detail: 'Test error',
          },
        },
      };

      vi.mocked(nodeApi.createNode).mockRejectedValue(errorResponse);

      const { result } = renderHook(() => useNodeOperations({ onError: mockOnError }));

      await act(async () => {
        await result.current.loadTree();
      });

      await act(async () => {
        try {
          await result.current.handleCreateNode('Test', 'FILE', 'root-1');
        } catch (err) {
          // Expected
        }
      });

      expect(mockOnError).toHaveBeenCalledWith('Test error');
    });
  });
});

