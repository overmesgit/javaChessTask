import ictk.boardgame.Move;
import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessMove;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by user on 12/12/14.
 */
public class RequestHandler {
    private HashMap<Long, GameState> chessGameHashMap = new HashMap<>();
    Random rand = new Random();

    public RequestHandler() {
        try {
            Files.walk(Paths.get("games")).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    GameState gameState = GameState.loadGame(filePath);
                    if (gameState != null) {
                        chessGameHashMap.put(gameState.getId(), gameState);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response[] processRequest(Request request) {
        Response[] response = null;

        try {
            String currentMessage = strip(request.getMessage());
            String currentState = request.state.getOrDefault("state", "");

            switch (currentState) {
                case "":
                    response = setStartState(request);
                    break;
                case "start":
                    switch (currentMessage) {
                        case "0":
                            response = createNewGame(request);
                            break;
                        case "1":
                            response = setConnectToGameState(request);
                            break;
                    }
                    break;
                case "waitenemy":
                    switch (currentMessage) {
                        case "0":
                            response = setColorState(request, "white");
                            break;
                        case "1":
                            response = setColorState(request, "black");
                            break;
                    }
                    break;
                case "turn":
                    response = makeTurnState(request, currentMessage);
                    break;
                case "getid":
                    response = connectToGame(request, currentMessage);
                    break;
                case "leave":
                    response = leaveGame(request);
                    break;
                default:
                    response = new Response[]{new Response(request, "error\n")};
            }

        } catch (Exception e) {
            response = new Response[]{new Response(request, "error\n")};
            e.printStackTrace();
        }
        request.clearMessage();
        return response;
    }

    private Response[] leaveGame(Request request) {
        String gameid = request.state.get("gameid");
        if (gameid != null) {
            Long currentGameId = Long.valueOf(gameid);
            GameState currentGameState = chessGameHashMap.get(currentGameId);

            Request another = currentGameState.getAnother(request);

            Response[] responses = null;
            if (another != null) {
                another.state.put("state", "wait");
                responses = new Response[]{new Response(another, "Your enemy leave Wait enemy turn\n")};
            }
            currentGameState.removeRequest(request);
            return responses;
        }
        return null;
    }

    private Response[] connectToGame(Request request, String currentMessage) {
        long gameId = Long.valueOf(currentMessage);
        GameState gameState = chessGameHashMap.get(gameId);
        if (gameState != null) {
            boolean isFirstPlayer = gameState.addPlayer(request);
            if (isFirstPlayer) {
                return newGameState(request, gameId, gameState);
            } else {
                request.state.put("gameid", String.valueOf(gameId));
                int move = gameState.getGame().getPlayerToMove();

                Request another = gameState.getAnother(request);
                if (another.state.get("color").equals("white")) {
                    request.state.put("color", "black");
                } else {
                    request.state.put("color", "white");
                }

                String color = request.state.get("color");
                if ((color.equals("white") && move == 0) || (color.equals("black") && move == 1)) {
                    return new Response[]{setTurnState(request, gameState), setWaitState(another)};
                } else {
                    return new Response[]{setTurnState(another, gameState), setWaitState(request)};
                }
            }
        } else {
            String message = String.format("Wrong game id %s, enter game id:\n", gameId);
            return new Response[]{new Response(request, message)};
        }
    }

    private Response[] setConnectToGameState(Request request) {
        request.state.put("state", "getid");
        String message = String.format("Enter game id:\n");
        return new Response[]{new Response(request, message)};
    }

    private Response[] setColorState(Request request, String color) {
        request.state.put("color", color);
        request.state.put("state", "turn");

        String message = String.format("Wait enemy\n");
        return new Response[]{new Response(request, message)};
    }

    private Response[] newGameState(Request request, long gameId, GameState gameState) {
        request.state.put("gameid", String.valueOf(gameId));
        request.state.put("state", "waitenemy");
        String message = String.format("gameid:\n%s\n%s\nSelect color: 0 - white, 1 - black:\n", gameId, gameState.getBoard().toString());
        return new Response[]{new Response(request, message)};
    }

    private Response[] createNewGame(Request request) {
        long newGameId = randLong();
        GameState gameState = new GameState(newGameId, request);
        chessGameHashMap.put(newGameId, gameState);
        return newGameState(request, newGameId, gameState);
    }

    public Response[] setStartState(Request request) {
        request.state.put("state", "start");
        String message = "Create game: 0\nConnect to game: 1\n";
        return new Response[]{new Response(request, message)};
    }

    public Response setTurnState(Request request, GameState gameState) {
        request.state.put("state", "turn");
        String message = String.format("%s\nFrom row,col to row,col(5,2,5,4 - e2e4):\n", gameState.getBoard().toString());
        return new Response(request, message);
    }

    public Response setWaitState(Request request) {
        request.state.put("state", "wait");
        return new Response(request, "Wait enemy turn\n");
    }

    public Response[] makeTurnState(Request request, String message) {
        Long currentGameId = Long.valueOf(request.state.get("gameid"));
        GameState currentGameState = chessGameHashMap.get(currentGameId);
        ChessBoard currentBoard = currentGameState.getBoard();

        String[] data = message.split(",");
        try {
            Move currentMove = new ChessMove(currentBoard, Integer.valueOf(data[0]), Integer.valueOf(data[1]), Integer.valueOf(data[2]), Integer.valueOf(data[3]));
            currentBoard.playMove(currentMove);
        } catch (Exception e) {
            return new Response[]{setTurnState(request, currentGameState)};
        }

        request.state.put("state", "wait");
        message = String.format("gameid:\n%s\n%s\nWait enemy turn\n", currentGameId, currentBoard.toString());

        currentGameState.saveGame();

        Request anotherRequest = currentGameState.getAnother(request);
        return new Response[]{new Response(request, message), setTurnState(anotherRequest, currentGameState)};
    }

    public String strip(String text) {
        return text.replace("\n", "").replace("\r", "").trim();
    }

    public long randLong() {
        return Math.abs(rand.nextLong());
    }

}


