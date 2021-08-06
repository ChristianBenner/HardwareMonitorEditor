@echo off
set DEPLOY_FOLDER="..\HardwareMonitorClientBootstrapper\deploy\jre"
set JLINK_LOC="C:\Program Files\Java\jdk-16.0.2\bin\jlink"
set MEDUSA_LIB_LOC="lib\Medusa-11.5.jar"
set JMODS_LOC="lib\jmods"

if [%1]==[] (
	echo Using default deploy folder %DEPLOY_FOLDER%
) else (
	set DEPLOY_FOLDER= %1
	echo Deploy folder set to %DEPLOY_FOLDER%
)

if exist %DEPLOY_FOLDER% (
	echo JRE folder exists at %DEPLOY_FOLDER%, removing and generating new JRE
	rd /q /s %DEPLOY_FOLDER%
)

if [%2]==[] (
	echo Using default jLink path %JLINK_LOC%
) else (
	set JLINK_LOC=%2
)

if not exist %MEDUSA_LIB_LOC% (
	echo Error: Medusa library not found at location %MEDUSA_LIB_LOC%
	Exit /b
)

if not exist %JMODS_LOC% (
	echo Error: jmods not found at location %JMODS_LOC%
	Exit /b
)

%JLINK_LOC% --module-path %MEDUSA_LIB_LOC%;%JMODS_LOC%; --strip-debug --no-man-pages --add-modules javafx.controls,javafx.base,javafx.graphics,javafx.media,javafx.web,eu.hansolo.medusa --output %DEPLOY_FOLDER%
echo Created new JRE using jlink %JLINK_LOC% at location %DEPLOY_FOLDER%