# TreeVault

A hierarchical file management system with tag support, built with Spring Boot backend and React frontend.

## Quick Start with Docker

**Prerequisites:**
- Docker Desktop
- Docker Compose

**Setup Steps:**

1. **Create environment file:**
   
   Copy `.env.example` to `.env` in the project root:
   ```bash
   cp .env.example .env
   ```
   
   Or on Windows PowerShell:
   ```powershell
   Copy-Item .env.example .env
   ```
   
   The default values work fine for demonstration purposes. Edit `.env` only if you want custom database credentials.

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```
   
   **What happens on startup:**
   - PostgreSQL database starts with an empty database
   - Backend waits for database to be ready (health check)
   - Flyway automatically runs all migrations to create tables and schema
   - Frontend starts after backend is healthy

3. **Access the application:**
   - **Frontend:** http://localhost:3000
   - **API Docs:** http://localhost:8080/swagger-ui/index.html
   - **Health Check:** http://localhost:8080/actuator/health

## Managing the Application

**View logs:**
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

**Stop services:**
```bash
docker-compose down
```

**Clean everything (including database data):**
```bash
docker-compose down -v
```

**Rebuild after code changes:**
```bash
docker-compose up -d --build
```

## Run Tests

**Backend tests:**
```bash
cd treevault-backend

# Unit tests (fast, no Docker needed)
mvn clean test

# Integration/E2E tests (requires Docker + Testcontainers)
mvn clean verify
```

**Frontend tests:**
```bash
cd treevault-frontend
npm test --run
```

## Troubleshooting

**Frontend shows 404 errors:**
- The frontend is configured to call `http://localhost:8080/api/v1`
- Check if backend is running: `docker-compose logs backend`

**Backend fails to start:**
- Check if `.env` file exists with DB credentials
- View backend logs: `docker-compose logs backend`
- Verify Postgres is healthy: `docker-compose ps`

**Frontend tests fail with "findLastIndex is not a function":**
- This means you're using Node.js 16 or below
- Upgrade to Node.js 18 or higher: `node --version` to check current version
- Download from https://nodejs.org/ or use nvm/nvm-windows

**Ports already in use:**
- Check what's using ports 3000, 8080, or 5432
- Stop conflicting services or change ports in `docker-compose.yml`

**Database issues:**
- Clean and restart: `docker-compose down -v && docker-compose up -d`
- This removes all data and recreates the database from scratch

---

## Frontend

Modern React frontend for TreeVault with a clean, intuitive interface.

### Features

- 🌳 **Hierarchical Tree View**: Browse and navigate your file structure
- 🏷️ **Tag Management**: Add and remove tags from any node
- ✏️ **CRUD Operations**: Create, rename, and delete folders and files
- 🖱️ **Drag & Drop**: Move nodes by dragging them to new locations
- 📊 **Node Details Panel**: View metadata, timestamps, and tags
- 🎨 **Material-UI Design**: Clean, modern interface
- ⚡ **Real-time Updates**: Automatic tree refresh after operations
- 🔔 **User Feedback**: Snackbar notifications for all actions

### Tech Stack

- **React 18.3**: Modern React with hooks
- **TypeScript 5.6**: Type-safe development
- **Material-UI 5.16**: Component library and design system
- **Axios 1.7**: HTTP client for API communication
- **Zustand 4.5**: Lightweight state management
- **Vite 5.4**: Fast build tool and dev server

### Local Development Setup

**Prerequisites:**
- **Node.js 18+ and npm** (required - Node 16 and below will not work)
- TreeVault backend running on `http://localhost:8080`

**Installation:**

```bash
cd treevault-frontend
npm install
```


### Project Structure

