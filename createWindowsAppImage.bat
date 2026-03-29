@echo off
setlocal

REM プロジェクトルートへ移動
cd /d "%~dp0"

REM app-image（EXE直接起動用）を作成
REM カスタムdoLastタスクがあるため、configuration cacheはこの実行のみ無効化
call gradlew.bat --no-configuration-cache :app:createWindowsAppImage
if errorlevel 1 (
    echo [ERROR] createWindowsAppImage task failed.
    exit /b 1
)

echo [INFO] createWindowsAppImage task completed successfully.
exit /b 0
