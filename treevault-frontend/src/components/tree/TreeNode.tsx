import React from 'react';
import { TreeItem } from '@mui/x-tree-view';
import { Box, IconButton } from '@mui/material';
import { Folder, InsertDriveFile, MoreVert } from '@mui/icons-material';
import { NodeResponse } from '../../api/types';

interface TreeNodeProps {
  node: NodeResponse;
  parentNode: NodeResponse | null;
  dropTargetId: string | null;
  dropPosition: 'into' | 'before' | 'after' | null;
  onDragStart: (e: React.DragEvent, node: NodeResponse) => void;
  onDragOver: (e: React.DragEvent, node: NodeResponse) => void;
  onDragLeave: (e: React.DragEvent) => void;
  onDrop: (e: React.DragEvent, targetNode: NodeResponse, parentNode: NodeResponse | null) => void;
  onContextMenu: (nodeId: string, nodeName: string, nodeType: 'FOLDER' | 'FILE', anchorEl: HTMLElement) => void;
  searchQuery?: string;
}

export const TreeNode: React.FC<TreeNodeProps> = ({
  node,
  parentNode,
  dropTargetId,
  dropPosition,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onContextMenu,
  searchQuery = '',
}) => {
  // Highlight search matches in node name
  const highlightText = (text: string, query: string) => {
    if (!query.trim()) return text;
    // Escape special regex characters
    const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const parts = text.split(new RegExp(`(${escapedQuery})`, 'gi'));
    const lowerQuery = query.toLowerCase();
    return (
      <span>
        {parts.map((part, i) =>
          part.toLowerCase() === lowerQuery ? (
            <mark key={i} style={{ backgroundColor: 'yellow', padding: '0 2px', borderRadius: '2px' }}>
              {part}
            </mark>
          ) : (
            part
          )
        )}
      </span>
    );
  };
  const isDropTarget = dropTargetId === node.id;
  const showDropBefore = isDropTarget && dropPosition === 'before';
  const showDropAfter = isDropTarget && dropPosition === 'after';
  const showDropInto = isDropTarget && dropPosition === 'into';

  return (
    <TreeItem
      key={node.id}
      itemId={node.id}
      label={
        <Box sx={{ position: 'relative' }}>
          {/* Drop indicator - BEFORE */}
          {showDropBefore && (
            <Box
              sx={{
                position: 'absolute',
                top: -2,
                left: 0,
                right: 0,
                height: 4,
                backgroundColor: 'primary.main',
                borderRadius: 1,
                zIndex: 1000,
              }}
            />
          )}

          {/* Main node content */}
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              backgroundColor: showDropInto ? 'primary.light' : 'transparent',
              borderRadius: 1,
              transition: 'background-color 0.2s',
              cursor: 'grab',
              padding: '4px 8px',
              position: 'relative',
              '&:active': {
                cursor: 'grabbing',
              },
            }}
            draggable
            onDragStart={(e) => onDragStart(e, node)}
            onDragOver={(e) => onDragOver(e, node)}
            onDragLeave={onDragLeave}
            onDrop={(e) => onDrop(e, node, parentNode)}
          >
            {node.type === 'FOLDER' ? <Folder /> : <InsertDriveFile />}
            <span>{highlightText(node.name, searchQuery)}</span>
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                onContextMenu(node.id, node.name, node.type, e.currentTarget);
              }}
            >
              <MoreVert fontSize="small" />
            </IconButton>
          </Box>

          {/* Drop indicator - AFTER */}
          {showDropAfter && (
            <Box
              sx={{
                position: 'absolute',
                bottom: -2,
                left: 0,
                right: 0,
                height: 4,
                backgroundColor: 'primary.main',
                borderRadius: 1,
                zIndex: 1000,
              }}
            />
          )}
        </Box>
      }
    >
      {node.children?.map(child => (
        <TreeNode
          key={child.id}
          node={child}
          parentNode={node}
          dropTargetId={dropTargetId}
          dropPosition={dropPosition}
          onDragStart={onDragStart}
          onDragOver={onDragOver}
          onDragLeave={onDragLeave}
          onDrop={onDrop}
          onContextMenu={onContextMenu}
          searchQuery={searchQuery}
        />
      ))}
    </TreeItem>
  );
};

