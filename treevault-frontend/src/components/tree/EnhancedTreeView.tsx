import React, { useCallback, useEffect, useState } from 'react';
import { SimpleTreeView } from '@mui/x-tree-view';
import {
  Box,
  Paper,
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
  Refresh as RefreshIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useTreeStore } from '../../store/treeStore';
import { CreateNodeDialog } from '../dialogs/CreateNodeDialog';
import { RenameNodeDialog } from '../dialogs/RenameNodeDialog';
import { ConfirmDialog } from '../dialogs/ConfirmDialog';
import { NodeDetailsPanel } from '../common/NodeDetailsPanel';
import { TreeNode } from './TreeNode';
import { useDragAndDrop } from '../../hooks/useDragAndDrop';
import { useNodeOperations } from '../../hooks/useNodeOperations';

export const EnhancedTreeView: React.FC = () => {
  const [contextMenu, setContextMenu] = useState<{
    nodeId: string;
    nodeName: string;
    nodeType: 'FOLDER' | 'FILE';
    anchorEl: HTMLElement | null;
  } | null>(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
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
  } = useTreeStore();

  const showSnackbar = useCallback((message: string, severity: 'success' | 'error' | 'info' = 'info') => {
    setSnackbar({ open: true, message, severity });
  }, []);

  const handleSuccess = useCallback((message: string) => {
    showSnackbar(message, 'success');
  }, [showSnackbar]);

  const handleError = useCallback((message: string) => {
    showSnackbar(message, 'error');
  }, [showSnackbar]);

  // Use custom hooks
  const {
    tree,
    loadTree,
    handleCreateNode: createNode,
    handleRenameNode: renameNode,
    handleDeleteNode: deleteNode,
    handleAddTag,
    handleRemoveTag,
    handleMoveNode,
    findNodeById,
    getSelectedNode,
  } = useNodeOperations({
    onSuccess: handleSuccess,
    onError: handleError,
  });

  const {
    dropTarget,
    dropPosition,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDrop: performDrop,
  } = useDragAndDrop();

  useEffect(() => {
    loadTree();
  }, [loadTree]);

  const handleCreateNodeDialog = async (name: string, type: 'FOLDER' | 'FILE') => {
    if (!contextMenu) return;
    await createNode(name, type, contextMenu.nodeId);
  };

  const handleRenameNodeDialog = async (newName: string) => {
    if (!contextMenu) return;
    await renameNode(contextMenu.nodeId, newName);
    // Update context menu with the new name to avoid stale state
    setContextMenu({
      ...contextMenu,
      nodeName: newName
    });
  };

  const handleDeleteNodeDialog = async () => {
    if (!contextMenu) return;
    await deleteNode(contextMenu.nodeId);
  };

  const handleDropWrapper = async (
    e: React.DragEvent,
    targetNode: any,
    parentNode: any
  ) => {
    await performDrop(
      e,
      targetNode,
      parentNode,
      handleMoveNode,
      (message) => showSnackbar(message, 'error')
    );
  };

  const handleContextMenu = (
    nodeId: string,
    nodeName: string,
    nodeType: 'FOLDER' | 'FILE',
    anchorEl: HTMLElement
  ) => {
    setContextMenu({ nodeId, nodeName, nodeType, anchorEl });
  };

  // Wrapper functions for tag operations that include the selected node ID
  const handleAddTagWrapper = async (key: string, value: string) => {
    if (selectedNodeId) {
      await handleAddTag(selectedNodeId, key, value);
    }
  };

  const handleRemoveTagWrapper = async (key: string) => {
    if (selectedNodeId) {
      await handleRemoveTag(selectedNodeId, key);
    }
  };

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
              <TreeNode
                node={tree.root}
                parentNode={null}
                dropTargetId={dropTarget}
                dropPosition={dropPosition}
                onDragStart={handleDragStart}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDropWrapper}
                onContextMenu={handleContextMenu}
              />
            </SimpleTreeView>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Box sx={{ height: '600px' }}>
            <NodeDetailsPanel
              node={getSelectedNode()}
              onAddTag={handleAddTagWrapper}
              onRemoveTag={handleRemoveTagWrapper}
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
            // Get fresh node data from the tree before opening dialog
            if (contextMenu && tree) {
              const currentNode = findNodeById(contextMenu.nodeId);
              if (currentNode) {
                setContextMenu({
                  nodeId: currentNode.id,
                  nodeName: currentNode.name,
                  nodeType: currentNode.type,
                  anchorEl: null
                });
              }
            }
            setCreateDialogOpen(true);
          }}
        >
          Create Child Node
        </MenuItem>
        <MenuItem
          onClick={() => {
            // Get fresh node data from the tree before opening dialog
            if (contextMenu && tree) {
              const currentNode = findNodeById(contextMenu.nodeId);
              if (currentNode) {
                setContextMenu({
                  nodeId: currentNode.id,
                  nodeName: currentNode.name,
                  nodeType: currentNode.type,
                  anchorEl: null
                });
              }
            }
            setRenameDialogOpen(true);
          }}
        >
          Rename
        </MenuItem>
        <MenuItem
          onClick={() => {
            // Get fresh node data from the tree before opening dialog
            if (contextMenu && tree) {
              const currentNode = findNodeById(contextMenu.nodeId);
              if (currentNode) {
                setContextMenu({
                  nodeId: currentNode.id,
                  nodeName: currentNode.name,
                  nodeType: currentNode.type,
                  anchorEl: null
                });
              }
            }
            setDeleteDialogOpen(true);
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
            onCreate={handleCreateNodeDialog}
            parentName={contextMenu.nodeName}
          />

          <RenameNodeDialog
            open={renameDialogOpen}
            onClose={() => {
              setRenameDialogOpen(false);
              setContextMenu(null);
            }}
            onRename={handleRenameNodeDialog}
            currentName={contextMenu.nodeName}
            nodeType={contextMenu.nodeType}
          />

          <ConfirmDialog
            open={deleteDialogOpen}
            onClose={() => {
              setDeleteDialogOpen(false);
              setContextMenu(null);
            }}
            onConfirm={handleDeleteNodeDialog}
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
