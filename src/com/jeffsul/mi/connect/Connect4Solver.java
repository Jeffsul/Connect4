package com.jeffsul.mi.connect;

import java.util.Scanner;

public class Connect4Solver {
  
  private static final int ROWS = 6;
  private static final int COLS = 7;
  
  private static final int INPUTS = ROWS * COLS * 2 + 1;
  private static final int HIDDEN = 40;
  
  private static final int OFFSET = ROWS * COLS;
  private static byte[] inputs = new byte[INPUTS];
  
  private static final int TRIALS = 100000;
  
  private static final double WEIGHT_DIVISOR = 4;
  
  private double[][] initWeights1;
  private double[] initWeights2;

  private double[][] w1;
  private double[] w2;
  
  private int winCount;
  
  private double rate = 0.1;

  public Connect4Solver() {
    w1 = new double[INPUTS][HIDDEN];
    w2 = new double[HIDDEN];
    initWeights1 = new double[INPUTS][HIDDEN];
    initWeights2 = new double[HIDDEN];
    // Initialize weights.
    for (int i = 0; i < INPUTS; i++) {
      for (int j = 0; j < HIDDEN; j++) {
        w1[i][j] = (Math.random() - 0.5) / WEIGHT_DIVISOR;
        initWeights1[i][j] = w1[i][j];
      }
    }
    for (int i = 0; i < HIDDEN; i++) {
      w2[i] = (Math.random() - 0.5) / WEIGHT_DIVISOR;
      initWeights2[i] = w2[i];
    }

    for (int i = 0; i < TRIALS; i++) {
      playGame();
    }
    System.out.println("Done Training\n");
    //for (int i = 0; i < 1000; i++) {
      //playVersusInitial();
    //}
    //System.out.println("WINS: " + winCount);
    
    Scanner sc = new Scanner(System.in);
    while (true) {
      String s = "";
      for (int i = 0; i < ROWS; i++) {
        s += sc.nextLine();
      }
      if (s.equals("q")) {
        break;
      }
      byte[][] board = parseBoard(s);
      boolean red = true;
      int redCount = 0;
      int blackCount = 0;
      for (int i = 0; i < board.length; i++) {
        for (int j = 0; j < board[i].length; j++) {
          if (board[i][j] == 1) {
            redCount++;
          } else if (board[i][j] == 2) {
            blackCount++;
          }
        }
      }
      if (redCount > blackCount) {
        red = false;
      }
      board = makeBestMove(board, red);
      print(board);
    }
    sc.close();
    
    l("\nW2:");
    for (int i = 0; i < w2.length; i++) {
      l(i + ": " + initWeights2[i] + " -> " + w2[i]);
    }
  }

  private static byte[][] parseBoard(String s) {
    byte[][] board = new byte[COLS][ROWS];
    for (int i = 0; i < s.length(); i++) {
      byte b = 0;
      if (s.charAt(i) == 'X') {
        b = 1;
      } else if (s.charAt(i) == 'O') {
        b = 2;
      }
      board[i % COLS][ROWS - 1 - i/COLS] = b;
    }
    return board;
  }

  private static void l(String msg) {
    //System.out.println(msg);
  }
  
  private byte[][] makeBestMove(byte[][] board, boolean red) {
    double max = Double.MIN_VALUE;
    int bestMove = -1;
    int rowVal = 0;
    outer: for (int col = 0; col < COLS; col++) {
      int j = 0;
      while (board[col][j] != 0) {
        j++;
        if (j == ROWS) {
          continue outer;
        }
      }
      board[col][j] = (byte)(red ? 1 : 2);
      double score;
      if (hasConnect4(board)) {
        score = red ? 1 : 0;
      } else {
        score = eval(getInputs(board));
      }
      board[col][j] = (byte) 0;
      if (!red) {
        score = 1 - score;
      }
      if (score > max) {
        max = score;
        bestMove = col;
        rowVal = j;
      }
    }
    board[bestMove][rowVal] = (byte)(red ? 1 : 2);
    return board;
  }

  private void playGame() {
    byte[][] board = new byte[COLS][ROWS];
    boolean red = true;
    boolean win = false;
    int moveCount = 0;
    while (true) {
      moveCount++;
      l("\nStarting move " + moveCount + " (" + (red ? "red" : "black") +")");
      // Evaluate current state.
      byte[] inputs = getInputs(board);
      double out = eval(inputs);
      if (!red) {
        out = 1 - out;
      }
      l("Predicted state (out): " + out);
      // Evaluate possible moves.
      double max = Double.MIN_VALUE;
      int bestMove = -1;
      int rowVal = 0;
      outer: for (int i = 0; i < COLS; i++) {
        int j = 0;
        while (board[i][j] != 0) {
          j++;
          if (j == ROWS) {
            continue outer;
          }
        }
        //int index = i * ROWS + j + (red ? 0 : OFFSET);
        //inputs[index] = (byte) 1;
        board[i][j] = (byte)(red ? 1 : 2);
        //print(board);
        double score;
        if (hasConnect4(board)) {
          win = true;
          score = red ? 1 : 0;
        } else {
          score = eval(getInputs(board));
        }
        board[i][j] = (byte) 0;
        if (!red) {
          score = 1 - score;
        }
        l("Score for Column " + i + ": " + score);
        if (score > max) {
          max = score;
          bestMove = i;
          rowVal = j;
        }
        //inputs[index] = (byte) 0;
      }
      if (!red) {
        max = 1 - max;
        out = 1 - out;
      }
      double outputError = (max - out) * out * (1 - out);
      l("Output error: " + outputError);
      double[] hiddenErrors = new double[HIDDEN];
      for (int i = 0; i < HIDDEN; i++) {
        double in = 0;
        for (int j = 0; j < inputs.length; j++) {
          in += inputs[j] * w1[j][i];
        }
        hiddenErrors[i] = outputError * w2[i];
        double w2delta = rate * s(in) * outputError;
        l("W2_delta (" + i + "): " + w2delta);
        w2[i] += w2delta;
      }
      for (int i = 0; i < INPUTS; i++) {
        for (int j = 0; j < HIDDEN; j++) {
          w1[i][j] += rate * hiddenErrors[j] * inputs[i];
        }
      }
      if (bestMove == -1) {
        break;
      }
      l("Best move: " + bestMove + " (" + max + ")");
      board[bestMove][rowVal] = (byte)(red ? 1 : 2);
      red = !red;
      if (win) {
        break;
      }
      //print(board);
    }
    //print(board);
  }
  
