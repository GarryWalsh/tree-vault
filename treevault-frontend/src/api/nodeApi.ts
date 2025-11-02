import { TreeResponse, NodeResponse, CreateNodeRequest } from './types';
import { apiClient } from './client';

export const nodeApi = {
  // All operations delegate to backend - no validation or business logic here
  getTree: () => 
    apiClient.get<TreeResponse>('/api/v1/tree'),
    
  getNode: (id: string) => 
    apiClient.get<NodeResponse>(`/api/v1/nodes/${id}`),
    
  createNode: (data: CreateNodeRequest) => 
    apiClient.post<NodeResponse>('/api/v1/nodes', data),
    
  updateNode: (id: string, name: string) => 
    apiClient.put<NodeResponse>(`/api/v1/nodes/${id}`, { name }),
    
  deleteNode: (id: string) => 
    apiClient.delete(`/api/v1/nodes/${id}`),
    
  moveNode: (id: string, newParentId: string, position: number) => 
    apiClient.post<NodeResponse>(`/api/v1/nodes/${id}/move`, {
      newParentId,
      position
    }),
    
  addTag: (nodeId: string, key: string, value: string) => 
    apiClient.post(`/api/v1/nodes/${nodeId}/tags`, { key, value }),
    
  removeTag: (nodeId: string, key: string) => 
    apiClient.delete(`/api/v1/nodes/${nodeId}/tags/${key}`)
};

