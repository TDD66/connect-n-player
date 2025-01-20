package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    return getBestMove(board);
  }

  private int getBestMove(Board board) {
    int bestMove = -1;
    int bestScore = Integer.MIN_VALUE;

    for (int depth = 0; depth < MAX_DEPTH; depth++) {
      int[] result = miniMaxWithAlphaBeta(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true);

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

  private int[] miniMaxWithAlphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximisingPlayer) throws InvalidMoveException {

    if(depth == 0){
      return new int[]{alpha, beta};
    }

    int bestMove = -1;
    int bestScore = isMaximisingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    List<Integer> moves = getPossibleMoves(board);

    for(Integer move : moves){
      Board newBoard = new Board(board, move, getCounter());
      int[] result = miniMaxWithAlphaBeta(newBoard, depth - 1, alpha, beta, !isMaximisingPlayer);
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
}
