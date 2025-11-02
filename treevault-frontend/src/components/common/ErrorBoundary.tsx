import { Component, ErrorInfo, ReactNode } from 'react';
import { Box, Paper, Typography, Button, Alert } from '@mui/material';
import { Error as ErrorIcon, Refresh as RefreshIcon } from '@mui/icons-material';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    errorInfo: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error, errorInfo: null };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
    this.setState({
      error,
      errorInfo,
    });
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  public render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            backgroundColor: '#f5f5f5',
            p: 3,
          }}
        >
          <Paper elevation={3} sx={{ maxWidth: 600, p: 4, textAlign: 'center' }}>
            <ErrorIcon color="error" sx={{ fontSize: 64, mb: 2 }} />
            <Typography variant="h4" gutterBottom color="error">
              Oops! Something went wrong
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              The application encountered an unexpected error. Don't worry, your data is safe.
            </Typography>

            <Alert severity="error" sx={{ mb: 3, textAlign: 'left' }}>
              <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-word' }}>
                {this.state.error?.toString()}
              </Typography>
            </Alert>

            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
              <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={this.handleReload}
              >
                Reload Application
              </Button>
              <Button variant="outlined" onClick={this.handleReset}>
                Try Again
              </Button>
            </Box>

            {import.meta.env.DEV && this.state.errorInfo && (
              <Box sx={{ mt: 3, textAlign: 'left' }}>
                <Typography variant="caption" color="text.secondary">
                  Stack trace (development only):
                </Typography>
                <Paper
                  variant="outlined"
                  sx={{
                    p: 2,
                    mt: 1,
                    maxHeight: 200,
                    overflow: 'auto',
                    backgroundColor: '#f9f9f9',
                  }}
                >
                  <Typography
                    variant="caption"
                    component="pre"
                    sx={{ fontFamily: 'monospace', fontSize: '0.7rem', margin: 0 }}
                  >
                    {this.state.errorInfo.componentStack}
                  </Typography>
                </Paper>
              </Box>
            )}
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

