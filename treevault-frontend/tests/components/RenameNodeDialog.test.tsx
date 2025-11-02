import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { RenameNodeDialog } from '../../src/components/dialogs/RenameNodeDialog';

describe('RenameNodeDialog', () => {
  it('should render the dialog when open', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    expect(screen.getByText('Rename Folder')).toBeInTheDocument();
    expect(screen.getByLabelText(/New Name/i)).toHaveValue('Old Name');
  });

  it('should render correct title for FILE type', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="file.txt"
        nodeType="FILE"
      />
    );

    expect(screen.getByText('Rename File')).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    const { container } = render(
      <RenameNodeDialog
        open={false}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Test"
        nodeType="FOLDER"
      />
    );

    expect(container.querySelector('[role="dialog"]')).not.toBeInTheDocument();
  });

  it('should initialize input with current name', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="My Folder"
        nodeType="FOLDER"
      />
    );

    expect(screen.getByLabelText(/New Name/i)).toHaveValue('My Folder');
  });

  it('should call onRename with new name when form is submitted', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: 'New Name' } });

    const renameButton = screen.getByRole('button', { name: /Rename/i });
    fireEvent.click(renameButton);

    expect(mockOnRename).toHaveBeenCalledWith('New Name');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should trim whitespace from name before submitting', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: '  New Name  ' } });

    const renameButton = screen.getByRole('button', { name: /Rename/i });
    fireEvent.click(renameButton);

    expect(mockOnRename).toHaveBeenCalledWith('New Name');
  });

  it('should disable rename button when name is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: '' } });

    const renameButton = screen.getByRole('button', { name: /Rename/i });
    expect(renameButton).toBeDisabled();
  });

  it('should disable rename button when name is only whitespace', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: '   ' } });

    const renameButton = screen.getByRole('button', { name: /Rename/i });
    expect(renameButton).toBeDisabled();
  });

  it('should disable rename button when name is unchanged', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Same Name"
        nodeType="FOLDER"
      />
    );

    // Name is already "Same Name" from initialization
    const renameButton = screen.getByRole('button', { name: /Rename/i });
    expect(renameButton).toBeDisabled();
  });

  it('should not call onRename when name is unchanged', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Same Name"
        nodeType="FOLDER"
      />
    );

    const renameButton = screen.getByRole('button', { name: /Rename/i });
    fireEvent.click(renameButton);

    expect(mockOnRename).not.toHaveBeenCalled();
  });

  it('should call onClose when cancel is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Test"
        nodeType="FOLDER"
      />
    );

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalled();
    expect(mockOnRename).not.toHaveBeenCalled();
  });

  it('should reset input to original name when cancelled', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Original Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: 'Changed Name' } });

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should submit form when Enter key is pressed', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Old Name"
        nodeType="FOLDER"
      />
    );

    const input = screen.getByLabelText(/New Name/i);
    fireEvent.change(input, { target: { value: 'New Name' } });
    fireEvent.keyPress(input, { key: 'Enter', code: 'Enter', charCode: 13 });

    expect(mockOnRename).toHaveBeenCalledWith('New Name');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should update input value when currentName prop changes', () => {
    const mockOnClose = vi.fn();
    const mockOnRename = vi.fn();

    const { rerender } = render(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Name 1"
        nodeType="FOLDER"
      />
    );

    expect(screen.getByLabelText(/New Name/i)).toHaveValue('Name 1');

    rerender(
      <RenameNodeDialog
        open={true}
        onClose={mockOnClose}
        onRename={mockOnRename}
        currentName="Name 2"
        nodeType="FOLDER"
      />
    );

    expect(screen.getByLabelText(/New Name/i)).toHaveValue('Name 2');
  });
});

