package tcpsnake;

/**
 * Common constants for the Client and Server classes.
 */
public class Common {
    public static final int MAX_PLAYERS = 4;
    public static final int MATRIX_SIZE = 16;

    // Color codes for players
    public static final String RESET_COLOR = "\u001B[0m";
    public static final String P1_COLOR = "\u001B[31m"; // Red
    public static final String P2_COLOR = "\u001B[34m"; // Blue
    public static final String P3_COLOR = "\u001B[32m"; // Green
    public static final String P4_COLOR = "\u001B[33m"; // Yellow

    // Matrix cell types
    public static final byte EMPTY = '.';
    public static final byte FRUIT = 'O';
    public static final byte SPECIAL_FRUIT = '*'; // Special golden apple
    public static final byte OBSTACLE = '#';

    // Players
    public static final byte[] PLAYER_HEADS = {'X', 'Y', 'Z', 'W'};
    public static final byte[] PLAYER_BODIES = {'x', 'y', 'z', 'w'};

    // Round status
    public static final byte END_FINAL = -3;
    public static final byte ROUND_END = -2;
    public static final byte ROUND_STARTED = -1;
    public static final byte NOT_STARTED = 0;
}