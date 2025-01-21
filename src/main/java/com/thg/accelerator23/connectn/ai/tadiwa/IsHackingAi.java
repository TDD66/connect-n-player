package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

import java.util.*;


public class IsHackingAi extends Player {
  private long startTime;
  private Map<String, Integer> transpositionTable;
  private static final long TIME_LIMIT = 10_000_000_000L;
  private static final int MAX_DEPTH = 2;

  public IsHackingAi(Counter counter) {
    //TODO: fill in your name here
    super(counter, IsHackingAi.class.getName());
    this.startTime = System.nanoTime();
    this.transpositionTable = new HashMap<>();
  }

  @Override
  public int makeMove(Board board) {
    //TODO: some crazy analysis
    //TODO: make sure said analysis uses less than 2G of heap and returns within 10 seconds on whichever machine is running it

      try {
          return getBestMove(board);
      } catch (InvalidMoveException e) {
          return 4;
      }
  }

  private int getBestMove(Board board) throws InvalidMoveException {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;
    Map<Integer, Integer> spaces = populateFreeColumns(board);

    for (int depth = 0; depth < MAX_DEPTH; depth++) {
      int[] result = miniMaxWithAlphaBeta(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, spaces,true);

      if (result[1] > bestScore) {
        bestMove = result[0];
        bestScore = result[1];
      }

      if (System.nanoTime() - startTime > TIME_LIMIT) {
        break;
      }
    }
    return bestMove;
  }

  private int[] miniMaxWithAlphaBeta(Board board, int depth, int alpha, int beta, Map<Integer, Integer> spaces, boolean isMaximisingPlayer) throws InvalidMoveException {
    List<Integer> moves = legalColumns(spaces);
    if(depth == MAX_DEPTH || moves.isEmpty()){
      return new int[]{-1, evaluateBoard(board)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    List<Integer> sortedMoves = new ArrayList<>(moves);
    sortedMoves.sort((a, b) -> moveHeuristic(board, b) - moveHeuristic(board, a));

    for(Integer move : sortedMoves){

      Board newBoard = new Board(board, move, getCounter());
      Map<Integer, Integer> newSpaces = new HashMap<>(spaces);
      newSpaces.put(move, spaces.get(move) - 1);

      if(spaces.get(move) == 0){
        spaces.remove(move);
      }

      int[] result = miniMaxWithAlphaBeta(newBoard, depth + 1, alpha, beta, newSpaces,!isMaximisingPlayer);
      if(isMaximisingPlayer) {
        if(result[1] > bestScore){
          bestScore = result[1];
          bestMove = move;
        }
        alpha = Math.max(alpha, bestScore);
      }
      else {
        if(result[1] < bestScore){
          bestScore = result[1];
          bestMove = move;
        }
        beta = Math.min(beta, bestScore);
      }
      if(beta <= alpha) {
        break;
      }
    }
    return new int[] {bestMove, bestScore};
  }

  private int evaluateBoard(Board board) {
    int score = 0;

    Counter[][] counterPlacements = board.getCounterPlacements();
    score += evaluateSlidingWindow(counterPlacements, this.getCounter());
    score -= evaluateSlidingWindow(counterPlacements, this.getCounter().getOther());

    return score;
  }

  private int evaluateSlidingWindow(Counter[][] counters, Counter counter) {
    int score = 0, height = 8, width = 10;

    for (int row = 0; row < height; row++) {
      for (int col = 0; col <= width - 4; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row],
                counters[col + 2][row],
                counters[col + 3][row],
        };
        score += evaluateWindow(window, counter);
      }
    }

    for (int col = 0; col < width; col++) {
      for (int row = 0; row < height - 3; row++) {
        Counter[] window = {
                counters[col][row],
                counters[col][row + 1],
                counters[col][row + 2],
                counters[col][row + 3]
        };
        score += evaluateWindow(window, counter);
      }
    }

    for (int row = 3; row < height; row++) {
      for (int col = 3; col < width; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col - 1][row - 1],
                counters[col - 2][row - 2],
                counters[col - 3][row - 3],
        };
        score += evaluateWindow(window, counter);
      }
    }

    for (int row = 0; row < height - 3; row++) {
      for (int col = 0; col < width - 3; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row + 1],
                counters[col + 2][row + 2],
                counters[col + 3][row + 3],
        };
        score += evaluateWindow(window, counter);
      }
    }

    return score;
  }

  private int evaluateWindow(Counter[] window, Counter counter) {
    Counter opponentCounter = counter.getOther();
    int myCount = 0, opponentCount = 0;

      for (Counter value : window) {
          if (value == counter) {
              myCount++;
          } else if (value == opponentCounter) {
              opponentCount++;
          }
      }

    if(myCount == 4){
      return 1000;
    }
    else if(opponentCount == 4){
      return -1000;
    }
    else if(myCount == 3 && opponentCount == 0) {
      return 100;
    }
    else if(opponentCount == 3 && myCount == 0) {
      return -100;
    }
    else if(myCount == 2 && opponentCount == 0) {
      return 10;
    }
    else if(opponentCount == 2 && myCount == 0) {
      return -10;
    }
    else if(myCount == 1 && opponentCount == 0) {
      return 1;
    }
    else if(opponentCount == 1 && myCount == 0) {
      return -1;
    }

    return 0;
  }

  private int moveHeuristic(Board board, int column) {
    int score = 0;

    score += distanceFromCentre(column);

    if(isWinningMove(board, column, this.getCounter())) {
      score += 100;
    } else if (isWinningMove(board, column, this.getCounter())) {
      score -= 100;
    }

    return score;
  }

  private int distanceFromCentre(int column) {
    if (column == 4 || column == 5) {
      return 10;
    }
    else if(column == 3 || column == 6) {
      return 5;
    }

    else if(column < 3 ){
      return 4 - column + 1;
    }
    return column + 1 - 5;
  }

  private boolean isWinningMove(Board board, int column, Counter counter) {
    return false;
  }

  private Map<Integer, Integer> populateFreeColumns(Board board) {
    Map<Integer, Integer> freeColumns = new HashMap<>();
    for(int x = 0; x < 10; x++){
      for(int y = 7; y >= 0; y--){
        Position position = new Position(x, y);
        if(!board.hasCounterAtPosition(position)){
          freeColumns.put(x, y + 1);
          break;
        }
      }
    }
    return freeColumns;
  }

  private List<Integer> legalColumns(Map<Integer, Integer> spaces) {
      return spaces.keySet().stream().toList();
  }
}
