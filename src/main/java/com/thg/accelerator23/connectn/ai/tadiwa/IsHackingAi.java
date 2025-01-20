package com.thg.accelerator23.connectn.ai.tadiwa;

import com.thehutgroup.accelerator.connectn.player.Board;
import com.thehutgroup.accelerator.connectn.player.Counter;
import com.thehutgroup.accelerator.connectn.player.Player;

import java.util.HashMap;
import java.util.Map;


public class IsHackingAi extends Player {
  private long startTime;
  private static final int timeLimit = 10000;
  private Map<String, Integer> transpositionTable;

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
    return 4;
  }
}
