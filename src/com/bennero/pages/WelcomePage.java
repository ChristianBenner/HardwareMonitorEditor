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

import com.bennero.common.TransitionType;
import com.bennero.config.ProgramConfigManager;
import com.bennero.config.SaveManager;
import com.bennero.core.ApplicationCore;
import com.bennero.core.CoreUtils;
import com.bennero.network.NetworkScanner;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.util.Optional;

/**
 * Welcome Page is a page shown to the user upon first starting the application. It consists of multiple
 * sub-pages/slides which guide the user through creating there first save file.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class WelcomePage extends StackPane
{
    private String selectedSaveDirectory = null;
    private ProgramConfigManager programConfigManager;

    public WelcomePage()
    {
        this.programConfigManager = ProgramConfigManager.getInstance();
        super.setId("standard-pane");

        BorderPane welcomeSlideBorder = new BorderPane();
        VBox welcomeSlide = new VBox();
        welcomeSlide.setSpacing(5.0);

        Label welcomeLabel = new Label("Welcome");
        Button welcomeSlideContinueButton = new Button("Continue");
        welcomeSlide.setId("hw-welcome-page-pane");
        welcomeLabel.setId("hw-welcome-page-title");
        welcomeSlideContinueButton.setId("hw-default-button");
        welcomeSlide.setAlignment(Pos.CENTER);

        Image image = new Image(getClass().getClassLoader().getResourceAsStream("hardware_monitor_cover.png"));
        ImageView imageView = new ImageView(image);
        ApplicationCore.getInstance().getWidthProperty().addListener((observableValue, number, t1) ->
        {
            imageView.setFitWidth((double) t1 * 0.7);
        });

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(ApplicationCore.getInstance().getWidthProperty().getValue() * 0.7);
        welcomeSlide.getChildren().add(imageView);

        welcomeSlide.getChildren().add(welcomeSlideContinueButton);

        Label donationText = new Label("This software is free and open source, please consider donating");
        donationText.setTextAlignment(TextAlignment.CENTER);
        donationText.setWrapText(true);
        donationText.setId("hw-welcome-page-donate-text");
        BorderPane.setAlignment(donationText, Pos.CENTER);

        welcomeSlideBorder.setCenter(welcomeSlide);
        welcomeSlideBorder.setBottom(donationText);

        VBox fileAreaSelectSlide = new VBox();
        fileAreaSelectSlide.setSpacing(5.0);
        VBox labelBox = new VBox();
        labelBox.setAlignment(Pos.CENTER);
        Label chooseLocationLabel = new Label("Please choose a location to save your custom layouts");
        chooseLocationLabel.setTextAlignment(TextAlignment.CENTER);
        chooseLocationLabel.setWrapText(true);
        Label selectedLocationLabel = new Label();

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setSpacing(5.0);
        Button setLocationButton = new Button("Select Folder");
        Button fileAreaSelectSlideContinueButton = new Button("Continue");
        fileAreaSelectSlideContinueButton.setDisable(true);
        fileAreaSelectSlide.setId("hw-welcome-page-file-area-selection-pane");
        chooseLocationLabel.setId("hw-welcome-page-choose-location-text");
        setLocationButton.setId("hw-default-button");
        fileAreaSelectSlideContinueButton.setId("hw-default-button");
        labelBox.getChildren().add(chooseLocationLabel);
        buttonBox.getChildren().add(setLocationButton);
        buttonBox.getChildren().add(fileAreaSelectSlideContinueButton);
        fileAreaSelectSlide.setAlignment(Pos.CENTER);
        fileAreaSelectSlide.getChildren().add(labelBox);
        fileAreaSelectSlide.getChildren().add(buttonBox);
        setLocationButton.setOnAction(actionEvent ->
        {
            File selectedDirectory = CoreUtils.showDirectorySelector();

            if (selectedDirectory != null)
            {
                if (labelBox.getChildren().contains(selectedLocationLabel))
                {
                    labelBox.getChildren().remove(selectedLocationLabel);
                }

                selectedLocationLabel.setText(selectedDirectory.getAbsolutePath());
                selectedLocationLabel.setId("hw-welcome-page-choose-location-text");
                selectedLocationLabel.setTextAlignment(TextAlignment.CENTER);
                selectedLocationLabel.setWrapText(true);
                labelBox.getChildren().add(selectedLocationLabel);
                selectedSaveDirectory = selectedDirectory.getAbsolutePath();
                setLocationButton.setText("Change");
                fileAreaSelectSlideContinueButton.setDisable(false);
            }
        });

        fileAreaSelectSlideContinueButton.setOnAction(actionEvent ->
        {
            if (selectedSaveDirectory != null && !selectedSaveDirectory.isEmpty())
            {
                boolean success = false;
                while (!success)
                {
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
                            if (new File(selectedSaveDirectory + "\\" + fileName).exists())
                            {
                                // Error pop-up because it will not fit
                                Alert alert = new Alert(Alert.AlertType.WARNING, "Save File Already Exists", ButtonType.OK);
                                alert.setContentText("A save file with that name already exists in the directory, please use another name");
                                alert.showAndWait();
                            }
                            else
                            {
                                programConfigManager.setFileAreaPath(selectedSaveDirectory);
                                programConfigManager.setLastLoadedFilePath(selectedSaveDirectory + "\\" + fileName);
                                SaveManager.getInstance().newSave(new File(selectedSaveDirectory + "\\" + fileName));
                                NetworkScanner.handleScan();
                                success = true;
                            }
                        }
                    }
                    else
                    {
                        break;
                    }
                }
            }
        });

        StackPane ref = this;
        welcomeSlideContinueButton.setOnAction(actionEvent ->
        {
            getChildren().add(fileAreaSelectSlide);
            Transition transition = TransitionType.getTransition(TransitionType.SWIPE_LEFT, 1000, ref,
                    fileAreaSelectSlide);
            transition.setOnFinished(actionEvent1 -> getChildren().remove(welcomeSlide));
            transition.play();
        });

        super.getChildren().add(welcomeSlideBorder);
    }
}
