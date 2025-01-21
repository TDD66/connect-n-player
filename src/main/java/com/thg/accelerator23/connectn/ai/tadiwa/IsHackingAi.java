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

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    Set<Integer> moves = legalColumns(spaces);

    for(Integer move : moves){

      Board newBoard = new Board(board, move, getCounter());
      spaces.put(move, spaces.get(move) - 1);

      if(spaces.get(move) == 0){
        spaces.remove(move);
      }

      int[] result = miniMaxWithAlphaBeta(newBoard, depth - 1, alpha, beta, spaces,!isMaximisingPlayer);
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

  private List<Integer> getPossibleMoves(Board board){
    return List.of(0,1,2,3,4,5,6,7,8,9);
  }

  private int evaluateBoard(Board board) {
    // Evaluate board with heuristics
    return 0;
  }

  private List<Integer> sortMovesByHeuristic(Board board) {
    return new ArrayList<>();
  }

  private Map<Integer, Integer> populateFreeColumns(Board board) {
    Map<Integer, Integer> freeColumns = new HashMap<>();
    for(int x = 0; x < 10; x++){
      for(int y = 0; y < 8; y++){
        Position position = new Position(x, y);
        if(!board.hasCounterAtPosition(position)){
          freeColumns.put(x, y);
          break;
        }
      }
    }
    return freeColumns;
  }

  private Set<Integer> legalColumns(Map<Integer, Integer> spaces) {
      return spaces.keySet();
  }
}
