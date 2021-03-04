set BOOTSTRAPPER_DEPLOY_FOLDER=..\HardwareMonitorClientBootstrapper\deploy

if [%1]==[] (
echo "Using default deploy folder %BOOTSTRAPPER_DEPLOY_FOLDER%"
) ELSE (
set BOOTSTRAPPER_DEPLOY_FOLDER=%1
echo "Deploy folder set to %1"
)

IF NOT EXIST %BOOTSTRAPPER_DEPLOY_FOLDER% (
	echo "Bootstrapper Deploy Folder not found at '%BOOTSTRAPPER_DEPLOY_FOLDER%'"
	Exit /b
)

set JRE_FOLDER_NAME=jre
set JRE_FOLDER_PATH=%BOOTSTRAPPER_DEPLOY_FOLDER%\%JRE_FOLDER_NAME%

if EXIST %JRE_FOLDER_PATH% (
	echo "JRE folder exists at %JRE_FOLDER_PATH%, removing and generating new JRE"
	rd /q /s %JRE_FOLDER_PATH%
)

"C:\Program Files\Java\jdk-14.0.1\bin\jlink" --module-path lib\Medusa-11.5.jar;lib\jmods; --add-modules javafx.controls,javafx.base,javafx.graphics,javafx.media,javafx.web,eu.hansolo.medusa --output %JRE_FOLDER_PATH%
echo "Created new JRE using jlink at location %JRE_FOLDER_PATH%