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

package com.bennero.util;

import com.bennero.common.Constants;
import com.bennero.common.PageData;
import com.bennero.common.TransitionType;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Generates a new PageData object by providing some default values and two colours that go well together. The colour
 * pair is picked randomly from a list of pre-defined backgrounds and foregrounds using the same index.
 *
 * @see         PageData
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class PageGenerator
{
    private static final Color PAGE_BACKGROUND_COLOURS[] =
            {
                    Color.BLACK,
                    Color.web("#FC766AFF"),
                    Color.web("#5B84B1FF"),
                    Color.web("#5F4B8BFF"),
                    Color.web("#E69A8DFF"),
                    Color.web("#00A4CCFF"),
                    Color.web("#F95700FF"),
                    Color.web("#00203FFF"),
                    Color.web("#ADEFD1FF"),
                    Color.web("#606060FF"),
                    Color.web("#D6ED17FF"),
                    Color.web("#ED2B33FF"),
                    Color.web("#D85A7FFF"),
                    Color.web("#2C5F2D"),
                    Color.web("#97BC62FF"),
                    Color.web("#00539CFF"),
                    Color.web("#EEA47FFF"),
                    Color.web("#0063B2FF"),
                    Color.web("#9CC3D5FF"),
                    Color.web("#D198C5FF"),
                    Color.web("#E0C568FF"),
                    Color.web("#CBCE91FF"),
                    Color.web("#EA738DFF"),
                    Color.web("#B1624EFF"),
                    Color.web("#5CC8D7FF"),
                    Color.web("#89ABE3FF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#E3CD81FF"),
                    Color.web("#B1B3B3FF"),
                    Color.web("#101820FF"),
                    Color.web("#F2AA4CFF"),
                    Color.web("#2BAE66FF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#2D2926FF"),
                    Color.web("#E94B3CFF"),
                    Color.web("#DAA03DFF"),
                    Color.web("#616247FF"),
                    Color.web("#990011FF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#CBCE91FF"),
                    Color.web("#76528BFF"),
                    Color.web("#333D79FF"),
                    Color.web("#FAEBEFFF"),
                    Color.web("#F93822FF"),
                    Color.web("#FDD20EFF"),
                    Color.web("#F95700FF"),
                    Color.web("#FFFFFFFF"),
                    Color.web("#FFD662FF"),
                    Color.web("#00539CFF"),
                    Color.web("#DF6589FF"),
                    Color.web("#3C1053FF"),
                    Color.web("#FFE77AFF"),
                    Color.web("#2C5F2DFF"),
                    Color.web("#DD4132FF"),
                    Color.web("#9E1030FF"),
                    Color.web("#00B1D2FF"),
                    Color.web("#FDDB27FF")
            };
    private static final Color PAGE_FOREGROUND_COLOURS[] =
            {
                    Color.WHITE,
                    Color.web("#5B84B1FF"),
                    Color.web("#FC766AFF"),
                    Color.web("#E69A8DFF"),
                    Color.web("#5F4B8BFF"),
                    Color.web("#F95700FF"),
                    Color.web("#00A4CCFF"),
                    Color.web("#ADEFD1FF"),
                    Color.web("#00203FFF"),
                    Color.web("#D6ED17FF"),
                    Color.web("#606060FF"),
                    Color.web("#D85A7FFF"),
                    Color.web("#ED2B33FF"),
                    Color.web("#97BC62FF"),
                    Color.web("#2C5F2D"),
                    Color.web("#EEA47FFF"),
                    Color.web("#00539CFF"),
                    Color.web("#9CC3D5FF"),
                    Color.web("#0063B2FF"),
                    Color.web("#E0C568FF"),
                    Color.web("#D198C5FF"),
                    Color.web("#EA738DFF"),
                    Color.web("#CBCE91FF"),
                    Color.web("#5CC8D7FF"),
                    Color.web("#B1624EFF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#89ABE3FF"),
                    Color.web("#B1B3B3FF"),
                    Color.web("#E3CD81FF"),
                    Color.web("#F2AA4CFF"),
                    Color.web("#101820FF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#2BAE66FF"),
                    Color.web("#E94B3CFF"),
                    Color.web("#2D2926FF"),
                    Color.web("#616247FF"),
                    Color.web("#DAA03DFF"),
                    Color.web("#FCF6F5FF"),
                    Color.web("#990011FF"),
                    Color.web("#76528BFF"),
                    Color.web("#CBCE91FF"),
                    Color.web("#FAEBEFFF"),
                    Color.web("#333D79FF"),
                    Color.web("#FDD20EFF"),
                    Color.web("#F93822FF"),
                    Color.web("#FFFFFFFF"),
                    Color.web("#F95700FF"),
                    Color.web("#00539CFF"),
                    Color.web("#FFD662FF"),
                    Color.web("#3C1053FF"),
                    Color.web("#DF6589FF"),
                    Color.web("#2C5F2DFF"),
                    Color.web("#FFE77AFF"),
                    Color.web("#9E1030FF"),
                    Color.web("#DD4132FF"),
                    Color.web("#FDDB27FF"),
                    Color.web("#00B1D2FF")
            };
    private static int s_id = 0;

    public static void setNextAvailablePageId(int id)
    {
        s_id = id;
    }

    public static PageData generatePage(String pageTitle)
    {
        Random random = new Random();

        // Pick random colours from list
        int randomIndex = random.nextInt(PAGE_BACKGROUND_COLOURS.length - 1);
        if (randomIndex > PAGE_FOREGROUND_COLOURS.length)
        {
            randomIndex = 0;
        }

        final int rows = random.nextInt(3) + 1;
        final int columns = random.nextInt(3) + 1;

        return new PageData(s_id++, PAGE_BACKGROUND_COLOURS[randomIndex], PAGE_FOREGROUND_COLOURS[randomIndex],
                PAGE_FOREGROUND_COLOURS[randomIndex].darker(), rows, columns, s_id,
                TransitionType.getRandomTransition(random), 1000, 10000, pageTitle,
                true, Constants.TEXT_ALIGNMENT_CENTER, "Subheading", false,
                Constants.TEXT_ALIGNMENT_CENTER);
    }
}
