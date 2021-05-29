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

import com.bennero.client.config.ProgramConfigManager;
import com.bennero.client.config.SaveData;
import com.bennero.client.config.SaveManager;
import com.bennero.client.core.ApplicationCore;
import com.bennero.client.core.CoreUtils;
import com.bennero.client.core.Window;
import com.bennero.client.network.NetworkClient;
import com.bennero.client.network.NetworkScanner;
import com.bennero.client.states.NetworkScanStateData;
import com.bennero.client.states.PageEditorStateData;
import com.bennero.client.states.PageOverviewStateData;
import com.bennero.client.ui.ClientOptions;
import com.bennero.client.ui.NewPageButton;
import com.bennero.client.ui.PageInfo;
import com.bennero.client.util.PageGenerator;
import com.bennero.common.PageData;
import com.bennero.common.logging.LogLevel;
import com.bennero.common.logging.Logger;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Optional;

/**
 * PageOverview is a page that shows all of the user created sensor pages. This provides an easy way for them to locate
 * and select a specific page for editing.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PageOverview extends StackPane
{
    private static final Insets PADDING = new Insets(10, 10, 10, 10);
    private static final double H_GAP = 10.0;
    private static final double V_GAP = 10.0;
    private static final int NUM_ELEMENTS_ROW = 4;
    private static final int NUM_SPACES_PER_ROW = NUM_ELEMENTS_ROW - 1;
    private static final int ELEMENT_WIDTH = (int) ((Window.WINDOW_WIDTH_PX - (NUM_SPACES_PER_ROW * PageOverview.H_GAP) -
            PADDING.getLeft() - PADDING.getRight()) / NUM_ELEMENTS_ROW);
    private static final int ELEMENT_HEIGHT = (int) (ELEMENT_WIDTH / 1.6);
    private static final float ELEMENT_WIDTH_HEIGHT_RATIO = 1.6f;

    private FlowPane pageOverviewList;
    private NewPageButton newPageButton;
    private ScrollPane scrollPane;
    private ArrayList<PageInfo> pageInfoList;
    private int elementWidth;
    private SaveManager saveManager;

    public PageOverview()
    {
        super.setId("standard-pane");
        super.setPadding(PADDING);

        saveManager = SaveManager.getInstance();

        BorderPane contentsPane = new BorderPane();

        Label title = new Label("Pages");
        BorderPane.setAlignment(title, Pos.TOP_CENTER);

        newPageButton = new NewPageButton(ELEMENT_WIDTH, ELEMENT_HEIGHT);

        loadPageIcons();

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setContent(pageOverviewList);

        // Set CSS styling
        contentsPane.setId("standard-pane");
        title.setId("pane-title");
        scrollPane.setId("overview-pane-scroll-pane");
        newPageButton.setId("add-button");

        contentsPane.setTop(title);
        contentsPane.setCenter(scrollPane);

        Group topLeftGroup = new Group();
        StackPane.setAlignment(topLeftGroup, Pos.TOP_LEFT);

        HBox topLeftButtonBox = new HBox();
        topLeftButtonBox.setSpacing(5.0);

        Image openSaveIcon = new Image(getClass().getClassLoader().getResourceAsStream("open_save_icon.png"));
        Image openSaveIconHover = new Image(getClass().getClassLoader().getResourceAsStream("open_save_icon_hover.png"));
        Image newSaveIcon = new Image(getClass().getClassLoader().getResourceAsStream("new_save_icon.png"));
        Image newSaveIconHover = new Image(getClass().getClassLoader().getResourceAsStream("new_save_icon_hover.png"));
        Image settingsIcon = new Image(getClass().getClassLoader().getResourceAsStream("settings_icon.png"));
        Image settingsIconHover = new Image(getClass().getClassLoader().getResourceAsStream("settings_icon_hover.png"));
        Image disconnectIcon = new Image(getClass().getClassLoader().getResourceAsStream("disconnect_icon.png"));
        Image disconnectIconHover = new Image(getClass().getClassLoader().getResourceAsStream("disconnect_icon_hover.png"));

        Button openSaveButton = new Button();
        openSaveButton.setCursor(Cursor.HAND);
        openSaveButton.setPrefSize(32, 32);
        openSaveButton.setBackground(new Background(new BackgroundImage(openSaveIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        openSaveButton.setOnMouseEntered(mouseEvent -> openSaveButton.setBackground(new Background(new BackgroundImage(openSaveIconHover, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        openSaveButton.setOnMouseExited(mouseEvent -> openSaveButton.setBackground(new Background(new BackgroundImage(openSaveIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));

        //openSaveButton.setId("hw-default-button");
        openSaveButton.setOnAction(actionEvent ->
        {
            if (saveManager.displayOpenSaveUI())
            {
                ApplicationCore.s_setApplicationState(new PageOverviewStateData());
            }
        });
        topLeftButtonBox.getChildren().add(openSaveButton);

        Button newButton = new Button();
        newButton.setCursor(Cursor.HAND);
        newButton.setPrefSize(32, 32);
        newButton.setBackground(new Background(new BackgroundImage(newSaveIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        newButton.setOnMouseEntered(mouseEvent -> newButton.setBackground(new Background(new BackgroundImage(newSaveIconHover, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        newButton.setOnMouseExited(mouseEvent -> newButton.setBackground(new Background(new BackgroundImage(newSaveIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        topLeftButtonBox.getChildren().add(newButton);

        newButton.setOnAction(actionEvent ->
        {
            if (saveManager.displayNewSaveUI())
            {
                ApplicationCore.s_setApplicationState(new PageOverviewStateData());
            }
        });

        Button optionsButton = new Button();
        optionsButton.setCursor(Cursor.HAND);
        optionsButton.setPrefSize(32, 32);
        optionsButton.setBackground(new Background(new BackgroundImage(settingsIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        optionsButton.setOnMouseEntered(mouseEvent -> optionsButton.setBackground(new Background(new BackgroundImage(settingsIconHover, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        optionsButton.setOnMouseExited(mouseEvent -> optionsButton.setBackground(new Background(new BackgroundImage(settingsIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        optionsButton.setOnAction(actionEvent ->
        {
            SaveData saveData = saveManager.getSaveData();

            // Display the options pane
            ClientOptions clientOptions = new ClientOptions(saveData.getSensorUpdateTime(),
                    saveData.getSensorAnimationDuration(),
                    (observableValue, integer, t1) ->
                    {
                        saveManager.getSaveData().setSensorUpdateTime(t1);
                    }, (observableValue, integer, t1) ->
            {
                saveManager.getSaveData().setSensorAnimationDuration(t1);
            }, event ->
            {
                // Save config
                saveManager.getSaveData().save();
            });
            clientOptions.show();
        });

        StackPane.setAlignment(optionsButton, Pos.TOP_RIGHT);
        topLeftButtonBox.getChildren().add(optionsButton);

        Button disconnectButton = new Button();
        disconnectButton.setCursor(Cursor.HAND);
        disconnectButton.setPrefSize(32, 32);
        disconnectButton.setBackground(new Background(new BackgroundImage(disconnectIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
        disconnectButton.setOnMouseEntered(mouseEvent -> disconnectButton.setBackground(new Background(new BackgroundImage(disconnectIconHover, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        disconnectButton.setOnMouseExited(mouseEvent -> disconnectButton.setBackground(new Background(new BackgroundImage(disconnectIcon, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT))));
        disconnectButton.setOnAction(actionEvent ->
        {
            final String HOSTNAME = ProgramConfigManager.getInstance().getConnectionInformation().getHostname();

            Alert disconnectAlert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to disconnect from " + HOSTNAME + "?",
                    ButtonType.YES, ButtonType.NO);
            disconnectAlert.setTitle("Disconnect");
            disconnectAlert.setHeaderText("Disconnect");
            Optional<ButtonType> result = disconnectAlert.showAndWait();
            if(result.isPresent())
            {
                if(result.get() == ButtonType.YES)
                {
                    // Disconnect
                    Logger.log(LogLevel.INFO, getClass().getName(),
                            "Disconnecting from Hardware Monitor '" + HOSTNAME + "'");

                    // Disconnect from current hardware monitor
                    boolean disconnected = NetworkClient.getInstance().disconnect();

                    if(disconnected)
                    {
                        NetworkScanner.handleScan();
                    }
                    else
                    {
                        Alert failedToDisconnectAlert = new Alert(Alert.AlertType.ERROR,
                                "An error occurred when disconnecting from " + HOSTNAME,
                                ButtonType.OK);
                        failedToDisconnectAlert.setTitle("Failed to disconnect");
                        failedToDisconnectAlert.setHeaderText("Failed to disconnect");
                        failedToDisconnectAlert.showAndWait();
                    }
                }
            }
        });

        StackPane.setAlignment(disconnectButton, Pos.TOP_RIGHT);
        topLeftButtonBox.getChildren().add(disconnectButton);
        topLeftGroup.getChildren().add(topLeftButtonBox);

        Group topRightGroup = new Group();
        StackPane.setAlignment(topRightGroup, Pos.TOP_RIGHT);

        HBox topRightButtonBox = new HBox();
        topRightButtonBox.setSpacing(5.0);

        Button donateButton = new Button("Donate");
        donateButton.setId("hw-donate-button");
        donateButton.setOnAction(actionEvent -> CoreUtils.openBrowser("https://www.paypal.com/donate?hosted_button_id=R7QL6UW899UJU"));
        StackPane.setAlignment(donateButton, Pos.TOP_RIGHT);
        topRightButtonBox.getChildren().add(donateButton);
        topRightGroup.getChildren().add(topRightButtonBox);

        setNewPageListener(event ->
        {
            String pageTitle = "None";
            boolean success = false;
            while (!success)
            {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Enter Page Name");
                textInputDialog.setHeaderText("Enter Page Name");
                textInputDialog.setContentText("Enter a name for your page:");
                Optional<String> result = textInputDialog.showAndWait();
                if (result.isPresent())
                {
                    if (!result.get().isEmpty() && !result.get().replaceAll(" ", "").isEmpty())
                    {
                        pageTitle = result.get();
                        success = true;
                    }
                }
                else
                {
                    break;
                }
            }

            if (success)
            {
                PageData pageData = PageGenerator.generatePage(pageTitle);
                saveManager.getSaveData().addPageData(pageData);
                addPageInfoUI(pageData);
                NetworkClient.getInstance().writePageMessage(pageData);
            }
        });

        super.getChildren().addAll(contentsPane, topLeftGroup, topRightGroup);
    }

    public void loadPageIcons()
    {
        pageOverviewList = new FlowPane();
        pageOverviewList.setId("overview-pane-list-pane");
        pageOverviewList.setHgap(H_GAP);
        pageOverviewList.setVgap(V_GAP);
        pageOverviewList.widthProperty().addListener((observableValue, number, t1) ->
        {
            // Re-calculate the widths of the page overview buttons
            elementWidth = (int) ((pageOverviewList.getWidth() - (NUM_SPACES_PER_ROW * PageOverview.H_GAP)) /
                    NUM_ELEMENTS_ROW);
            for (PageInfo pageInfo : pageInfoList)
            {
                pageInfo.setMinSize(elementWidth, elementWidth / ELEMENT_WIDTH_HEIGHT_RATIO);
            }
            newPageButton.setMinSize(elementWidth, elementWidth / ELEMENT_WIDTH_HEIGHT_RATIO);
        });

        pageInfoList = new ArrayList<>();

        for (int i = 0; i < saveManager.getSaveData().getPageDataList().size(); i++)
        {
            PageData pageData = saveManager.getSaveData().getPageDataList().get(i);
            PageInfo pageInfo = new PageInfo(pageData, elementWidth, ELEMENT_HEIGHT);
            pageInfo.setOnMouseClicked(mouseEvent -> ApplicationCore.s_setApplicationState(new PageEditorStateData(pageData)));
            pageInfo.setCursor(Cursor.HAND);
            pageInfoList.add(pageInfo);
            pageOverviewList.getChildren().add(pageInfo);
        }

        pageOverviewList.getChildren().add(newPageButton);
    }

    public void setNewPageListener(EventHandler eventHandler)
    {
        newPageButton.setOnAction(eventHandler);
    }

    public void addPageInfoUI(PageData page)
    {
        ApplicationCore.s_setApplicationState(new PageEditorStateData(page));
    }
}
