import { useState, useMemo, useEffect } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Container, Box, Typography, IconButton, Tooltip } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { EnhancedTreeView } from './components/tree/EnhancedTreeView';

function App() {
  // Initialize dark mode from localStorage or default to dark mode
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    const savedMode = localStorage.getItem('darkMode');
    return savedMode ? JSON.parse(savedMode) : true;
  });

  // Save dark mode preference to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('darkMode', JSON.stringify(darkMode));
  }, [darkMode]);

  // Create theme based on dark mode state
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: darkMode ? 'dark' : 'light',
          primary: {
            main: '#1976d2',
          },
        },
      }),
    [darkMode]
  );

  const toggleDarkMode = () => {
    setDarkMode((prev) => !prev);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="xl">
        <Box sx={{ py: 4 }}>
          {/* Header with dark mode toggle */}
          <Box sx={{ position: 'relative', mb: 4 }}>
            <Typography variant="h3" gutterBottom align="center">
              🗂️ TreeVault
            </Typography>
            <Typography variant="subtitle1" gutterBottom align="center" color="text.secondary">
              Hierarchical File Manager with Tag Support
            </Typography>
            
            {/* Dark mode toggle button positioned in top right */}
            <Tooltip title={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}>
              <IconButton
                onClick={toggleDarkMode}
                sx={{
                  position: 'absolute',
                  top: 0,
                  right: 0,
                  color: 'text.primary',
                }}
                aria-label="toggle dark mode"
              >
                {darkMode ? <Brightness7 /> : <Brightness4 />}
              </IconButton>
            </Tooltip>
          </Box>
          
          <EnhancedTreeView />
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;

