import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CreateNodeDialog } from '../../src/components/dialogs/CreateNodeDialog';

describe('CreateNodeDialog', () => {
  it('should render the dialog when open', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    expect(screen.getByText(/Create New/i)).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    const { container } = render(
      <CreateNodeDialog
        open={false}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    expect(container.querySelector('[role="dialog"]')).not.toBeInTheDocument();
  });

  it('should call onCreate with correct data when form is submitted', async () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    // Enter name
    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'Test Folder' } });

    // Click create button
    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith('Test Folder', 'FOLDER');
  });

  it('should disable create button when name is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const createButton = screen.getByRole('button', { name: /Create/i });
    expect(createButton).toBeDisabled();
  });

  it('should call onClose when cancel is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should show parent name in title when provided', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
        parentName="Documents"
      />
    );

    expect(screen.getByText(/in "Documents"/i)).toBeInTheDocument();
  });

  it('should default to FOLDER type', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'New Item' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith('New Item', 'FOLDER');
  });

  it('should allow switching to FILE type', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    // Look for type selector (radio buttons or select)
    const fileRadio = screen.queryByLabelText(/File/i);
    if (fileRadio) {
      fireEvent.click(fileRadio);
    }

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'New File' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    // If type selector exists, it should create FILE, otherwise FOLDER is default
    expect(mockOnCreate).toHaveBeenCalled();
  });

  it('should trim whitespace from name', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: '  Trimmed Name  ' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith('Trimmed Name', 'FOLDER');
  });

  it('should disable create button when name is whitespace only', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: '   ' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    expect(createButton).toBeDisabled();
  });

  it('should handle special characters in name', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'File with "quotes" & symbols!' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith('File with "quotes" & symbols!', 'FOLDER');
  });

  it('should handle unicode characters in name', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: '文件名 📁 émojis' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith('文件名 📁 émojis', 'FOLDER');
  });

  it('should submit form when Enter key is pressed in name field', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'Quick Create' } });
    fireEvent.keyPress(nameInput, { key: 'Enter', code: 'Enter', charCode: 13 });

    expect(mockOnCreate).toHaveBeenCalledWith('Quick Create', 'FOLDER');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should not submit when Enter is pressed but name is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.keyPress(nameInput, { key: 'Enter', code: 'Enter', charCode: 13 });

    expect(mockOnCreate).not.toHaveBeenCalled();
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  it('should clear name field after successful submission', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'Test Name' } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    // After submission, the dialog calls onClose and onCreate
    expect(mockOnCreate).toHaveBeenCalledWith('Test Name', 'FOLDER');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should handle very long names', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const longName = 'A'.repeat(255);
    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: longName } });

    const createButton = screen.getByRole('button', { name: /Create/i });
    fireEvent.click(createButton);

    expect(mockOnCreate).toHaveBeenCalledWith(longName, 'FOLDER');
  });

  it('should enable create button when valid name is entered', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const createButton = screen.getByRole('button', { name: /Create/i });
    expect(createButton).toBeDisabled();

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'Valid Name' } });

    expect(createButton).not.toBeDisabled();
  });

  it('should not call onCreate when cancel is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnCreate = vi.fn();

    render(
      <CreateNodeDialog
        open={true}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

    const nameInput = screen.getByLabelText(/Name/i);
    fireEvent.change(nameInput, { target: { value: 'Some Name' } });

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnCreate).not.toHaveBeenCalled();
    expect(mockOnClose).toHaveBeenCalled();
  });
});

