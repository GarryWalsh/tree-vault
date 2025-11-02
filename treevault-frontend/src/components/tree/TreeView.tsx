import React, { useEffect, useState } from 'react';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import { Box, Paper, IconButton, Menu, MenuItem, CircularProgress, Alert } from '@mui/material';
import {
  ExpandMore,
  ChevronRight,
  Folder,
  InsertDriveFile,
  MoreVert
} from '@mui/icons-material';
import { nodeApi } from '../../api/nodeApi';
import { useTreeStore } from '../../store/treeStore';
import { TreeResponse, NodeResponse } from '../../api/types';

export const TreeView: React.FC = () => {
  const [tree, setTree] = useState<TreeResponse | null>(null);
  const [contextMenu, setContextMenu] = useState<{
    nodeId: string;
    anchorEl: HTMLElement | null;
  } | null>(null);
  
  const { 
    selectedNodeId,
    expandedNodeIds,
    loading,
    error,
    selectNode,
    setExpandedNodes,
    setLoading,
    setError
  } = useTreeStore();
  
  // Load tree on mount
  useEffect(() => {
    loadTree();
  }, []);
  
  // Pure API call - backend provides all data structure and business logic
  const loadTree = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await nodeApi.getTree();
      setTree(response.data); // Trust backend for all tree structure and business rules
    } catch (err: any) {
      // Display error from backend ProblemDetail response - all errors come from backend
      setError(err.response?.data?.detail || 'Failed to load tree');
    } finally {
      setLoading(false);
    }
  };
  
  // NO client-side validation - backend handles all validation and returns ProblemDetail on errors
  const handleCreateNode = async (parentId: string, type: 'FOLDER' | 'FILE') => {
    const name = prompt(`Enter ${type.toLowerCase()} name:`);
    if (!name) return; // Simple UI check only - no business validation
    
    try {
      // Backend validates: name format, length, invalid characters, duplicate names, etc.
      await nodeApi.createNode({
        name,
        type,
        parentId: parentId || undefined
      });
      await loadTree(); // Reload tree from backend after successful operation
    } catch (err: any) {
      // Display backend ProblemDetail response - all validation errors come from backend
      const errorDetail = err.response?.data?.detail || 'Failed to create node';
      alert(errorDetail);
    }
  };
  
  const handleDeleteNode = async (nodeId: string) => {
    if (!confirm('Delete this node and all its children?')) return;
    
    try {
      await nodeApi.deleteNode(nodeId);
      await loadTree(); // Reload tree from backend
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to delete node');
    }
  };
  
  const renderNode = (node: NodeResponse): JSX.Element => (
    <TreeItem
      key={node.id}
      itemId={node.id}
      label={
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {node.type === 'FOLDER' ? <Folder /> : <InsertDriveFile />}
          <span>{node.name}</span>
          <IconButton
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              setContextMenu({ nodeId: node.id, anchorEl: e.currentTarget });
            }}
          >
            <MoreVert fontSize="small" />
          </IconButton>
        </Box>
      }
    >
      {node.children?.map(renderNode)}
    </TreeItem>
  );
  
  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;
  if (!tree) return <Alert severity="info">No data</Alert>;
  
  return (
    <Paper elevation={2} sx={{ p: 2, height: '100%' }}>
      <SimpleTreeView
        slots={{
          collapseIcon: ExpandMore,
          expandIcon: ChevronRight
        }}
        expandedItems={expandedNodeIds}
        selectedItems={selectedNodeId ?? null}
        onExpandedItemsChange={(_event, itemIds) => setExpandedNodes(Array.isArray(itemIds) ? itemIds : [])}
        onSelectedItemsChange={(_event, itemId) => {
          if (Array.isArray(itemId)) {
            selectNode(itemId[0] ?? null);
          } else {
            selectNode(itemId ?? null);
          }
        }}
      >
        {renderNode(tree.root)}
      </SimpleTreeView>
      
      <Menu
        open={Boolean(contextMenu)}
        anchorEl={contextMenu?.anchorEl}
        onClose={() => setContextMenu(null)}
      >
        <MenuItem onClick={() => {
          if (contextMenu) {
            handleCreateNode(contextMenu.nodeId, 'FOLDER');
            setContextMenu(null);
          }
        }}>
          New Folder
        </MenuItem>
        <MenuItem onClick={() => {
          if (contextMenu) {
            handleCreateNode(contextMenu.nodeId, 'FILE');
            setContextMenu(null);
          }
        }}>
          New File
        </MenuItem>
        <MenuItem onClick={() => {
          if (contextMenu) {
            handleDeleteNode(contextMenu.nodeId);
            setContextMenu(null);
          }
        }}>
          Delete
        </MenuItem>
      </Menu>
    </Paper>
  );
};

