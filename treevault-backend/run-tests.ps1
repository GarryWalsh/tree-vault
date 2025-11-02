# PowerShell script to run tests using Docker
# This works without installing Maven locally
# Testcontainers will automatically detect Docker Desktop on Windows

Write-Host "Running tests with Docker..." -ForegroundColor Green

# Navigate to backend directory
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptPath

try {
    # Get absolute path for Windows Docker volume mount
    $absolutePath = (Resolve-Path .).Path
    
    Write-Host "Working directory: $absolutePath" -ForegroundColor Cyan
    
    # Run tests in Docker container
    # Note: On Windows, Docker Desktop handles Docker socket access automatically
    docker run --rm `
        -v "${absolutePath}:/app" `
        -w /app `
        maven:3.9-eclipse-temurin-21 `
        mvn clean test
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nAll tests passed!" -ForegroundColor Green
    } else {
        Write-Host "`nTests failed. Check output above." -ForegroundColor Red
        exit $LASTEXITCODE
    }
} catch {
    Write-Host "Error running tests: $_" -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}

