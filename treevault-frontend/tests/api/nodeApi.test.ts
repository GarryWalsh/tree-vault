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
});

