import React, { useState } from 'react';
import {
  Paper,
  Typography,
  Box,
  Chip,
  Stack,
  Divider,
  Button,
} from '@mui/material';
import { Add as AddIcon, Close as CloseIcon } from '@mui/icons-material';
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

  if (!node) {
    return (
      <Paper elevation={2} sx={{ p: 3, height: '100%' }}>
        <Typography variant="body2" color="text.secondary">
          Select a node to view details
        </Typography>
      </Paper>
    );
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <>
      <Paper elevation={2} sx={{ p: 3, height: '100%', overflow: 'auto' }}>
        <Typography variant="h6" gutterBottom>
          {node.name}
        </Typography>

        <Stack spacing={2}>
          <Box>
            <Typography variant="caption" color="text.secondary">
              Type
            </Typography>
            <Typography variant="body2">{node.type}</Typography>
          </Box>

          <Box>
            <Typography variant="caption" color="text.secondary">
              Path
            </Typography>
            <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
              {node.path}
            </Typography>
          </Box>

          <Box>
            <Typography variant="caption" color="text.secondary">
              ID
            </Typography>
            <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
              {node.id}
            </Typography>
          </Box>

          {node.parentId && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Parent ID
              </Typography>
              <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                {node.parentId}
              </Typography>
            </Box>
          )}

          {node.position !== undefined && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Position
              </Typography>
              <Typography variant="body2">{node.position}</Typography>
            </Box>
          )}

          <Box>
            <Typography variant="caption" color="text.secondary">
              Created
            </Typography>
            <Typography variant="body2">{formatDate(node.createdAt)}</Typography>
          </Box>

          <Box>
            <Typography variant="caption" color="text.secondary">
              Updated
            </Typography>
            <Typography variant="body2">{formatDate(node.updatedAt)}</Typography>
          </Box>

          {node.version !== undefined && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Version
              </Typography>
              <Typography variant="body2">{node.version}</Typography>
            </Box>
          )}

          <Divider />

          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="subtitle2">Tags</Typography>
              <Button
                size="small"
                startIcon={<AddIcon />}
                onClick={() => setAddTagOpen(true)}
              >
                Add Tag
              </Button>
            </Box>
            {node.tags && Object.keys(node.tags).length > 0 ? (
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {Object.entries(node.tags).map(([key, value]) => (
                  <Chip
                    key={key}
                    label={`${key}: ${value}`}
                    size="small"
                    onDelete={() => setTagToDelete(key)}
                    deleteIcon={<CloseIcon />}
                  />
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No tags
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

