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

import com.bennero.common.PageData;
import com.bennero.common.Sensor;
import com.bennero.client.core.ApplicationCore;
import com.bennero.client.core.CoreUtils;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.util.PageGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

/**
 * SaveManager is a singleton that provides the ability to create or load save data. Save data is the pages that the
 * user has created and customised to display their systems hardware information.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SaveManager
{
    private static SaveManager instance = null;

    private SaveData currentSaveData;

    private SaveManager()
    {
        currentSaveData = null;
    }

    public static SaveManager getInstance()
    {
        if (instance == null)
        {
            instance = new SaveManager();
        }

        return instance;
    }

    public static boolean displayOpenSaveUI()
    {
        File selectedFile = CoreUtils.showFileSelector();

        if (selectedFile != null)
        {
            // Remove all pages from hardware monitor
            purgeCurrentPages();
            SaveManager.getInstance().loadSave(selectedFile);

            return true;
        }

        return false;
    }

    public static boolean displayNewSaveUI()
    {
        final ProgramConfigManager programConfigManager = ProgramConfigManager.getInstance();
        final SaveManager saveManager = SaveManager.getInstance();

        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Enter Layout Name");
        textInputDialog.setHeaderText("Enter Layout Name");
        textInputDialog.setContentText("Enter a name for your layout:");
        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent())
        {
            if (!result.get().isEmpty() && !result.get().replaceAll(" ", "").isEmpty())
            {
                String fileName = result.get();
                if (!fileName.endsWith(".bhwms"))
                {
                    fileName += ".bhwms";
                }

                // Check if that already exists
                if (new File(programConfigManager.getFileAreaPath() + "\\" + fileName).exists())
                {
                    // Error pop-up because it will not fit
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Save File Already Exists", ButtonType.OK);
                    alert.setContentText("A save file with that name already exists in the directory, please use another name");
                    alert.showAndWait();
                }
                else
                {
                    // Remove all pages from hardware monitor
                    saveManager.purgeCurrentPages();

                    // Update the config so that this is the last opened file and therefore will open on next
                    // start-up
                    saveManager.newSave(new File(programConfigManager.getFileAreaPath() + "\\" + fileName));
                    return true;
                }
            }
        }

        return false;
    }

    private static void purgeCurrentPages()
    {
        // If current save data is not null, remove all the pages from the monitor
        SaveData saveData = SaveManager.getInstance().getSaveData();
        if (saveData != null)
        {
            final NetworkClient networkClient = NetworkClient.getInstance();
            final ArrayList<PageData> pageData = saveData.getPageDataList();
            for (int i = 0; i < pageData.size(); i++)
            {
                networkClient.removePageMessage((byte) pageData.get(i).getUniqueId());
            }
        }
    }

    public boolean loadPreviousSave()
    {
        ProgramConfigManager programConfigManager = ProgramConfigManager.getInstance();
        if (programConfigManager.isLastLoadedFilePathAvailable())
        {
            return loadSave(new File(programConfigManager.getLastLoadedFilePath()));
        }

        return false;
    }

    public boolean loadSave(File file)
    {
        if (file != null && file.exists())
        {
            currentSaveData = new SaveData(file);
            ArrayList<PageData> pageDataList = currentSaveData.getPageDataList();
            int highestId = 0;
            for (int i = 0; i < pageDataList.size(); i++)
            {
                if (pageDataList.get(i).getUniqueId() > highestId)
                {
                    highestId = pageDataList.get(i).getUniqueId();
                }
            }

            PageGenerator.setNextAvailablePageId(highestId + 1);

            // Send all of the pages to the monitor
            if (NetworkClient.getInstance().isConnected())
            {
                for (PageData pageData : pageDataList)
                {
                    NetworkClient.getInstance().writePageMessage(pageData);

                    // Send all sensors contained in the pages to the monitor
                    for (Sensor sensor : pageData.getSensorList())
                    {
                        NetworkClient.getInstance().writeSensorMessage(sensor, (byte) pageData.getUniqueId());
                    }
                }
            }

            ProgramConfigManager.getInstance().setLastLoadedFilePath(file.getAbsolutePath());
            ApplicationCore.getInstance().getWindow().updateWindowTitle(file.getName());
            return true;
        }

        return false;
    }

    public void newSave(File file)
    {
        if (!file.getAbsolutePath().endsWith(".bhwms"))
        {
            currentSaveData = new SaveData(new File(file.getAbsolutePath() + ".bhwms"));
        }
        else
        {
            currentSaveData = new SaveData(file);
        }

        currentSaveData.save();

        ProgramConfigManager.getInstance().setLastLoadedFilePath(file.getAbsolutePath());
    }

    public boolean containsSaveData()
    {
        return currentSaveData != null;
    }

    public SaveData getSaveData()
    {
        return currentSaveData;
    }
}