  private void playVersusInitial() {
    byte[][] board = new byte[COLS][ROWS];
    boolean red = true;
    while (true) {
      boolean win = false;
      // Evaluate possible moves.
      double max = Double.MIN_VALUE;
      int bestMove = -1;
      int rowVal = 0;
      outer: for (int i = 0; i < COLS; i++) {
        int j = 0;
        while (board[i][j] != 0) {
          j++;
          if (j == ROWS) {
            continue outer;
          }
        }
        int index = i * ROWS + j + (red ? 0 : OFFSET);
        board[i][j] = (byte)(red ? 1 : 2);
        double score;
        if (hasConnect4(board)) {
          win = true;
          score = red ? 1 : 0;
        } else {
          if (red) {
            score = eval(getInputs(board));
          } else {
            score = evalInit(getInputs(board));
          }
        }
        board[i][j] = (byte) 0;
        if (!red) {
          score = 1 - score;
        }
        if (score > max) {
          max = score;
          bestMove = i;
          rowVal = j;
        }
      }
      board[bestMove][rowVal] = (byte)(red ? 1 : 2);
      if (win) {
        if (red) {
          winCount++;
        }
        break;
      }
      red = !red;
    }
    //print(board);
  }
  
  private static void print(byte[][] table) {
    String[] rows = new String[ROWS];
    for (int i = 0; i < rows.length; i++) {
      rows[i] = "";
    }
    for (int i = 0; i < table.length; i++) {
      for (int j = 0; j < table[i].length; j++) {
        if (table[i][j] == 0) {
          rows[j] += "-";
        } else if (table[i][j] == 1) {
          rows[j] += "X";
        } else {
          rows[j] += "O";
        }
      }
    }
    System.out.println();
    for (int i = rows.length - 1; i >= 0; i--) {
      System.out.println(rows[i]);
    }
    System.out.println();
  }
  
  private static double s(double t) {
    return 1 / (1 + Math.exp(-t));
  }
  
  private double eval(byte[] inputs) {
    double score = 0;
    for (int i = 0; i < HIDDEN; i++) {
      double net = 0;
      for (int j = 0; j < inputs.length; j++) {
        net += inputs[j] * w1[j][i];
      }
      score += s(net) * w2[i];
    }
    return s(score);
  }
  
  private double evalInit(byte[] inputs) {
    double score = 0;
    for (int i = 0; i < HIDDEN; i++) {
      double net = 0;
      for (int j = 0; j < inputs.length; j++) {
        net += inputs[j] * initWeights1[j][i];
      }
      score += s(net * initWeights2[i]);
    }
    return s(score);
  }
  
  private static byte[] getInputs(byte[][] board) {
    for (int i = 0; i < board.length; i++) {
      for (int j = 0; j < board[i].length; j++) {
        inputs[i * ROWS + j] = (byte) (board[i][j] == 1 ? 1 : 0);
        inputs[i * ROWS + j + OFFSET] = (byte) (board[i][j] == 2 ? 1 : 0);
      }
    }
    // Input bias:
    inputs[INPUTS - 1] = 1;
    return inputs;
  }
  
  private boolean hasConnect4(byte[][] board) {
    for (int i = 0; i < COLS - 3; i++) {
      rows: for (int j = 0; j < ROWS; j++) {
        if (board[i][j] == 0) {
          continue;
        }
        for (int k = 1; k < 4; k++) {
          if (board[i + k][j] != board[i][j]) {
            continue rows;
          }
        }
        //System.out.println("ROW:" + i + "," + j);
        return true;
      }
    }
    for (int i = 0; i < COLS; i++) {
      rows: for (int j = 0; j < ROWS - 3; j++) {
        if (board[i][j] == 0) {
          continue;
        }
        for (int k = 1; k < 4; k++) {
          if (board[i][j + k] != board[i][j]) {
            continue rows;
          }
        }
        //System.out.println("COL:" + i + "," + j);
        return true;
      }
    }
    for (int i = 0; i < COLS - 3; i++) {
      rows: for (int j = 0; j < ROWS - 3; j++) {
        if (board[i][j] == 0) {
          continue;
        }
        for (int k = 1; k < 4; k++) {
          if (board[i + k][j + k] != board[i][j]) {
            continue rows;
          }
        }
        //System.out.println("DIAG:" + i + "," + j);
        return true;
      }
    }
    for (int i = COLS - 1; i > 2; i--) {
      rows: for (int j = 0; j < ROWS - 3; j++) {
        if (board[i][j] == 0) {
          continue;
        }
        for (int k = 1; k < 4; k++) {
          if (board[i - k][j + k] != board[i][j]) {
            continue rows;
          }
        }
        //System.out.println("DIAG2:" + i + "," + j);
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) {
    new Connect4Solver();
  }
}
