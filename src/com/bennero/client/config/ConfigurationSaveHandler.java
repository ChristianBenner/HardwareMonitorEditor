/*
 * ============================================ GNU GENERAL PUBLIC LICENSE =============================================
 * Hardware Monitor for the remote monitoring of a systems hardware information
 * Copyright (C) 2021  Christian Benner
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Additional terms included with this license are to:
 * - Preserve legal notices and author attributions such as this one. Do not remove the original author license notices
 *   from the program
 * - Preserve the donation button and its link to the original authors donation page (christianbenner35@gmail.com)
 * - Only break the terms if given permission from the original author christianbenner35@gmail.com
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * =====================================================================================================================
 */

package com.bennero.client.config;

import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;

/**
 * ConfigurationSaveHandler is a base class that provides some basic methods for saving and reading configuration save
 * data files.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public abstract class ConfigurationSaveHandler extends DefaultHandler {
    // Class name for logging
    private static String CLASS_NAME = ConfigurationSaveHandler.class.getSimpleName();

    private final File file;

    private SAXParserFactory factory;

    public ConfigurationSaveHandler(final File file) {
        this.file = file;
        factory = SAXParserFactory.newInstance();

        Logger.log(LogLevel.INFO, CLASS_NAME, "Using configuration file '" + file.getAbsolutePath() + "'");
    }

    protected abstract void read(String uri, String localName, String qName, Attributes attributes);

    protected abstract void save(XMLStreamWriter streamWriter) throws XMLStreamException;

    protected void read() {
        if (doesFileExist()) {
            try {
                factory.newSAXParser().parse(file, this);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try {
            Logger.log(LogLevel.INFO, CLASS_NAME, "Saving configuration data");
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
            save(streamWriter);
            streamWriter.flush();
            streamWriter.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean doesFileExist() {
        return file.exists();
    }

    protected void writeIndentation(XMLStreamWriter streamWriter, int depth, boolean newLine) throws XMLStreamException {
        String indentation = newLine ? "\n" : "";
        for (int i = 0; i < depth; i++) {
            indentation += "\t";
        }

        streamWriter.writeCharacters(indentation);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        read(uri, localName, qName, attributes);
    }
}
