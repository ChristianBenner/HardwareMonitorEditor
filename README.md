# HardwareMonitorClient
This is the editor required to operate and customise a Hardware Monitor. HardwareMonitorClient project is one of many sub projects to the Hardware Monitor System. The other projects are the [HardwareMonitorClientBootstrapper](https://github.com/ChristianBenner/HardwareMonitorClientBootstrapper), [HardwareMonitorCommon](https://github.com/ChristianBenner/HardwareMonitorCommon), [HardwareMonitor](https://github.com/ChristianBenner/HardwareMonitor) and [NativeInterface](https://github.com/ChristianBenner/NativeInterface).

The Hardware Monitor Client (often referred to as 'Hardware Monitor Editor') is used to communicate and customise a Hardware Monitor. This includes sending hardware sensor data and customised layout/gauge style data.

# Features
- **Customisable Pages**: Multiple pages can be used to categorise different sensor data types. Users can customise titles, colours, layout, gauges and transitions of each page to achieve a unique design.
- **Save System**: Users can create as many designs as they want, save files can be shared with other users. Designs are saved automatically so that no changes are lost.
- **Page Overview**: A useful overview page will display all of the different customised pages within one design.
- **Network Scanning**: The system will scan the network for Hardware Monitors to connect to and provide a list of available devices to the user. Automatic connection to the last connected device means that the user does not need to configure the application to connect to a Hardware Monitor every time it is launched. Eliminates the need for determining and entering IP addresses.
- **Gauges**: Tons of animated and customisable gauges provided by [Medusa](https://github.com/HanSolo/Medusa) library. Provides many ways to present hardware sensor data.
- **Efficiency**: Being a Hardware Monitor, the system is designed to have as little impact on system hardware resource as possible. The usage of JNI through my other library [NativeInterface](https://github.com/ChristianBenner/NativeInterface) to communicate with [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library results in miniscule CPU usage. The software has a state tracking system to construct the GUI only when in use.
- **Not Intrusive**: After the user has created their first design, the GUI can be closed and the software will continue to run in the background/system tray. Upon user sign-in, the software will run in the background, automatically connecting and communicating with the previously connected to Hardware Monitor.
- **Extensive Hardware Monitoring**: The usage of [OpenHardwareMonitor](https://github.com/openhardwaremonitor/openhardwaremonitor) library means that many of the users hardware sensors can be monitored such as CPU, GPU, memory and storage devices.

# Building and Launching
This software is designed to be launched using the [HardwareMonitorClientBootstrapper](https://github.com/ChristianBenner/HardwareMonitorClientBootstrapper). The bootstrapper, and therefor this software, is only currently supported to run on Windows. The monitor however should be compatible with multiple operating systems such as Windows, Raspberry Pi, Ubuntu and MacOS.
