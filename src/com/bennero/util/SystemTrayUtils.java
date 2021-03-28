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

package com.bennero.util;

import com.bennero.core.ApplicationCore;
import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Some basic static utility functions for handling adding and removing the app to the OS system tray.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class SystemTrayUtils
{
    public static TrayIcon addToSystemTray() throws Exception
    {
        // Check if system tray is supported
        if (!SystemTray.isSupported())
        {
            System.err.println("System Tray not supported");
            throw new Exception("System Tray not supported");
        }

        // Retrieve system tray
        SystemTray systemTray = SystemTray.getSystemTray();

        // 'Open' menu item
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.addActionListener(e -> ApplicationCore.getInstance().getWindow().show());

        // 'Exit' menu item
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.addActionListener(e ->
        {
            Platform.exit();
        });

        // Create pop-up menu
        PopupMenu popupMenu = new PopupMenu();
        popupMenu.add(openMenuItem);
        popupMenu.add(exitMenuItem);

        // Get and create system tray icon
        Image imageIcon = ImageIO.read(SystemTrayUtils.class.getClassLoader().getResourceAsStream("icon.png"));
        TrayIcon trayIcon = new TrayIcon(imageIcon, "Hardware Monitor Editor", popupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                ApplicationCore.getInstance().getWindow().show();
            }
        });

        systemTray.add(trayIcon);
        return trayIcon;
    }

    public static void removeFromSystemTray(TrayIcon trayIcon)
    {
        if (trayIcon != null)
        {
            SystemTray systemTray = SystemTray.getSystemTray();
            systemTray.remove(trayIcon);
        }
    }
}
