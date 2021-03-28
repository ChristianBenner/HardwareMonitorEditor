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

package com.bennero.ui.coloureditor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * RGBTab is a tab for the ColourEditor pop-up. It implements ColourTab and extends Tab (JavaFX). The RGB tab has three
 * sliders that allows the user to accurately choose a colour by configuring the values (0-255) of each RGB channel.
 * This means that the user can control the amount of Red, Green or Blue in the colour independently. The tab also
 * displays the colour that represents the channel values so that the user can configure their desired colour easier.
 *
 * @see         ColourTab
 * @see         Tab
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class RGBTab extends Tab implements ColourTab
{
    private static final Font SLIDER_FONT = new Font(12);

    private Slider redSlider;
    private Slider greenSlider;
    private Slider blueSlider;
    private HBox colourBox;
    private ChangeListener listener;

    public RGBTab()
    {
        super("RGB");

        GridPane rgbPane = new GridPane();
        rgbPane.setId("standard-pane");
        Label redSliderLabel = new Label("Red");
        redSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        redSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(redSliderLabel, HPos.LEFT);

        redSlider = new Slider();

        //redSlider.setShowTickLabels(true);
        redSlider.setMax(255);
        redSlider.setMin(0);

        GridPane.setHgrow(redSlider, Priority.ALWAYS);

        Label greenSliderLabel = new Label("Green");
        greenSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        greenSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(greenSliderLabel, HPos.LEFT);
        greenSlider = new Slider();
        greenSlider.setMax(255);
        greenSlider.setMin(0);

        GridPane.setHgrow(greenSlider, Priority.ALWAYS);

        Label blueSliderLabel = new Label("Blue");
        blueSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        blueSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(blueSliderLabel, HPos.LEFT);
        blueSlider = new Slider();
        blueSlider.setMax(255);
        blueSlider.setMin(0);
        blueSlider.setPadding(new Insets(0, 0, 10, 0));

        GridPane.setHgrow(blueSlider, Priority.ALWAYS);

        redSlider.valueProperty().addListener((observableValue, number, val) ->
                redSliderLabel.setText("Red: " + (int) redSlider.getValue()));
        greenSlider.valueProperty().addListener((observableValue, number, val) ->
                greenSliderLabel.setText("Green: " + (int) greenSlider.getValue()));
        blueSlider.valueProperty().addListener((observableValue, number, val) ->
                blueSliderLabel.setText("Blue: " + (int) blueSlider.getValue()));


        colourBox = new HBox();
        colourBox.setBackground(new Background(new BackgroundFill(Color.rgb((int) redSlider.getValue(),
                (int) greenSlider.getValue(), (int) blueSlider.getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
        GridPane.setHgrow(colourBox, Priority.ALWAYS);
        GridPane.setVgrow(colourBox, Priority.ALWAYS);

        // Create a change listener and add to each slider
        ChangeListener<Number> sliderChangeListener = (observableValue, number, t1) -> colourBox.setBackground(
                new Background(new BackgroundFill(Color.rgb((int) redSlider.getValue(), (int) greenSlider.getValue(),
                        (int) blueSlider.getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
        redSlider.valueProperty().addListener(sliderChangeListener);
        greenSlider.valueProperty().addListener(sliderChangeListener);
        blueSlider.valueProperty().addListener(sliderChangeListener);

        rgbPane.add(redSliderLabel, 0, 0);
        rgbPane.add(redSlider, 0, 1);
        rgbPane.add(greenSliderLabel, 0, 2);
        rgbPane.add(greenSlider, 0, 3);
        rgbPane.add(blueSliderLabel, 0, 4);
        rgbPane.add(blueSlider, 0, 5);
        rgbPane.add(colourBox, 0, 6);

        redSliderLabel.setId("colour-editor-label");
        greenSliderLabel.setId("colour-editor-label");
        blueSliderLabel.setId("colour-editor-label");

        super.setContent(rgbPane);
        super.setClosable(false);
    }

    @Override
    public void setColourData(Color colour)
    {
        if (colour != null)
        {
            redSlider.setValue((int) (colour.getRed() * 255.0));
            greenSlider.setValue((int) (colour.getGreen() * 255.0));
            blueSlider.setValue((int) (colour.getBlue() * 255.0));
        }
        else
        {
            redSlider.setValue(0);
            greenSlider.setValue(0);
            blueSlider.setValue(0);
        }
    }

    @Override
    public void setHandler(ChangeListener<Color> handler, Color colourIn)
    {
        setColourData(colourIn);

        listener = (observableValue, o, t1) -> handler.changed(null, null,
                Color.rgb((int) redSlider.getValue(), (int) greenSlider.getValue(), (int) blueSlider.getValue()));
        colourBox.backgroundProperty().addListener(listener);
    }

    @Override
    public void removeHandler()
    {
        if (listener != null)
        {
            colourBox.backgroundProperty().removeListener(listener);
        }
    }
}
