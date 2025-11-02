import { describe, it, expect, beforeEach } from 'vitest';
import { useTreeStore } from '../../src/store/treeStore';

describe('treeStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    const store = useTreeStore.getState();
    store.selectNode(null);
    store.setExpandedNodes([]);
    store.setLoading(false);
    store.setError(null);
  });

  describe('initial state', () => {
    it('should have null selectedNodeId initially', () => {
      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBeNull();
    });

    it('should have empty expandedNodeIds initially', () => {
      const state = useTreeStore.getState();
      expect(state.expandedNodeIds).toEqual([]);
    });

    it('should have loading false initially', () => {
      const state = useTreeStore.getState();
      expect(state.loading).toBe(false);
    });

    it('should have null error initially', () => {
      const state = useTreeStore.getState();
      expect(state.error).toBeNull();
    });
  });

  describe('selectNode', () => {
    it('should set the selected node id', () => {
      const { selectNode } = useTreeStore.getState();
      selectNode('node-123');

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('node-123');
    });

    it('should update selected node id when called multiple times', () => {
      const { selectNode } = useTreeStore.getState();
      
      selectNode('node-1');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');

      selectNode('node-2');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-2');

      selectNode('node-3');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-3');
    });

    it('should allow setting selected node to null', () => {
      const { selectNode } = useTreeStore.getState();
      
      selectNode('node-123');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-123');

      selectNode(null);
      expect(useTreeStore.getState().selectedNodeId).toBeNull();
    });

    it('should handle selecting the same node multiple times', () => {
      const { selectNode } = useTreeStore.getState();
      
      selectNode('node-123');
      selectNode('node-123');
      selectNode('node-123');

      expect(useTreeStore.getState().selectedNodeId).toBe('node-123');
    });
  });

  describe('setExpandedNodes', () => {
    it('should set expanded node ids', () => {
      const { setExpandedNodes } = useTreeStore.getState();
      const nodeIds = ['node-1', 'node-2', 'node-3'];

      setExpandedNodes(nodeIds);

      const state = useTreeStore.getState();
      expect(state.expandedNodeIds).toEqual(nodeIds);
    });

    it('should replace existing expanded nodes', () => {
      const { setExpandedNodes } = useTreeStore.getState();
      
      setExpandedNodes(['node-1', 'node-2']);
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-1', 'node-2']);

      setExpandedNodes(['node-3', 'node-4']);
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-3', 'node-4']);
    });

    it('should allow setting empty array', () => {
      const { setExpandedNodes } = useTreeStore.getState();
      
      setExpandedNodes(['node-1', 'node-2']);
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-1', 'node-2']);

      setExpandedNodes([]);
      expect(useTreeStore.getState().expandedNodeIds).toEqual([]);
    });

    it('should handle large arrays of expanded nodes', () => {
      const { setExpandedNodes } = useTreeStore.getState();
      const largeArray = Array.from({ length: 100 }, (_, i) => `node-${i}`);

      setExpandedNodes(largeArray);

      const state = useTreeStore.getState();
      expect(state.expandedNodeIds).toEqual(largeArray);
      expect(state.expandedNodeIds.length).toBe(100);
    });

    it('should handle duplicate node ids in array', () => {
      const { setExpandedNodes } = useTreeStore.getState();
      const nodesWithDuplicates = ['node-1', 'node-2', 'node-1', 'node-3'];

      setExpandedNodes(nodesWithDuplicates);

      const state = useTreeStore.getState();
      expect(state.expandedNodeIds).toEqual(nodesWithDuplicates);
    });
  });

  describe('setLoading', () => {
    it('should set loading to true', () => {
      const { setLoading } = useTreeStore.getState();
      
      setLoading(true);

      const state = useTreeStore.getState();
      expect(state.loading).toBe(true);
    });

    it('should set loading to false', () => {
      const { setLoading } = useTreeStore.getState();
      
      setLoading(true);
      expect(useTreeStore.getState().loading).toBe(true);

      setLoading(false);
      expect(useTreeStore.getState().loading).toBe(false);
    });

    it('should toggle loading state multiple times', () => {
      const { setLoading } = useTreeStore.getState();
      
      setLoading(true);
      expect(useTreeStore.getState().loading).toBe(true);

      setLoading(false);
      expect(useTreeStore.getState().loading).toBe(false);

      setLoading(true);
      expect(useTreeStore.getState().loading).toBe(true);
    });
  });

  describe('setError', () => {
    it('should set error message', () => {
      const { setError } = useTreeStore.getState();
      const errorMessage = 'Something went wrong';

      setError(errorMessage);

      const state = useTreeStore.getState();
      expect(state.error).toBe(errorMessage);
    });

    it('should clear error by setting to null', () => {
      const { setError } = useTreeStore.getState();
      
      setError('Error occurred');
      expect(useTreeStore.getState().error).toBe('Error occurred');

      setError(null);
      expect(useTreeStore.getState().error).toBeNull();
    });

    it('should update error message', () => {
      const { setError } = useTreeStore.getState();
      
      setError('First error');
      expect(useTreeStore.getState().error).toBe('First error');

      setError('Second error');
      expect(useTreeStore.getState().error).toBe('Second error');
    });

    it('should handle long error messages', () => {
      const { setError } = useTreeStore.getState();
      const longError = 'This is a very long error message that contains a lot of text to simulate real-world error scenarios where error messages can be quite detailed and lengthy.';

      setError(longError);

      const state = useTreeStore.getState();
      expect(state.error).toBe(longError);
    });
  });

  describe('multiple state updates', () => {
    it('should handle multiple simultaneous state updates', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      selectNode('node-123');
      setExpandedNodes(['node-1', 'node-2']);
      setLoading(true);
      setError('Test error');

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('node-123');
      expect(state.expandedNodeIds).toEqual(['node-1', 'node-2']);
      expect(state.loading).toBe(true);
      expect(state.error).toBe('Test error');
    });

    it('should maintain independent state for each property', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      selectNode('node-1');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');
      expect(useTreeStore.getState().expandedNodeIds).toEqual([]);

      setExpandedNodes(['node-2', 'node-3']);
      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-2', 'node-3']);

      setLoading(true);
      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-2', 'node-3']);
      expect(useTreeStore.getState().loading).toBe(true);

      setError('Error');
      expect(useTreeStore.getState().selectedNodeId).toBe('node-1');
      expect(useTreeStore.getState().expandedNodeIds).toEqual(['node-2', 'node-3']);
      expect(useTreeStore.getState().loading).toBe(true);
      expect(useTreeStore.getState().error).toBe('Error');
    });
  });

  describe('state persistence across actions', () => {
    it('should not affect other state when updating selectedNodeId', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      setExpandedNodes(['node-1']);
      setLoading(true);
      setError('Initial error');

      selectNode('node-2');

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('node-2');
      expect(state.expandedNodeIds).toEqual(['node-1']);
      expect(state.loading).toBe(true);
      expect(state.error).toBe('Initial error');
    });

    it('should not affect other state when updating expandedNodeIds', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      selectNode('selected-node');
      setLoading(true);
      setError('Initial error');

      setExpandedNodes(['node-1', 'node-2']);

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('selected-node');
      expect(state.expandedNodeIds).toEqual(['node-1', 'node-2']);
      expect(state.loading).toBe(true);
      expect(state.error).toBe('Initial error');
    });

    it('should not affect other state when updating loading', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      selectNode('selected-node');
      setExpandedNodes(['node-1']);
      setError('Initial error');

      setLoading(false);

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('selected-node');
      expect(state.expandedNodeIds).toEqual(['node-1']);
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Initial error');
    });

    it('should not affect other state when updating error', () => {
      const { selectNode, setExpandedNodes, setLoading, setError } = useTreeStore.getState();
      
      selectNode('selected-node');
      setExpandedNodes(['node-1']);
      setLoading(true);

      setError('New error');

      const state = useTreeStore.getState();
      expect(state.selectedNodeId).toBe('selected-node');
      expect(state.expandedNodeIds).toEqual(['node-1']);
      expect(state.loading).toBe(true);
      expect(state.error).toBe('New error');
    });
  });

  describe('edge cases', () => {
    it('should handle empty string as selected node id', () => {
      const { selectNode } = useTreeStore.getState();
      
      selectNode('');

      expect(useTreeStore.getState().selectedNodeId).toBe('');
    });

    it('should handle very long node id', () => {
      const { selectNode } = useTreeStore.getState();
      const longId = 'a'.repeat(1000);

      selectNode(longId);

      expect(useTreeStore.getState().selectedNodeId).toBe(longId);
    });

    it('should handle special characters in node id', () => {
      const { selectNode } = useTreeStore.getState();
      const specialId = 'node-123!@#$%^&*()';

      selectNode(specialId);

      expect(useTreeStore.getState().selectedNodeId).toBe(specialId);
    });

    it('should handle empty string as error', () => {
      const { setError } = useTreeStore.getState();
      
      setError('');

      expect(useTreeStore.getState().error).toBe('');
    });
  });
});

