@echo off
set DEPLOY_FOLDER=Releases\Unversioned\Binaries

set DEPLOY_FILE=%1
echo "File path set to %DEPLOY_FILE%"
if not exist "%DEPLOY_FILE%" (
	echo "Error: File '%DEPLOY_FILE%' not found"
	exit /b
)

if [%2]==[] (
	echo "Using default deploy folder %DEPLOY_FOLDER%"
) else (
	set DEPLOY_FOLDER=%2
	echo "Deploy folder set to %2"
)

if not exist "%DEPLOY_FOLDER%" mkdir %DEPLOY_FOLDER%

echo "Copying display application '%DEPLOY_FILE%' to deploy folder '%DEPLOY_FOLDER%'
xcopy %DEPLOY_FILE% %DEPLOY_FOLDER% /e /y /i /r