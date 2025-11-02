# TreeVault Frontend-Backend Integration Guide

## Overview

The TreeVault frontend is fully integrated with the backend REST API. All operations are delegated to the backend, which handles validation, business logic, and data persistence.

## API Integration Status

### ✅ Fully Implemented Features

#### 1. Tree Operations
- **Get Tree**: `GET /api/v1/tree`
  - Loads entire tree structure on mount
  - Called after every mutation operation to refresh state
  - Displays in hierarchical tree view with expand/collapse

#### 2. Node CRUD Operations
- **Create Node**: `POST /api/v1/nodes`
  - Via right-click context menu → "Create Child Node"
  - Dialog for choosing type (FOLDER/FILE) and name
  - Backend validates name format, duplicates, max depth
  
- **Get Node**: `GET /api/v1/nodes/{id}`
  - Available via API client
  - Node details displayed in NodeDetailsPanel when selected
  
- **Update Node**: `PUT /api/v1/nodes/{id}`
  - Via right-click context menu → "Rename"
  - Dialog for entering new name
  - Backend validates name format and duplicates
  
- **Delete Node**: `DELETE /api/v1/nodes/{id}`
  - Via right-click context menu → "Delete"
  - Confirmation dialog with warning
  - Backend handles recursive deletion of children

#### 3. Move Operations
- **Move Node**: `POST /api/v1/nodes/{id}/move`
  - Implemented with drag-and-drop interface
  - Visual feedback (cursor changes, drop target highlighting)
  - Client-side validation:
    - Can only drop into folders
    - Cannot move folder into itself or descendants
  - Backend validation:
    - Circular reference detection
    - Position validation
    - Name conflict detection in target parent

#### 4. Tag Operations
- **Add Tag**: `POST /api/v1/nodes/{id}/tags`
  - Via "Add Tag" button in NodeDetailsPanel
  - Dialog for entering key and value
  - Backend validates tag format and limits
  
- **Remove Tag**: `DELETE /api/v1/nodes/{id}/tags/{key}`
  - Via chip delete icon in NodeDetailsPanel
  - Confirmation dialog
  - Backend validates tag existence

## Component Architecture

### API Layer (`src/api/`)

#### `client.ts`
- Axios instance with base URL configuration
- Response/error interceptors
- Environment-aware API URL (Docker vs local)

```typescript
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});
```

#### `nodeApi.ts`
- Clean API method wrappers
- All 8 backend endpoints covered
- Typed requests and responses

#### `types.ts`
- TypeScript interfaces matching backend DTOs
- `NodeResponse`, `TreeResponse`, `CreateNodeRequest`, etc.

### UI Components

#### `EnhancedTreeView.tsx` (Main Component)
- Manages tree state and operations
- Integrates all dialogs and API calls
- Drag-and-drop implementation
- Error handling with snackbar notifications
- Split view: Tree (left) + Details Panel (right)

#### Dialog Components
- `CreateNodeDialog`: Type selection + name input
- `RenameNodeDialog`: Name input with current value
- `AddTagDialog`: Key-value pair input
- `ConfirmDialog`: Reusable confirmation for dangerous actions

#### `NodeDetailsPanel`
- Displays node metadata (ID, path, dates, version)
- Tag management (add/remove with chips)
- Formatted timestamps
- Responsive layout

### State Management (`src/store/treeStore.ts`)

Zustand store for UI-only state:
- `selectedNodeId`: Currently selected node
- `expandedNodeIds`: Expanded tree nodes
- `loading`: Loading indicator state
- `error`: Error message display

**Note**: Tree data is NOT in global state. Always fetched fresh from backend after mutations.

## Backend Integration Details

### Request Flow

1. **User Action** → Component event handler
2. **API Call** → `nodeApi` method
3. **HTTP Request** → Backend REST endpoint
4. **Backend Processing** → Validation + business logic
5. **Response** → Success or ProblemDetail error
6. **UI Update** → Reload tree + show notification

### Error Handling

All errors from backend are handled uniformly:

```typescript
try {
  await nodeApi.someOperation(params);
  await loadTree(); // Refresh
  showSnackbar('Success!', 'success');
} catch (err: any) {
  const errorDetail = err.response?.data?.detail || 'Operation failed';
  showSnackbar(errorDetail, 'error');
}
```

Backend returns RFC 7807 ProblemDetail responses:
- `title`: Error type
- `detail`: Human-readable message
- `status`: HTTP status code

### No Client-Side Business Logic

The frontend is a thin UI layer:
- ✅ UI state management (selection, expansion)
- ✅ User input collection (dialogs, forms)
- ✅ Visual feedback (loading, notifications)
- ❌ No validation (all in backend)
- ❌ No business rules (all in backend)
- ❌ No data transformation (all in backend)

