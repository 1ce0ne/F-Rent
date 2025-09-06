@echo off
chcp 1251 >nul
echo Creating Docker archive for akkubatt-work...

:: Remove old archives
if exist akkubatt-work.tar del akkubatt-work.tar

:: Check Docker is running
docker version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running
    pause
    exit /b 1
)

:: Remove old image if exists
docker rmi akkubatt-work:latest >nul 2>&1

:: Build image
echo Building image...
docker build -t akkubatt-work:latest .
if errorlevel 1 (
    echo ERROR: Failed to build image
    pause
    exit /b 1
)

:: Check image was created
docker images | findstr akkubatt-work >nul
if errorlevel 1 (
    echo ERROR: Image not found after build
    pause
    exit /b 1
)

:: Show created image info
echo Image details:
docker images akkubatt-work:latest

:: Create archive with verification
echo Creating archive...
docker save akkubatt-work:latest > akkubatt-work.tar
if errorlevel 1 (
    echo ERROR: Failed to create archive
    pause
    exit /b 1
)

:: Check archive size
for %%A in (akkubatt-work.tar) do set size=%%~zA
if %size% LSS 50000000 (
    echo ERROR: Archive too small ^(%size% bytes^)
    del akkubatt-work.tar
    pause
    exit /b 1
)

echo SUCCESS: Archive created successfully ^(%size% bytes^)

:: Upload archive to server
echo Uploading archive to server...
scp .\akkubatt-work.tar work_root:/tmp/
if errorlevel 1 (
    echo ERROR: Failed to upload archive
    pause
    exit /b 1
)

:: Load Docker image on server
echo Loading Docker image on server...
ssh work_root "docker load -i /tmp/akkubatt-work.tar && echo 'Docker image loaded successfully!'"
if errorlevel 1 (
    echo ERROR: Failed to load Docker image
    pause
    exit /b 1
)

:: Clean up temporary files on server
echo Cleaning up temporary files...
ssh work_root "rm -f /tmp/akkubatt-work.tar && echo 'Cleanup completed'"

echo.
echo ========================================
echo SUCCESS: Docker image deployed!
echo ========================================
echo.
echo Docker image akkubatt-work:latest is now available on server
echo.
echo You can check with:
echo ssh work_root "docker images | grep akkubatt-work"
echo.
pause
