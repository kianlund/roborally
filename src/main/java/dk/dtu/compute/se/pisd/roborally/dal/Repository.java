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
package dk.dtu.compute.se.pisd.roborally.dal;

import dk.dtu.compute.se.pisd.roborally.fileaccess.LoadBoard;
import dk.dtu.compute.se.pisd.roborally.model.*;
import dk.dtu.compute.se.pisd.roborally.view.BoardView;
import dk.dtu.compute.se.pisd.roborally.view.SpaceView;

import javax.xml.transform.Result;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ...
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
class Repository implements IRepository {
	
	private static final String GAME_GAMEID = "gameID";

	private static final String GAME_NAME = "name";

	private static final String GAME_BOARDNAME = "boardName";

	private static final String GAME_CURRENTPLAYER = "currentPlayer";

	private static final String GAME_PHASE = "phase";

	private static final String GAME_STEP = "step";
	
	private static final String PLAYER_PLAYERID = "playerID";
	
	private static final String PLAYER_NAME = "name";

	private static final String PLAYER_COLOUR = "colour";
	
	private static final String PLAYER_GAMEID = "gameID";
	
	private static final String PLAYER_POSITION_X = "positionX";

	private static final String PLAYER_POSITION_Y = "positionY";

	private static final String PLAYER_HEADING = "heading";

	private static final String FIELD_GAMEID = "gameID";

	private static final String FIELD_PLAYERID = "playerID";

	private static final String FIELD_TYPE = "type";

	private static final int FIELD_TYPE_REGISTER = 0;

	private static final int FIELD_TYPE_RAND = 1;

	private static final String FIELD_POS = "position";

	private static final String FIELD_VISIBLE = "visible";

	private static final String FIELD_COMMAND = "command";

	private Connector connector;
	
	Repository(Connector connector){

		this.connector = connector;
	}