```
src/
├── api/                    # API client and types
│   ├── client.ts          # Axios configuration
│   ├── nodeApi.ts         # Node API endpoints
│   └── types.ts           # TypeScript interfaces
├── components/
│   ├── common/            # Shared components
│   │   └── NodeDetailsPanel.tsx
│   ├── dialogs/           # Dialog components
│   │   ├── AddTagDialog.tsx
│   │   ├── ConfirmDialog.tsx
│   │   ├── CreateNodeDialog.tsx
│   │   └── RenameNodeDialog.tsx
│   └── tree/              # Tree view components
│       ├── EnhancedTreeView.tsx
│       └── TreeView.tsx
├── store/                 # State management
│   └── treeStore.ts       # Zustand store
├── App.tsx                # Main application
└── main.tsx              # Entry point
```

### API Integration

The frontend integrates with the following backend endpoints:

**Node Operations:**
- `GET /api/v1/tree` - Get entire tree structure
- `GET /api/v1/nodes/{id}` - Get specific node
- `POST /api/v1/nodes` - Create new node
- `PUT /api/v1/nodes/{id}` - Update node name
- `DELETE /api/v1/nodes/{id}` - Delete node
- `POST /api/v1/nodes/{id}/move` - Move node to new parent

**Tag Operations:**
- `POST /api/v1/nodes/{id}/tags` - Add tag to node
- `DELETE /api/v1/nodes/{id}/tags/{key}` - Remove tag from node

### Usage Guide

**Creating Nodes:**

1. Right-click on any folder node
2. Select "Create Child Node"
3. Choose type (Folder or File) and enter name
4. Click "Create"

**Renaming Nodes:**

1. Right-click on any node
2. Select "Rename"
3. Enter new name
4. Click "Rename"

**Moving Nodes (Drag & Drop):**

1. Click and hold on any node
2. Drag to target folder (will highlight)
3. Release to drop

Note: Can only drop into folders, not files. Cannot move a folder into itself or its descendants.

**Managing Tags:**

1. Select a node to view its details in the right panel
2. Click "Add Tag" button
3. Enter tag key and value
4. Click chip's X icon to remove a tag

**Deleting Nodes:**

1. Right-click on any node
2. Select "Delete"
3. Confirm deletion (will delete node and all children)

### Error Handling

All API errors are handled gracefully:
- Backend validation errors are displayed as snackbar notifications
- Network errors show appropriate error messages
- Tree automatically reloads after successful operations

### State Management

Uses Zustand for minimal, focused state:
- `selectedNodeId` - Currently selected node
- `expandedNodeIds` - List of expanded tree nodes
- `loading` - Loading state for async operations
- `error` - Error messages

Tree data is fetched fresh from backend, not stored in global state.

### Development Principles

- **Backend-First**: All business logic and validation in backend
- **Type Safety**: Full TypeScript coverage with strict types
- **Error Boundaries**: Graceful error handling at all levels
- **Accessibility**: Material-UI components are accessible by default
- **Responsive**: Works on desktop and mobile devices
- **Performance**: Minimal re-renders, efficient state updates

### Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

---

## Backend

Spring Boot backend with Clean Architecture, providing RESTful APIs for hierarchical file management.

### Tech Stack

- **Java 21**: Latest LTS version
- **Spring Boot 3.3**: Framework and REST APIs
- **PostgreSQL**: Database with ltree extension
- **Flyway**: Database migrations
- **Maven**: Build tool

### Local Development Setup

**Prerequisites:**
- Java 21
- Maven 3.9+
- PostgreSQL 15+ with ltree extension

**Configuration:**

Update `src/main/resources/application.yml` with your local database settings.

**Run Backend:**

```bash
cd treevault-backend
mvn spring-boot:run
```

**Build:**

```bash
mvn clean package
```

### Architecture

The backend follows Clean Architecture principles:

- **API Layer**: REST controllers, DTOs, exception handlers
- **Application Layer**: Use cases and business workflows
- **Domain Layer**: Core business logic, entities, services
- **Infrastructure Layer**: Database persistence, external integrations

### Database Schema

Uses PostgreSQL's `ltree` extension for efficient hierarchical queries:

- **nodes**: Stores tree structure with path-based hierarchy
- **tags**: Key-value tags attached to nodes
- Indexes optimized for path traversal and tag lookups

## License

Part of the TreeVault project.
