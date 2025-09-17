## API Endpoints

### Core Functionality
- `POST /api/storage/folders` - Create folder
- `DELETE /api/storage/folders` - Delete folder
- `GET /api/storage/folders` - List user's root folders
- `GET /api/storage/contents` - List all contents in root directory (**NEW**)
- `GET /api/storage/folders/{folderName}/contents` - List folder contents (**IMPROVED**)

### File Operations
- `POST /api/storage/files/upload-url` - Generate upload URL
- `POST /api/storage/files/download-url` - Generate download URL
- `DELETE /api/storage/files/{fileName}` - Delete file

### Existence Checks
- `GET /api/storage/files/{fileName}/exists` - Check file existence
- `GET /api/storage/folders/{folderName}/exists` - Check folder existence

## Testing the Fixes

### 1. Test Folder Creation
```bash
curl -X POST "http://localhost:8080/api/storage/folders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"folderName": "documents"}'
```

### 2. Test Root Directory Browsing
```bash
curl -X GET "http://localhost:8080/api/storage/contents" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. Test Folder Contents (with proper path handling)
```bash
# Browse documents folder
curl -X GET "http://localhost:8080/api/storage/folders/documents/contents" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Browse root (these should all work the same way now)
curl -X GET "http://localhost:8080/api/storage/folders/root/contents" \
  -H "Authorization: Bearer YOUR_TOKEN"

curl -X GET "http://localhost:8080/api/storage/folders//contents" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Test Nested Folder Creation and Browsing
```bash
# Create nested folders
curl -X POST "http://localhost:8080/api/storage/folders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"folderName": "documents/reports"}'

curl -X POST "http://localhost:8080/api/storage/folders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"folderName": "documents/images"}'

# Browse documents folder - should only show direct children (reports/, images/)
curl -X GET "http://localhost:8080/api/storage/folders/documents/contents" \
  -H "Authorization: Bearer YOUR_TOKEN"
```