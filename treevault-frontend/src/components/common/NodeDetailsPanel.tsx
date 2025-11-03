import React, { useState } from 'react';
import {
  Paper,
  Typography,
  Box,
  Chip,
  Stack,
  Divider,
  Button,
  Card,
  CardContent,
} from '@mui/material';
import { 
  Add as AddIcon, 
  Close as CloseIcon,
  Folder as FolderIcon,
  InsertDriveFile as FileIcon,
} from '@mui/icons-material';
import { NodeResponse } from '../../api/types';
import { AddTagDialog } from '../dialogs/AddTagDialog';
import { ConfirmDialog } from '../dialogs/ConfirmDialog';

interface NodeDetailsPanelProps {
  node: NodeResponse | null;
  onAddTag: (key: string, value: string) => void;
  onRemoveTag: (key: string) => void;
}

export const NodeDetailsPanel: React.FC<NodeDetailsPanelProps> = ({
  node,
  onAddTag,
  onRemoveTag,
}) => {
  const [addTagOpen, setAddTagOpen] = useState(false);
  const [tagToDelete, setTagToDelete] = useState<string | null>(null);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!node) {
    return (
      <Paper elevation={2} sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
          Node Details
        </Typography>

        <Box
          sx={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            gap: 0.5,
          }}
        >
          <Typography variant="h6" color="text.secondary" sx={{ fontWeight: 400 }}>
            No node selected
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Select a node to view its details
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <>
      <Paper elevation={2} sx={{ p: 2, height: '100%', overflow: 'auto', display: 'flex', flexDirection: 'column' }}>
        {/* Header Section */}
        <Box sx={{ mb: 1.5 }}>
          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
            Node Details
          </Typography>
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
            {node.type === 'FOLDER' ? (
              <FolderIcon color="primary" sx={{ fontSize: 28 }} />
            ) : (
              <FileIcon color="action" sx={{ fontSize: 28 }} />
            )}
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" sx={{ wordBreak: 'break-word', mb: 0.5 }}>
                {node.name}
              </Typography>
              <Chip 
                label={node.type} 
                size="small" 
                color={node.type === 'FOLDER' ? 'primary' : 'default'}
                sx={{ fontWeight: 500, height: '20px' }}
              />
            </Box>
          </Box>
        </Box>

        <Stack spacing={1.5} sx={{ flex: 1 }}>
          {/* Path Section */}
          <Card variant="outlined" sx={{ bgcolor: 'background.default' }}>
            <CardContent sx={{ py: 1, px: 1.5, '&:last-child': { pb: 1 } }}>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.25, fontWeight: 600 }}>
                PATH
              </Typography>
              <Typography variant="body2" sx={{ wordBreak: 'break-all', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                {node.path}
              </Typography>
            </CardContent>
          </Card>

          {/* Metadata Section */}
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600, fontSize: '0.875rem' }}>
              Metadata
            </Typography>
            <Stack spacing={0.5}>
              <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                  ID
                </Typography>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', wordBreak: 'break-all' }}>
                  {node.id}
                </Typography>
              </Box>

              {node.parentId && (
                <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                    Parent ID
                  </Typography>
                  <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', wordBreak: 'break-all' }}>
                    {node.parentId}
                  </Typography>
                </Box>
              )}

              {node.position !== undefined && (
                <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                    Position
                  </Typography>
                  <Typography variant="body2" sx={{ fontSize: '0.8rem' }}>
                    {node.position}
                  </Typography>
                </Box>
              )}

              <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                  Created
                </Typography>
                <Typography variant="body2" sx={{ fontSize: '0.8rem' }}>
                  {formatDate(node.createdAt)}
                </Typography>
              </Box>

              <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                  Updated
                </Typography>
                <Typography variant="body2" sx={{ fontSize: '0.8rem' }}>
                  {formatDate(node.updatedAt)}
                </Typography>
              </Box>

              {node.version !== undefined && (
                <Box sx={{ display: 'grid', gridTemplateColumns: '80px 1fr', gap: 1, alignItems: 'start' }}>
                  <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, pt: 0.25 }}>
                    Version
                  </Typography>
                  <Typography variant="body2" sx={{ fontSize: '0.8rem' }}>
                    {node.version}
                  </Typography>
                </Box>
              )}
            </Stack>
          </Box>

          <Divider sx={{ my: 0.5 }} />

          {/* Tags Section */}
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.75 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600, fontSize: '0.875rem' }}>
                Tags
              </Typography>
              <Button
                size="small"
                variant="outlined"
                startIcon={<AddIcon />}
                onClick={() => setAddTagOpen(true)}
                sx={{ py: 0.25, px: 1, fontSize: '0.75rem' }}
              >
                Add Tag
              </Button>
            </Box>
            {node.tags && Object.keys(node.tags).length > 0 ? (
              <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                {Object.entries(node.tags).map(([key, value]) => (
                  <Chip
                    key={key}
                    label={`${key}: ${value}`}
                    size="small"
                    onDelete={() => setTagToDelete(key)}
                    deleteIcon={<CloseIcon />}
                    sx={{ fontWeight: 500, height: '24px', fontSize: '0.75rem' }}
                  />
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic', fontSize: '0.8rem' }}>
                No tags added yet
              </Typography>
            )}
          </Box>
        </Stack>
      </Paper>

      <AddTagDialog
        open={addTagOpen}
        onClose={() => setAddTagOpen(false)}
        onAdd={onAddTag}
        nodeName={node.name}
      />

      <ConfirmDialog
        open={tagToDelete !== null}
        onClose={() => setTagToDelete(null)}
        onConfirm={() => {
          if (tagToDelete) {
            onRemoveTag(tagToDelete);
            setTagToDelete(null);
          }
        }}
        title="Remove Tag"
        message={`Are you sure you want to remove the tag "${tagToDelete}"?`}
        confirmText="Remove"
        danger
      />
    </>
  );
};

