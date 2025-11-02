import { create } from 'zustand';

interface TreeStore {
  selectedNodeId: string | null;
  expandedNodeIds: string[];
  loading: boolean;
  error: string | null;
  
  // UI-only actions
  selectNode: (id: string | null) => void;
  setExpandedNodes: (ids: string[]) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useTreeStore = create<TreeStore>((set) => ({
  selectedNodeId: null,
  expandedNodeIds: [],
  loading: false,
  error: null,
  
  selectNode: (id) => set({ selectedNodeId: id }),

  setExpandedNodes: (ids) => set({ expandedNodeIds: ids }),
  
  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error })
}));

