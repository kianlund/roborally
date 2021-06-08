package dk.dtu.compute.se.pisd.roborally.controller;

import com.mysql.cj.protocol.x.XMessage;
import dk.dtu.compute.se.pisd.roborally.exceptions.ImpossibleMoveException;
import dk.dtu.compute.se.pisd.roborally.model.Board;
import dk.dtu.compute.se.pisd.roborally.model.Heading;
import dk.dtu.compute.se.pisd.roborally.model.Player;
import dk.dtu.compute.se.pisd.roborally.model.Space;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameControllerTest {

    private final int TEST_WIDTH = 8;
    private final int TEST_HEIGHT = 8;

    private GameController gameController;

    @BeforeEach
    void setUp() {
        Board board = new Board(TEST_WIDTH, TEST_HEIGHT);
        gameController = new GameController(board);
        for (int i = 0; i < 6; i++) {
            Player player = new Player(board, null,"Player " + i);
            board.addPlayer(player);
            player.setSpace(board.getSpace(1,i));
            player.setHeading(Heading.values()[(i + 1) % Heading.values().length]);
        }
        board.setCurrentPlayer(board.getPlayer(0));
    }

    @AfterEach
    void tearDown() {
        gameController = null;
    }

    @Test
    void someTest() {
        Board board = gameController.board;

        Player player = board.getCurrentPlayer();
        gameController.moveCurrentPlayerToSpace(board.getSpace(0, 4));

        Assertions.assertEquals(player, board.getSpace(0, 4).getPlayer(), "Player " + player.getName() + " should beSpace (0,4)!");
    }

    @Test
    void moveCurrentPlayerToSpace() {
        Board board = gameController.board;;
        Player player1 = board.getPlayer(0);
        Player player2 = board.getPlayer(1);

        gameController.moveCurrentPlayerToSpace((board.getSpace(0, 4)));

        Assertions.assertEquals(player1, board.getSpace(0, 4).getPlayer());
        Assertions.assertNull(board.getSpace(0, 0).getPlayer());
        Assertions.assertEquals(player2, board.getCurrentPlayer());
    }


    @Test
    void startProgrammingPhase() {
    }

    @Test
    void finishProgrammingPhase() {
    }

    @Test
    void executePrograms() {
    }

    @Test
    void executeStep() {
    }

    @Test
    void MoveToSpace() throws ImpossibleMoveException {
        Board board = gameController.board;
        Player current = board.getCurrentPlayer();

        current.setSpace(board.getSpace(0, 4));
        current.setHeading(Heading.SOUTH);

        gameController.moveToSpace(current, board.getSpace(0,5), Heading.SOUTH);

        Assertions.assertEquals(board.getSpace(0,5), current.getSpace());
        Assertions.assertEquals(Heading.SOUTH,current.getHeading());
    }

    @Test
    void moveForward() {
        Board board = gameController.board;
        Player current = board.getCurrentPlayer();

        current.setSpace(board.getSpace(0, 4));
        current.setHeading(Heading.SOUTH);

        gameController.moveForward(current);

        Assertions.assertEquals(board.getSpace(0, 5),current.getSpace(),"Player " + current.getName() + " should be at space (0,5) " + "Player is at " + current.getSpace());

    }

    @Test
    void fastForward() {
        Board board = gameController.board;
        Player current = board.getCurrentPlayer();

        current.setSpace(board.getSpace(2, 3));
        current.setHeading(Heading.SOUTH);

        gameController.fastForward(current);

        Assertions.assertEquals(board.getSpace(2,5),current.getSpace());
        
    }

    @Test
    void turnRight() {
        Board board = gameController.board;
        Player current = board.getCurrentPlayer();

        current.setHeading(Heading.SOUTH);

        gameController.turnRight(current);

        Assertions.assertEquals(Heading.WEST,current.getHeading()); //South -> West
    }

    @Test
    void turnLeft() {
        Board board = gameController.board;
        Player current = board.getCurrentPlayer();

        current.setHeading(Heading.SOUTH);

        gameController.turnLeft(current);

        Assertions.assertEquals(Heading.EAST,current.getHeading()); //South -> East

    }

    @Test
    void moveCards() {
    }

    @Test
    void notImplememted() {
    }
}