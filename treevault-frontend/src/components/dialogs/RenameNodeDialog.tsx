import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
} from '@mui/material';

interface RenameNodeDialogProps {
  open: boolean;
  onClose: () => void;
  onRename: (newName: string) => void;
  currentName: string;
  nodeType: 'FOLDER' | 'FILE';
}

export const RenameNodeDialog: React.FC<RenameNodeDialogProps> = ({
  open,
  onClose,
  onRename,
  currentName,
  nodeType,
}) => {
  const [name, setName] = useState(currentName);

  useEffect(() => {
    setName(currentName);
  }, [currentName]);

  const handleSubmit = () => {
    if (name.trim() && name.trim() !== currentName) {
      onRename(name.trim());
      onClose();
    }
  };

  const handleClose = () => {
    setName(currentName);
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Rename {nodeType === 'FOLDER' ? 'Folder' : 'File'}</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          label="New Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              handleSubmit();
            }
          }}
          sx={{ mt: 2 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={!name.trim() || name.trim() === currentName}
        >
          Rename
        </Button>
      </DialogActions>
    </Dialog>
  );
};

