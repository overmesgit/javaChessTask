import ictk.boardgame.chess.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Random;

/**
 * Created by user on 12/13/14.
 */
public class GameState {
    private final long gameId;
    Request firstPlayerRequest;

    Request secondPlayerRequest;
    ChessGame game;

    public GameState(long id, Request firstPlayerRequest) {
        gameId = id;
        game = new ChessGame();
        this.firstPlayerRequest = firstPlayerRequest;
    }

    public GameState(long id, ChessGame game) {
        gameId = id;
        this.game = game;
    }

    public ChessGame getGame() {
        return game;
    }

    public Request getFirstPlayerRequest() {
        return firstPlayerRequest;
    }

    public Request getSecondPlayerRequest() {
        return secondPlayerRequest;
    }

    public ChessBoard getBoard() {
        return (ChessBoard) game.getBoard();
    }

    public boolean addPlayer(Request request) {
        if (firstPlayerRequest == null) {
            firstPlayerRequest = request;
            if (secondPlayerRequest == null) {
                return true;
            } else {
                return false;
            }
        } else {
            secondPlayerRequest = request;
            if (firstPlayerRequest.state.get("color").equals("white")) {
                request.state.put("color", "black");
            } else {
                request.state.put("color", "white");
            }
            return false;
        }
    }

    public Request getAnother(Request request) {
        if (firstPlayerRequest == request) {
            return secondPlayerRequest;
        } else {
            return firstPlayerRequest;
        }
    }

    public void removeRequest(Request request) {
        if (firstPlayerRequest == request) {
            firstPlayerRequest = null;
        }

        if (secondPlayerRequest == request) {
            secondPlayerRequest = null;
        }
    }

    public void saveGame() {
        try {
            String path = String.format("games/%s", gameId);

            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            SerializableBoard serializableBoard = new SerializableBoard((ChessBoard) game.getBoard(), gameId);
            out.writeObject(serializableBoard);

            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameState loadGame(Path path) {
        SerializableBoard serializableChessBoard = null;
        try {
            FileInputStream fileIn = new FileInputStream(path.toString());
            ObjectInputStream in = new ObjectInputStream(fileIn);

            serializableChessBoard = (SerializableBoard) in.readObject();
            in.close();
            fileIn.close();

            ChessGame chessGame = new ChessGame(new ChessGameInfo(), serializableChessBoard.getBoard());
            return new GameState(serializableChessBoard.getGameId(), chessGame);
        } catch(IOException | ClassNotFoundException i) {
            i.printStackTrace();
        }
        return null;
    }

    public Long getId() {
        return gameId;
    }
}

class SerializableBoard implements Serializable {
    private final char[][] matrix;
    private final boolean isBlackMove;
    private final boolean castleWK;
    private final boolean castleWQ;
    private final boolean castleBK;
    private final boolean castleBQ;
    private final char enpassant;
    private final int plyCount;
    private final int moveCount;

    private final long gameId;

    SerializableBoard(ChessBoard board, long gameId) {
        this.gameId = gameId;
        matrix = toCharArray(board);
        isBlackMove = board.isBlackMove();
        castleWK = board.isWhiteCastleableKingside();
        castleWQ = board.isWhiteCastleableQueenside();
        castleBK = board.isBlackCastleableKingside();
        castleBQ = board.isBlackCastleableQueenside();
        enpassant = '-';
        plyCount = board.get50MoveRulePlyCount();
        moveCount = board.getCurrentMoveNumber();
    }

    public ChessBoard getBoard() {
        return new ChessBoard(matrix, isBlackMove, castleWK, castleWQ, castleBK, castleBQ, enpassant, plyCount, moveCount);
    }

    public long getGameId() {
        return gameId;
    }

    public char[][] toCharArray (ChessBoard current) {
        char[][] board = new char[ChessBoard.MAX_RANK][ChessBoard.MAX_FILE];
        for (byte r=0; r < ChessBoard.MAX_RANK; r++)
            for (byte f=0; f < ChessBoard.MAX_FILE; f++)
                if (current.getSquare(f+1, r+1).isOccupied()) {
                    switch (current.getSquare(f+1, r+1).piece.getIndex() % ChessPiece.BLACK_OFFSET) {
                        case Pawn.INDEX:   board[f][r] = 'P'; break;
                        case Knight.INDEX: board[f][r] = 'N'; break;
                        case Bishop.INDEX: board[f][r] = 'B'; break;
                        case Rook.INDEX:   board[f][r] = 'R'; break;
                        case Queen.INDEX:  board[f][r] = 'Q'; break;
                        case King.INDEX:   board[f][r] = 'K'; break;
                        default:
                    }
                    if (current.getSquare(f+1, r+1).piece.getIndex() >= ChessPiece.BLACK_OFFSET)
                        board[f][r] = Character.toLowerCase(board[f][r]);
                }
        return board;
    }

}
