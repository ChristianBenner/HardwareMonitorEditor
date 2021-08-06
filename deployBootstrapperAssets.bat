@echo off
set BOOTSTRAPPER_DEPLOY_FOLDER=..\HardwareMonitorClientBootstrapper\deploy

if [%1]==[] (
echo "Using default deploy folder %BOOTSTRAPPER_DEPLOY_FOLDER%"
) ELSE (
set BOOTSTRAPPER_DEPLOY_FOLDER= %1
echo "Deploy folder set to %1"
)

set APPLICATION_JAR_PATH=out\artifacts\HardwareMonitorEditor\HardwareMonitorEditor.jar

echo "Copying application jar '%APPLICATION_JAR_PATH%' to bootstrapper deploy folder '%BOOTSTRAPPER_DEPLOY_FOLDER%'
copy %APPLICATION_JAR_PATH% %BOOTSTRAPPER_DEPLOY_FOLDER%

