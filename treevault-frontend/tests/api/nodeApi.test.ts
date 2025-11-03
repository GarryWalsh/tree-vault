import { describe, it, expect, beforeEach, vi } from 'vitest';
import { nodeApi } from '../../src/api/nodeApi';
import { apiClient } from '../../src/api/client';

// Mock axios client
vi.mock('../../src/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('nodeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getTree', () => {
    it('should call GET /api/v1/tree', async () => {
      const mockResponse = {
        data: {
          root: {
            id: '1',
            name: 'Root',
            type: 'FOLDER',
            children: [],
            path: '/Root',
            createdAt: '2025-01-01T00:00:00',
            updatedAt: '2025-01-01T00:00:00',
          },
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await nodeApi.getTree();

      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/tree');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getNode', () => {
    it('should call GET /api/v1/nodes/:id', async () => {
      const nodeId = '123';
      const mockResponse = {
        data: {
          id: nodeId,
          name: 'Test Node',
          type: 'FILE',
          path: '/Root/Test Node',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await nodeApi.getNode(nodeId);

      expect(apiClient.get).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}`);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('createNode', () => {
    it('should call POST /api/v1/nodes with node data', async () => {
      const createData = {
        name: 'New Folder',
        type: 'FOLDER' as const,
        parentId: '1',
      };

      const mockResponse = {
        data: {
          id: '2',
          ...createData,
          path: '/Root/New Folder',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.createNode(createData);

      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/nodes', createData);
      expect(result).toEqual(mockResponse);
    });
  });

  describe('updateNode', () => {
    it('should call PUT /api/v1/nodes/:id with new name', async () => {
      const nodeId = '123';
      const newName = 'Updated Name';
      const mockResponse = {
        data: {
          id: nodeId,
          name: newName,
          type: 'FILE',
          path: '/Root/Updated Name',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.put).mockResolvedValue(mockResponse);

      const result = await nodeApi.updateNode(nodeId, newName);

      expect(apiClient.put).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}`, { name: newName });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('deleteNode', () => {
    it('should call DELETE /api/v1/nodes/:id', async () => {
      const nodeId = '123';

      vi.mocked(apiClient.delete).mockResolvedValue({});

      await nodeApi.deleteNode(nodeId);

      expect(apiClient.delete).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}`);
    });
  });

  describe('moveNode', () => {
    it('should call POST /api/v1/nodes/:id/move with move data', async () => {
      const nodeId = '123';
      const newParentId = '456';
      const position = 2;

      const mockResponse = {
        data: {
          id: nodeId,
          name: 'Moved Node',
          type: 'FILE',
          parentId: newParentId,
          position,
          path: '/Root/NewParent/Moved Node',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.moveNode(nodeId, newParentId, position);

      expect(apiClient.post).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}/move`, {
        newParentId,
        position,
      });
      expect(result).toEqual(mockResponse);
    });
  });

  describe('addTag', () => {
    it('should call POST /api/v1/nodes/:id/tags with tag data', async () => {
      const nodeId = '123';
      const key = 'category';
      const value = 'important';

      vi.mocked(apiClient.post).mockResolvedValue({});

      await nodeApi.addTag(nodeId, key, value);

      expect(apiClient.post).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}/tags`, {
        key,
        value,
      });
    });
  });

  describe('removeTag', () => {
    it('should call DELETE /api/v1/nodes/:id/tags/:key', async () => {
      const nodeId = '123';
      const key = 'category';

      vi.mocked(apiClient.delete).mockResolvedValue({});

      await nodeApi.removeTag(nodeId, key);

      expect(apiClient.delete).toHaveBeenCalledWith(`/api/v1/nodes/${nodeId}/tags/${key}`);
    });
  });

  // Error Handling Tests
  describe('error handling', () => {
    describe('getTree errors', () => {
      it('should handle network error', async () => {
        vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'));

        await expect(nodeApi.getTree()).rejects.toThrow('Network error');
      });

      it('should handle 404 error', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Tree not found' },
          },
        };
        vi.mocked(apiClient.get).mockRejectedValue(error);

        await expect(nodeApi.getTree()).rejects.toEqual(error);
      });

      it('should handle 500 error', async () => {
        const error = {
          response: {
            status: 500,
            data: { detail: 'Internal server error' },
          },
        };
        vi.mocked(apiClient.get).mockRejectedValue(error);

        await expect(nodeApi.getTree()).rejects.toEqual(error);
      });
    });

    describe('getNode errors', () => {
      it('should handle node not found', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Node not found' },
          },
        };
        vi.mocked(apiClient.get).mockRejectedValue(error);

        await expect(nodeApi.getNode('nonexistent')).rejects.toEqual(error);
      });

      it('should handle invalid node ID', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Invalid node ID format' },
          },
        };
        vi.mocked(apiClient.get).mockRejectedValue(error);

        await expect(nodeApi.getNode('invalid-id')).rejects.toEqual(error);
      });
    });

    describe('createNode errors', () => {
      it('should handle duplicate name error', async () => {
        const error = {
          response: {
            status: 409,
            data: { detail: 'Node with this name already exists' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        const createData = {
          name: 'Duplicate',
          type: 'FILE' as const,
          parentId: '1',
        };

        await expect(nodeApi.createNode(createData)).rejects.toEqual(error);
      });

      it('should handle invalid parent error', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Parent node not found' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        const createData = {
          name: 'New Node',
          type: 'FILE' as const,
          parentId: 'nonexistent',
        };

        await expect(nodeApi.createNode(createData)).rejects.toEqual(error);
      });

      it('should handle validation error for empty name', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Node name cannot be empty' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        const createData = {
          name: '',
          type: 'FILE' as const,
          parentId: '1',
        };

        await expect(nodeApi.createNode(createData)).rejects.toEqual(error);
      });
    });

    describe('updateNode errors', () => {
      it('should handle duplicate name on update', async () => {
        const error = {
          response: {
            status: 409,
            data: { detail: 'Another node with this name already exists' },
          },
        };
        vi.mocked(apiClient.put).mockRejectedValue(error);

        await expect(nodeApi.updateNode('123', 'ExistingName')).rejects.toEqual(error);
      });

      it('should handle updating non-existent node', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Node not found' },
          },
        };
        vi.mocked(apiClient.put).mockRejectedValue(error);

        await expect(nodeApi.updateNode('nonexistent', 'NewName')).rejects.toEqual(error);
      });
    });

    describe('deleteNode errors', () => {
      it('should handle deleting non-existent node', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Node not found' },
          },
        };
        vi.mocked(apiClient.delete).mockRejectedValue(error);

        await expect(nodeApi.deleteNode('nonexistent')).rejects.toEqual(error);
      });

      it('should handle permission error', async () => {
        const error = {
          response: {
            status: 403,
            data: { detail: 'Cannot delete root node' },
          },
        };
        vi.mocked(apiClient.delete).mockRejectedValue(error);

        await expect(nodeApi.deleteNode('root')).rejects.toEqual(error);
      });
    });

    describe('moveNode errors', () => {
      it('should handle circular reference error', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Cannot move node into itself or its descendants' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        await expect(nodeApi.moveNode('123', '123', 0)).rejects.toEqual(error);
      });

      it('should handle invalid position error', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Invalid position' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        await expect(nodeApi.moveNode('123', '456', -1)).rejects.toEqual(error);
      });

      it('should handle moving to non-folder error', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Target parent must be a folder' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        await expect(nodeApi.moveNode('123', 'file-id', 0)).rejects.toEqual(error);
      });
    });

    describe('addTag errors', () => {
      it('should handle duplicate tag key error', async () => {
        const error = {
          response: {
            status: 409,
            data: { detail: 'Tag with this key already exists' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        await expect(nodeApi.addTag('123', 'existingKey', 'value')).rejects.toEqual(error);
      });

      it('should handle invalid tag key error', async () => {
        const error = {
          response: {
            status: 400,
            data: { detail: 'Tag key cannot be empty' },
          },
        };
        vi.mocked(apiClient.post).mockRejectedValue(error);

        await expect(nodeApi.addTag('123', '', 'value')).rejects.toEqual(error);
      });
    });

    describe('removeTag errors', () => {
      it('should handle tag not found error', async () => {
        const error = {
          response: {
            status: 404,
            data: { detail: 'Tag not found' },
          },
        };
        vi.mocked(apiClient.delete).mockRejectedValue(error);

        await expect(nodeApi.removeTag('123', 'nonexistentKey')).rejects.toEqual(error);
      });
    });
  });

  // Edge Cases
  describe('edge cases', () => {
    it('should handle creating node with special characters in name', async () => {
      const createData = {
        name: 'File with "quotes" and \'apostrophes\'',
        type: 'FILE' as const,
        parentId: '1',
      };

      const mockResponse = {
        data: {
          id: '2',
          ...createData,
          path: '/Root/File with "quotes" and \'apostrophes\'',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.createNode(createData);

      expect(result).toEqual(mockResponse);
    });

    it('should handle creating node with unicode characters', async () => {
      const createData = {
        name: '文件名 📁 émojis',
        type: 'FOLDER' as const,
        parentId: '1',
      };

      const mockResponse = {
        data: {
          id: '2',
          ...createData,
          path: '/Root/文件名 📁 émojis',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.createNode(createData);

      expect(result).toEqual(mockResponse);
    });

    it('should handle very long node names', async () => {
      const longName = 'A'.repeat(255);
      const createData = {
        name: longName,
        type: 'FILE' as const,
        parentId: '1',
      };

      const mockResponse = {
        data: {
          id: '2',
          ...createData,
          path: `/Root/${longName}`,
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.createNode(createData);

      expect(result).toEqual(mockResponse);
    });

    it('should handle tag with empty value', async () => {
      vi.mocked(apiClient.post).mockResolvedValue({});

      await nodeApi.addTag('123', 'status', '');

      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/nodes/123/tags', {
        key: 'status',
        value: '',
      });
    });

    it('should handle tag with special characters', async () => {
      vi.mocked(apiClient.post).mockResolvedValue({});

      await nodeApi.addTag('123', 'key:with:colons', 'value/with/slashes');

      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/nodes/123/tags', {
        key: 'key:with:colons',
        value: 'value/with/slashes',
      });
    });

    it('should handle moving node to position 0', async () => {
      const mockResponse = {
        data: {
          id: '123',
          name: 'Moved Node',
          type: 'FILE',
          parentId: '456',
          position: 0,
          path: '/Root/NewParent/Moved Node',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.moveNode('123', '456', 0);

      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/nodes/123/move', {
        newParentId: '456',
        position: 0,
      });
      expect(result).toEqual(mockResponse);
    });

    it('should handle moving node to large position', async () => {
      const mockResponse = {
        data: {
          id: '123',
          name: 'Moved Node',
          type: 'FILE',
          parentId: '456',
          position: 999,
          path: '/Root/NewParent/Moved Node',
          createdAt: '2025-01-01T00:00:00',
          updatedAt: '2025-01-01T00:00:00',
        },
      };

      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await nodeApi.moveNode('123', '456', 999);

      expect(result).toEqual(mockResponse);
    });
  });
});

