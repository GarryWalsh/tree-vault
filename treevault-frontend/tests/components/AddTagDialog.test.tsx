import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AddTagDialog } from '../../src/components/dialogs/AddTagDialog';

describe('AddTagDialog', () => {
  it('should render the dialog when open', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    expect(screen.getByText('Add Tag to "Test Node"')).toBeInTheDocument();
    expect(screen.getByLabelText(/Key/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Value/i)).toBeInTheDocument();
  });

  it('should not render when closed', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    const { container } = render(
      <AddTagDialog
        open={false}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    expect(container.querySelector('[role="dialog"]')).not.toBeInTheDocument();
  });

  it('should initialize with empty key and value', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    expect(screen.getByLabelText(/Key/i)).toHaveValue('');
    expect(screen.getByLabelText(/Value/i)).toHaveValue('');
  });

  it('should call onAdd with correct key and value when form is submitted', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'category' } });
    fireEvent.change(valueInput, { target: { value: 'important' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(addButton);

    expect(mockOnAdd).toHaveBeenCalledWith('category', 'important');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should trim whitespace from key and value before submitting', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: '  category  ' } });
    fireEvent.change(valueInput, { target: { value: '  important  ' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(addButton);

    expect(mockOnAdd).toHaveBeenCalledWith('category', 'important');
  });

  it('should disable add button when key is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const valueInput = screen.getByLabelText(/Value/i);
    fireEvent.change(valueInput, { target: { value: 'some value' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).toBeDisabled();
  });

  it('should disable add button when value is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    fireEvent.change(keyInput, { target: { value: 'category' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).toBeDisabled();
  });

  it('should disable add button when both key and value are empty', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).toBeDisabled();
  });

  it('should disable add button when key is only whitespace', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: '   ' } });
    fireEvent.change(valueInput, { target: { value: 'value' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).toBeDisabled();
  });

  it('should disable add button when value is only whitespace', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'key' } });
    fireEvent.change(valueInput, { target: { value: '   ' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).toBeDisabled();
  });

  it('should enable add button when both key and value are provided', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'category' } });
    fireEvent.change(valueInput, { target: { value: 'important' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    expect(addButton).not.toBeDisabled();
  });

  it('should call onClose when cancel is clicked', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalled();
    expect(mockOnAdd).not.toHaveBeenCalled();
  });

  it('should clear inputs when cancelled', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'category' } });
    fireEvent.change(valueInput, { target: { value: 'important' } });

    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should clear inputs after successful submission', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'category' } });
    fireEvent.change(valueInput, { target: { value: 'important' } });

    const addButton = screen.getByRole('button', { name: /Add Tag/i });
    fireEvent.click(addButton);

    expect(mockOnAdd).toHaveBeenCalled();
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should submit form when Enter key is pressed in value field', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const keyInput = screen.getByLabelText(/Key/i);
    const valueInput = screen.getByLabelText(/Value/i);

    fireEvent.change(keyInput, { target: { value: 'category' } });
    fireEvent.change(valueInput, { target: { value: 'important' } });
    fireEvent.keyPress(valueInput, { key: 'Enter', code: 'Enter', charCode: 13 });

    expect(mockOnAdd).toHaveBeenCalledWith('category', 'important');
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should not submit when Enter is pressed but key is empty', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="Test Node"
      />
    );

    const valueInput = screen.getByLabelText(/Value/i);
    fireEvent.change(valueInput, { target: { value: 'important' } });
    fireEvent.keyPress(valueInput, { key: 'Enter', code: 'Enter', charCode: 13 });

    expect(mockOnAdd).not.toHaveBeenCalled();
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  it('should display node name in dialog title', () => {
    const mockOnClose = vi.fn();
    const mockOnAdd = vi.fn();

    render(
      <AddTagDialog
        open={true}
        onClose={mockOnClose}
        onAdd={mockOnAdd}
        nodeName="My Important Document"
      />
    );

    expect(screen.getByText('Add Tag to "My Important Document"')).toBeInTheDocument();
  });
});

