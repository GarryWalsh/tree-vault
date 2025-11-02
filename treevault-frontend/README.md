# TreeVault Frontend

Modern React frontend for TreeVault - a hierarchical file management system with tag support.

## Features

- 🌳 **Hierarchical Tree View**: Browse and navigate your file structure
- 🏷️ **Tag Management**: Add and remove tags from any node
- ✏️ **CRUD Operations**: Create, rename, and delete folders and files
- 🖱️ **Drag & Drop**: Move nodes by dragging them to new locations
- 📊 **Node Details Panel**: View metadata, timestamps, and tags
- 🎨 **Material-UI Design**: Clean, modern interface
- ⚡ **Real-time Updates**: Automatic tree refresh after operations
- 🔔 **User Feedback**: Snackbar notifications for all actions

## Tech Stack

- **React 18.3**: Modern React with hooks
- **TypeScript 5.6**: Type-safe development
- **Material-UI 5.16**: Component library and design system
- **Axios 1.7**: HTTP client for API communication
- **Zustand 4.5**: Lightweight state management
- **Vite 5.4**: Fast build tool and dev server

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- TreeVault backend running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install
```

### Configuration

Create a `.env.local` file for local development:

```env
VITE_API_URL=http://localhost:8080
```

For Docker deployment, the API URL is configured via docker-compose.yml.

### Development

```bash
# Start development server
npm run dev

# Open http://localhost:5173
```

### Build

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

### Testing

```bash
# Run tests
npm test
```

## Project Structure

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

## API Integration

The frontend integrates with the following backend endpoints:

### Node Operations
- `GET /api/v1/tree` - Get entire tree structure
- `GET /api/v1/nodes/{id}` - Get specific node
- `POST /api/v1/nodes` - Create new node
- `PUT /api/v1/nodes/{id}` - Update node name
- `DELETE /api/v1/nodes/{id}` - Delete node
- `POST /api/v1/nodes/{id}/move` - Move node to new parent

### Tag Operations
- `POST /api/v1/nodes/{id}/tags` - Add tag to node
- `DELETE /api/v1/nodes/{id}/tags/{key}` - Remove tag from node

## Usage

### Creating Nodes

1. Right-click on any folder node
2. Select "Create Child Node"
3. Choose type (Folder or File) and enter name
4. Click "Create"

### Renaming Nodes

1. Right-click on any node
2. Select "Rename"
3. Enter new name
4. Click "Rename"

### Moving Nodes (Drag & Drop)

1. Click and hold on any node
2. Drag to target folder (will highlight)
3. Release to drop

Note: Can only drop into folders, not files. Cannot move a folder into itself or its descendants.

### Managing Tags

1. Select a node to view its details in the right panel
2. Click "Add Tag" button
3. Enter tag key and value
4. Click chip's X icon to remove a tag

### Deleting Nodes

1. Right-click on any node
2. Select "Delete"
3. Confirm deletion (will delete node and all children)

## Error Handling

All API errors are handled gracefully:
- Backend validation errors are displayed as snackbar notifications
- Network errors show appropriate error messages
- Tree automatically reloads after successful operations

## Docker Deployment

The frontend is containerized and configured via docker-compose:

```yaml
frontend:
  build:
    context: ./treevault-frontend
    dockerfile: Dockerfile
  environment:
    VITE_API_URL: http://backend:8080
  ports:
    - "3000:80"
```

## State Management

Uses Zustand for minimal, focused state:
- `selectedNodeId` - Currently selected node
- `expandedNodeIds` - List of expanded tree nodes
- `loading` - Loading state for async operations
- `error` - Error messages

Tree data is fetched fresh from backend, not stored in global state.

## Development Principles

- **Backend-First**: All business logic and validation in backend
- **Type Safety**: Full TypeScript coverage with strict types
- **Error Boundaries**: Graceful error handling at all levels
- **Accessibility**: Material-UI components are accessible by default
- **Responsive**: Works on desktop and mobile devices
- **Performance**: Minimal re-renders, efficient state updates

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+

## License

Part of the TreeVault project.

