import { useState, useMemo, useEffect } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Container, Box, Typography, IconButton, Tooltip, Popover } from '@mui/material';
import { Brightness4, Brightness7, Palette } from '@mui/icons-material';
import { EnhancedTreeView } from './components/tree/EnhancedTreeView';

// Define available themes
const THEMES = {
  teal: { name: 'Teal', color: '#5bc0be' },
  blue: { name: 'Blue', color: '#1976d2' },
  purple: { name: 'Purple', color: '#9c27b0' },
  green: { name: 'Green', color: '#2e7d32' },
  orange: { name: 'Orange', color: '#ed6c02' },
  pink: { name: 'Pink', color: '#d81b60' },
  indigo: { name: 'Indigo', color: '#3f51b5' },
  amber: { name: 'Amber', color: '#ff6f00' },
} as const;

type ThemeKey = keyof typeof THEMES;

function App() {
  // Initialize dark mode from localStorage or default to dark mode
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    const savedMode = localStorage.getItem('darkMode');
    return savedMode ? JSON.parse(savedMode) : true;
  });

  // Initialize theme color from localStorage or default to teal
  const [selectedTheme, setSelectedTheme] = useState<ThemeKey>(() => {
    const savedTheme = localStorage.getItem('themeColor');
    return (savedTheme && savedTheme in THEMES) ? savedTheme as ThemeKey : 'teal';
  });

  // Save dark mode preference to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('darkMode', JSON.stringify(darkMode));
  }, [darkMode]);

  // Save theme color preference to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('themeColor', selectedTheme);
  }, [selectedTheme]);

  // Create theme based on dark mode state and selected theme color
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: darkMode ? 'dark' : 'light',
          primary: {
            main: THEMES[selectedTheme].color,
          },
        },
      }),
    [darkMode, selectedTheme]
  );

  const [themeAnchorEl, setThemeAnchorEl] = useState<HTMLButtonElement | null>(null);

  const toggleDarkMode = () => {
    setDarkMode((prev) => !prev);
  };

  const handleThemeClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setThemeAnchorEl(event.currentTarget);
  };

  const handleThemeClose = () => {
    setThemeAnchorEl(null);
  };

  const handleThemeSelect = (themeKey: ThemeKey) => {
    setSelectedTheme(themeKey);
    handleThemeClose();
  };

  const themePopoverOpen = Boolean(themeAnchorEl);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="xl">
        <Box sx={{ py: 4 }}>
          {/* Header with dark mode toggle and theme picker */}
          <Box sx={{ position: 'relative', mb: 4 }}>
            <Typography variant="h3" gutterBottom align="center">
              🗂️ TreeVault
            </Typography>
            <Typography variant="subtitle1" gutterBottom align="center" color="text.secondary">
              Hierarchical File Manager with Tag Support
            </Typography>
            
            {/* Theme controls positioned in top right */}
            <Box
              sx={{
                position: 'absolute',
                top: 0,
                right: 0,
                display: 'flex',
                alignItems: 'center',
                gap: 1,
              }}
            >
              {/* Theme picker */}
              <Tooltip title="Choose theme color">
                <IconButton
                  onClick={handleThemeClick}
                  sx={{
                    color: 'text.primary',
                  }}
                  aria-label="choose theme"
                >
                  <Palette />
                </IconButton>
              </Tooltip>
              
              <Popover
                open={themePopoverOpen}
                anchorEl={themeAnchorEl}
                onClose={handleThemeClose}
                anchorOrigin={{
                  vertical: 'bottom',
                  horizontal: 'right',
                }}
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
              >
                <Box
                  sx={{
                    p: 2,
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, 1fr)',
                    gap: 1.5,
                    maxWidth: 200,
                  }}
                >
                  {(Object.keys(THEMES) as ThemeKey[]).map((themeKey) => (
                    <Tooltip key={themeKey} title={THEMES[themeKey].name} placement="top">
                      <Box
                        onClick={() => handleThemeSelect(themeKey)}
                        sx={{
                          width: 36,
                          height: 36,
                          borderRadius: '50%',
                          backgroundColor: THEMES[themeKey].color,
                          border: '3px solid',
                          borderColor: selectedTheme === themeKey ? 'text.primary' : 'transparent',
                          cursor: 'pointer',
                          transition: 'all 0.2s ease',
                          boxShadow: selectedTheme === themeKey ? 2 : 0,
                          '&:hover': {
                            transform: 'scale(1.1)',
                            boxShadow: 3,
                          },
                        }}
                      />
                    </Tooltip>
                  ))}
                </Box>
              </Popover>

              {/* Dark mode toggle button */}
              <Tooltip title={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}>
                <IconButton
                  onClick={toggleDarkMode}
                  sx={{
                    color: 'text.primary',
                  }}
                  aria-label="toggle dark mode"
                >
                  {darkMode ? <Brightness7 /> : <Brightness4 />}
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
          
          <EnhancedTreeView />
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;

