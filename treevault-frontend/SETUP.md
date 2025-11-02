# TreeVault Frontend Setup Guide

## Prerequisites

- **Node.js 18+** (recommended: Node.js 20)
- **npm 8+**
- TreeVault backend running (see backend README)

## Quick Start

### 1. Install Dependencies

```bash
cd treevault-frontend
npm install
```

### 2. Configure Environment

Create a `.env.local` file in the `treevault-frontend` directory:

```bash
# For local development
VITE_API_URL=http://localhost:8080
```

**Important**: Environment variable names in Vite must be prefixed with `VITE_` to be exposed to the client.

### 3. Start Development Server

```bash
npm run dev
```

The application will be available at: **http://localhost:3000**

## Environment Configuration

### Local Development

When running both frontend and backend locally:

**File**: `.env.local`
```env
VITE_API_URL=http://localhost:8080
```

- Backend should be running on port 8080
- Frontend runs on port 3000
- Vite proxy configuration forwards `/api` requests to backend

### Docker Compose

When using Docker Compose (both services containerized):

The environment variable is set in `docker-compose.yml`:
```yaml
frontend:
  environment:
    VITE_API_URL: http://backend:8080
```

No `.env.local` file needed - Docker handles it.

### Production Build

For production deployment:

```bash
# Set the API URL for your production backend
export VITE_API_URL=https://api.yourdomain.com

# Build
npm run build

# Output will be in dist/ directory
```

## Available Scripts

### Development

```bash
# Start dev server with hot reload
npm run dev

# Start dev server on custom port
npm run dev -- --port 5000
```

### Build

```bash
# Type check and build for production
npm run build

# Build without type checking (faster, not recommended)
npx vite build
```

### Preview

```bash
# Preview production build locally
npm run preview
```

### Testing

```bash
# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

## Project Structure

```
treevault-frontend/
├── src/
│   ├── api/              # API client and types
│   ├── components/       # React components
│   │   ├── common/       # Shared components
│   │   ├── dialogs/      # Dialog components
│   │   └── tree/         # Tree view components
│   ├── store/            # State management (Zustand)
│   ├── App.tsx           # Main app component
│   ├── main.tsx          # Entry point
│   └── index.css         # Global styles
├── tests/                # Test files
├── public/               # Static assets
├── dist/                 # Build output (generated)
└── node_modules/         # Dependencies (generated)
```

## Environment Variables

### VITE_API_URL

The backend API base URL.

**Default**: `http://localhost:8080`

**Examples**:
- Local: `http://localhost:8080`
- Docker: `http://backend:8080`
- Production: `https://api.yourdomain.com`

The API client automatically appends `/api/v1` to endpoints, so you only need to specify the base URL.

### How Vite Handles Environment Variables

1. **Development** (npm run dev):
   - Reads `.env.local` file
   - Variables prefixed with `VITE_` are exposed to client
   - Hot reloads on changes

2. **Build** (npm run build):
   - Reads `.env.local` file or environment variables
   - Values are inlined at build time
   - No runtime configuration possible

3. **Access in Code**:
   ```typescript
   const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
   ```

## Common Setup Issues

### Issue: "Cannot find module" errors

**Solution**: Reinstall dependencies
```bash
rm -rf node_modules package-lock.json
npm install
```

### Issue: CORS errors when calling API

**Solution**: Ensure backend CORS configuration allows frontend origin:
```java
// Backend: CorsConfig.java
allowedOrigins("http://localhost:3000")
```

### Issue: API calls fail with 404

**Solution**: Check VITE_API_URL is correct:
```bash
# In browser console:
console.log(import.meta.env.VITE_API_URL)
```

### Issue: Hot reload not working

**Solution**: 
1. Check Vite dev server is running
2. Try restarting dev server
3. Clear browser cache

### Issue: Build fails with TypeScript errors

**Solution**: Fix TypeScript errors or temporarily skip check:
```bash
npx vite build --skipLibCheck
```

### Issue: Tests fail to run

**Solution**: Ensure test dependencies are installed:
```bash
npm install --save-dev @testing-library/react @testing-library/jest-dom vitest
```

## Docker Setup

### Build Docker Image

```bash
docker build -t treevault-frontend .
```

### Run Container

```bash
docker run -p 3000:80 -e VITE_API_URL=http://backend:8080 treevault-frontend
```

### Docker Compose (Recommended)

From project root:
```bash
docker-compose up
```

Access:
- Frontend: http://localhost:3000
- Backend: http://localhost:8080

## IDE Setup

### VS Code

Recommended extensions:
- ESLint
- Prettier
- TypeScript Vue Plugin (Volar)

### Settings

Create `.vscode/settings.json`:
```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib"
}
```

## Troubleshooting

### Port Already in Use

If port 3000 is occupied:
```bash
# Use different port
npm run dev -- --port 5173

# Or kill process using port 3000
# Linux/Mac:
lsof -ti:3000 | xargs kill -9
# Windows:
netstat -ano | findstr :3000
taskkill /PID <PID> /F
```

### Backend Not Responding

1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Check database is running
3. Check backend logs for errors

### Missing .env.local File

Create it manually:
```bash
echo "VITE_API_URL=http://localhost:8080" > .env.local
```

## Production Deployment

### 1. Build Static Files

```bash
npm run build
```

Output: `dist/` directory

### 2. Serve with Nginx

Copy `dist/` contents to Nginx web root:
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. Environment Configuration

Set `VITE_API_URL` before build:
```bash
export VITE_API_URL=https://api.production.com
npm run build
```

## Next Steps

1. **Start Backend**: Follow backend README to start Spring Boot app
2. **Start Frontend**: Run `npm run dev` 
3. **Open Browser**: Navigate to http://localhost:3000
4. **Create Nodes**: Right-click root to create folders/files
5. **Add Tags**: Select a node and use details panel
6. **Drag & Drop**: Move nodes by dragging

## Additional Resources

- [Vite Documentation](https://vitejs.dev/)
- [React Documentation](https://react.dev/)
- [Material-UI Documentation](https://mui.com/)
- [Vitest Documentation](https://vitest.dev/)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review backend logs for API errors
3. Check browser console for frontend errors
4. Verify environment configuration

## License

Part of the TreeVault project.

