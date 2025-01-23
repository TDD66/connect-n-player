package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

public class IsHackingAi extends Player {
  private long startTime;
  private static final long TIME_LIMIT = 10_000_000_000L;
  private static final int MIN_DEPTH = 6;
  private static final int MAX_DEPTH = 50;

  private final int[] columnOrder;
  private static final int HEIGHT = 8;
  private static final int WIDTH = 10;

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
    Counter[][] counterPlacements = board.getCounterPlacements();

    for(int move : columnOrder) {
      try {
        Counter[][] newCounterPlacements = makeMoveArray(counterPlacements, getCounter(), move);
        int[] result = miniMaxWithAlphaBeta(newCounterPlacements, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

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

  private int[] miniMaxWithAlphaBeta(Counter[][] counterPlacements, int depth, int alpha, int beta, boolean isMaximisingPlayer) throws TimeOutException {
    if(isTimeUp()){
      throw new TimeOutException();
    }

    if (depth == 0 || isGameTerminal(counterPlacements)){
      return new int[] {-1, evaluateBoard(counterPlacements)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    Counter counter = isMaximisingPlayer ? getCounter() : getCounter().getOther();

    for (Integer move : columnOrder) {

      if(!isColumnPlayable(counterPlacements, move)) continue;

      Counter[][] newCounterPlacements;
      try{
        newCounterPlacements = makeMoveArray(counterPlacements, counter, move);
      } catch (InvalidMoveException e) {
        continue;
      }

      int[] result = miniMaxWithAlphaBeta(newCounterPlacements, depth - 1, alpha, beta, !isMaximisingPlayer);
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

  public boolean isGameTerminal(Counter[][] counterPlacements) {
    boolean boardFull = true;

    for(int x = 0; x < WIDTH; x++) {
      for (int y = 0; y < HEIGHT; y++) {
        Counter counter = counterPlacements[x][y];
        if (counter != null) {
          if(hasWon(counterPlacements, x, y, counter)) {
            return true;
          }
        } else if(boardFull){
          boardFull = false;
        }
      }
    }

    return boardFull;
  }

  private boolean hasWon(Counter[][] counterPlacements, int x, int y, Counter counter) {
    return checkDirection(counterPlacements, x, y, counter, 1, 0) ||
           checkDirection(counterPlacements, x, y, counter, 0, 1) ||
           checkDirection(counterPlacements, x, y, counter, 1, 1) ||
           checkDirection(counterPlacements, x, y, counter, 1, -1);
  }

  private boolean checkDirection(Counter[][] counterPlacements, int x, int y, Counter counter, int dx, int dy) {
    int neededForWin = 4;

    for(int i = 0; i < 4; i++){
      int nx = x + i * dx, ny = y + i * dy;
      if(isWithinBoardArray(nx, ny)) {
        Counter boardCounter = counterPlacements[nx][ny];
        if (counter.equals(boardCounter)) {
          neededForWin--;
        }
      } else break;
    }

    return neededForWin == 0;
  }

  private int evaluateBoard(Counter[][] counterPlacements) {
    int score = 0;

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
    int score = 0;

    score += scoreDirection(counterPlacements, x, y, counter, 1, 0);
    score += scoreDirection(counterPlacements, x, y, counter, 1, 1);
    score += scoreDirection(counterPlacements, x, y, counter, 1, -1);
    score += scoreDirection(counterPlacements, x, y, counter, 0, 1);

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
        else if(counterPlacements[nx][ny] == null){
          openSpaces++;
        }
      }
    }

    if(count == 4) {
      return Integer.MAX_VALUE;
    }
    else if(count == 3 && openSpaces == 1) {
      return 500;
    }
    else if(count == 2 && openSpaces == 2){
      return 50;
    }
    return 0;
  }


  private boolean isColumnPlayable(Counter[][] counterPlacements, int column) {
    for(int row = HEIGHT - 1; row >= 0; row--) {
      if(counterPlacements[column][row] == null) {
        return true;
      }
    }

    return false;
  }

  private boolean isWinningMove(Board board, Counter counter, int column) {
    Counter[][] counterPlacements = board.getCounterPlacements();
    try{
      Counter[][] newCounterPlacements = makeMoveArray(counterPlacements, counter, column);
      for (int x = 0; x < WIDTH; x++) {
        for (int y = 0; y < HEIGHT; y++) {
          Counter counterAtPosition = newCounterPlacements[x][y];
            if (counter.equals(counterAtPosition)) {
              if(hasWon(newCounterPlacements, x, y, counter)) {
                return true;
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
    for (int x = 0; x < board.getConfig().getWidth(); x++) {
      if(isWinningMove(board, counter, x)) {
        return x;
      }
    }
    return -1;
  }

  private Counter[][] makeMoveArray(Counter[][] board, Counter counter, int column) throws InvalidMoveException {
    if (column < 0 || column >= WIDTH) {
      throw new InvalidMoveException("Column out of bounds");
    }
    for(int row = 0; row < HEIGHT; row++) {
      if(board[column][row] == null) {
        board[column][row] = counter;
        return board;
      }
    }

    throw new InvalidMoveException("No space in column");
  }

  private boolean isWithinBoardArray(int x, int y){
    return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
  }

  private boolean isTimeUp() {
    return System.nanoTime() - startTime > TIME_LIMIT - 5_000_000L;
  }

  private static class TimeOutException extends RuntimeException {
  }

}