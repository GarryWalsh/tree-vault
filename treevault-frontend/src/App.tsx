import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Container, Box, Typography } from '@mui/material';
import { EnhancedTreeView } from './components/tree/EnhancedTreeView';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="xl">
        <Box sx={{ py: 4 }}>
          <Typography variant="h3" gutterBottom align="center" sx={{ mb: 4 }}>
            🗂️ TreeVault
          </Typography>
          <Typography variant="subtitle1" gutterBottom align="center" color="text.secondary" sx={{ mb: 4 }}>
            Hierarchical File Manager with Tag Support
          </Typography>
          <EnhancedTreeView />
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;

