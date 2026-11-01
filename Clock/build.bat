@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo 正在清理旧的编译产物...
if exist build rmdir /s /q build
mkdir build

echo 正在编译 src\clock\*.java ...
javac -encoding UTF-8 -d build src\clock\*.java

if errorlevel 1 (
    echo.
    echo 编译失败，请检查上面的错误信息。
    pause
    exit /b 1
)

echo.
echo 编译完成，输出目录：build\
pause
