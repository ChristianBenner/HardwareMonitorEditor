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

import com.bennero.common.Constants;
import com.bennero.client.ui.coloureditor.ColourEditor;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * The TextFieldEditPane is a custom UI component that is designed to provide editing tools for a text field. Editing
 * tools include: Text alignment, colour and text field removal. It is to be shown above a text field on mouse hover and
 * therefor has a semi-transparent background.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class TextFieldEditPane extends StackPane
{
    private final static int ALIGNMENT_BUTTON_WIDTH = 32;
    private final static int ALIGNMENT_BUTTON_HEIGHT = 32;

    private ColouredTextField textField;
    private Color colour;
    private ColourEditor colourEditor;

    public TextFieldEditPane(ColouredTextField textField,
                             int alignment,
                             Background hoverBackground,
                             EventHandler<ActionEvent> deleteEvent,
                             ChangeListener<Integer> alignmentHandler,
                             ChangeListener<Color> colourHandler,
                             ChangeListener<String> textHandler)
    {
        this.colour = textField.getColour();

        Image alignLeftIcon = new Image(getClass().getClassLoader().getResourceAsStream("align_left_icon.png"));
        Image alignCenterIcon = new Image(getClass().getClassLoader().getResourceAsStream("align_center_icon.png"));
        Image alignRightIcon = new Image(getClass().getClassLoader().getResourceAsStream("align_right_icon.png"));
        Image changeColourIcon = new Image(getClass().getClassLoader().getResourceAsStream("change_colour_icon.png"));
        Image removeIcon = new Image(getClass().getClassLoader().getResourceAsStream("remove_icon.png"));

        this.textField = textField;
        this.colourEditor = new ColourEditor();
        textField.textProperty().addListener(textHandler);

        Button alignLeftButton = new Button("");
        alignLeftButton.setCursor(Cursor.HAND);
        alignLeftButton.setPrefSize(ALIGNMENT_BUTTON_WIDTH, ALIGNMENT_BUTTON_HEIGHT);
        alignLeftButton.setBackground(new Background(new BackgroundImage(alignLeftIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));

        Button alignCenterButton = new Button("");
        alignCenterButton.setCursor(Cursor.HAND);
        alignCenterButton.setPrefSize(ALIGNMENT_BUTTON_WIDTH, ALIGNMENT_BUTTON_HEIGHT);
        alignCenterButton.setBackground(new Background(new BackgroundImage(alignCenterIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));

        Button alignRightButton = new Button("");
        alignRightButton.setCursor(Cursor.HAND);
        alignRightButton.setPrefSize(ALIGNMENT_BUTTON_WIDTH, ALIGNMENT_BUTTON_HEIGHT);
        alignRightButton.setBackground(new Background(new BackgroundImage(alignRightIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));

        Button changeColourButton = new Button("");
        changeColourButton.setCursor(Cursor.HAND);
        changeColourButton.setPrefSize(ALIGNMENT_BUTTON_WIDTH, ALIGNMENT_BUTTON_HEIGHT);
        changeColourButton.setBackground(new Background(new BackgroundImage(changeColourIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
        changeColourButton.setOnAction(actionEvent ->
        {
            // Open colour editor
            colourEditor.setHandler(colour, (observableValue, color, t1) ->
            {
                setColour(t1);
                colourHandler.changed(null, null, t1);
            });
        });

        Button deleteButton = new Button("");
        deleteButton.setCursor(Cursor.HAND);
        deleteButton.setPrefSize(ALIGNMENT_BUTTON_WIDTH, ALIGNMENT_BUTTON_HEIGHT);
        deleteButton.setBackground(new Background(new BackgroundImage(removeIcon, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));

        deleteButton.setOnAction(deleteEvent);

        HBox hBox = new HBox();
        hBox.getChildren().add(alignLeftButton);
        hBox.getChildren().add(alignCenterButton);
        hBox.getChildren().add(alignRightButton);
        hBox.getChildren().add(changeColourButton);
        hBox.getChildren().add(deleteButton);

        AnchorPane optionsAnchorPane = new AnchorPane();
        optionsAnchorPane.setVisible(false);
        optionsAnchorPane.getChildren().add(hBox);

        super.setOnMouseEntered(mouseEvent ->
        {
            textField.setBackground(hoverBackground);
            optionsAnchorPane.setVisible(true);
        });

        super.setOnMouseExited(mouseEvent ->
        {
            textField.setBackground(Background.EMPTY);
            optionsAnchorPane.setVisible(false);
        });

        Group optionsGroup = new Group(optionsAnchorPane);

        // Setup buttons to set the text alignment but also change the position of the options group
        alignLeftButton.setOnAction(actionEvent ->
        {
            textField.setAlignment(Pos.CENTER_LEFT);
            StackPane.setAlignment(optionsGroup, Pos.CENTER_RIGHT);
            alignmentHandler.changed(null, null, Constants.TEXT_ALIGNMENT_LEFT);
        });
        alignCenterButton.setOnAction(actionEvent ->
        {
            textField.setAlignment(Pos.CENTER);
            alignmentHandler.changed(null, null, Constants.TEXT_ALIGNMENT_CENTER);
        });
        alignRightButton.setOnAction(actionEvent ->
        {
            textField.setAlignment(Pos.CENTER_RIGHT);
            StackPane.setAlignment(optionsGroup, Pos.CENTER_LEFT);
            alignmentHandler.changed(null, null, Constants.TEXT_ALIGNMENT_RIGHT);
        });


        // Align the text
        switch (alignment)
        {
            case Constants.TEXT_ALIGNMENT_LEFT:
                textField.setAlignment(Pos.CENTER_LEFT);
                StackPane.setAlignment(optionsGroup, Pos.CENTER_RIGHT);
                break;
            case Constants.TEXT_ALIGNMENT_CENTER:
                textField.setAlignment(Pos.CENTER);
                StackPane.setAlignment(optionsGroup, Pos.CENTER_RIGHT);
                break;
            case Constants.TEXT_ALIGNMENT_RIGHT:
                textField.setAlignment(Pos.CENTER_RIGHT);
                StackPane.setAlignment(optionsGroup, Pos.CENTER_LEFT);
                break;
        }


        super.getChildren().add(textField);
        super.getChildren().add(optionsGroup);
    }

    public void setColour(Color colour)
    {
        String textColourString = "#" + colour.toString().substring(2);
        textField.setStyle("-fx-text-inner-color: " + textColourString + ";");
        this.colour = colour;
    }
}
