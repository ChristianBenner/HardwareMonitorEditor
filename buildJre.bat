@echo off
set DEPLOY_FOLDER="..\HardwareMonitorClientBootstrapper\deploy\jre"
set JLINK_LOC="C:\Users\chris\.jdks\openjdk-22.0.1\bin\jlink"
set MEDUSA_LIB_LOC="C:\libs\medusa-17.0.0.jar"
set SERIAL_LIB_LOC="C:\libs\jSerialComm-2.11.0.jar"
set JMODS_LOC="C:\libs\jmods"

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
	echo Using jLink path %2
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

if not exist %SERIAL_LIB_LOC% (
	echo Error: serial lib not found at location %SERIAL_LIB_LOC%
	Exit /b
)

%JLINK_LOC% --module-path %MEDUSA_LIB_LOC%;%JMODS_LOC%;%SERIAL_LIB_LOC%; --strip-debug --no-man-pages --add-modules javafx.controls,javafx.base,javafx.graphics,javafx.media,javafx.web,eu.hansolo.medusa,com.fazecast.jSerialComm --output %DEPLOY_FOLDER%
echo Created new JRE using jlink %JLINK_LOC% at location %DEPLOY_FOLDER%