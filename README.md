# TreeVault

Hierarchical file management system with tag support. Spring Boot backend with Clean Architecture, React frontend with Material-UI.

![TreeVault Application Screenshot](docs/treevault-app.png)

## System Overview

**Stack:**
- **Backend:** Java 21, Spring Boot 3.3, PostgreSQL 16
- **Frontend:** React 18, TypeScript 5.6, Material-UI 5.16, Vite 5.4
- **Deployment:** Docker Compose with health checks and automatic migrations

**Features:**
- Hierarchical tree structure with drag-and-drop reordering
- Tag management with key-value pairs
- CRUD operations with optimistic locking
- Path-based hierarchy for efficient queries
- RFC 9457 error handling
- OpenAPI documentation

## Quick Start with Docker

**Prerequisites:**
- Docker Desktop
- Docker Compose

**Setup Steps:**

1. **Configure database credentials:**
   
   Create `.env` file in the project root:
   ```bash
   DB_NAME=treevault
   DB_USER=treevault
   DB_PASSWORD=treevault123
   ```
   
   These environment variables are **required**. Docker Compose will fail to start without them.

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```
   
   **Startup sequence:**
   - PostgreSQL starts with configured credentials
   - Backend waits for database health check
   - Flyway runs migrations (creates schema and indexes)
   - Frontend starts after backend is healthy

3. **Access the application:**
   - **Frontend:** http://localhost:3000
   - **API:** http://localhost:8080/api/v1
   - **API Docs:** http://localhost:8080/swagger-ui/index.html
   - **Health:** http://localhost:8080/actuator/health

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

## Testing

**Backend:**
```bash
cd treevault-backend

# Unit tests only (fast, no Docker)
mvn test

# All tests including integration (requires Docker for Testcontainers)
mvn verify
```

**Frontend:**
```bash
cd treevault-frontend

# Run all tests once
npm test

# Watch mode for development
npm test -- --watch
```

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
│   │   ├── ErrorBoundary.tsx
│   │   └── NodeDetailsPanel.tsx
│   ├── dialogs/           # Dialog components
│   │   ├── AddTagDialog.tsx
│   │   ├── ConfirmDialog.tsx
│   │   ├── CreateNodeDialog.tsx
│   │   └── RenameNodeDialog.tsx
│   └── tree/              # Tree view components
│       ├── EnhancedTreeView.tsx
│       └── TreeNode.tsx
├── hooks/                 # Custom React hooks
│   ├── useDragAndDrop.ts  # Drag-drop logic
│   └── useNodeOperations.ts
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

**Create Node:** Right-click folder → Create Child Node → Choose type and name

**Rename Node:** Right-click node → Rename → Enter new name

**Move Node:** Drag node to target folder (highlights on hover)
- Cannot drop into files
- Cannot move folder into itself or descendants

**Manage Tags:** Select node → Add Tag in details panel → Enter key/value
- Remove via chip X icon

**Delete Node:** Right-click node → Delete → Confirm
- Deletes node and all children

### Error Handling

- Backend errors displayed via snackbar notifications
- Network failures show user-friendly messages
- Tree refetches after successful operations
- ErrorBoundary catches React component errors

### State Management

Zustand stores only UI state:
- `selectedNodeId`: Currently selected node
- `expandedNodeIds`: Expanded tree nodes
- `loading`: Async operation state
- `error`: Error messages

Tree data fetched from backend on demand, not cached globally.

### Design Decisions

1. **Thin Client Architecture**: Frontend is a pure presentation layer. All business logic, validation, and data integrity rules live in the backend. Frontend only handles UI state and user interactions.

2. **Zustand Over Redux**: Minimal state management using Zustand. Tree data fetched from backend on demand, not stored globally. Only UI state (`selectedNodeId`, `expandedNodeIds`, `loading`) persists.

3. **Material-UI Components**: Leverages battle-tested components for accessibility, theming, and responsive design. TreeView from `@mui/x-tree-view` provides drag-and-drop foundation.

4. **Backend-Driven Validation**: Frontend performs zero business validation. All rules enforced server-side, frontend displays backend error messages directly.

5. **Optimistic Updates Avoided**: No local state mutations before server confirmation. Tree refetches after successful operations to maintain single source of truth.

6. **Context Menu Pattern**: Right-click menu for all node operations. Familiar desktop-like UX for file management.

7. **Type Safety**: Strict TypeScript with explicit API types. Response DTOs match backend contracts exactly.

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
- **PostgreSQL 16**: Database with path-based hierarchy
- **Flyway**: Database migrations
- **Maven**: Build tool

### Local Development Setup

**Prerequisites:**
- Java 21
- Maven 3.9+
- PostgreSQL 15+

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

### Architecture & Design Decisions

**Clean Architecture Layers:**

```
API Layer (Controllers, DTOs, Exception Handlers)
         ↓
