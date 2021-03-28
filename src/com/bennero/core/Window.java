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

package com.bennero.core;

import com.bennero.common.TransitionType;
import com.bennero.states.StateData;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static com.bennero.Version.*;

/**
 * Window handles the GUI components of the application. It is responsible for displaying the correct GUI for the
 * applications current state when the window is visible.
 *
 * @author      Christian Benner
 * @version     %I%, %G%
 * @since       1.0
 */
public class Window
{
    public static final int WINDOW_WIDTH_PX = 800;
    public static final int WINDOW_HEIGHT_PX = 480;
    private static final String WINDOW_TITLE = "Hardware Monitor Editor v" + VERSION_MAJOR + "." + VERSION_MINOR + "." +
            VERSION_PATCH;

    private Stage stage;
    private StackPane basePane;
    private Node currentPage;
    private String titleSaveString;

    public Window(Stage stage)
    {
        this.stage = stage;
        titleSaveString = "";
    }

    /**
     * Method designed to launch the application natively using JavaFX base class methods. This provides a way for the
     * native C# bootstrapper to request to start the program.
     *
     * @since   1.0
     */
    public void updateWindowTitle(String saveString)
    {
        titleSaveString = saveString;
        stage.setTitle(WINDOW_TITLE + ": " + saveString);
    }

    public void initGui()
    {
        if (basePane == null)
        {
            basePane = new StackPane();
            Scene uiScene = new Scene(basePane, WINDOW_WIDTH_PX, WINDOW_HEIGHT_PX);
            uiScene.getStylesheets().add("stylesheet.css");
            basePane.setId("standard-pane");

            if (!titleSaveString.isEmpty())
            {
                stage.setTitle(WINDOW_TITLE + ": " + titleSaveString);
            }
            else
            {
                stage.setTitle(WINDOW_TITLE);
            }

            stage.setScene(uiScene);
            stage.setOnCloseRequest(windowEvent -> destroyGui());
        }
    }

    public void destroyGui()
    {
        stage.hide();
        currentPage = null;
        basePane = null;
    }

    public void changeGuiState(StateData newStateData)
    {
        initGui();

        Node newPage = newStateData.createGUI();
        basePane.getChildren().add(newPage);

        Transition transition = TransitionType.getTransition(newStateData.getTransitionType(), 1000,
                basePane, newPage);
        transition.setOnFinished(actionEvent1 ->
        {
            basePane.getChildren().remove(currentPage);
            currentPage = newPage;
        });
        transition.play();
    }

    public void show()
    {
        Platform.runLater(() ->
        {
            StateData currentState = ApplicationCore.s_getApplicationState();
            if (currentState != null)
            {
                initGui();

                // Load GUI that is associated to the current application state
                basePane.getChildren().clear();
                basePane.getChildren().add(currentState.createGUI());
                stage.show();
            }
            else
            {
                System.err.println("ERROR: No current state data available, cannot show display");
            }
        });
    }

    public ReadOnlyDoubleProperty getWidthProperty()
    {
        return stage.widthProperty();
    }

    public Stage getStage()
    {
        return this.stage;
    }

    public boolean isShowing()
    {
        return stage.isShowing();
    }
}
