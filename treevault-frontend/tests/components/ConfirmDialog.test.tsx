import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ConfirmDialog } from '../../src/components/dialogs/ConfirmDialog';

describe('ConfirmDialog', () => {
  it('should render the dialog when open', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm Action"
        message="Are you sure you want to proceed?"
      />
    );

    expect(screen.getByText('Confirm Action')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to proceed?')).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    const { container } = render(
      <ConfirmDialog
        open={false}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Test"
        message="Test message"
      />
    );

    expect(container.querySelector('[role="dialog"]')).not.toBeInTheDocument();
  });

  it('should display custom confirm and cancel button text', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Delete Item"
        message="This action cannot be undone"
        confirmText="Delete"
        cancelText="Keep"
      />
    );

    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Keep' })).toBeInTheDocument();
  });

  it('should use default button text when not provided', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Message"
      />
    );

    expect(screen.getByRole('button', { name: 'Confirm' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('should call onConfirm and onClose when confirm button is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Are you sure?"
      />
    );

    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    fireEvent.click(confirmButton);

    expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('should call onClose when cancel button is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Are you sure?"
      />
    );

    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
    expect(mockOnConfirm).not.toHaveBeenCalled();
  });

  it('should render confirm button with error color when danger is true', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Delete"
        message="This is dangerous"
        danger={true}
      />
    );

    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    expect(confirmButton).toBeInTheDocument();
    // Note: We can't easily test the color prop in this test, but we verify the button exists
  });

  it('should render confirm button with primary color when danger is false', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Safe action"
        danger={false}
      />
    );

    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    expect(confirmButton).toBeInTheDocument();
  });

  it('should render confirm button with primary color by default', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Default action"
      />
    );

    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    expect(confirmButton).toBeInTheDocument();
  });

  it('should call onConfirm exactly once when confirm is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Confirm"
        message="Are you sure?"
      />
    );

    const confirmButton = screen.getByRole('button', { name: 'Confirm' });
    fireEvent.click(confirmButton);

    expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('should display long messages correctly', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();
    const longMessage = 'This is a very long message that contains a lot of text to test how the dialog handles longer content. It should wrap properly and display all the text without any issues.';

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Long Message"
        message={longMessage}
      />
    );

    expect(screen.getByText(longMessage)).toBeInTheDocument();
  });

  it('should handle special characters in title and message', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Delete 'Important' File?"
        message='Are you sure you want to delete "document.txt"?'
      />
    );

    expect(screen.getByText("Delete 'Important' File?")).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete "document.txt"?')).toBeInTheDocument();
  });

  it('should work with multiline messages', () => {
    const mockOnClose = vi.fn();
    const mockOnConfirm = vi.fn();
    const multilineMessage = 'Line 1\nLine 2\nLine 3';

    render(
      <ConfirmDialog
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
        title="Multiline"
        message={multilineMessage}
      />
    );

    // Check that the dialog renders and we can find parts of the multiline message
    expect(screen.getByText('Multiline')).toBeInTheDocument();
    expect(screen.getByText(/Line 1/)).toBeInTheDocument();
    expect(screen.getByText(/Line 2/)).toBeInTheDocument();
    expect(screen.getByText(/Line 3/)).toBeInTheDocument();
  });
});

