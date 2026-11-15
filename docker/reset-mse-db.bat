@echo off
setlocal

REM ============================================================
REM Reset local MSE databases:
REM   - empty mse_platform (keep the empty database)
REM   - drop mse / mse_tenant / mse_tenant1 / mse_tenant2 / mse_tenant3
REM Usage: double-click, or run `reset-mse-db.bat` from a shell.
REM ============================================================

set CONTAINER=mse-mysql
set MYSQL_USER=root
set MYSQL_PASS=123456
set SQLFILE=%TEMP%\mse-reset.sql

echo [1/3] Generating cleanup SQL ...
(echo SET FOREIGN_KEY_CHECKS=0;) > "%SQLFILE%"
docker exec %CONTAINER% mysql -N --default-character-set=utf8mb4 -u%MYSQL_USER% -p%MYSQL_PASS% -e "SELECT CONCAT('DROP TABLE IF EXISTS mse_platform.', table_name, ';') FROM information_schema.tables WHERE table_schema='mse_platform';" 2>nul >> "%SQLFILE%"
(echo SET FOREIGN_KEY_CHECKS=1;
echo DROP DATABASE IF EXISTS mse;
echo DROP DATABASE IF EXISTS mse_tenant;
echo DROP DATABASE IF EXISTS mse_tenant1;
echo DROP DATABASE IF EXISTS mse_tenant2;
echo DROP DATABASE IF EXISTS mse_tenant3;) >> "%SQLFILE%"

echo [2/3] Executing cleanup ...
docker exec -i %CONTAINER% mysql --default-character-set=utf8mb4 -u%MYSQL_USER% -p%MYSQL_PASS% < "%SQLFILE%" 2>nul
if errorlevel 1 (
    echo [ERROR] Cleanup failed. Check that Docker container %CONTAINER% is running and credentials are correct.
    del "%SQLFILE%" >nul 2>&1
    pause
    exit /b 1
)
del "%SQLFILE%" >nul 2>&1

echo [3/3] Verifying result:
docker exec %CONTAINER% mysql --default-character-set=utf8mb4 -u%MYSQL_USER% -p%MYSQL_PASS% -e "SHOW DATABASES LIKE 'mse%%'; SELECT COUNT(*) AS mse_platform_remaining_tables FROM information_schema.tables WHERE table_schema='mse_platform';" 2>nul

echo.
echo Done: only the empty mse_platform database remains.
echo Note: if MSE services are still running, they will recreate baseline tables / tenant databases on next start.
pause
