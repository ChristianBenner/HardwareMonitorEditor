@echo off
set SOLUTION_FILE=%1
if not exist "%SOLUTION_FILE%" (
	echo "Error: Solution '%SOLUTION_FILE%' not found"
	exit /b
)

echo "Compiling bootstrapper %SOLUTION_FILE%"
msbuild %SOLUTION_FILE% /p:Configuration=Release /p:Platform="x64"