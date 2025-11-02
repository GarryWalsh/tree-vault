export interface NodeResponse {
  id: string;
  name: string;
  type: 'FOLDER' | 'FILE';
  parentId?: string;
  children?: NodeResponse[];
  tags?: Record<string, string>;
  path: string;
  position?: number;
  version?: number;
  createdAt: string;
  updatedAt: string;
}

export interface TreeResponse {
  root: NodeResponse;
}

export interface CreateNodeRequest {
  name: string;
  type: 'FOLDER' | 'FILE';
  parentId?: string;
  tags?: Record<string, string>;
}

export interface UpdateNodeRequest {
  name: string;
}

export interface MoveNodeRequest {
  newParentId: string;
  position: number;
}

export interface TagRequest {
  key: string;
  value: string;
}

export interface TagResponse {
  key: string;
  value: string;
}

