#!/bin/bash

# Storage API Testing Script
# Usage: ./test_storage.sh <email> <token> [base_url]
# Example: ./test_storage.sh user@example.com your_jwt_token http://localhost:8080

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
BASE_URL="http://localhost:8080"
EMAIL="udaykhare77@gmail.com"
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoiVWRheSBLaGFyZSIsInRva2VuVHlwZSI6ImFjY2VzcyIsInVzZXJJZCI6IjEwMzUwOTA2NjE4NDU3NjYzNDE4NiIsImVtYWlsIjoidWRheWtoYXJlNzdAZ21haWwuY29tIiwic3ViIjoiMTAzNTA5MDY2MTg0NTc2NjM0MTg2IiwiaXNzIjoib21uaW5ldC1zZWN1cml0eSIsImlhdCI6MTc1NzkzODA3OCwiZXhwIjoxNzU3OTQxNjc4fQ.qWEqxQvHtpeQ22-X62cWJqy4uk_91daq5k_ugS9PszCvm3TiiRVXx6HXgJBjjGwJ5Rra0jFRPjaR8Un-aGTQpg"

# Parse arguments
if [ $# -lt 2 ]; then
    echo "Usage: $0 <email> <token> [base_url]"
    echo "Example: $0 user@example.com your_jwt_token http://localhost:8080"
    exit 1
fi

EMAIL="$1"
TOKEN="$2"
if [ $# -ge 3 ]; then
    BASE_URL="$3"
fi

API_BASE="${BASE_URL}/api/storage"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to make API calls
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local description="$4"
    
    print_status "Testing: $description"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "$API_BASE$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" \
            -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$API_BASE$endpoint")
    fi
    
    # Extract HTTP status code (last line)
    http_code=$(echo "$response" | tail -n1)
    # Extract response body (all lines except last)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" -eq 200 ]; then
        print_success "$description - HTTP $http_code"
        echo "Response: $response_body" | jq '.' 2>/dev/null || echo "Response: $response_body"
    else
        print_error "$description - HTTP $http_code"
        echo "Response: $response_body"
    fi
    
    echo
}

# Function to test security (should fail)
test_security() {
    local description="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    
    print_status "Security Test: $description"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "$API_BASE$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" \
            -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$API_BASE$endpoint")
    fi
    
    # Extract HTTP status code (last line)
    http_code=$(echo "$response" | tail -n1)
    # Extract response body (all lines except last)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" -eq 400 ]; then
        print_success "Security test passed - Request properly rejected (HTTP $http_code)"
    else
        print_warning "Security test concern - Request not rejected (HTTP $http_code)"
    fi
    
    echo "Response: $response_body"
    echo
}

echo "=========================================="
echo "Storage API Testing Script"
echo "=========================================="
echo "Email: $EMAIL"
echo "Token: ${TOKEN:0:20}..."
echo "Base URL: $BASE_URL"
echo "=========================================="
echo

# Test 1: List root contents
api_call "GET" "/contents" "" "List root contents"

# Test 2: List user folders
api_call "GET" "/folders" "" "List user folders"

# Test 3: Create folders
api_call "POST" "/folders" '{"folderName": "documents"}' "Create documents folder"
api_call "POST" "/folders" '{"folderName": "images"}' "Create images folder"
api_call "POST" "/folders" '{"folderName": "documents/reports"}' "Create nested reports folder"
api_call "POST" "/folders" '{"folderName": "documents/contracts"}' "Create nested contracts folder"

# Test 4: List folders after creation
api_call "GET" "/folders" "" "List user folders after creation"
api_call "GET" "/contents" "" "List root contents after creation"

# Test 5: Browse folder contents
api_call "GET" "/folders/documents/contents" "" "Browse documents folder"
api_call "GET" "/folders/images/contents" "" "Browse images folder"

# Test 6: Check folder existence
api_call "GET" "/folders/documents/exists" "" "Check if documents folder exists"
api_call "GET" "/folders/nonexistent/exists" "" "Check if nonexistent folder exists"

# Test 7: Generate upload URLs
api_call "POST" "/files/upload-url" '{"fileName": "test.txt"}' "Generate upload URL for root file"
api_call "POST" "/files/upload-url" '{"fileName": "documents/report.pdf"}' "Generate upload URL for nested file"
api_call "POST" "/files/upload-url" '{"fileName": "images/photo.jpg"}' "Generate upload URL for image file"

# Test 8: Check file existence
api_call "GET" "/files/test.txt/exists" "" "Check if test.txt exists"
api_call "GET" "/files/documents/report.pdf/exists" "" "Check if report.pdf exists"

# Test 9: Generate download URLs (these might fail if files don't exist)
print_warning "The following download URL tests might fail if files don't actually exist:"
api_call "POST" "/files/download-url" '{"fileName": "test.txt"}' "Generate download URL for test.txt"
api_call "POST" "/files/download-url" '{"fileName": "documents/report.pdf"}' "Generate download URL for report.pdf"

echo "=========================================="
echo "SECURITY TESTS"
echo "=========================================="
echo

# Security Test 1: Directory traversal in folder creation
test_security "Directory traversal in folder creation" "POST" "/folders" '{"folderName": "../../../etc"}'

# Security Test 2: Directory traversal in folder browsing
test_security "Directory traversal in folder browsing" "GET" "/folders/..%2F..%2Fetc/contents" ""

# Security Test 3: Directory traversal in file operations
test_security "Directory traversal in file upload" "POST" "/files/upload-url" '{"fileName": "../../../etc/passwd"}'

# Security Test 4: Directory traversal in file download
test_security "Directory traversal in file download" "POST" "/files/download-url" '{"fileName": "../../../etc/passwd"}'

# Security Test 5: Absolute path in folder creation
test_security "Absolute path in folder creation" "POST" "/folders" '{"folderName": "/absolute/path"}'

# Security Test 6: Null bytes in folder name
test_security "Null bytes in folder name" "POST" "/folders" '{"folderName": "test\u0000folder"}'

# Security Test 7: Backslashes in folder name
test_security "Backslashes in folder name" "POST" "/folders" '{"folderName": "test\\folder"}'

echo "=========================================="
echo "CLEANUP TESTS"
echo "=========================================="
echo

# Cleanup: Delete created folders (in reverse order for nested folders)
api_call "DELETE" "/folders" '{"folderName": "documents/contracts"}' "Delete contracts folder"
api_call "DELETE" "/folders" '{"folderName": "documents/reports"}' "Delete reports folder"
api_call "DELETE" "/folders" '{"folderName": "documents"}' "Delete documents folder"
api_call "DELETE" "/folders" '{"folderName": "images"}' "Delete images folder"

# Final verification
api_call "GET" "/folders" "" "List user folders after cleanup"
api_call "GET" "/contents" "" "List root contents after cleanup"

echo "=========================================="
echo "Testing completed!"
echo "=========================================="
