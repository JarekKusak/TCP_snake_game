package tcpsnake;

/**
 * Common constants for game elements.
 */
public class Common {
    public static final int MAX_PLAYERS = 4;
    public static final int MATRIX_SIZE = 16;

    public static final byte P1_HEAD = 'X';
    public static final byte P2_HEAD = 'Y';
    public static final byte P1_BODY = 'x';
    public static final byte P2_BODY = 'y';
    public static final byte FRUIT = 'O';
    public static final byte EMPTY = '.';

    public static final byte END_FINAL = -3;
    public static final byte ROUND_END = -2;
    public static final byte ROUND_STARTED = -1;
    public static final byte NOT_STARTED = 0;
}