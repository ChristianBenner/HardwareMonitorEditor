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

package com.bennero.client.pages;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * An information pane that includes a title, subtitle, loading icon spinner and an optional programmable button to help
 * navigate away from the page
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class LoadingInformationPage extends StackPane {
    public LoadingInformationPage(String title, String subtitle) {
        init(title, subtitle, null, null);
    }

    public LoadingInformationPage(String title) {
        init(title, null, null, null);
    }

    public LoadingInformationPage(String title, String subtitle, String buttonText, EventHandler eventHandler) {
        init(title, subtitle, buttonText, eventHandler);
    }

    private void init(String title, String info, String buttonText, EventHandler buttonEvent) {
        super.setId("standard-pane");

        VBox slide = new VBox();
        slide.setId("hw-welcome-page-pane");
        StackPane.setAlignment(slide, Pos.CENTER);
        slide.setAlignment(Pos.CENTER);
        slide.setSpacing(5.0);

        Label titleLabel = new Label(title);
        titleLabel.setId("hw-welcome-page-title");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        slide.getChildren().add(titleLabel);

        if (info != null) {
            Label infoLabel = new Label(info);
            infoLabel.setId("hw-welcome-page-subtitle");
            infoLabel.setWrapText(true);
            infoLabel.setTextAlignment(TextAlignment.CENTER);
            slide.getChildren().add(infoLabel);
        }

        ProgressIndicator progressIndicator = new ProgressIndicator();
        slide.getChildren().add(progressIndicator);

        if (buttonEvent != null && buttonText != null) {
            Button button = new Button(buttonText);
            button.setId("hw-default-button");
            button.setOnAction(buttonEvent);
            slide.getChildren().add(button);
        }

        super.getChildren().add(slide);
    }
}
