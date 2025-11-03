import { useState } from 'react';
import { NodeResponse } from '../api/types';

export interface DragAndDropState {
  draggedNode: NodeResponse | null;
  dropTarget: string | null;
  dropPosition: 'into' | 'before' | 'after' | null;
}

export interface DragAndDropHandlers {
  handleDragStart: (e: React.DragEvent, node: NodeResponse) => void;
  handleDragOver: (e: React.DragEvent, node: NodeResponse) => void;
  handleDragLeave: (e: React.DragEvent) => void;
  handleDrop: (
    e: React.DragEvent,
    targetNode: NodeResponse,
    parentNode: NodeResponse | null,
    onMove: (nodeId: string, newParentId: string, position: number) => Promise<void>,
    onError: (message: string) => void
  ) => Promise<void>;
  resetDragState: () => void;
  isDescendant: (parent: NodeResponse, potentialDescendant: NodeResponse) => boolean;
}

export const useDragAndDrop = (): DragAndDropState & DragAndDropHandlers => {
  const [draggedNode, setDraggedNode] = useState<NodeResponse | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const [dropPosition, setDropPosition] = useState<'into' | 'before' | 'after' | null>(null);

  const isDescendant = (parent: NodeResponse, potentialDescendant: NodeResponse): boolean => {
    if (parent.id === potentialDescendant.id) return true;
    if (!parent.children) return false;
    return parent.children.some((child) => isDescendant(child, potentialDescendant));
  };

  const handleDragStart = (e: React.DragEvent, node: NodeResponse) => {
    e.stopPropagation();
    setDraggedNode(node);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent, node: NodeResponse) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (!draggedNode || draggedNode.id === node.id) {
      return;
    }

    // Get the bounding rectangle of the target element
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const mouseY = e.clientY - rect.top;
    const elementHeight = rect.height;
    
    // Determine drop position based on mouse position
    // Top 25% = before, Bottom 25% = after, Middle 50% = into (if folder)
    const topThreshold = elementHeight * 0.25;
    const bottomThreshold = elementHeight * 0.75;
    
    let position: 'into' | 'before' | 'after';
    
    if (mouseY < topThreshold) {
      position = 'before';
    } else if (mouseY > bottomThreshold) {
      position = 'after';
    } else if (node.type === 'FOLDER') {
      position = 'into';
    } else {
      // For files, prefer before/after based on which is closer
      position = mouseY < elementHeight / 2 ? 'before' : 'after';
    }
    
    setDropTarget(node.id);
    setDropPosition(position);
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDropTarget(null);
    setDropPosition(null);
  };

  const handleDrop = async (
    e: React.DragEvent,
    targetNode: NodeResponse,
    parentNode: NodeResponse | null,
    onMove: (nodeId: string, newParentId: string, position: number) => Promise<void>,
    onError: (message: string) => void
  ) => {
    e.preventDefault();
    e.stopPropagation();
    
    const currentDropPosition = dropPosition;
    setDropTarget(null);
    setDropPosition(null);

    console.log('Drop event:', { 
      draggedNode: draggedNode ? { id: draggedNode.id, name: draggedNode.name, type: draggedNode.type } : null,
      targetNode: { id: targetNode.id, name: targetNode.name, type: targetNode.type },
      dropPosition: currentDropPosition,
      parentNode: parentNode ? { id: parentNode.id, name: parentNode.name } : null
    });

    if (!draggedNode || draggedNode.id === targetNode.id) {
      console.log('Drop cancelled: no dragged node or same node');
      setDraggedNode(null);
      return;
    }

    try {
      if (currentDropPosition === 'into') {
        // Drop INTO folder
        if (targetNode.type !== 'FOLDER') {
          console.log('Drop rejected: target is not a folder');
          onError('Can only move nodes into folders');
          setDraggedNode(null);
          return;
        }

        // Prevent dropping a folder into itself or its descendants
        if (isDescendant(draggedNode, targetNode)) {
          console.log('Drop rejected: circular reference detected');
          onError('Cannot move a folder into itself or its descendants');
          setDraggedNode(null);
          return;
        }

        // Calculate position (append to end)
        const position = targetNode.children ? targetNode.children.length : 0;
        console.log('Proceeding with move INTO folder, position:', position);

        await onMove(draggedNode.id, targetNode.id, position);
      } else if (currentDropPosition === 'before' || currentDropPosition === 'after') {
        // Drop BEFORE or AFTER sibling (reordering)
        if (!parentNode) {
          console.log('Drop rejected: no parent node for sibling reordering');
          onError('Cannot reorder root level nodes');
          setDraggedNode(null);
          return;
        }

        // Prevent dropping a folder into its own descendant
        if (isDescendant(draggedNode, parentNode)) {
          console.log('Drop rejected: cannot move to descendant');
          onError('Cannot move a folder into its own descendant');
          setDraggedNode(null);
          return;
        }

        // Find target node position in parent's children
        const targetIndex = parentNode.children?.findIndex(child => child.id === targetNode.id) ?? -1;
        
        if (targetIndex === -1) {
          console.log('Drop rejected: target not found in parent children');
          onError('Failed to determine drop position');
          setDraggedNode(null);
          return;
        }

        // Calculate new position
        let newPosition = currentDropPosition === 'before' ? targetIndex : targetIndex + 1;
        
        // If moving within same parent, adjust for removal of dragged node
        if (draggedNode.parentId === parentNode.id) {
          const draggedIndex = parentNode.children?.findIndex(child => child.id === draggedNode.id) ?? -1;
          if (draggedIndex !== -1 && draggedIndex < newPosition) {
            newPosition--;
          }
        }

        console.log('Proceeding with sibling reorder:', {
          draggedNode: draggedNode.name,
          targetNode: targetNode.name,
          position: currentDropPosition,
          newPosition,
          parentId: parentNode.id
        });

        await onMove(draggedNode.id, parentNode.id, newPosition);
      }
    } finally {
      setDraggedNode(null);
    }
  };

  const resetDragState = () => {
    setDraggedNode(null);
    setDropTarget(null);
    setDropPosition(null);
  };

  return {
    draggedNode,
    dropTarget,
    dropPosition,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    resetDragState,
    isDescendant,
  };
};


