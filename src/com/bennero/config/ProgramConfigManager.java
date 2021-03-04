/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * An additional term included with this license is the requirement to preserve legal notices and author attributions
 * such as this one. Do not remove the original author license notices from the program unless given permission from
 * the original author: christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.config;

import com.bennero.networking.ConnectionInformation;
import org.xml.sax.Attributes;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;

import static com.bennero.networking.NetworkUtils.ip4AddressToString;
import static com.bennero.networking.NetworkUtils.macAddressToString;

/**
 * The ProgramConfigManager class is a singleton that manages the reading and saving of all program wide configuration
 * data. This includes data such as the last loaded save and information of the last connected device. Program wide save
 * data is necessary in providing automated connection to a hardware monitor upon log-on.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class ProgramConfigManager extends ConfigurationSaveHandler
{
    private static final String CONFIG_FILE_PATH = System.getenv("APPDATA") + "\\BennerHardwareMonitor";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String CONFIG_TAG = "config";
    private static final String FILE_AREA_PATH_TAG = "fileAreaPath";
    private static final String LAST_LOADED_FILE_PATH_TAG = "lastLoadedFilePath";
    private static final String LAST_CONNECTED_MAJOR_VERSION_TAG = "lastConnectedVersionMajor";
    private static final String LAST_CONNECTED_MINOR_VERSION_TAG = "lastConnectedVersionMinor";
    private static final String LAST_CONNECTED_PATCH_VERSION_TAG = "lastConnectedVersionPatch";
    private static final String LAST_CONNECTED_IP4_TAG = "lastConnectedIp4";
    private static final String LAST_CONNECTED_MAC_TAG = "lastConnectedMac";
    private static final String LAST_CONNECTED_HOSTNAME = "lastConnectedHostname";

    private static ProgramConfigManager singletonInstance = null;

    private String fileAreaPath;
    private String lastLoadedFilePath;
    private byte lastConnectedMajorVersion;
    private byte lastConnectedMinorVersion;
    private byte lastConnectedPatchVersion;
    private String lastConnectedIp4;
    private String lastConnectedMac;
    private String lastConnectedHostname;

    private boolean fileAreaPathAvailable;
    private boolean lastLoadedFilePathAvailable;
    private boolean lastConnectedMajorVersionAvailable;
    private boolean lastConnectedMinorVersionAvailable;
    private boolean lastConnectedPatchVersionAvailable;
    private boolean lastConnectedIp4Available;
    private boolean lastConnectedMacAvailable;
    private boolean lastConnectedHostnameAvailable;

    private ProgramConfigManager()
    {
        super(new File(CONFIG_FILE_PATH + "\\" + CONFIG_FILE_NAME));

        // Create save location folder
        File saveLocationFile = new File(CONFIG_FILE_PATH);
        if (!saveLocationFile.exists())
        {
            saveLocationFile.mkdirs();
        }

        fileAreaPathAvailable = false;
        lastLoadedFilePathAvailable = false;
        lastConnectedMajorVersionAvailable = false;
        lastConnectedMinorVersionAvailable = false;
        lastConnectedPatchVersionAvailable = false;
        lastConnectedIp4Available = false;
        lastConnectedMacAvailable = false;
        lastConnectedHostnameAvailable = false;
        super.read();
    }

    public static ProgramConfigManager getInstance()
    {
        if (singletonInstance == null)
        {
            singletonInstance = new ProgramConfigManager();
        }

        return singletonInstance;
    }

    @Override
    protected void read(String uri, String localName, String qName, Attributes attributes)
    {
        System.out.println("Start Element: " + qName);

        switch (qName)
        {
            case CONFIG_TAG:
                for (int i = 0; i < attributes.getLength(); i++)
                {
                    String attributeName = attributes.getQName(i);
                    String attributeValue = attributes.getValue(i);
                    System.out.print(" '" + attributeName + "':" + attributeValue);

                    // Parse attributes
                    if (attributeName.compareTo(FILE_AREA_PATH_TAG) == 0)
                    {
                        fileAreaPath = attributeValue;
                        fileAreaPathAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_LOADED_FILE_PATH_TAG) == 0)
                    {
                        lastLoadedFilePath = attributeValue;
                        lastLoadedFilePathAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_MAJOR_VERSION_TAG) == 0)
                    {
                        lastConnectedMajorVersion = (byte) Integer.parseInt(attributeValue);
                        lastConnectedMajorVersionAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_MINOR_VERSION_TAG) == 0)
                    {
                        lastConnectedMinorVersion = (byte) Integer.parseInt(attributeValue);
                        lastConnectedMinorVersionAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_PATCH_VERSION_TAG) == 0)
                    {
                        lastConnectedPatchVersion = (byte) Integer.parseInt(attributeValue);
                        lastConnectedPatchVersionAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_IP4_TAG) == 0)
                    {
                        lastConnectedIp4 = attributeValue;
                        lastConnectedIp4Available = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_MAC_TAG) == 0)
                    {
                        lastConnectedMac = attributeValue;
                        lastConnectedMacAvailable = true;
                    }
                    else if (attributeName.compareTo(LAST_CONNECTED_HOSTNAME) == 0)
                    {
                        lastConnectedHostname = attributeValue;
                        lastConnectedHostnameAvailable = true;
                    }
                }

                System.out.println();
                break;
        }
    }

    @Override
    protected void save(XMLStreamWriter streamWriter) throws XMLStreamException
    {
        int depth = 0;
        streamWriter.writeStartDocument();
        writeIndentation(streamWriter, depth, true);
        streamWriter.writeStartElement(CONFIG_TAG);

        if (fileAreaPathAvailable)
        {
            streamWriter.writeAttribute(FILE_AREA_PATH_TAG, fileAreaPath);
        }

        if (lastLoadedFilePathAvailable)
        {
            streamWriter.writeAttribute(LAST_LOADED_FILE_PATH_TAG, lastLoadedFilePath);
        }

        if (lastConnectedMajorVersionAvailable)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_MAJOR_VERSION_TAG,
                    Integer.toString(lastConnectedMajorVersion & 0xFF));
        }

        if (lastConnectedMinorVersionAvailable)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_MINOR_VERSION_TAG,
                    Integer.toString(lastConnectedMinorVersion & 0xFF));
        }

        if (lastConnectedPatchVersionAvailable)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_PATCH_VERSION_TAG,
                    Integer.toString(lastConnectedPatchVersion & 0xFF));
        }

        if (lastConnectedIp4Available)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_IP4_TAG, lastConnectedIp4);
        }

        if (lastConnectedMacAvailable)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_MAC_TAG, lastConnectedMac);
        }

        if (lastConnectedHostnameAvailable)
        {
            streamWriter.writeAttribute(LAST_CONNECTED_HOSTNAME, lastConnectedHostname);
        }

        streamWriter.writeEndElement();
        writeIndentation(streamWriter, --depth, true);
        streamWriter.writeEndDocument();
    }

    public String getFileAreaPath()
    {
        return fileAreaPath;
    }

    public void setFileAreaPath(String fileAreaPath)
    {
        this.fileAreaPath = fileAreaPath;
        this.fileAreaPathAvailable = true;
        super.save();
    }

    public String getLastLoadedFilePath()
    {
        return lastLoadedFilePath;
    }

    public void setLastLoadedFilePath(String lastLoadedFilePath)
    {
        this.lastLoadedFilePath = lastLoadedFilePath;
        this.lastLoadedFilePathAvailable = true;
        super.save();
    }

    public String getLastConnectedIp4()
    {
        return lastConnectedIp4;
    }

    public void setLastConnectedIp4(String lastConnectedIp4)
    {
        this.lastConnectedIp4 = lastConnectedIp4;
        this.lastConnectedIp4Available = true;
        super.save();
    }

    public String getLastConnectedMac()
    {
        return lastConnectedMac;
    }

    public void setLastConnectedMac(String lastConnectedMac)
    {
        this.lastConnectedMac = lastConnectedMac;
        this.lastConnectedMacAvailable = true;
        super.save();
    }

    public String getLastConnectedHostname()
    {
        return lastConnectedHostname;
    }

    public void setLastConnectedHostname(String lastConnectedHostname)
    {
        this.lastConnectedHostname = lastConnectedHostname;
        this.lastConnectedHostnameAvailable = true;
        super.save();
    }

    public boolean containsCompleteConnectionInfo()
    {
        return lastConnectedMajorVersionAvailable && lastConnectedMinorVersionAvailable &&
                lastConnectedPatchVersionAvailable && lastConnectedIp4Available && lastConnectedMacAvailable &&
                lastConnectedHostnameAvailable && lastConnectedIp4 != null && lastConnectedMac != null &&
                lastConnectedHostname != null && !lastConnectedIp4.isEmpty() && !lastConnectedMac.isEmpty() &&
                !lastConnectedHostname.isEmpty();
    }

    public void setConnectionData(ConnectionInformation connectionInformation)
    {
        // Build IP4 address string
        final String macAddressStr = macAddressToString(connectionInformation.getMacAddress());
        final String ip4AddressStr = ip4AddressToString(connectionInformation.getIp4Address());
        setConnectionData(connectionInformation.getMajorVersion(), connectionInformation.getMinorVersion(),
                connectionInformation.getPatchVersion(), ip4AddressStr, macAddressStr,
                connectionInformation.getHostname());
    }

    public void setConnectionData(byte lastConnectedMajorVersion, byte lastConnectedMinorVersion,
                                  byte lastConnectedPatchVersion, String lastConnectedIp4,
                                  String lastConnectedMac, String lastConnectedHostname)
    {
        this.lastConnectedMajorVersion = lastConnectedMajorVersion;
        this.lastConnectedMinorVersion = lastConnectedMinorVersion;
        this.lastConnectedPatchVersion = lastConnectedPatchVersion;
        this.lastConnectedIp4 = lastConnectedIp4;
        this.lastConnectedMac = lastConnectedMac;
        this.lastConnectedHostname = lastConnectedHostname;
        this.lastConnectedMajorVersionAvailable = true;
        this.lastConnectedMinorVersionAvailable = true;
        this.lastConnectedPatchVersionAvailable = true;
        this.lastConnectedIp4Available = true;
        this.lastConnectedMacAvailable = true;
        this.lastConnectedHostnameAvailable = true;
        super.save();
    }

    public ConnectionInformation getConnectionInformation()
    {
        return new ConnectionInformation(lastConnectedMajorVersion, lastConnectedMinorVersion,
                lastConnectedPatchVersion, lastConnectedMac, lastConnectedIp4, lastConnectedHostname);
    }

    public boolean isFileAreaPathAvailable()
    {
        return fileAreaPathAvailable;
    }

    public boolean isLastLoadedFilePathAvailable()
    {
        return lastLoadedFilePathAvailable;
    }

    public boolean isLastConnectedIp4Available()
    {
        return lastConnectedIp4Available;
    }

    public boolean isLastConnectedMacAvailable()
    {
        return lastConnectedMacAvailable;
    }

    public boolean isLastConnectedHostnameAvailable()
    {
        return lastConnectedHostnameAvailable;
    }
}