@echo off
cd /d %~dp0

javac Cats2048.java
if %errorlevel% neq 0 (
  echo コンパイルに失敗しました。
  pause
  exit /b
)

java Cats2048
pause