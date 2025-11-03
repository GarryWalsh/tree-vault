import { create } from 'zustand';
import { TreeResponse, NodeResponse } from '../api/types';

interface TreeStore {
  tree: TreeResponse | null;
  selectedNodeId: string | null;
  expandedNodeIds: string[];
  loading: boolean;
  error: string | null;
  
  // Tree data actions
  setTree: (tree: TreeResponse | null) => void;
  updateNodeInTree: (nodeId: string, updater: (node: NodeResponse) => NodeResponse) => void;
  addNodeToTree: (parentId: string, newNode: NodeResponse) => void;
  removeNodeFromTree: (nodeId: string) => void;
  
  // UI actions
  selectNode: (id: string | null) => void;
  setExpandedNodes: (ids: string[]) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

// Helper function to recursively update a node
const updateNodeRecursive = (node: NodeResponse, nodeId: string, updater: (node: NodeResponse) => NodeResponse): NodeResponse => {
  if (node.id === nodeId) {
    return updater(node);
  }
  
  if (node.children) {
    return {
      ...node,
      children: node.children.map(child => updateNodeRecursive(child, nodeId, updater))
    };
  }
  
  return node;
};

// Helper function to add a node to a parent
const addNodeRecursive = (node: NodeResponse, parentId: string, newNode: NodeResponse): NodeResponse => {
  if (node.id === parentId) {
    return {
      ...node,
      children: [...(node.children || []), newNode].sort((a, b) => (a.position || 0) - (b.position || 0))
    };
  }
  
  if (node.children) {
    return {
      ...node,
      children: node.children.map(child => addNodeRecursive(child, parentId, newNode))
    };
  }
  
  return node;
};

// Helper function to remove a node
const removeNodeRecursive = (node: NodeResponse, nodeId: string): NodeResponse => {
  if (node.children) {
    return {
      ...node,
      children: node.children
        .filter(child => child.id !== nodeId)
        .map(child => removeNodeRecursive(child, nodeId))
    };
  }
  
  return node;
};

export const useTreeStore = create<TreeStore>((set) => ({
  tree: null,
  selectedNodeId: null,
  expandedNodeIds: [],
  loading: false,
  error: null,
  
  setTree: (tree) => set({ tree }),
  
  updateNodeInTree: (nodeId, updater) => set((state) => {
    if (!state.tree) return state;
    
    return {
      tree: {
        ...state.tree,
        root: updateNodeRecursive(state.tree.root, nodeId, updater)
      }
    };
  }),
  
  addNodeToTree: (parentId, newNode) => set((state) => {
    if (!state.tree) return state;
    
    return {
      tree: {
        ...state.tree,
        root: addNodeRecursive(state.tree.root, parentId, newNode)
      }
    };
  }),
  
  removeNodeFromTree: (nodeId) => set((state) => {
    if (!state.tree) return state;
    
    return {
      tree: {
        ...state.tree,
        root: removeNodeRecursive(state.tree.root, nodeId)
      }
    };
  }),
  
  selectNode: (id) => set({ selectedNodeId: id }),

  setExpandedNodes: (ids) => set({ expandedNodeIds: ids }),
  
  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error })
}));

