package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

public class IsHackingAi extends Player {
  private long startTime;
  private static final long TIME_LIMIT = 10_000_000_000L;
  private static final int MIN_DEPTH = 6;
  private static final int MAX_DEPTH = 80;

  private final int[] columnOrder;
  private static final int HEIGHT = 8;
  private static final int WIDTH = 10;
  private static final int[] dx = new int[]{1, 0, 1, 1};
  private static final int[] dy = new int[]{0, 1, 1, -1};

  public IsHackingAi(Counter counter) {
    //TODO: fill in your name here
    super(counter, IsHackingAi.class.getName());
    columnOrder = new int[]{4, 5, 3, 6, 2, 7, 1, 8, 0, 9};
  }

  @Override
  public int makeMove(Board board) {
    //TODO: some crazy analysis
    //TODO: make sure said analysis uses less than 2G of heap and returns within 10 seconds on whichever machine is running it
    this.startTime = System.nanoTime();
    int move = getBestMove(board);
    long timeTaken = System.nanoTime() - this.startTime;
    System.out.printf("%d seconds%n", timeTaken);
    return move;
  }

  private int getBestMove(Board board) {
    int canIWin = checkInstantWin(board, getCounter());
    if(canIWin != -1) {
      return canIWin;
    }

    int canILose = checkInstantWin(board, getCounter().getOther());
    if(canILose != -1) {
      return canILose;
    }

    return iterativeDeepeningSearch(board);
  }

  private int iterativeDeepeningSearch(Board board) {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;

    for(int depth = MIN_DEPTH; !isTimeUp() && depth <= MAX_DEPTH; depth += 2) {
      try{
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

    for(int move : columnOrder) {
      try {
        Board newBoard = new Board(board, move, getCounter());
        int[] result = miniMaxWithAlphaBeta(newBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

        if (result[1] > bestScore) {
          bestScore = result[1];
          bestMove = move;
        }
      }
      catch (InvalidMoveException ignored) {}
      catch (TimeOutException timeOutException) {
        System.out.println("Depth failed at: " + depth);
        throw timeOutException;
      }
    }
    return new int[]{bestMove, bestScore};
  }

  private int[] miniMaxWithAlphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximisingPlayer) throws TimeOutException {
    if(isTimeUp()){
      throw new TimeOutException();
    }

    if (depth == 0 || isGameTerminal(board)){
      return new int[] {-1, evaluateBoard(board)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    Counter counter = isMaximisingPlayer ? getCounter() : getCounter().getOther();

    for (Integer move : columnOrder) {

      if(!isColumnPlayable(board, move)) continue;

      Board newBoard;
      try{
        newBoard = new Board(board, move, counter);
      } catch (InvalidMoveException e) {
        continue;
      }

      int[] result = miniMaxWithAlphaBeta(newBoard, depth - 1, alpha, beta, !isMaximisingPlayer);
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

    return new int[]{bestMove, bestScore};
  }

  public boolean isGameTerminal(Board board) {
    int width = board.getConfig().getWidth();
    int height = board.getConfig().getHeight();
    boolean boardFull = true;

    for(int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Position position = new Position(x, y);
        if (board.hasCounterAtPosition(position)) {
          Counter counter = board.getCounterAtPosition(position);
          if(hasWon(board, position, counter)) {
            return true;
          }
        } else if(boardFull){
          boardFull = false;
        }
      }
    }

    return boardFull;
  }

  private boolean hasWon(Board board, Position position, Counter counter) {
    return checkDirection(board, position, counter, 1, 0) ||
           checkDirection(board, position, counter, 0, 1) ||
           checkDirection(board, position, counter, 1, 1) ||
           checkDirection(board, position, counter, 1, -1);
  }

  private boolean checkDirection(Board board, Position position, Counter counter, int dx, int dy) {
    Counter[][] counterPlacements = board.getCounterPlacements();
    int neededForWin = 4, x = position.getX(), y = position.getY();

    for(int i = 0; i < 4; i++) {
      int nx = x + i * dx, ny = y + i * dy;
      if (isWithinBoardArray(nx, ny)) {
        Counter boardCounter = counterPlacements[nx][ny];
        if (counter.equals(boardCounter)) {
          neededForWin--;
        } else break;
      }
    }

    return neededForWin == 0;
  }

  private int evaluateBoard(Board board) {
    int score = 0;
    Counter[][] counterPlacements = board.getCounterPlacements();

    for (int x = 0; x < WIDTH; x++) {
      for (int y = 0; y < HEIGHT; y++) {
        Counter counter = counterPlacements[x][y];
        if (counter != null) {
          if(counter == this.getCounter()) {
            score += evaluatePosition(counterPlacements, x, y, counter);
          }
          else {
            score -= evaluatePosition(counterPlacements, x, y, counter);
          }
        }
      }
    }
    return score;
  }

  private int evaluatePosition(Counter[][] counterPlacements, int x, int y, Counter counter) {
    int score = 0, returned;
    // Directional Checks
    for(int i = 0; i < 4; i++) {
      returned = scoreDirection(counterPlacements, x, y, counter, dx[i], dy[i]);
      if(returned == Integer.MAX_VALUE){
        return returned;
      }
      score += returned;
    }

    return score;
  }

  private int scoreDirection(Counter[][] counterPlacements, int x, int y, Counter counter, int dx, int dy) {
    int count = 0;
    int openSpaces = 0;

    for(int i = 0; i < 4; i++){
      int nx = x + i * dx, ny = y + i * dy;
      if(isWithinBoardArray(nx, ny)) {
        Counter boardCounter = counterPlacements[nx][ny];
        if(counter.equals(boardCounter)) {
          count++;
        }
        else if(boardCounter == null){
          openSpaces++;
        }
      }
    }

    if(count == 4) {
      return Integer.MAX_VALUE;
    }
    else if(count == 3 && openSpaces == 1) {
      return 100;
    }
    else if(count == 2 && openSpaces == 2){
      return 10;
    }
    return 0;
  }


  private boolean isColumnPlayable(Board board, int column) {
    Position position = new Position(column, board.getConfig().getHeight() - 1);
    return !board.hasCounterAtPosition(position);
  }

  private boolean isWinningMove(Board board, Counter counter, int column) {
    try{
      Board newBoard = new Board(board, column, counter);
      for (int x = 0; x < newBoard.getConfig().getWidth(); x++) {
        for (int y = 0; y < newBoard.getConfig().getHeight(); y++) {
          Position position = new Position(x, y);
          if (newBoard.hasCounterAtPosition(position)) {
            if (newBoard.getCounterAtPosition(position).equals(counter)) {
              if(hasWon(newBoard, position, counter)) {
                return true;
              }
            }
          }
        }
      }
    }
    catch(InvalidMoveException ignored) {
    }
    return false;
  }

  private int checkInstantWin(Board board, Counter counter) {
    for (int x = 0; x < WIDTH; x++) {
      if(isWinningMove(board, counter, x)) {
        return x;
      }
    }
    return -1;
  }

  private boolean isWithinBoardArray(int x, int y){
    return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
  }

  private boolean isTimeUp() {
    return System.nanoTime() - startTime > TIME_LIMIT - 500_000_000L;
  }

  private static class TimeOutException extends RuntimeException {
  }

  private static class WinningStateException extends RuntimeException {

  }

}