import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { NodeDetailsPanel } from '../../src/components/common/NodeDetailsPanel';
import { NodeResponse } from '../../src/api/types';

describe('NodeDetailsPanel', () => {
  const mockNode: NodeResponse = {
    id: '123',
    name: 'Test Node',
    type: 'FILE',
    path: '/Root/Test Node',
    createdAt: '2025-01-01T10:00:00',
    updatedAt: '2025-01-02T15:30:00',
    parentId: 'parent-123',
    position: 0,
    version: 1,
    tags: {
      category: 'important',
      status: 'active',
    },
  };

  it('should display placeholder when no node is selected', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={null}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Select a node to view its details')).toBeInTheDocument();
  });

  it('should display node name', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Test Node')).toBeInTheDocument();
  });

  it('should display node type', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('FILE')).toBeInTheDocument();
  });

  it('should display node path', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('PATH')).toBeInTheDocument();
    expect(screen.getByText('/Root/Test Node')).toBeInTheDocument();
  });

  it('should display node ID', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('123')).toBeInTheDocument();
  });

  it('should display parent ID when present', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Parent ID')).toBeInTheDocument();
    expect(screen.getByText('parent-123')).toBeInTheDocument();
  });

  it('should not display parent ID section when not present', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const nodeWithoutParent = { ...mockNode, parentId: undefined };

    render(
      <NodeDetailsPanel
        node={nodeWithoutParent}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    // Parent ID label should not be present
    const parentIdTexts = screen.queryAllByText('Parent ID');
    expect(parentIdTexts).toHaveLength(0);
  });

  it('should display position when present', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Position')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('should display formatted dates', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Created')).toBeInTheDocument();
    expect(screen.getByText('Updated')).toBeInTheDocument();
    // Note: The exact formatted date will depend on locale, so we just check the labels exist
  });

  it('should display version when present', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Version')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('should display tags section', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('Tags')).toBeInTheDocument();
    expect(screen.getByText(/category: important/i)).toBeInTheDocument();
    expect(screen.getByText(/status: active/i)).toBeInTheDocument();
  });

  it('should display "No tags" message when node has no tags', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const nodeWithoutTags = { ...mockNode, tags: {} };

    render(
      <NodeDetailsPanel
        node={nodeWithoutTags}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('No tags added yet')).toBeInTheDocument();
  });

  it('should display "No tags" message when tags is undefined', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const nodeWithoutTags = { ...mockNode, tags: undefined };

    render(
      <NodeDetailsPanel
        node={nodeWithoutTags}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('No tags added yet')).toBeInTheDocument();
  });

  it('should open AddTagDialog when Add Tag button is clicked', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    const addTagButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(addTagButton);

    // The AddTagDialog should now be open and display the node name
    expect(screen.getByText(`Add Tag to "Test Node"`)).toBeInTheDocument();
  });

  it('should call onAddTag when tag is added through AddTagDialog', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    // Open dialog
    const addTagButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(addTagButton);

    // Fill in tag details
    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);
    fireEvent.change(keyInput, { target: { value: 'priority' } });
    fireEvent.change(valueInput, { target: { value: 'high' } });

    // Submit
    const submitButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(submitButton);

    expect(mockOnAddTag).toHaveBeenCalledWith('priority', 'high');
  });

  it('should open ConfirmDialog when tag delete button is clicked', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    // Find and click the delete button on one of the tags
    const chips = screen.getAllByRole('button');
    const categoryChipButton = chips.find(chip => chip.textContent?.includes('category: important'));
    
    if (categoryChipButton) {
      fireEvent.click(categoryChipButton.querySelector('svg') || categoryChipButton);
    }

    // The confirm dialog should now be visible
    expect(screen.getByText('Remove Tag')).toBeInTheDocument();
  });

  it('should call onRemoveTag when tag removal is confirmed', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();

    render(
      <NodeDetailsPanel
        node={mockNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    // Get the tag chip and click its delete button
    const tags = screen.getAllByRole('button').filter(btn => 
      btn.textContent?.includes('category') || btn.textContent?.includes('status')
    );
    
    // Click delete on the first tag chip
    if (tags[0]) {
      const svg = tags[0].querySelector('svg');
      if (svg) {
        fireEvent.click(svg);
      }
    }

    // Now confirm the deletion
    const removeButton = screen.getByRole('button', { name: /Remove/i });
    fireEvent.click(removeButton);

    expect(mockOnRemoveTag).toHaveBeenCalled();
  });

  it('should display folder type correctly', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const folderNode = { ...mockNode, type: 'FOLDER' as const };

    render(
      <NodeDetailsPanel
        node={folderNode}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText('FOLDER')).toBeInTheDocument();
  });

  it('should handle node with many tags', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const nodeWithManyTags = {
      ...mockNode,
      tags: {
        tag1: 'value1',
        tag2: 'value2',
        tag3: 'value3',
        tag4: 'value4',
        tag5: 'value5',
      },
    };

    render(
      <NodeDetailsPanel
        node={nodeWithManyTags}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText(/tag1: value1/i)).toBeInTheDocument();
    expect(screen.getByText(/tag2: value2/i)).toBeInTheDocument();
    expect(screen.getByText(/tag3: value3/i)).toBeInTheDocument();
    expect(screen.getByText(/tag4: value4/i)).toBeInTheDocument();
    expect(screen.getByText(/tag5: value5/i)).toBeInTheDocument();
  });

  it('should handle node with long path', () => {
    const mockOnAddTag = vi.fn();
    const mockOnRemoveTag = vi.fn();
    const nodeWithLongPath = {
      ...mockNode,
      path: '/Root/Very/Long/Path/With/Many/Nested/Folders/And/More/Levels/Test Node',
    };

    render(
      <NodeDetailsPanel
        node={nodeWithLongPath}
        onAddTag={mockOnAddTag}
        onRemoveTag={mockOnRemoveTag}
      />
    );

    expect(screen.getByText(nodeWithLongPath.path)).toBeInTheDocument();
  });
});

