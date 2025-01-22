package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

import java.util.*;


public class IsHackingAi extends Player {
  private long startTime;
  private Map<Integer, TranspositionEntry> transpositionTable;
  private static final long TIME_LIMIT = 10_000_000_000_000_000L;
  private static final int MIN_DEPTH = 2;
  private static final int MAX_DEPTH = 10;

  public IsHackingAi(Counter counter) {
    //TODO: fill in your name here
    super(counter, IsHackingAi.class.getName());
    this.transpositionTable = new HashMap<>();
  }

  @Override
  public int makeMove(Board board) {
    //TODO: some crazy analysis
    //TODO: make sure said analysis uses less than 2G of heap and returns within 10 seconds on whichever machine is running it
    this.startTime = System.nanoTime();
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

    int[] result = miniMaxWithAlphaBeta(board, MAX_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, spaces,true);
    bestMove = result[0];
    bestScore = result[1];

    float timeTaken = (float) (System.nanoTime() - this.startTime) / (float) 1_000_000_000L;
    System.out.println(String.format("%.2f seconds", timeTaken));
    return bestMove;
  }

  private int[] miniMaxWithAlphaBeta(Board board, int depth, int alpha, int beta, Map<Integer, Integer> spaces, boolean isMaximisingPlayer) throws InvalidMoveException {

    TranspositionEntry entry = transpositionTableLookup(board);
    if(entry != null && entry.depth >= depth) {
      return new int[]{-1, entry.score};
    }

    List<Integer> moves = legalColumns(spaces);
    if(depth == 0 || moves.isEmpty()){
      return new int[]{-1, evaluateBoard(board)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    Counter counter = isMaximisingPlayer ? getCounter() : getCounter().getOther();

    List<Integer> sortedMoves = new ArrayList<>(moves);
    sortedMoves.sort((a, b) -> moveHeuristic(board, b) - moveHeuristic(board, a));
    for(Integer move : sortedMoves){
      Board newBoard = new Board(board, move, counter);
      Map<Integer, Integer> newSpaces = new HashMap<>(spaces);
      newSpaces.put(move, spaces.get(move) - 1);

      if(newSpaces.get(move) == 0){
        newSpaces.remove(move);
      }

      int[] result = miniMaxWithAlphaBeta(newBoard, depth - 1, alpha, beta, newSpaces,!isMaximisingPlayer);
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
    storeInTranspositionTable(board, bestScore, depth);

    return new int[] {bestMove, bestScore};
  }

  private int evaluateBoard(Board board) {
    int score = 0;

    Counter[][] counterPlacements = board.getCounterPlacements();
    score += evaluateSlidingWindow(counterPlacements, this.getCounter());
    score += centreColumnBias(board);

    return score;
  }

  private int evaluateSlidingWindow(Counter[][] counters, Counter counter) {
    int score = 0, height = 8, width = 10;

    score += evaluateHorizontal(counters, counter, height, width);
    score += evaluateVertical(counters, counter, width, height);
    score += evaluateLeftDiag(counters, counter, height, width);
    score += evaluateRightDiag(counters, counter, height, width);

    return score;
  }

  private int evaluateRightDiag(Counter[][] counters, Counter counter, int height, int width) {
    int score = 0;
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

  private int evaluateLeftDiag(Counter[][] counters, Counter counter, int height, int width) {
    int score = 0;
    for (int row = height - 1; row >= 3; row--) {
      for (int col = 0; col < width - 3; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row - 1],
                counters[col + 2][row - 2],
                counters[col + 3][row - 3],
        };
        score += evaluateWindow(window, counter);
      }
    }
    return score;
  }

  private int evaluateVertical(Counter[][] counters, Counter counter, int width, int height) {
    int score = 0;
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
    return score;
  }

  private int evaluateHorizontal(Counter[][] counters, Counter counter, int height, int width) {
    int score = 0;
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

  private int centreColumnBias(Board board) {
    int score = 0, height = 8;
    int[] centreColumns = {4, 5};

    for(int col: centreColumns){
      for(int row = 0; row < height; row++){
        Position position = new Position(col, row);
        if(board.hasCounterAtPosition(position)){
          if(board.getCounterAtPosition(position).equals(this.getCounter())){
            score += 1;
          }
          else {
            score -= 1;
          }
        }
      }
    }
    return score;
  }

  private int moveHeuristic(Board board, int column) {
    int score = 0;

    score += distanceFromCentre(column);

//    if(isWinningMove(board, column, this.getCounter())) {
//      score += 100;
//    } else if (isWinningMove(board, column, this.getCounter().getOther())) {
//      score += 100;
//    }

    return score;
  }

  private int distanceFromCentre(int column) {
    if (column == 4 || column == 5) {
      return 5;
    }
    else if(column == 3 || column == 6) {
      return 2;
    }
    return 0;
  }

  private boolean isWinningMove(Board board, int column, Counter counter) {
    Board newBoard;
    
    try {
      newBoard = new Board(board, column, counter);
    } catch (InvalidMoveException invalidMoveException) {
      return false;
    }

    Counter[][] counters = newBoard.getCounterPlacements();
    return evaluateWins(counters, counter);
  }

  private boolean evaluateWins(Counter[][] counters, Counter counter) {
    return evaluateVerticalWin(counters, counter) ||
            evaluateHorizontalWin(counters, counter) ||
            evaluateRightDiagonalWin(counters, counter) ||
            evaluateLeftDiagonalWin(counters, counter);
  }

  private boolean evaluateHorizontalWin(Counter[][] counters, Counter counter) {
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 10 - 3; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row],
                counters[col + 2][row],
                counters[col + 3][row]
        };
        if (evaluateWindow(window, counter) == 1000){
          return true;
        }
      }
    }
    return false;
  }

  private boolean evaluateVerticalWin(Counter[][] counters, Counter counter) {
    for(int col = 0; col < 10; col++) {
      for (int row = 0; row < 8 - 3; row++) {
        Counter[] window = {
                counters[col][row],
                counters[col][row + 1],
                counters[col][row + 2],
                counters[col][row + 3]
        };
        if (evaluateWindow(window, counter) == 1000){
          return true;
        }
      }
    }
    return false;
  }

  private boolean evaluateRightDiagonalWin(Counter[][] counters, Counter counter) {
    for(int row = 0; row < 8 - 3; row++){
      for (int col = 0; col < 10 - 3; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row + 1],
                counters[col + 2][row + 2],
                counters[col + 3][row + 3]
        };
        if (evaluateWindow(window, counter) == 1000){
          return true;
        }
      }
    }
    return false;
  }

  private boolean evaluateLeftDiagonalWin(Counter[][] counters, Counter counter) {

    for (int row = 8 - 1; row >= 3; row--) {
      for (int col = 0; col < 10 - 3; col++) {
        Counter[] window = {
                counters[col][row],
                counters[col + 1][row - 1],
                counters[col + 2][row - 2],
                counters[col + 3][row - 3],
        };
        if (evaluateWindow(window, counter) == 1000){
          return true;
        }
      }
    }
    return false;
  }


  private Map<Integer, Integer> populateFreeColumns(Board board) {
    Map<Integer, Integer> freeColumns = new HashMap<>();
    for(int x = 0; x < 10; x++){
      for(int y = 0; y < 8; y++){
        Position position = new Position(x, y);
        if(!board.hasCounterAtPosition(position)){
          freeColumns.put(x, 8 - y);
          break;
        }
      }
    }
    return freeColumns;
  }

  private List<Integer> legalColumns(Map<Integer, Integer> spaces) {
      return spaces.keySet().stream().toList();
  }

  private TranspositionEntry transpositionTableLookup(Board board) {
    int boardHash = hashBoard(board);
    return this.transpositionTable.getOrDefault(boardHash, null);
  }

  private void storeInTranspositionTable(Board board, int score, int depth) {
    int boardHash = hashBoard(board);
    this.transpositionTable.put(boardHash, new TranspositionEntry(score, depth));
  }

  private int hashBoard(Board board) {
    return board.hashCode();
  }

  private static class TranspositionEntry {
    int score;
    int depth;

    TranspositionEntry(int score, int depth) {
      this.score = score;
      this.depth = depth;
    }
  }
}