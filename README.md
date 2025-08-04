# Omninet Security - OAuth2 Backend with Todo Showcase

A Spring Boot application demonstrating OAuth2 authentication with GitHub and Google providers, featuring a loosely coupled Todo API for demonstration purposes.

## Features

### OAuth2 Authentication
- **GitHub OAuth2** - Login with GitHub account
- **Google OAuth2** - Login with Google account
- **Session Management** - Secure session handling
- **CORS Support** - Ready for React frontend integration

### Todo API (Loosely Coupled)
- **CRUD Operations** - Create, Read, Update, Delete todos
- **User Isolation** - Each user sees only their todos
- **Statistics** - Todo completion statistics
- **RESTful Design** - Standard REST endpoints

## Architecture

The application is designed with loose coupling in mind:

- **OAuth2 Module**: Can be extracted and used in other projects
- **Todo Module**: Completely separate from auth, easily removable
- **Clean Separation**: Authentication and business logic are decoupled

## Setup Instructions

### 1. OAuth2 Provider Configuration

#### GitHub OAuth App
1. Go to GitHub Settings → Developer settings → OAuth Apps
2. Create a New OAuth App with:
   - **Application name**: Omninet Security
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
3. Note down Client ID and Client Secret

#### Google OAuth2 Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google+ API
4. Create OAuth 2.0 credentials with:
   - **Authorized JavaScript origins**: `http://localhost:8080`
   - **Authorized redirect URIs**: `http://localhost:8080/login/oauth2/code/google`
5. Note down Client ID and Client Secret

### 2. Environment Variables

Set the following environment variables:

```bash
export GITHUB_CLIENT_ID="your-github-client-id"
export GITHUB_CLIENT_SECRET="your-github-client-secret"
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

Or create a `.env` file (not recommended for production):

```bash
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### 3. Running the Application

```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/user` | Get current authenticated user |
| GET | `/api/auth/status` | Get authentication status |
| POST | `/api/auth/logout` | Logout current user |

### Todo Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/todos` | Get all todos for authenticated user |
| POST | `/api/todos` | Create a new todo |
| GET | `/api/todos/{id}` | Get specific todo |
| PUT | `/api/todos/{id}` | Update todo |
| PATCH | `/api/todos/{id}/toggle` | Toggle todo completion |
| DELETE | `/api/todos/{id}` | Delete todo |
| GET | `/api/todos/stats` | Get todo statistics |

### Request/Response Examples

#### Create Todo
```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Learn OAuth2", "description": "Implement OAuth2 with Spring Boot"}' \
  --cookie-jar cookies.txt
```

#### Get User Info
```bash
curl -X GET http://localhost:8080/api/auth/user \
  --cookie cookies.txt
```

## Frontend Integration

This backend is designed to work with React frontends. Key considerations:

### CORS Configuration
- Configured for `localhost:3000` and `localhost:3001`
- Supports credentials for session-based auth

### Session-Based Authentication
- Uses HTTP-only cookies for security
- No JWT tokens needed for simple setups
- Automatic session management

### Example React Integration
```javascript
// Check authentication status
const checkAuth = async () => {
  const response = await fetch('http://localhost:8080/api/auth/status', {
    credentials: 'include'
  });
  return response.json();
};

// Fetch todos
const getTodos = async () => {
  const response = await fetch('http://localhost:8080/api/todos', {
    credentials: 'include'
  });
  return response.json();
};
```

## Project Structure

```
src/main/java/org/zemo/omninetsecurity/
├── config/
│   └── SecurityConfig.java          # OAuth2 and security configuration
├── controller/
│   ├── AuthController.java          # Authentication endpoints
│   ├── HomeController.java          # Web routes
│   └── TodoController.java          # Todo API endpoints
├── model/
│   ├── User.java                    # User model
│   └── Todo.java                    # Todo model
├── service/
│   └── TodoService.java             # Todo business logic
└── OmninetSecurityApplication.java  # Main application class
```

## Extracting OAuth2 Module

To use just the OAuth2 functionality in another project:

1. Copy these files:
   - `config/SecurityConfig.java`
   - `controller/AuthController.java`
   - `model/User.java`
   - `application.properties` (OAuth2 sections)

2. Remove Todo-related files:
   - `controller/TodoController.java`
   - `service/TodoService.java`
   - `model/Todo.java`

3. Update dependencies in `pom.xml` (keep OAuth2 and security starters)

## Security Features

- **CSRF Protection**: Disabled for API usage, can be enabled for web forms
- **CORS**: Configured for cross-origin requests
- **Session Security**: HTTP-only cookies, secure in production
- **OAuth2 Scopes**: Minimal required permissions

## Production Considerations

1. **Environment Variables**: Use proper secret management
2. **HTTPS**: Enable SSL in production
3. **Database**: Replace in-memory storage with persistent database
4. **Session Store**: Use Redis or database for session storage
5. **CORS**: Restrict origins to your actual frontend domains

## Development Tips

- Check `application.properties` for debug logging
- Use browser dev tools to inspect OAuth2 flow
- Test API endpoints with tools like Postman or curl
- Monitor application logs for OAuth2 debugging

## Technologies Used

- **Spring Boot 3.5.4**
- **Spring Security 6**
- **Spring OAuth2 Client**
- **Lombok** (for reducing boilerplate)
- **Thymeleaf** (for simple web templates)
- **Java 21**
