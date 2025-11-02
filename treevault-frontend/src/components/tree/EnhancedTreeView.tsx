import React, { useEffect, useState } from 'react';
import { SimpleTreeView, TreeItem } from '@mui/x-tree-view';
import {
  Box,
  Paper,
  IconButton,
  Menu,
  MenuItem,
  Alert,
  Grid,
  Snackbar,
  Button,
  Typography,
  Skeleton,
} from '@mui/material';
import {
  ExpandMore,
  ChevronRight,
  Folder,
  InsertDriveFile,
  MoreVert,
  Refresh as RefreshIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { nodeApi } from '../../api/nodeApi';
import { useTreeStore } from '../../store/treeStore';
import { TreeResponse, NodeResponse } from '../../api/types';
import { CreateNodeDialog } from '../dialogs/CreateNodeDialog';
import { RenameNodeDialog } from '../dialogs/RenameNodeDialog';
import { ConfirmDialog } from '../dialogs/ConfirmDialog';
import { NodeDetailsPanel } from '../common/NodeDetailsPanel';

export const EnhancedTreeView: React.FC = () => {
  const [tree, setTree] = useState<TreeResponse | null>(null);
  const [contextMenu, setContextMenu] = useState<{
    nodeId: string;
    nodeName: string;
    nodeType: 'FOLDER' | 'FILE';
    anchorEl: HTMLElement | null;
  } | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [draggedNode, setDraggedNode] = useState<NodeResponse | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: 'success' | 'error' | 'info';
  }>({ open: false, message: '', severity: 'info' });

  const {
    selectedNodeId,
    expandedNodeIds,
    loading,
    error,
    selectNode,
    setExpandedNodes,
    setLoading,
    setError,
  } = useTreeStore();

  useEffect(() => {
    loadTree();
  }, []);

  const loadTree = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await nodeApi.getTree();
      setTree(response.data);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to load tree');
    } finally {
      setLoading(false);
    }
  };

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'info' = 'info') => {
    setSnackbar({ open: true, message, severity });
  };

  const handleCreateNode = async (name: string, type: 'FOLDER' | 'FILE') => {
    if (!contextMenu) return;

    try {
      await nodeApi.createNode({
        name,
        type,
        parentId: contextMenu.nodeId,
      });
      await loadTree();
      showSnackbar(`${type === 'FOLDER' ? 'Folder' : 'File'} "${name}" created successfully`, 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to create node';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleRenameNode = async (newName: string) => {
    if (!contextMenu) return;

    try {
      await nodeApi.updateNode(contextMenu.nodeId, newName);
      await loadTree();
      showSnackbar(`Renamed to "${newName}" successfully`, 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to rename node';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleDeleteNode = async () => {
    if (!contextMenu) return;

    try {
      await nodeApi.deleteNode(contextMenu.nodeId);
      await loadTree();
      if (selectedNodeId === contextMenu.nodeId) {
        selectNode(null);
      }
      showSnackbar('Node deleted successfully', 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to delete node';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleAddTag = async (key: string, value: string) => {
    if (!selectedNodeId) return;

    try {
      await nodeApi.addTag(selectedNodeId, key, value);
      await loadTree();
      showSnackbar('Tag added successfully', 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to add tag';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleRemoveTag = async (key: string) => {
    if (!selectedNodeId) return;

    try {
      await nodeApi.removeTag(selectedNodeId, key);
      await loadTree();
      showSnackbar('Tag removed successfully', 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to remove tag';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleMoveNode = async (nodeId: string, newParentId: string, position: number) => {
    try {
      await nodeApi.moveNode(nodeId, newParentId, position);
      await loadTree();
      showSnackbar('Node moved successfully', 'success');
    } catch (err: any) {
      const errorDetail = err.response?.data?.detail || 'Failed to move node';
      showSnackbar(errorDetail, 'error');
    }
  };

  const handleDragStart = (e: React.DragEvent, node: NodeResponse) => {
    e.stopPropagation();
    setDraggedNode(node);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, nodeId: string) => {
    e.preventDefault();
    e.stopPropagation();
    if (draggedNode && draggedNode.id !== nodeId) {
      setDropTarget(nodeId);
      e.dataTransfer.dropEffect = 'move';
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDropTarget(null);
  };

  const handleDrop = async (e: React.DragEvent, targetNode: NodeResponse) => {
    e.preventDefault();
    e.stopPropagation();
    setDropTarget(null);

    if (!draggedNode || draggedNode.id === targetNode.id) {
      setDraggedNode(null);
      return;
    }

    // Can only drop into folders
    if (targetNode.type !== 'FOLDER') {
      showSnackbar('Can only move nodes into folders', 'error');
      setDraggedNode(null);
      return;
    }

    // Prevent dropping a folder into itself or its descendants
    if (isDescendant(draggedNode, targetNode)) {
      showSnackbar('Cannot move a folder into itself or its descendants', 'error');
      setDraggedNode(null);
      return;
    }

    // Calculate position (append to end)
    const position = targetNode.children ? targetNode.children.length : 0;

    await handleMoveNode(draggedNode.id, targetNode.id, position);
    setDraggedNode(null);
  };

  const isDescendant = (parent: NodeResponse, potentialDescendant: NodeResponse): boolean => {
    if (parent.id === potentialDescendant.id) return true;
    if (!parent.children) return false;
    return parent.children.some((child) => isDescendant(child, potentialDescendant));
  };

  const findNodeById = (node: NodeResponse, id: string): NodeResponse | null => {
    if (node.id === id) return node;
    if (node.children) {
      for (const child of node.children) {
        const found = findNodeById(child, id);
        if (found) return found;
      }
    }
    return null;
  };

  const getSelectedNode = (): NodeResponse | null => {
    if (!tree || !selectedNodeId) return null;
    return findNodeById(tree.root, selectedNodeId);
  };

  const renderNode = (node: NodeResponse): JSX.Element => (
    <TreeItem
      key={node.id}
      itemId={node.id}
      label={
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            backgroundColor: dropTarget === node.id ? 'action.hover' : 'transparent',
            transition: 'background-color 0.2s',
            cursor: 'grab',
            '&:active': {
              cursor: 'grabbing',
            },
          }}
          draggable
          onDragStart={(e) => handleDragStart(e, node)}
          onDragOver={(e) => handleDragOver(e, node.id)}
          onDragLeave={handleDragLeave}
          onDrop={(e) => handleDrop(e, node)}
        >
          {node.type === 'FOLDER' ? <Folder /> : <InsertDriveFile />}
          <span>{node.name}</span>
          <IconButton
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              setContextMenu({
                nodeId: node.id,
                nodeName: node.name,
                nodeType: node.type,
                anchorEl: e.currentTarget,
              });
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

  if (loading) {
    return (
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Paper elevation={2} sx={{ p: 2, height: '600px' }}>
            <Skeleton variant="text" width="60%" height={40} />
            <Skeleton variant="rectangular" width="100%" height={60} sx={{ mt: 2 }} />
            <Skeleton variant="rectangular" width="90%" height={60} sx={{ mt: 1, ml: 3 }} />
            <Skeleton variant="rectangular" width="80%" height={60} sx={{ mt: 1, ml: 6 }} />
            <Skeleton variant="rectangular" width="90%" height={60} sx={{ mt: 1, ml: 3 }} />
            <Skeleton variant="rectangular" width="100%" height={60} sx={{ mt: 2 }} />
          </Paper>
        </Grid>
        <Grid item xs={12} md={6}>
          <Paper elevation={2} sx={{ p: 3, height: '600px' }}>
            <Skeleton variant="text" width="50%" height={40} />
            <Skeleton variant="text" width="30%" height={30} sx={{ mt: 2 }} />
            <Skeleton variant="text" width="80%" height={30} sx={{ mt: 1 }} />
            <Skeleton variant="text" width="30%" height={30} sx={{ mt: 2 }} />
            <Skeleton variant="text" width="90%" height={30} sx={{ mt: 1 }} />
          </Paper>
        </Grid>
      </Grid>
    );
  }

  if (error) {
    return (
      <Paper elevation={2} sx={{ p: 4, textAlign: 'center' }}>
        <ErrorIcon color="error" sx={{ fontSize: 64, mb: 2 }} />
        <Typography variant="h5" gutterBottom color="error">
          Failed to Load Tree
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          {error}
        </Typography>
        <Button
          variant="contained"
          startIcon={<RefreshIcon />}
          onClick={loadTree}
        >
          Retry
        </Button>
      </Paper>
    );
  }

  if (!tree) {
    return (
      <Paper elevation={2} sx={{ p: 4, textAlign: 'center' }}>
        <Typography variant="h6" color="text.secondary" gutterBottom>
          No Tree Data Available
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          The tree structure could not be loaded from the backend.
        </Typography>
        <Button
          variant="contained"
          startIcon={<RefreshIcon />}
          onClick={loadTree}
        >
          Load Tree
        </Button>
      </Paper>
    );
  }

  return (
    <>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Paper elevation={2} sx={{ p: 2, height: '600px', overflow: 'auto' }}>
            <SimpleTreeView
              slots={{
                collapseIcon: ExpandMore,
                expandIcon: ChevronRight,
              }}
              expandedItems={expandedNodeIds}
              selectedItems={selectedNodeId ?? undefined}
              onExpandedItemsChange={(_event, itemIds) =>
                setExpandedNodes(Array.isArray(itemIds) ? itemIds : [])
              }
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
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Box sx={{ height: '600px' }}>
            <NodeDetailsPanel
              node={getSelectedNode()}
              onAddTag={handleAddTag}
              onRemoveTag={handleRemoveTag}
            />
          </Box>
        </Grid>
      </Grid>

      <Menu
        open={Boolean(contextMenu)}
        anchorEl={contextMenu?.anchorEl}
        onClose={() => setContextMenu(null)}
      >
        <MenuItem
          onClick={() => {
            setCreateDialogOpen(true);
            setContextMenu({ ...contextMenu!, anchorEl: null });
          }}
        >
          Create Child Node
        </MenuItem>
        <MenuItem
          onClick={() => {
            setRenameDialogOpen(true);
            setContextMenu({ ...contextMenu!, anchorEl: null });
          }}
        >
          Rename
        </MenuItem>
        <MenuItem
          onClick={() => {
            setDeleteDialogOpen(true);
            setContextMenu({ ...contextMenu!, anchorEl: null });
          }}
        >
          Delete
        </MenuItem>
      </Menu>

      {contextMenu && (
        <>
          <CreateNodeDialog
            open={createDialogOpen}
            onClose={() => {
              setCreateDialogOpen(false);
              setContextMenu(null);
            }}
            onCreate={handleCreateNode}
            parentName={contextMenu.nodeName}
          />

          <RenameNodeDialog
            open={renameDialogOpen}
            onClose={() => {
              setRenameDialogOpen(false);
              setContextMenu(null);
            }}
            onRename={handleRenameNode}
            currentName={contextMenu.nodeName}
            nodeType={contextMenu.nodeType}
          />

          <ConfirmDialog
            open={deleteDialogOpen}
            onClose={() => {
              setDeleteDialogOpen(false);
              setContextMenu(null);
            }}
            onConfirm={handleDeleteNode}
            title="Delete Node"
            message={`Are you sure you want to delete "${contextMenu.nodeName}" and all its children? This action cannot be undone.`}
            confirmText="Delete"
            danger
          />
        </>
      )}

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