Application Layer (Use Cases, Orchestration)
         ↓
Domain Layer (Entities, Value Objects, Domain Services)
         ↓
Infrastructure Layer (JPA Repositories, Adapters)
```

**Layer Responsibilities:**

- **API**: HTTP concerns, request/response mapping, OpenAPI documentation
- **Application**: Coordinate use cases, transaction boundaries, business workflows
- **Domain**: Core business rules, validation, entity lifecycle, domain logic
- **Infrastructure**: Database access, query implementation, persistence adapters

**Key Design Decisions:**

1. **Clean Architecture**: Enforces dependency inversion—domain layer has zero external dependencies. Business logic is testable without database or web frameworks.

2. **Path-Based Hierarchy**: Uses string paths (`/parent/child/grandchild`) instead of recursive parent_id queries. Enables efficient subtree queries, ancestry checks, and depth calculations with simple string operations.

3. **No ltree Extension**: Standard string path column with text pattern indexes. Avoids PostgreSQL-specific extensions for better portability and simpler deployment.

4. **Position Management**: Each node has a `position` field for explicit ordering. `UNIQUE(parent_id, position)` constraint with `DEFERRABLE INITIALLY DEFERRED` allows safe reordering within transactions.

5. **Optimistic Locking**: Version field on nodes prevents concurrent modification conflicts. Critical for multi-user environments without pessimistic locks.

6. **MapStruct DTOs**: Compile-time DTO mapping eliminates reflection overhead. Clear separation between domain entities and API contracts.

7. **RFC 9457 Problem Details**: Standardized error responses with machine-readable `type` URIs and human-readable `detail` messages.

8. **Tag Validation**: Domain enforces tag key format (`^[a-zA-Z0-9_-]+$`, max 100 chars) and value constraints (max 500 chars). Business rules live in domain layer, not database.

### Testing Strategy

- **Unit Tests**: Domain and application layers tested in isolation with mocks
- **Integration Tests**: Full Spring context with Testcontainers PostgreSQL
- **Test Pyramid**: Fast unit tests (80%), integration tests (20%)

### Database Schema

**nodes table:**
- `id` (UUID): Primary key
- `name`: Node name (max 255 chars)
- `type`: FOLDER or FILE enum
- `parent_id`: Self-referencing foreign key (CASCADE delete)
- `path`: String path for hierarchy (e.g., `/root/child`)
- `depth`: Cached depth for quick filtering
- `position`: Ordering within parent
- `version`: Optimistic locking
- `created_at`, `updated_at`: Audit timestamps

**tags table:**
- `id` (UUID): Primary key
- `node_id`: Foreign key to nodes (CASCADE delete)
- `tag_key`: Tag key (max 100 chars, validated format)
- `tag_value`: Tag value (max 500 chars)
- `created_at`: Audit timestamp
- Unique constraint: `(node_id, tag_key)`

**Indexes:**
- `idx_nodes_path_pattern`: Path pattern matching (text_pattern_ops)
- `idx_nodes_parent`: Parent-child lookups
- `idx_nodes_type`: Filter by type (folder/file)
- `idx_nodes_created_at`: Sort by creation time
- `idx_tags_node`: Tag lookup by node
- `idx_tags_key`: Tag lookup by key

## License

Part of the TreeVault project.
