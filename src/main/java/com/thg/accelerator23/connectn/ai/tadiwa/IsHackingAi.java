package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

import java.util.*;
import java.util.concurrent.TimeoutException;


public class IsHackingAi extends Player {
  private long startTime;
  private static final long TIME_LIMIT = 10_000_000_000L;
  private static final int MIN_DEPTH = 6;
  private static final int MAX_DEPTH = 50;

  private static final int CENTRE_ADJUSTMENT = 1;

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

  private int centreColumnBias(Board board, Counter counter) {
    Counter[][] counterPlacements = board.getCounterPlacements();
    int score = 0, height = 8;
    int[] centreColumns = {4, 5};

    for (int col : centreColumns) {
      for (int row = 0; row < height; row++) {
        Counter counterAtPosition = counterPlacements[col][row];
        if (counterAtPosition != null) {
          if (counterAtPosition.equals(counter)) {
            score += CENTRE_ADJUSTMENT;
          }
        }
      }
    }
    return score;
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
    int neededForWin = 4, x = position.getX(), y = position.getY();

    for(int i = 0; i < 4; i++){
      Position nextPosition = new Position(x + i * dx, y + i * dy);
      Counter boardCounter = board.getCounterAtPosition(nextPosition);
      if(board.isWithinBoard(nextPosition) &&
         counter.equals(boardCounter)
      ) {
        neededForWin--;
      }
      else break;
    }

    return neededForWin == 0;
  }

  private int evaluateBoard(Board board) {
    int score = 0;
    int height = board.getConfig().getHeight(), width = board.getConfig().getWidth();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Position position = new Position(x, y);
        if (board.hasCounterAtPosition(position)) {
          Counter counter = board.getCounterAtPosition(position);
          if(counter == this.getCounter()) {
            score += evaluatePosition(board, position, counter);
          }
          else {
            score -= evaluatePosition(board, position, counter);
          }
        }
      }
    }
    return score;
  }

  private int evaluatePosition(Board board, Position position, Counter counter) {
    int score = 0;

    score += scoreDirection(board, position, counter, 1, 0);
    score += scoreDirection(board, position, counter, 1, 1);
    score += scoreDirection(board, position, counter, 1, -1);
    score += scoreDirection(board, position, counter, 0, 1);
    score += centreColumnBias(board, counter);

    return score;
  }

  private int scoreDirection(Board board, Position position, Counter counter, int dx, int dy) {
    Counter[][] counterPlacements = board.getCounterPlacements();
    int count = 0;
    int openSpaces = 0;
    int x = position.getX(), y = position.getY();

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