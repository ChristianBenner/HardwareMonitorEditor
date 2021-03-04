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

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * HSVTab is a tab for the ColourEditor pop-up. It implements ColourTab and extends Tab (JavaFX). The HSV tab provides
 * three sliders for configuring the HSV values of a colour. It also displays the colour that represents the values of
 * the sliders.
 *
 * @see         ColourTab
 * @see         Tab
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class HSVTab extends Tab implements ColourTab
{
    private static final Font SLIDER_FONT = new Font(12);

    private Slider hueSlider;
    private Slider saturationSlider;
    private Slider brightnessSlider;
    private HBox colourBox;
    private ChangeListener listener;

    public HSVTab()
    {
        super("HSV");

        GridPane hsvPane = new GridPane();
        hsvPane.setId("standard-pane");

        Label hueSliderLabel = new Label("Hue: ");
        hueSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        hueSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(hueSliderLabel, HPos.LEFT);
        hueSlider = new Slider();
        hueSlider.setMax(360.0);
        hueSlider.setMin(0.0);

        GridPane.setHgrow(hueSlider, Priority.ALWAYS);

        Label saturationSliderLabel = new Label("Saturation: ");
        saturationSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        saturationSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(saturationSliderLabel, HPos.LEFT);
        saturationSlider = new Slider();
        saturationSlider.setMax(1.0);
        saturationSlider.setMin(0.0);

        GridPane.setHgrow(saturationSlider, Priority.ALWAYS);

        Label brightnessSliderLabel = new Label("Brightness: ");
        brightnessSliderLabel.setPadding(new Insets(0, 0, 0, 5));
        brightnessSliderLabel.setFont(SLIDER_FONT);
        GridPane.setHalignment(brightnessSliderLabel, HPos.LEFT);
        brightnessSlider = new Slider();
        brightnessSlider.setMax(1.0);
        brightnessSlider.setMin(0.0);
        brightnessSlider.setPadding(new Insets(0, 0, 10, 0));

        GridPane.setHgrow(brightnessSlider, Priority.ALWAYS);

        colourBox = new HBox();
        colourBox.setBackground(new Background(new BackgroundFill(Color.hsb((int) hueSlider.getValue(),
                saturationSlider.getValue(), brightnessSlider.getValue()), CornerRadii.EMPTY, Insets.EMPTY)));
        GridPane.setHgrow(colourBox, Priority.ALWAYS);
        GridPane.setVgrow(colourBox, Priority.ALWAYS);

        // Create a change listener and add to each slider
        ChangeListener<Number> sliderChangeListener = (observableValue, number, t1) -> colourBox.setBackground(
                new Background(new BackgroundFill(Color.hsb((int) hueSlider.getValue(), saturationSlider.getValue(),
                        brightnessSlider.getValue()), CornerRadii.EMPTY, Insets.EMPTY)));

        hueSlider.valueProperty().addListener(sliderChangeListener);
        saturationSlider.valueProperty().addListener(sliderChangeListener);
        brightnessSlider.valueProperty().addListener(sliderChangeListener);

        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        hueSlider.valueProperty().addListener((observableValue, number, val) ->
                hueSliderLabel.setText("Hue: " + (int) hueSlider.getValue()));
        saturationSlider.valueProperty().addListener((observableValue, number, val) ->
                saturationSliderLabel.setText("Saturation: " + decimalFormat.format(saturationSlider.getValue())));
        brightnessSlider.valueProperty().addListener((observableValue, number, val) ->
                brightnessSliderLabel.setText("Brightness: " + decimalFormat.format(brightnessSlider.getValue())));

        hsvPane.add(hueSliderLabel, 0, 0);
        hsvPane.add(hueSlider, 0, 1);
        hsvPane.add(saturationSliderLabel, 0, 2);
        hsvPane.add(saturationSlider, 0, 3);
        hsvPane.add(brightnessSliderLabel, 0, 4);
        hsvPane.add(brightnessSlider, 0, 5);
        hsvPane.add(colourBox, 0, 6);

        hueSliderLabel.setId("colour-editor-label");
        saturationSliderLabel.setId("colour-editor-label");
        brightnessSliderLabel.setId("colour-editor-label");

        super.setContent(hsvPane);
        super.setClosable(false);
    }

    @Override
    public void setColourData(Color colour)
    {
        if (colour != null)
        {
            hueSlider.setValue(colour.getHue());
            saturationSlider.setValue(colour.getSaturation());
            brightnessSlider.setValue(colour.getBrightness());
        }
        else
        {
            hueSlider.setValue(0);
            saturationSlider.setValue(0);
            brightnessSlider.setValue(0);
        }
    }

    @Override
    public void setHandler(ChangeListener<Color> handler, Color colourIn)
    {
        setColourData(colourIn);

        listener = (observableValue, o, t1) -> handler.changed(null, null,
                Color.hsb((int) hueSlider.getValue(), saturationSlider.getValue(), brightnessSlider.getValue()));
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
