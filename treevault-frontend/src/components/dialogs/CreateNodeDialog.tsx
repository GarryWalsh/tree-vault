import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
} from '@mui/material';

interface CreateNodeDialogProps {
  open: boolean;
  onClose: () => void;
  onCreate: (name: string, type: 'FOLDER' | 'FILE') => void;
  parentName?: string;
}

export const CreateNodeDialog: React.FC<CreateNodeDialogProps> = ({
  open,
  onClose,
  onCreate,
  parentName,
}) => {
  const [name, setName] = useState('');
  const [type, setType] = useState<'FOLDER' | 'FILE'>('FOLDER');

  const handleSubmit = () => {
    if (name.trim()) {
      onCreate(name.trim(), type);
      setName('');
      setType('FOLDER');
      onClose();
    }
  };

  const handleClose = () => {
    setName('');
    setType('FOLDER');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Create New {type === 'FOLDER' ? 'Folder' : 'File'}
        {parentName && ` in "${parentName}"`}
      </DialogTitle>
      <DialogContent>
        <FormControl component="fieldset" sx={{ mb: 2, mt: 1 }}>
          <FormLabel component="legend">Type</FormLabel>
          <RadioGroup
            row
            value={type}
            onChange={(e) => setType(e.target.value as 'FOLDER' | 'FILE')}
          >
            <FormControlLabel value="FOLDER" control={<Radio />} label="Folder" />
            <FormControlLabel value="FILE" control={<Radio />} label="File" />
          </RadioGroup>
        </FormControl>
        <TextField
          autoFocus
          fullWidth
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyPress={(e) => {
            if (e.key === 'Enter') {
              handleSubmit();
            }
          }}
          placeholder={`Enter ${type.toLowerCase()} name`}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={!name.trim()}>
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
};

