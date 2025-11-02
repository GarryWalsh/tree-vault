import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Stack,
} from '@mui/material';

interface AddTagDialogProps {
  open: boolean;
  onClose: () => void;
  onAdd: (key: string, value: string) => void;
  nodeName: string;
}

export const AddTagDialog: React.FC<AddTagDialogProps> = ({
  open,
  onClose,
  onAdd,
  nodeName,
}) => {
  const [key, setKey] = useState('');
  const [value, setValue] = useState('');

  const handleSubmit = () => {
    if (key.trim() && value.trim()) {
      onAdd(key.trim(), value.trim());
      setKey('');
      setValue('');
      onClose();
    }
  };

  const handleClose = () => {
    setKey('');
    setValue('');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Tag to "{nodeName}"</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            autoFocus
            fullWidth
            label="Key"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="Enter tag key"
          />
          <TextField
            fullWidth
            label="Value"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                handleSubmit();
              }
            }}
            placeholder="Enter tag value"
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={!key.trim() || !value.trim()}
        >
          Add Tag
        </Button>
      </DialogActions>
    </Dialog>
  );
};

