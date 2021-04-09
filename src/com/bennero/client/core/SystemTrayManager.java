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

package com.bennero.client.core;

import com.bennero.client.util.SystemTrayUtils;
import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class SystemTrayManager
{
    private static SystemTrayManager instance = null;

    private TrayIcon trayIcon;
    private boolean addedToSystemTray;

    public static SystemTrayManager getInstance()
    {
        if(instance == null)
        {
            instance = new SystemTrayManager();
        }

        return instance;
    }

    private SystemTrayManager()
    {
        addedToSystemTray = false;
    }

    public void addToSystemTray()
    {
        // Make it so that the application process does not end on window close
        Platform.setImplicitExit(false);

        // Removes the system tray icon if the application is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> removeFromSystemTray()));

        // Check if system tray is supported
        if (!SystemTray.isSupported())
        {
            System.err.println("System Tray not supported");
        }
        else
        {
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
            try
            {
                Image imageIcon = ImageIO.read(SystemTrayUtils.class.getClassLoader().getResourceAsStream("icon.png"));
                trayIcon = new TrayIcon(imageIcon, "Hardware Monitor Editor", popupMenu);
                trayIcon.setImageAutoSize(true);
                trayIcon.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        ApplicationCore.getInstance().getWindow().show();
                    }
                });

                // Retrieve system tray
                SystemTray systemTray = SystemTray.getSystemTray();
                systemTray.add(trayIcon);

                addedToSystemTray = true;
            }
            catch (IOException ioException)
            {
                System.err.println("Failed to load system tray icon");
                ioException.printStackTrace();
            }
            catch (AWTException systemTrayException)
            {
                System.err.println("Failed to add application to system tray");
                systemTrayException.printStackTrace();
            }
        }
    }

    public void removeFromSystemTray()
    {
        if (trayIcon != null && addedToSystemTray == true)
        {
            SystemTray systemTray = SystemTray.getSystemTray();
            systemTray.remove(trayIcon);
        }
    }
}
