import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Chess {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Chess Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(ChessBoard.TILE_SIZE * ChessBoard.BOARD_SIZE + 200,
                                                 ChessBoard.TILE_SIZE * ChessBoard.BOARD_SIZE + 40));
            frame.setMinimumSize(new Dimension(600, 600));
            frame.add(new ChessBoard());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

class ChessBoard extends JPanel implements MouseListener, MouseMotionListener {
    public static final int TILE_SIZE = 80;
    public static final int BOARD_SIZE = 8;

    private final Board chessBoard = new Board();
    private Square selectedSquare = Square.NONE;
    private String draggedPieceSymbol = null;
    private Point dragPoint = null;

    private int whiteScore = 0;
    private int blackScore = 0;

    private Image boardImage;
    private final Map<String, Image> pieceImages = new HashMap<>();
    private final List<Square> possibleMoves = new ArrayList<>();

    public ChessBoard() {
        loadResources();
        addMouseListener(this);
        addMouseMotionListener(this);
        setPreferredSize(new Dimension(TILE_SIZE * BOARD_SIZE + 150, TILE_SIZE * BOARD_SIZE));
        chessBoard.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    private void loadResources() {
        try {
            boardImage = loadImage("assets/board.png");
            String[] pieceCodes = {"P", "p", "N", "n", "B", "b", "R", "r", "Q", "q", "K", "k"};
            String[] pieceFiles = {
                "pawn_white.png", "pawn_black.png", "knight_white.png", "knight_black.png",
                "bishop_white.png", "bishop_black.png", "rook_white.png", "rook_black.png",
                "queen_white.png", "queen_black.png", "king_white.png", "king_black.png"
            };
    
            for (int i = 0; i < pieceCodes.length; i++) {
                try {
                    Image image = loadImage("assets/" + pieceFiles[i]);
                    pieceImages.put(pieceCodes[i], image);
                    System.out.println("Loaded image for " + pieceCodes[i] + ": assets/" + pieceFiles[i]);
                } catch (IOException e) {
                    System.err.println("Error loading image for " + pieceCodes[i] + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Error loading board image: " + e.getMessage());
        }
    }

    private Image loadImage(String path) throws IOException {
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource == null) {
            System.err.println("Image not found: " + path);
            throw new IOException("Image not found: " + path);
        }
        System.out.println("Loading image from: " + resource);
        return ImageIO.read(resource);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        drawBoard(g2d);
        drawPieces(g2d);
        drawPossibleMoves(g2d);
        drawDraggedPiece(g2d);
        drawScores(g2d);
    }

    private void drawBoard(Graphics2D g2d) {
        if (boardImage != null) {
            g2d.drawImage(boardImage, 0, 0, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE, this);
        } else {
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    g2d.setColor((row + col) % 2 == 0 ? new Color(238, 238, 210) : new Color(118, 150, 86));
                    g2d.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void drawPieces(Graphics2D g2d) {
        for (Square square : Square.values()) {
            if (square == Square.NONE) continue;

            Piece piece = chessBoard.getPiece(square);
            if (piece != Piece.NONE) {
                if (square == selectedSquare && dragPoint != null) continue;
                drawPiece(g2d, mapPieceSymbol(piece), square);
            }
        }
    }

    private void drawPossibleMoves(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0, 0, 0, 77)); // 30% opacity black

        for (Square square : possibleMoves) {
            int col = square.getFile().ordinal();
            int rank = square.getRank().ordinal();
            int visualRow = BOARD_SIZE - 1 - rank;

            int x = col * TILE_SIZE + TILE_SIZE / 4;
            int y = visualRow * TILE_SIZE + TILE_SIZE / 4;
            int dotSize = TILE_SIZE / 2;

            g2d.fillOval(x, y, dotSize, dotSize);
        }
    }

    private void drawDraggedPiece(Graphics2D g2d) {
        if (draggedPieceSymbol != null && dragPoint != null) {
            Image pieceImage = pieceImages.get(draggedPieceSymbol);
            if (pieceImage != null) {
                int x = dragPoint.x - TILE_SIZE / 2;
                int y = dragPoint.y - TILE_SIZE / 2;
                g2d.drawImage(pieceImage, x, y, TILE_SIZE, TILE_SIZE, this);
            }
        }
    }

    private void drawPiece(Graphics g, String pieceSymbol, Square square) {
        Image pieceImage = pieceImages.get(pieceSymbol);
        if (pieceImage == null) {
            System.err.println("No image found for piece symbol: " + pieceSymbol);
            return;
        }
    
        int col = square.getFile().ordinal();
        int rank = square.getRank().ordinal();
        int visualRow = BOARD_SIZE - 1 - rank;
    
        int x = col * TILE_SIZE;
        int y = visualRow * TILE_SIZE;
        g.drawImage(pieceImage, x, y, TILE_SIZE, TILE_SIZE, this);
    }
    

    private void drawScores(Graphics2D g2d) {
        int textX = TILE_SIZE * BOARD_SIZE + 20;
        int boardHeight = TILE_SIZE * BOARD_SIZE;

        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.BLACK);

        g2d.drawString(chessBoard.getSideToMove() == Side.WHITE ? "White's Turn" : "Black's Turn", textX, 30);
        g2d.drawString("Black: " + (blackScore > 0 ? "+" + blackScore : "0"), textX, 70);
        g2d.drawString("White: " + (whiteScore > 0 ? "+" + whiteScore : "0"), textX, boardHeight - 30);
    }

    private int getPieceWeight(String pieceSymbol) {
        if (pieceSymbol == null) return 0;
        switch (pieceSymbol) {
            case "P": case "p": return 1; // Pawn
            case "N": case "n": return 3; // Knight
            case "B": case "b": return 3; // Bishop
            case "R": case "r": return 5; // Rook
            case "Q": case "q": return 9; // Queen
            case "K": case "k": return 0; // King (not scored)
            default: return 0;
        }
    }

    private void movePiece(Square targetSquare) {
        Piece movingPiece = chessBoard.getPiece(selectedSquare);
        Piece promotionPiece = Piece.NONE;

        if (movingPiece.getPieceType() == PieceType.PAWN) {
            Side side = movingPiece.getPieceSide();
            if ((side == Side.WHITE && targetSquare.getRank() == Rank.RANK_8) ||
                (side == Side.BLACK && targetSquare.getRank() == Rank.RANK_1)) {
                promotionPiece = Piece.make(side, PieceType.QUEEN);
            }
        }

        Move move = new Move(selectedSquare, targetSquare, promotionPiece);
        try {
            Move validMove = null;
            for (Move legalMove : MoveGenerator.generateLegalMoves(chessBoard)) {
                if (legalMove.getFrom() == move.getFrom() && legalMove.getTo() == move.getTo()) {
                    validMove = legalMove;
                    break;
                }
            }

            if (validMove != null) {
                Piece capturedPiece = chessBoard.getPiece(targetSquare);
                if (capturedPiece != Piece.NONE) {
                    String pieceSymbol = mapPieceSymbol(capturedPiece);
                    if (pieceSymbol == null || pieceSymbol.isEmpty()) {
                        System.err.println("Invalid piece symbol for scoring: " + capturedPiece.toString());
                    } else {
                        int capturedValue = getPieceWeight(pieceSymbol);
                        if (movingPiece.getPieceSide() == Side.WHITE) {
                            whiteScore += capturedValue;
                        } else {
                            blackScore += capturedValue;
                        }
                    }
                }
                chessBoard.doMove(validMove);
                selectedSquare = Square.NONE;
                draggedPieceSymbol = null;
                possibleMoves.clear();
            }
        } catch (MoveGeneratorException e) {
            e.printStackTrace();
        }
        repaint();
    }

    private String mapPieceSymbol(Piece piece) {
        if (piece == Piece.NONE) return "";
        Side side = piece.getPieceSide();
        PieceType type = piece.getPieceType();
    
        switch (type) {
            case PAWN: return side == Side.WHITE ? "P" : "p";
            case KNIGHT: return side == Side.WHITE ? "N" : "n";
            case BISHOP: return side == Side.WHITE ? "B" : "b";
            case ROOK: return side == Side.WHITE ? "R" : "r";
            case QUEEN: return side == Side.WHITE ? "Q" : "q";
            case KING: return side == Side.WHITE ? "K" : "k";
            default:
                System.err.println("Unrecognized piece type: " + type);
                return "";
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int col = e.getX() / TILE_SIZE;
        int visualRow = e.getY() / TILE_SIZE;
        int rank = BOARD_SIZE - 1 - visualRow;

        if (rank >= 0 && rank < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
            Square clickedSquare = Square.squareAt(rank * BOARD_SIZE + col);

            if (possibleMoves.contains(clickedSquare)) {
                movePiece(clickedSquare);
                return;
            }

            Piece piece = chessBoard.getPiece(clickedSquare);
            if (piece != Piece.NONE && piece.getPieceSide() == chessBoard.getSideToMove()) {
                selectedSquare = clickedSquare;
                draggedPieceSymbol = mapPieceSymbol(piece);
                calculatePossibleMoves();
            } else {
                selectedSquare = Square.NONE;
                draggedPieceSymbol = null;
                possibleMoves.clear();
            }
        }
        repaint();
    }

    private void calculatePossibleMoves() {
        possibleMoves.clear();
        try {
            for (Move move : MoveGenerator.generateLegalMoves(chessBoard)) {
                if (move.getFrom() == selectedSquare) {
                    possibleMoves.add(move.getTo());
                }
            }
        } catch (MoveGeneratorException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggedPieceSymbol != null) {
            dragPoint = e.getPoint();
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggedPieceSymbol != null) {
            int col = e.getX() / TILE_SIZE;
            int visualRow = e.getY() / TILE_SIZE;
            int rank = BOARD_SIZE - 1 - visualRow;

            if (rank >= 0 && rank < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                Square clickedSquare = Square.squareAt(rank * BOARD_SIZE + col);
                if (possibleMoves.contains(clickedSquare)) {
                    movePiece(clickedSquare);
                }
            }
        }

        dragPoint = null;
        repaint();
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}