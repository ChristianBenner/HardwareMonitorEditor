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

package com.bennero.ui;

import com.bennero.common.PageData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

/**
 * PageInfo is a custom UI component that is designed to give a small overview of a page title and subtitle (if it has
 * one). It is used on the PageOverview page so that the user can easily differentiate pages before selecting one to
 * edit.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PageInfo extends StackPane
{
    private static final Font TEXT_FONT = new Font(18.0);
    private static final Font SUBHEADING_TEXT_FONT = new Font(12.0);
    private static final CornerRadii BACKGROUND_CORNER_RADII = new CornerRadii(15.0);

    private PageData pageData;

    private VBox textBox;
    private Label title;
    private Label subheading;

    public PageInfo(final PageData pageData, final int width, final int height)
    {
        this.pageData = pageData;
        super.setMinSize(width, height);

        textBox = new VBox();
        textBox.setAlignment(Pos.CENTER);
        super.getChildren().add(textBox);

        init();
    }

    public void init()
    {
        if (title != null)
        {
            textBox.getChildren().remove(title);
        }

        if (subheading != null)
        {
            textBox.getChildren().remove(subheading);
        }

        super.setBackground(new Background(new BackgroundFill(pageData.getColour(), BACKGROUND_CORNER_RADII, Insets.EMPTY)));

        title = new Label(pageData.getTitle());
        title.setFont(TEXT_FONT);
        title.setTextFill(pageData.getTitleColour());
        textBox.getChildren().add(title);

        if (pageData.isSubtitleEnabled())
        {
            subheading = new Label(pageData.getSubtitle());
            subheading.setFont(SUBHEADING_TEXT_FONT);
            subheading.setTextFill(pageData.getSubtitleColour());
            textBox.getChildren().add(subheading);
        }
    }
}
