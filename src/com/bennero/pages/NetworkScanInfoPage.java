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

package com.bennero.pages;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Network scan information page is similar to the standard information page however it has been made so that it can
 * update its subtitle on when a new number of devices has been found
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class NetworkScanInfoPage extends StackPane
{
    private VBox optionPane;
    private int numberOfFoundDevices;
    private Label noOtherDevicesLabel;
    private Button viewOtherDevicesButton;

    public NetworkScanInfoPage(String information, EventHandler showOtherDevices)
    {
        super.setId("standard-pane");
        numberOfFoundDevices = 0;

        VBox slide = new VBox();
        slide.setId("hw-welcome-page-pane");
        slide.setSpacing(5.0);

        Label titleLabel = new Label("Scanning Network");
        titleLabel.setId("hw-welcome-page-title");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);

        Label infoLabel = new Label(information);
        infoLabel.setId("hw-welcome-page-subtitle");
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);

        optionPane = new VBox();
        optionPane.setAlignment(Pos.CENTER);

        noOtherDevicesLabel = new Label("No Other Devices Found");
        noOtherDevicesLabel.setId("hw-network-scanner-no-devices-label");
        noOtherDevicesLabel.setWrapText(true);
        noOtherDevicesLabel.setTextAlignment(TextAlignment.CENTER);
        optionPane.getChildren().add(noOtherDevicesLabel);

        // Initialised for future usage
        viewOtherDevicesButton = new Button("Other Devices (" + numberOfFoundDevices + ")");
        viewOtherDevicesButton.setId("hw-default-button");
        viewOtherDevicesButton.setOnAction(showOtherDevices);

        ProgressIndicator progressIndicator = new ProgressIndicator();

        slide.setAlignment(Pos.CENTER);
        slide.getChildren().add(titleLabel);
        slide.getChildren().add(infoLabel);
        slide.getChildren().add(optionPane);
        slide.getChildren().add(progressIndicator);
        StackPane.setAlignment(slide, Pos.CENTER);

        super.getChildren().add(slide);
    }

    public void setNumberOfFoundDevices(int numberOfFoundDevices)
    {
        if (this.numberOfFoundDevices != numberOfFoundDevices)
        {
            optionPane.getChildren().clear();

            if (numberOfFoundDevices == 0)
            {
                optionPane.getChildren().add(noOtherDevicesLabel);
            }
            else
            {
                viewOtherDevicesButton.setText("Other Devices (" + numberOfFoundDevices + ")");
                optionPane.getChildren().add(viewOtherDevicesButton);
            }
        }
    }
}
