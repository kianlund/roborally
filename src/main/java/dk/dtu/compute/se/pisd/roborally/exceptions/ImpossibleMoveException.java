package dk.dtu.compute.se.pisd.roborally.exceptions;

import dk.dtu.compute.se.pisd.roborally.model.Heading;
import dk.dtu.compute.se.pisd.roborally.model.Player;
import dk.dtu.compute.se.pisd.roborally.model.Space;

/**
 * Exception used with moveToSpace in case the function finds move that isn't possible, ie. wall
 */
public class ImpossibleMoveException extends Exception {
    public final Player player;
    public final Space space;
    public final Heading heading;

    public ImpossibleMoveException(Player player, Space space, Heading heading) {
        super ("move impossible");
        this.player = player;
        this.space = space;
        this.heading = heading;
    }
}