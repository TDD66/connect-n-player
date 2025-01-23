package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

public class IsHackingAi extends Player {
  private static final long TIME_LIMIT = 10_000_000_000L; // 10 seconds
  private static final int MIN_DEPTH = 6;
  private static final int MAX_DEPTH = 1000;
  private long startTime;

  private static final int WIDTH = 10;
  private static final int HEIGHT = 8;
  private static final long FULL_BOARD_MASK = 0xFF_FF_FF_FF_FF_FF_FF_FFL; // Full 8x10 board

  private final int[] columnOrder = {4, 5, 3, 6, 2, 7, 1, 8, 0, 9};

  public IsHackingAi(Counter counter) {
    super(counter, IsHackingAi.class.getName());
  }

  @Override
  public int makeMove(Board board) {
    this.startTime = System.nanoTime();
    return getBestMove(board);
  }

  private int getBestMove(Board board) {
//    int canIWin = checkInstantWin(board, getCounter());
//    if (canIWin != -1) {
//      return canIWin;
//    }
//
//    int canILose = checkInstantWin(board, getCounter().getOther());
//    if (canILose != -1) {
//      return canILose;
//    }

    return iterativeDeepeningSearch(board);
  }

  private int iterativeDeepeningSearch(Board board) {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;

    for (int depth = MIN_DEPTH; !isTimeUp() && depth <= MAX_DEPTH; depth += 2) {
      try {
        int[] result = searchAtDepth(board, depth);
        if (result[1] > bestScore) {
          bestScore = result[1];
          bestMove = result[0];
        }
      } catch (TimeOutException e) {
        System.out.println("Best move: " + bestMove);
        break;
      }
    }
    return bestMove;
  }

  private int[] searchAtDepth(Board board, int depth) throws TimeOutException {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;

    for (int move : columnOrder) {
      if (isColumnPlayable(board, move)) {
        try {
          Board newBoard = new Board(board, move, getCounter());
          long newRedBitboard = boardToBitboard(newBoard, getCounter());
          long newYellowBitboard = boardToBitboard(newBoard, getCounter().getOther());
          int[] result = miniMaxWithAlphaBeta(newRedBitboard, newYellowBitboard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

          if (result[1] > bestScore) {
            bestScore = result[1];
            bestMove = move;
          }
        } catch(InvalidMoveException ignored){}
        catch (TimeOutException e) {
          System.out.println("Depth failed at: " + depth);
          throw e;
        }
      }
    }

    return new int[]{bestMove, bestScore};
  }

  private int[] miniMaxWithAlphaBeta(long redBitboard, long yellowBitboard, int depth, int alpha, int beta, boolean isMaximisingPlayer) throws TimeOutException {
    if (isTimeUp()) {
      throw new TimeOutException();
    }

    if (depth == 0 || isGameTerminal(redBitboard, yellowBitboard)) {
      return new int[]{-1, evaluateBoard(redBitboard, yellowBitboard)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    for (int move : columnOrder) {
      if (isColumnPlayableBitboard(redBitboard, move, yellowBitboard)) {
        long newRedBitboard = makeMoveOnBitboard(redBitboard, move, getCounter());
        long newYellowBitboard = makeMoveOnBitboard(yellowBitboard, move, getCounter().getOther());
        int[] result = miniMaxWithAlphaBeta(newRedBitboard, newYellowBitboard, depth - 1, alpha, beta, !isMaximisingPlayer);

        if (isMaximisingPlayer) {
          if (result[1] > bestScore) {
            bestScore = result[1];
            bestMove = move;
          }
          alpha = Math.max(alpha, bestScore);
        } else {
          if (result[1] < bestScore) {
            bestScore = result[1];
            bestMove = move;
          }
          beta = Math.min(beta, bestScore);
        }

        if (beta <= alpha) {
          break;
        }
      }
    }

    return new int[]{bestMove, bestScore};
  }

  private long boardToBitboard(Board board, Counter counter) {
    long bitboard = 0L;
    for (int x = 0; x < WIDTH; x++) {
      for (int y = 0; y < HEIGHT; y++) {
        Position position = new Position(x, y);
        if (board.hasCounterAtPosition(position) && board.getCounterAtPosition(position).equals(counter)) {
          int index = x + y * WIDTH;
          bitboard |= (1L << index);
        }
      }
    }
    return bitboard;
  }

  private boolean isColumnPlayable(Board board, int column) {
    Position position = new Position(column, HEIGHT - 1);
    return !board.hasCounterAtPosition(position);
  }

  private boolean isColumnPlayableBitboard(long redBitboard, int column, long yellowBitboard) {
    long mask = 1L << (column + (HEIGHT - 1) * WIDTH);
    return (redBitboard & mask) == 0 && (yellowBitboard & mask) == 0;
  }

  private long makeMoveOnBitboard(long bitboard, int column, Counter counter) {
    int index = column + (findAvailableRow(column) * WIDTH);
    return bitboard | (1L << index);
  }

  private int findAvailableRow(int column) {
    for (int row = HEIGHT - 1; row >= 0; row--) {
      if (!isColumnFull(column, row)) {
        return row;
      }
    }
    return -1;
  }

  private boolean isColumnFull(int column, int row) {
    // Check the bitboard for the column's full status
    return false;  // Replace with actual bitboard check
  }

  private int evaluateBoard(long redBitboard, long yellowBitboard) {
    int score = 0;

    // Evaluate horizontal, vertical, and diagonal patterns here using bitwise operations
    score += evaluateLines(redBitboard, yellowBitboard);
    score += evaluateDiagonals(redBitboard, yellowBitboard);

    return score;
  }

  private int evaluateLines(long redBitboard, long yellowBitboard) {
    int score = 0;
    // Check each horizontal, vertical, and diagonal line here
    return score;
  }

  private int evaluateDiagonals(long redBitboard, long yellowBitboard) {
    int score = 0;
    // Check for diagonal patterns using bitwise operations
    return score;
  }

  private boolean isGameTerminal(long redBitboard, long yellowBitboard) {
    // Check for a terminal state using the bitboards
    return false;  // Replace with actual terminal check
  }

  private boolean isTimeUp() {
    return System.nanoTime() - startTime > TIME_LIMIT - 5_000_000L;
  }

  private static class TimeOutException extends RuntimeException {
  }
}
