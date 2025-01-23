package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

import java.util.*;


public class IsHackingAi extends Player {
  private long startTime;
  private static final long TIME_LIMIT = 10_000_000_000_000_000L;
  private static final int MIN_DEPTH = 2;
  private static final int MAX_DEPTH = 10;

  private final int[] columnOrder;

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
    try {
      return getBestMove(board);
    } catch (InvalidMoveException e) {
      return 4;
    }
  }

  private int getBestMove(Board board) throws InvalidMoveException {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;

    for (int depth = MIN_DEPTH; depth <= MAX_DEPTH; depth += 2) {
      int[] result = miniMaxWithAlphaBeta(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE,  true);
      if (result[1] > bestScore) {
        bestMove = result[0];
        bestScore = result[1];
      }

      if (System.nanoTime() - this.startTime > TIME_LIMIT) {
        break;
      }
    }
    float timeTaken = (float) (System.nanoTime() - this.startTime) / (float) 1_000_000_000L;
    System.out.printf("%.2f seconds%n", timeTaken);
    return bestMove;
  }

  private int[] miniMaxWithAlphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximisingPlayer) throws InvalidMoveException {
    if (depth == 0 || isGameTerminal(board)){
      return new int[] {-1, evaluateBoard(board)};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    Counter counter = isMaximisingPlayer ? getCounter() : getCounter().getOther();

    for (Integer move : columnOrder) {

      if(!isColumnPlayable(board, move)) continue;

      Board newBoard = new Board(board, move, counter);

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

  private int centreColumnBias(Board board) {
    int score = 0, height = 8;
    int[] centreColumns = {4, 5};

    for (int col : centreColumns) {
      for (int row = 0; row < height; row++) {
        Position position = new Position(col, row);
        if (board.hasCounterAtPosition(position)) {
          if (board.getCounterAtPosition(position).equals(this.getCounter())) {
            score += 1;
          } else {
            score -= 1;
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
          if(checkDirection(board, position, counter, 1, 0) ||
             checkDirection(board, position, counter, 0, 1) ||
             checkDirection(board, position, counter, 1, 1) ||
             checkDirection(board, position, counter, 1, -1)
          ) {
            return true;
          }
        } else if(boardFull){
          boardFull = false;
        }
      }
    }

    return boardFull;
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

    return score;
  }

  private int scoreDirection(Board board, Position position, Counter counter, int dx, int dy) {
    int count = 0;
    int openSpaces = 0;
    int x = position.getX(), y = position.getY();

    for(int i = 0; i < 4; i++){
      Position nextPosition = new Position(x + i * dx, y + i * dy);
      Counter boardCounter = board.getCounterAtPosition(nextPosition);
      if(board.isWithinBoard(nextPosition)) {
        if(counter.equals(boardCounter)) {
          count++;
        }
        else if(!board.hasCounterAtPosition(nextPosition)){
          openSpaces++;
        }
      }
    }

    if(count == 4) {
      return 1000;
    }
    else if(count == 3 && openSpaces == 1) {
      return 100;
    }
    else if(count == 2 && openSpaces == 2){
      return 50;
    }
    else if(count == 3) {
      return 25;
    }
    else if (count == 2){
      return 5;
    }

    return 0;
  }


  private boolean isColumnPlayable(Board board, int column) {
    Position position = new Position(column, board.getConfig().getHeight() - 1);
    return !board.hasCounterAtPosition(position);
  }

}