## Environment Configuration

### Local Development
```env
# .env.local
VITE_API_URL=http://localhost:8080
```

### Docker Compose
```yaml
environment:
  VITE_API_URL: http://backend:8080
```

### Production
Set `VITE_API_URL` to your backend URL during build.

## Testing Integration

### Manual Testing Checklist

1. **Tree Loading**
   - [ ] Tree loads on app start
   - [ ] Root node visible
   - [ ] Children expandable

2. **Create Operations**
   - [ ] Create folder via context menu
   - [ ] Create file via context menu
   - [ ] Error on invalid name
   - [ ] Error on duplicate name

3. **Update Operations**
   - [ ] Rename folder
   - [ ] Rename file
   - [ ] Error on invalid new name
   - [ ] Error on duplicate new name

4. **Delete Operations**
   - [ ] Delete empty folder
   - [ ] Delete folder with children (recursive)
   - [ ] Delete file
   - [ ] Confirmation dialog shows

5. **Move Operations**
   - [ ] Drag file to folder
   - [ ] Drag folder to folder
   - [ ] Cannot drag to file (error)
   - [ ] Cannot drag folder to itself (error)
   - [ ] Cannot drag folder to descendant (error)
   - [ ] Visual feedback during drag

6. **Tag Operations**
   - [ ] Add tag to node
   - [ ] View tags in details panel
   - [ ] Remove tag via chip
   - [ ] Error on invalid tag format
   - [ ] Error on tag limit exceeded

7. **UI Feedback**
   - [ ] Loading spinner during operations
   - [ ] Success notifications
   - [ ] Error notifications with backend messages
   - [ ] Tree refreshes after mutations

### Automated Testing

Run unit tests:
```bash
npm test
```

Test files should cover:
- API client configuration
- Component rendering
- User interaction flows
- Error handling

## Common Integration Issues

### Issue: API calls fail with CORS error
**Solution**: Ensure backend CORS configuration allows frontend origin:
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // Allow http://localhost:5173 for development
    }
}
```

### Issue: Tree not updating after operation
**Solution**: Ensure `loadTree()` is called after mutations:
```typescript
await nodeApi.createNode(data);
await loadTree(); // Essential!
```

### Issue: Drag and drop not working
**Solution**: Check that:
- `draggable` attribute is set on Box
- All drag event handlers are implemented
- Event propagation is stopped where needed

### Issue: Environment variable not working
**Solution**: 
- Vite requires `VITE_` prefix
- Restart dev server after changing .env
- Check import.meta.env.VITE_API_URL value

## Performance Considerations

- Tree is reloaded fully after each mutation (simple, consistent)
- For large trees (1000+ nodes), consider:
  - Backend pagination
  - Virtual scrolling
  - Incremental loading
  - Optimistic UI updates

## Future Enhancements

Potential improvements (not currently implemented):
- [ ] Search/filter nodes
- [ ] Bulk operations
- [ ] Keyboard shortcuts
- [ ] Node icons customization
- [ ] Tree export/import
- [ ] Undo/redo
- [ ] Real-time collaboration (WebSocket)
- [ ] Node permissions/access control

## API Client Reference

### Available Methods

```typescript
// Tree operations
nodeApi.getTree(): Promise<TreeResponse>

// Node operations  
nodeApi.getNode(id: string): Promise<NodeResponse>
nodeApi.createNode(data: CreateNodeRequest): Promise<NodeResponse>
nodeApi.updateNode(id: string, name: string): Promise<NodeResponse>
nodeApi.deleteNode(id: string): Promise<void>
nodeApi.moveNode(id: string, newParentId: string, position: number): Promise<NodeResponse>

// Tag operations
nodeApi.addTag(nodeId: string, key: string, value: string): Promise<void>
nodeApi.removeTag(nodeId: string, key: string): Promise<void>
```

### Type Definitions

```typescript
interface NodeResponse {
  id: string;
  name: string;
  type: 'FOLDER' | 'FILE';
  parentId?: string;
  children?: NodeResponse[];
  tags?: Record<string, string>;
  path: string;
  position?: number;
  version?: number;
  createdAt: string;
  updatedAt: string;
}

interface CreateNodeRequest {
  name: string;
  type: 'FOLDER' | 'FILE';
  parentId?: string;
  tags?: Record<string, string>;
}
```

## Conclusion

The TreeVault frontend is **fully integrated** with the backend API. All CRUD operations, tag management, and tree operations are implemented with proper error handling, user feedback, and consistent state management.

The architecture follows clean separation of concerns:
- Backend = Business logic, validation, persistence
- Frontend = User interface, interaction, presentation

This makes the system maintainable, testable, and easy to extend.

