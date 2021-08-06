@echo off
set RELEASE_FOLDER=Releases\Unversioned\Binaries

set DEPLOY_FILE=%1
echo "File path set to %DEPLOY_FILE%"
if not exist "%DEPLOY_FILE%" (
	echo "Error: File '%DEPLOY_FILE%' not found"
	exit /b
)

if [%2]==[] (
	echo "Using default deploy folder %RELEASE_FOLDER%"
) else (
	set RELEASE_FOLDER=%2
	echo "Deploy folder set to %2"
)

if not exist "%RELEASE_FOLDER%" mkdir %RELEASE_FOLDER%

echo "Copying editor installer '%DEPLOY_FILE%' to release folder '%RELEASE_FOLDER%'
xcopy %DEPLOY_FILE% %RELEASE_FOLDER% /e /y /i /r