	@Override
	public boolean createGameInDB(Board game) {
		if (game.getGameId() == null) {
			Connection connection = connector.getConnection();
			try {
				connection.setAutoCommit(false);

				PreparedStatement ps = getInsertGameStatementRGK();
				// TODO: the name should eventually set by the user
				//       for the game and should be then used 
				//       game.getName();
				ps.setString(1, "Date: " +  new Date()); // instead of name
				ps.setNull(2, Types.TINYINT); // game.getPlayerNumber(game.getCurrentPlayer())); is inserted after players!
				ps.setInt(3, game.getPhase().ordinal());
				ps.setInt(4, game.getStep());
				ps.setString(5, game.boardName);

				// If you have a foreign key constraint for current players,
				// the check would need to be temporarily disabled, since
				// MySQL does not have a per transaction validation, but
				// validates on a per row basis.
				// Statement statement = connection.createStatement();
				// statement.execute("SET foreign_key_checks = 0");
				
				int affectedRows = ps.executeUpdate();
				ResultSet generatedKeys = ps.getGeneratedKeys();
				if (affectedRows == 1 && generatedKeys.next()) {
					game.setGameId(generatedKeys.getInt(1));
				}
				generatedKeys.close();
				
				// Enable foreign key constraint check again:
				// statement.execute("SET foreign_key_checks = 1");
				// statement.close();

				createPlayersInDB(game);
				// TOODO this method needs to be implemented first
				createCardFieldsInDB(game);

				//createCardFieldsInDB(game); skal den med??

				// since current player is a foreign key, it can oly be
				// inserted after the players are created, since MySQL does
				// not have a per transaction validation, but validates on
				// a per row basis.
				ps = getSelectGameStatementU();
				ps.setInt(1, game.getGameId());

				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					rs.updateInt(GAME_CURRENTPLAYER, game.getPlayerNumber(game.getCurrentPlayer()));
					rs.updateRow();
				} else {
					throw new SQLException("did not receive new ID for game");
				}
				rs.close();

				connection.commit();
				connection.setAutoCommit(true);
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("Some DB error");
				
				try {
					connection.rollback();
					connection.setAutoCommit(true);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
		} else {
			System.err.println("Game cannot be created in DB, since it has a game id already!");
		}
		return false;
	}
		
	@Override
	public boolean updateGameInDB(Board game) {
		assert game.getGameId() != null;
		
		Connection connection = connector.getConnection();
		try {
			connection.setAutoCommit(false);

			PreparedStatement ps = getSelectGameStatementU();
			ps.setInt(1, game.getGameId());
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				rs.updateInt(GAME_CURRENTPLAYER, game.getPlayerNumber(game.getCurrentPlayer()));
				rs.updateInt(GAME_PHASE, game.getPhase().ordinal());
				rs.updateInt(GAME_STEP, game.getStep());
				rs.updateRow();
			} else {
				throw new SQLException("Update failed, no game with ID: " + game.getGameId() + " to update");
			}
			rs.close();

			updatePlayersInDB(game);

			updatePlayerCardFieldsInDB(game);
			/* TOODO this method needs to be implemented first
			updateCardFieldsInDB(game);
			*/

            connection.commit();
            connection.setAutoCommit(true);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Some DB error");
			
			try {
				connection.rollback();
				connection.setAutoCommit(true);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}

		return false;
	}
	
	@Override
	public Board loadGameFromDB(int id) {
		Board game;
		try {
			PreparedStatement ps = getSelectGameStatementU();
			ps.setInt(1, id);
			
			ResultSet rs = ps.executeQuery();
			int playerNo = -1;
			if (rs.next()) {
				// TODO the width and height could eventually come from the database
				// 	int width = AppController.BOARD_WIDTH;
				// 	int height = AppController.BOARD_HEIGHT;
				// 	game = new Board(width,height);
				// 	and we should also store the used game board in the database
				//      for now, we use the default game board
				game = LoadBoard.loadBoard(rs.getString(GAME_BOARDNAME));
				if (game == null) {
					return null;
				}
				playerNo = rs.getInt(GAME_CURRENTPLAYER);
				// TODO currently we do not set the games name (needs to be added)
				game.setPhase(Phase.values()[rs.getInt(GAME_PHASE)]);
				game.setStep(rs.getInt(GAME_STEP));
			} else {
				// TODO error handling
				return null;
			}
			rs.close();

			game.setGameId(id);			
			loadPlayersFromDB(game);

			if (playerNo >= 0 && playerNo < game.getPlayersNumber()) {
				game.setCurrentPlayer(game.getPlayer(playerNo));
			} else {
				// TODO  error handling
				return null;
			}

			loadCardFieldFromDB(game);

			return game;
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Some DB error");
		}
		return null;
	}
	
	@Override
	public List<GameInDB> getGames() {
		// TODO when there many games in the DB, fetching all available games
		//      from the DB is a bit extreme; eventually there should a
		//      methods that can filter the returned games in order to
		//      reduce the number of the returned games.
		List<GameInDB> result = new ArrayList<>();
		try {
			PreparedStatement ps = getSelectGameIdsStatement();
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int id = rs.getInt(GAME_GAMEID);
				String name = rs.getString(GAME_NAME);
				result.add(new GameInDB(id,name));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;		
	}

	private void createPlayersInDB(Board game) throws SQLException {
		// If we had the proper time, this code should be more defensive
		PreparedStatement ps = getSelectPlayersStatementU();
		ps.setInt(1, game.getGameId());

		ResultSet rs = ps.executeQuery();
		for (int i = 0; i < game.getPlayersNumber(); i++) {
			Player player = game.getPlayer(i);
			rs.moveToInsertRow();
			rs.updateInt(PLAYER_GAMEID, game.getGameId());
			rs.updateInt(PLAYER_PLAYERID, i);
			rs.updateString(PLAYER_NAME, player.getName());
			rs.updateString(PLAYER_COLOUR, player.getColor());
			rs.updateInt(PLAYER_POSITION_X, player.getSpace().x);
			rs.updateInt(PLAYER_POSITION_Y, player.getSpace().y);
			rs.updateInt(PLAYER_HEADING, player.getHeading().ordinal());
			rs.insertRow();
		}

		rs.close();
	}

	private void createCardFieldsInDB(Board game) throws SQLException {
		PreparedStatement ps = getSelectCardFieldStatementU();
		ps.setInt(1, game.getGameId());

		ResultSet rs = ps.executeQuery();
		for (int i = 0; i < game.getPlayersNumber(); i++) {
			for (int j = 0; j < 8; j++) { // there are 8 cards total in the card field. Register cards need to be saved also
				rs.moveToInsertRow();
				CommandCardField cmdCardField = game.getPlayer(i).getCardField(j);
				rs.updateInt(FIELD_GAMEID, game.getGameId());
				rs.updateInt(FIELD_PLAYERID, i);
				rs.updateInt(FIELD_TYPE,FIELD_TYPE_RAND);
				rs.updateInt(FIELD_POS,j);
				rs.updateBoolean(FIELD_VISIBLE, cmdCardField.isVisible());

				if (cmdCardField.getCard() != null) {
					if (cmdCardField.getCard().command == Command.FORWARD) { // ugly code, we should make it sparkle
						rs.updateInt(FIELD_COMMAND,0);
					} else if (cmdCardField.getCard().command == Command.RIGHT) {
						rs.updateInt(FIELD_COMMAND,1);
					} else if (cmdCardField.getCard().command == Command.LEFT) {
						rs.updateInt(FIELD_COMMAND,2);
					} else if (cmdCardField.getCard().command == Command.FAST_FORWARD) {
						rs.updateInt(FIELD_COMMAND,3);
					} else if (cmdCardField.getCard().command == Command.OPTION_LEFT_RIGHT) {
						rs.updateInt(FIELD_COMMAND,4);
					}
				} else {
					rs.updateInt(FIELD_COMMAND,-1);
				}
				rs.insertRow();

			}
			for (int j = 0; j < 5; j++) { // 5 register cards
				rs.moveToInsertRow();
				CommandCardField cmdCardField = game.getPlayer(i).getProgramField(j);
				rs.updateInt(FIELD_GAMEID, game.getGameId());
				rs.updateInt(FIELD_PLAYERID, i);
				rs.updateInt(FIELD_TYPE,FIELD_TYPE_REGISTER);
				rs.updateInt(FIELD_POS,j);
				rs.updateBoolean(FIELD_VISIBLE, cmdCardField.isVisible());

				if (cmdCardField.getCard() != null) {
					if (cmdCardField.getCard().command == Command.FORWARD) { // ugly code, we should make it sparkle
						rs.updateInt(FIELD_COMMAND,0);
					} else if (cmdCardField.getCard().command == Command.RIGHT) {
						rs.updateInt(FIELD_COMMAND,1);
					} else if (cmdCardField.getCard().command == Command.LEFT) {
						rs.updateInt(FIELD_COMMAND,2);
					} else if (cmdCardField.getCard().command == Command.FAST_FORWARD) {
						rs.updateInt(FIELD_COMMAND,3);
					} else if (cmdCardField.getCard().command == Command.OPTION_LEFT_RIGHT) {
						rs.updateInt(FIELD_COMMAND,4);
					}
				} else {
					rs.updateInt(FIELD_COMMAND, -1);
				}
				rs.insertRow();

			}
		}
		rs.close();
	}
	
	private void loadPlayersFromDB(Board game) throws SQLException {
		PreparedStatement ps = getSelectPlayersASCStatement();
		ps.setInt(1, game.getGameId());
		
		ResultSet rs = ps.executeQuery();
		int i = 0;
		while (rs.next()) {
			int playerId = rs.getInt(PLAYER_PLAYERID);
			if (i++ == playerId) {
				// With proper time, this code should be more defensive
				String name = rs.getString(PLAYER_NAME);
				String colour = rs.getString(PLAYER_COLOUR);
				Player player = new Player(game, colour ,name);
				game.addPlayer(player);
				
				int x = rs.getInt(PLAYER_POSITION_X);
				int y = rs.getInt(PLAYER_POSITION_Y);
				player.setSpace(game.getSpace(x,y));
				int heading = rs.getInt(PLAYER_HEADING);
				player.setHeading(Heading.values()[heading]);

				// TODO should also load players program and hand here
			} else {
				System.err.println("Game in DB does not have a player with id " + i +"!");
			}
		}
		rs.close();
	}

	private void updatePlayersInDB(Board game) throws SQLException {
		PreparedStatement ps = getSelectPlayersStatementU();
		ps.setInt(1, game.getGameId());

		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int playerId = rs.getInt(PLAYER_PLAYERID);
			// With enough time, this code should be more defensive
			Player player = game.getPlayer(playerId);
			// rs.updateString(PLAYER_NAME, player.getName()); // not needed: player's names does not change
			rs.updateInt(PLAYER_POSITION_X, player.getSpace().x);
			rs.updateInt(PLAYER_POSITION_Y, player.getSpace().y);
			rs.updateInt(PLAYER_HEADING, player.getHeading().ordinal());
			// TODO error handling
			// TODO take care of case when number of players changes, etc
			rs.updateRow();
		}
		rs.close();

		// TODO error handling/consistency check: check whether all players were updated
	}

	private void updatePlayerCardFieldsInDB(Board game) throws SQLException {
		PreparedStatement ps = getSelectCardFieldStatementU();
		ps.setInt(1, game.getGameId());

		int tempPlayerID;
		int tempCardFieldType;
		int tempCardFieldPos;
		Command tempCardFieldCmd;
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			tempPlayerID = rs.getInt(PLAYER_PLAYERID);
			tempCardFieldPos = rs.getInt(FIELD_POS);
			tempCardFieldType = rs.getInt(FIELD_TYPE);

			if (tempCardFieldType == 0) {
				if (game.getPlayer(tempPlayerID).getProgramField(tempCardFieldPos).getCard() != null) {
					tempCardFieldCmd = game.getPlayer(tempPlayerID).getProgramField(tempCardFieldPos).getCard().command;
					if (tempCardFieldCmd == Command.FORWARD) {
						rs.updateInt(FIELD_COMMAND,0);
					} else if (tempCardFieldCmd == Command.RIGHT) {
						rs.updateInt(FIELD_COMMAND,1);
					} else if (tempCardFieldCmd == Command.LEFT) {
						rs.updateInt(FIELD_COMMAND,2);
					} else if (tempCardFieldCmd == Command.FAST_FORWARD) {
						rs.updateInt(FIELD_COMMAND,3);
					} else if (tempCardFieldCmd == Command.OPTION_LEFT_RIGHT) {
						rs.updateInt(FIELD_COMMAND,4);
					}
				} else {
					rs.updateInt(FIELD_COMMAND,-1);
				}
			} else {
				if (game.getPlayer(tempPlayerID).getCardField(tempCardFieldPos).getCard() != null) {
					tempCardFieldCmd = game.getPlayer(tempPlayerID).getCardField(tempCardFieldPos).getCard().command;
					if (tempCardFieldCmd == Command.FORWARD) {
						rs.updateInt(FIELD_COMMAND,0);
					} else if (tempCardFieldCmd == Command.RIGHT) {
						rs.updateInt(FIELD_COMMAND,1);
					} else if (tempCardFieldCmd == Command.LEFT) {
						rs.updateInt(FIELD_COMMAND,2);
					} else if (tempCardFieldCmd == Command.FAST_FORWARD) {
						rs.updateInt(FIELD_COMMAND,3);
					} else if (tempCardFieldCmd == Command.OPTION_LEFT_RIGHT) {
						rs.updateInt(FIELD_COMMAND,4);
					}
				} else {
					rs.updateInt(FIELD_COMMAND,-1);
				}
			}
			rs.updateRow();
		}
		rs.close();
	}

