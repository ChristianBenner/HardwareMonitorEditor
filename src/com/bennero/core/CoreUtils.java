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

package com.bennero.core;

import com.bennero.config.ProgramConfigManager;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * CoreUtils provides some utilities such as file selection windows and the ability to open the users default browser to
 * a specified URL
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class CoreUtils
{
    public static File showDirectorySelector()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        return directoryChooser.showDialog(ApplicationCore.getInstance().getStage());
    }

    public static File showFileSelector()
    {
        ProgramConfigManager programConfigManager = ProgramConfigManager.getInstance();
        FileChooser fileChooser = new FileChooser();

        // If the program config manager contains a file area path, open the file chooser window there
        if (programConfigManager.isFileAreaPathAvailable())
        {
            File saveAreaFile = new File(programConfigManager.getFileAreaPath());
            if (saveAreaFile.exists() && saveAreaFile.isDirectory())
            {
                fileChooser.setInitialDirectory(saveAreaFile);
            }
        }

        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(
                "Benner Hardware Monitor Save (*.bhwms)", "*.bhwms");
        fileChooser.getExtensionFilters().add(extensionFilter);

        return fileChooser.showOpenDialog(ApplicationCore.getInstance().getStage());
    }

    public static void openBrowser(String url)
    {
        ApplicationCore.getInstance().getHostServices().showDocument(url);
    }
}