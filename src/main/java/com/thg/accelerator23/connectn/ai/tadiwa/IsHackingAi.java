package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;
import java.util.*;

public class IsHackingAi extends Player {
  private long startTime;
  private static final long TIME_LIMIT = 10_000_000_000L; // Reduced to 10 seconds
  private static final int MIN_DEPTH = 2;
  private static final int MAX_DEPTH = 10;

  private static final int WIN_SCORE = 1000;
  private static final int LOSE_SCORE = -1000;
  private static final int THREE_IN_ROW = 100;
  private static final int THREE_IN_ROW_OPPONENT = -100;
  private static final int TWO_IN_ROW = 10;
  private static final int TWO_IN_ROW_OPPONENT = -10;

  private static final int BOARD_WIDTH = 10;
  private static final int BOARD_HEIGHT = 8;
  private static final int CONNECT_N = 4;

  private final int[] columnOrder;

  public IsHackingAi(Counter counter) {
    super(counter, IsHackingAi.class.getName());
    this.columnOrder = new int[]{4, 5, 3, 6, 2, 7, 1, 8, 0, 9};
  }

  @Override
  public int makeMove(Board board) {
    this.startTime = System.nanoTime();
    try {
      return getBestMove(board);
    } catch (InvalidMoveException e) {
      return 4;
    }
  }

  private int getBestMove(Board board) throws InvalidMoveException {
    int bestMove = columnOrder[0]; // Default to center column
    int bestScore = Integer.MIN_VALUE;
    Map<Integer, Integer> spaces = populateFreeColumns(board);

    for (int depth = MIN_DEPTH; depth <= MAX_DEPTH; depth += 2) {
      if (isTimeUp()) break;

      int[] result = minimax(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, spaces, true);
      if (result[1] > bestScore) {
        bestMove = result[0];
        bestScore = result[1];
      }

      if (bestScore >= WIN_SCORE) break;
    }
    float timeTaken = System.nanoTime() - startTime;
    System.out.printf("%.2f seconds%n", timeTaken / (float) (1_000_000_000));
    return bestMove;
  }

  private int[] minimax(Board board, int depth, int alpha, int beta, Map<Integer, Integer> spaces, boolean isMaximizing) throws InvalidMoveException {
    if (isTimeUp() || depth == 0 || spaces.isEmpty()) {
      return new int[]{-1, evaluateBoard(board)};
    }

    int bestMove = -1;
    int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    Counter currentCounter = isMaximizing ? getCounter() : getCounter().getOther();

    for (int column : columnOrder) {
      if (!spaces.containsKey(column)) continue;

      Board newBoard = new Board(board, column, currentCounter);
      Map<Integer, Integer> newSpaces = updateSpaces(spaces, column);

      int[] result = minimax(newBoard, depth - 1, alpha, beta, newSpaces, !isMaximizing);

      if (isMaximizing) {
        if (result[1] > bestScore) {
          bestScore = result[1];
          bestMove = column;
        }
        alpha = Math.max(alpha, bestScore);
      } else {
        if (result[1] < bestScore) {
          bestScore = result[1];
          bestMove = column;
        }
        beta = Math.min(beta, bestScore);
      }

      if (beta <= alpha) break;
    }

    return new int[]{bestMove, bestScore};
  }

  private int evaluateBoard(Board board) {
    Counter[][] counters = board.getCounterPlacements();
    return evaluateLines(counters) + evaluateCenterControl(counters);
  }

  private int evaluateLines(Counter[][] counters) {
    int score = 0;
    score += evaluateDirection(counters, 0, 1);
    score += evaluateDirection(counters, 1, 0);
    score += evaluateDirection(counters, 1, 1);
    score += evaluateDirection(counters, 1, -1);
    return score;
  }

  private int evaluateDirection(Counter[][] counters, int deltaRow, int deltaCol) {
    int score = 0;
    int startRow = (deltaRow > 0) ? 0 : BOARD_HEIGHT - 1;
    int endRow = (deltaRow > 0) ? BOARD_HEIGHT - CONNECT_N + 1 : CONNECT_N - 1;

    for (int row = startRow; deltaRow > 0 ? row < endRow : row >= endRow; row += (deltaRow > 0 ? 1 : -1)) {
      for (int col = 0; col <= BOARD_WIDTH - CONNECT_N; col++) {
        Counter[] window = new Counter[CONNECT_N];
        for (int i = 0; i < CONNECT_N; i++) {
          window[i] = counters[col + i * Math.abs(deltaCol)][row + i * deltaRow];
        }
        score += evaluateWindow(window);
      }
    }
    return score;
  }

  private int evaluateWindow(Counter[] window) {
    int myCount = 0, oppCount = 0;
    Counter myCounter = getCounter();

    for (Counter c : window) {
      if (c == myCounter) myCount++;
      else if (c != null) oppCount++;
    }

    if (myCount == 4) return WIN_SCORE;
    if (oppCount == 4) return LOSE_SCORE;
    if (myCount == 3 && oppCount == 0) return THREE_IN_ROW;
    if (oppCount == 3 && myCount == 0) return THREE_IN_ROW_OPPONENT;
    if (myCount == 2 && oppCount == 0) return TWO_IN_ROW;
    if (oppCount == 2 && myCount == 0) return TWO_IN_ROW_OPPONENT;

    return myCount - oppCount;
  }

  private int evaluateCenterControl(Counter[][] counters) {
    int score = 0;
    Counter myCounter = getCounter();

    for (int col = 3; col <= 6; col++) {
      for (int row = 0; row < BOARD_HEIGHT; row++) {
        Counter counter = counters[col][row];
        if (counter == null) continue;

        int weight = BOARD_HEIGHT - row;
        score += (counter == myCounter ? 1 : -1) * weight * (5 - Math.abs(col - 4));
      }
    }
    return score;
  }

  private Map<Integer, Integer> updateSpaces(Map<Integer, Integer> spaces, int column) {
    Map<Integer, Integer> newSpaces = new HashMap<>(spaces);
    int remaining = spaces.get(column) - 1;
    if (remaining > 0) {
      newSpaces.put(column, remaining);
    } else {
      newSpaces.remove(column);
    }
    return newSpaces;
  }

  private Map<Integer, Integer> populateFreeColumns(Board board) {
    Map<Integer, Integer> freeColumns = new HashMap<>();
    for (int x = 0; x < BOARD_WIDTH; x++) {
      for (int y = 0; y < BOARD_HEIGHT; y++) {
        if (!board.hasCounterAtPosition(new Position(x, y))) {
          freeColumns.put(x, BOARD_HEIGHT - y);
          break;
        }
      }
    }
    return freeColumns;
  }

  private boolean isTimeUp() {
    return System.nanoTime() - startTime > TIME_LIMIT;
  }
}