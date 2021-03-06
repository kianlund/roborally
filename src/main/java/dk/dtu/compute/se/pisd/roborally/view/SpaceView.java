/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.view;

import dk.dtu.compute.se.pisd.designpatterns.observer.Subject;
import dk.dtu.compute.se.pisd.roborally.model.Heading;
import dk.dtu.compute.se.pisd.roborally.model.components.Checkpoint;
import dk.dtu.compute.se.pisd.roborally.model.components.ConveyorBelt;
import dk.dtu.compute.se.pisd.roborally.model.components.FieldAction;
import dk.dtu.compute.se.pisd.roborally.model.Player;
import dk.dtu.compute.se.pisd.roborally.model.Space;
import dk.dtu.compute.se.pisd.roborally.model.components.Gear;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * ...
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class SpaceView extends StackPane implements ViewObserver {

    final public static int SPACE_HEIGHT = 65; // 60; // 75;
    final public static int SPACE_WIDTH = 65;  // 60; // 75;

    public final Space space;


    public SpaceView(@NotNull Space space) {
        this.space = space;

        // XXX the following styling should better be done with styles
        this.setPrefWidth(SPACE_WIDTH);
        this.setMinWidth(SPACE_WIDTH);
        this.setMaxWidth(SPACE_WIDTH);

        this.setPrefHeight(SPACE_HEIGHT);
        this.setMinHeight(SPACE_HEIGHT);
        this.setMaxHeight(SPACE_HEIGHT);

        if ((space.x + space.y) % 2 == 0) {
            this.setStyle("-fx-background-color: white;");
        } else {
            this.setStyle("-fx-background-color: black;");
        }

        // updatePlayer();

        // This space view should listen to changes of the space
        space.attach(this);
        update(space);
    }

    private void updatePlayer() {

        Player player = space.getPlayer();
        if (player != null) {
            Polygon arrow = new Polygon(
                    0.0, 0.0,
                    10.0, 20.0,
                    20.0, 0.0 );
            try {
                arrow.setFill(Color.valueOf(player.getColor()));
            } catch (Exception e) {
                arrow.setFill(Color.MEDIUMPURPLE);
            }

            arrow.setRotate((90*player.getHeading().ordinal())%360);
            this.getChildren().add(arrow);
        }
    }

    /**
     * space.getActions().get(0) henter bare den første og ENESTE action der er i fieldactions/conveybelts etc. arraylist.
     */
    private void updateFieldAction() {
        FieldAction fieldaction = null;
        if (!(space.getActions().isEmpty())) {
            fieldaction = space.getActions().get(0); // TODO: Loop for every fieldaction
        }
        if (fieldaction instanceof ConveyorBelt) {
            Polygon shape = new Polygon(
                    0.0, 0.0,
                    20.0, 40.0,
                    40.0, 0.0 );
            shape.setFill(Color.LIGHTGREY);
            shape.setRotate((90*((ConveyorBelt) fieldaction).getHeading().ordinal())%360);

            this.getChildren().add(shape);
        }
        if (fieldaction instanceof Gear) {
            Circle shape = new Circle(25,25,20);
            shape.setFill(Color.BROWN);

            this.getChildren().add(shape);
        }
        if (fieldaction instanceof Checkpoint) {
            Rectangle shape = new Rectangle(30.0,30.0);
            shape.setFill(Color.LIGHTGREEN);
            Integer num = ((Checkpoint) fieldaction).getNumber();
            Text text = new Text(num.toString());

            this.getChildren().add(shape);
            this.getChildren().add(text);
        }
    }

    private void updateWalls() {
//        Heading walls = null;
        Pane pane = new Pane();
        Rectangle rectangle = new Rectangle(0.0,0.0,SPACE_WIDTH,SPACE_HEIGHT);
        rectangle.setFill(Color.TRANSPARENT);
        pane.getChildren().add(rectangle);

        Line line = new Line();
        List<Heading> walls = null;
        if (!(space.getWalls().isEmpty())) {
            walls = space.getWalls();
            for (Heading wall: walls) {
                switch (wall) {
                    case NORTH:
                        line = new Line(2,2,SPACE_WIDTH-2,2);
                        break;
                    case SOUTH:
                        line = new Line(2,SPACE_HEIGHT-2,SPACE_WIDTH-2,SPACE_HEIGHT-2);
                        break;
                    case WEST:
                        line = new Line(2,2,2,SPACE_HEIGHT-2);
                        break;
                    case EAST:
                        line = new Line(SPACE_WIDTH-2,2,SPACE_WIDTH-2,SPACE_HEIGHT-2);
                        break;
                }
            }
            line.setStroke(Color.RED);
            line.setStrokeWidth(5);
            pane.getChildren().add(line);
            this.getChildren().add(pane);
        }
    }

    @Override
    public void updateView(Subject subject) {
        if (subject == this.space) {
            this.getChildren().clear();
            updateFieldAction();
            updateWalls();
            updatePlayer();
        }
    }
}
