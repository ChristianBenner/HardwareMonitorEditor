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

package com.bennero.client.ui;

import com.bennero.client.network.NetworkClient;
import com.bennero.client.ui.coloureditor.ColourEditor;
import com.bennero.common.PageData;
import com.bennero.common.TransitionType;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * PageOptions is a pop-up that displays different options that can be configured on a provided page. This allows the
 * user to configure properties of the page such as its transition, title, subtitle, number of rows, number of columns
 * and colour. It is also where pages can be deleted.
 *
 * @author Christian Benner
 * @version %I%, %G%
 * @since 1.0
 */
public class PageOptions extends Stage {
    private BorderPane transitionTimeSpinner;

    public PageOptions(PageData pageData,
                       List<PageData> pages,
                       ChangeListener<Boolean> titleEnabledChange,
                       ChangeListener<Boolean> subheadingEnabledChange,
                       ChangeListener<Color> backgroundColourChange,
                       ChangeListener<Integer> rowChange,
                       ChangeListener<Integer> columnChange,
                       ChangeListener<Byte> nextPageChange,
                       ChangeListener<Integer> transitionTypeChange,
                       ChangeListener<Integer> transitionTimeChange,
                       ChangeListener<Integer> durationChange,
                       EventHandler deletePage,
                       EventHandler doneEditing) {
        super.setTitle("Page Options");
        super.initModality(Modality.APPLICATION_MODAL);
        super.setOnCloseRequest(windowEvent -> hide());

        BorderPane basePane = new BorderPane();
        basePane.setId("standard-pane");

        Label title = new Label("Page Options");
        title.setId("colour-editor-title");
        BorderPane.setAlignment(title, Pos.CENTER);

        ColourEditor colourEditor = new ColourEditor();

        List<TransitionTypeInfo> transitionTypeInfos = new ArrayList<>();
        transitionTypeInfos.add(new TransitionTypeInfo("Cut", TransitionType.CUT));
        transitionTypeInfos.add(new TransitionTypeInfo("Swipe Left", TransitionType.SWIPE_LEFT));
        transitionTypeInfos.add(new TransitionTypeInfo("Swipe Right", TransitionType.SWIPE_RIGHT));
        transitionTypeInfos.add(new TransitionTypeInfo("Swipe Down", TransitionType.SWIPE_DOWN));
        transitionTypeInfos.add(new TransitionTypeInfo("Swipe Up", TransitionType.SWIPE_UP));
        transitionTypeInfos.add(new TransitionTypeInfo("Fade", TransitionType.FADE));

        VBox optionsPane = new VBox();
        optionsPane.getChildren().add(UIHelper.createCheckboxOption("Title", pageData.isTitleEnabled(), titleEnabledChange));
        optionsPane.getChildren().add(UIHelper.createCheckboxOption("Subheading", pageData.isSubtitleEnabled(), subheadingEnabledChange));
        optionsPane.getChildren().add(UIHelper.createColourOption(colourEditor, "Background Colour", pageData.getColour(), backgroundColourChange));
        optionsPane.getChildren().add(UIHelper.createIntSpinnerOption("Rows", pageData.getRows(), 0, 10, rowChange));
        optionsPane.getChildren().add(UIHelper.createIntSpinnerOption("Columns", pageData.getColumns(), 0, 10, columnChange));

        // Discover next page from page ID
        boolean foundNextPage = false;
        for (int i = 0; i < pages.size() && !foundNextPage; i++) {
            if (pages.get(i).getUniqueId() == pageData.getNextPageId()) {
                foundNextPage = true;
                optionsPane.getChildren().add(UIHelper.createComboBoxOption("Next Page", pages.get(i), pages, (observableValue, transitionTypeInfo, t1) -> nextPageChange.changed(null, null, t1.getUniqueId())));
            }
        }

        if (!foundNextPage) {
            optionsPane.getChildren().add(UIHelper.createComboBoxOption("Next Page", pageData, pages, (observableValue, transitionTypeInfo, t1) -> nextPageChange.changed(null, null, t1.getUniqueId())));
        }

        // Discover the transition type info
        boolean foundTransition = false;
        for (int i = 0; i < transitionTypeInfos.size() && !foundTransition; i++) {
            if (transitionTypeInfos.get(i).id == pageData.getTransitionType()) {
                foundTransition = true;
                optionsPane.getChildren().add(UIHelper.createComboBoxOption("Transition", transitionTypeInfos.get(i), transitionTypeInfos, (observableValue, transitionTypeInfo, t1) ->
                {
                    if (t1.id == TransitionType.CUT) {
                        // Disable the duration transition spinner
                        transitionTimeSpinner.setDisable(true);
                    } else {
                        transitionTimeSpinner.setDisable(false);
                    }

                    transitionTypeChange.changed(null, null, t1.id);
                }));
            }
        }

        transitionTimeSpinner = UIHelper.createIntSpinnerOption("Transition (Ms)", pageData.getTransitionTime(),
                100, 999999, 100, (observableValue, integer, t1) ->
                        transitionTimeChange.changed(null, null, t1));

        // Cut is the only type of transition that has no duration associated to it - so disable the spinner if it is
        // cut duration on the page data.
        if (pageData.getTransitionType() == TransitionType.CUT) {
            transitionTimeSpinner.setDisable(true);
        }

        optionsPane.getChildren().add(transitionTimeSpinner);


        optionsPane.getChildren().add(UIHelper.createIntSpinnerOption("Duration", pageData.getDurationMs(), 0, 999999, 100, durationChange));

        Button deleteButton = new Button("Delete");
        deleteButton.setId("hw-delete-button");
        deleteButton.setOnAction(actionEvent ->
        {
            // Ask before deleting
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Remove Page?", ButtonType.YES, ButtonType.NO);
            alert.setContentText("Are you sure you want to remove the page? You wont be able to get it back.");

            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                NetworkClient.getInstance().writeRemovePageMessage((byte) pageData.getUniqueId());
                hide();
                deletePage.handle(actionEvent);
            }
        });

        BorderPane deleteButtonPane = new BorderPane();
        deleteButtonPane.setPadding(new Insets(5));
        deleteButtonPane.setRight(deleteButton);
        optionsPane.getChildren().add(deleteButtonPane);


        Button doneButton = new Button("Done");
        doneButton.setId("hw-default-button");
        doneButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        doneButton.setOnAction(actionEvent ->
        {
            hide();
            doneEditing.handle(actionEvent);
        });

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(optionsPane);

        basePane.setTop(title);
        basePane.setCenter(scrollPane);
        basePane.setBottom(doneButton);

        Scene dialogScene = new Scene(basePane, 350, 300);
        dialogScene.getStylesheets().add("stylesheet.css");
        super.setScene(dialogScene);
    }

    private class TransitionTypeInfo {
        private String name;
        private int id;

        public TransitionTypeInfo(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
