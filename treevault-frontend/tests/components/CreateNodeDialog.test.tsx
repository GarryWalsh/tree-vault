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
});