	private static final String SQL_INSERT_GAME =
			"INSERT INTO Game(name, currentPlayer, phase, step, boardName) VALUES (?, ?, ?, ?, ?)";

	private PreparedStatement insert_game_stmt = null;

	private PreparedStatement getInsertGameStatementRGK() {
		if (insert_game_stmt == null) {
			Connection connection = connector.getConnection();
			try {
				insert_game_stmt = connection.prepareStatement(
						SQL_INSERT_GAME,
						Statement.RETURN_GENERATED_KEYS);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return insert_game_stmt;
	}

	private static final String SQL_SELECT_GAME =
			"SELECT * FROM Game WHERE gameID = ?";
	
	private PreparedStatement select_game_stmt = null;
	
	private PreparedStatement getSelectGameStatementU() {
		if (select_game_stmt == null) {
			Connection connection = connector.getConnection();
			try {
				select_game_stmt = connection.prepareStatement(
						SQL_SELECT_GAME,
						ResultSet.TYPE_FORWARD_ONLY,
					    ResultSet.CONCUR_UPDATABLE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_game_stmt;
	}
		
	private static final String SQL_SELECT_PLAYERS =
			"SELECT * FROM Player WHERE gameID = ?";

	private PreparedStatement select_players_stmt = null;

	private PreparedStatement getSelectPlayersStatementU() {
		if (select_players_stmt == null) {
			Connection connection = connector.getConnection();
			try {
				select_players_stmt = connection.prepareStatement(
						SQL_SELECT_PLAYERS,
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_players_stmt;
	}

	private static final String SQL_SELECT_PLAYERS_ASC =
			"SELECT * FROM Player WHERE gameID = ? ORDER BY playerID ASC";
	
	private PreparedStatement select_players_asc_stmt = null;
	
	private PreparedStatement getSelectPlayersASCStatement() {
		if (select_players_asc_stmt == null) {
			Connection connection = connector.getConnection();
			try {
				// This statement does not need to be updatable
				select_players_asc_stmt = connection.prepareStatement(
						SQL_SELECT_PLAYERS_ASC);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_players_asc_stmt;
	}
	
	private static final String SQL_SELECT_GAMES =
			"SELECT gameID, name FROM Game";
	
	private PreparedStatement select_games_stmt = null;
	
	private PreparedStatement getSelectGameIdsStatement() {
		if (select_games_stmt == null) {
			Connection connection = connector.getConnection();
			try {
				select_games_stmt = connection.prepareStatement(
						SQL_SELECT_GAMES);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_games_stmt;
	}
	private static final String SQL_SELECT_CARD_FIELDS = "SELECT * FROM CardField WHERE gameID = ?";

	private PreparedStatement select_card_field_stat = null;

	private PreparedStatement getSelectCardFieldStatement() {
		if (select_card_field_stat == null) {
			Connection connection = connector.getConnection();
			try {
				select_card_field_stat = connection.prepareStatement(SQL_SELECT_CARD_FIELDS);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_card_field_stat;
	}

	private PreparedStatement select_card_field_stat_u = null;

	private PreparedStatement getSelectCardFieldStatementU() {
		if (select_card_field_stat_u == null) {
			Connection connection = connector.getConnection();
			try {
				select_card_field_stat_u = connection.prepareStatement(
						SQL_SELECT_CARD_FIELDS,
						ResultSet.TYPE_FORWARD_ONLY,
						ResultSet.CONCUR_UPDATABLE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return select_card_field_stat_u;
	}


	private void loadCardFieldFromDB(Board game) throws SQLException {
		PreparedStatement ps = getSelectCardFieldStatement();
		ps.setInt(1, game.getGameId());

		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int playerID = rs.getInt(FIELD_PLAYERID);
			Player player = game.getPlayer(playerID);
			int type = rs.getInt(FIELD_TYPE);
			int pos = rs.getInt(FIELD_POS);
			CommandCardField field;
			if (type == FIELD_TYPE_REGISTER) {
				field = player.getProgramField(pos);
			} else if (type == FIELD_TYPE_RAND) {
				field = player.getCardField(pos);
			} else {
				field = null;
			}
			if (field != null && rs.getInt(FIELD_COMMAND) != -1) {
				field.setVisible(rs.getBoolean(FIELD_VISIBLE));
				Command card = Command.values()[rs.getInt(FIELD_COMMAND)];
				field.setCard(new CommandCard(card));
			}
//			if (field != null) {
//				field.setVisible(rs.getBoolean(FIELD_VISIBLE));
//				Object c = rs.getObject(FIELD_COMMAND);
//				if (c != null) {
//					Command card = Command.values()[rs.getInt(FIELD_COMMAND)];
//					field.setCard(new CommandCard(card));
//				}
//			}
		}
		rs. close();
	}


}
