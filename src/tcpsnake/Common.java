package tcpsnake;

/**
 * Common constants for the Client and Server classes.
 */
public class Common
{
    public static final int MAX_PLAYERS = 4;

    public static final int MATRIX_SIZE = 32;
    public static final int WIN_MATRIX_SIZE = 960;

    public static final int WIN_CELL_SIZE = WIN_MATRIX_SIZE /  MATRIX_SIZE;

    /* number of matrix cell types */
    public static final int ENTITIES = 11;

    /* matrix cells */
    public static final byte P1 = 0;
    public static final byte P2 = 1;
    public static final byte P3 = 2;
    public static final byte P4 = 3;
    public static final byte P1_HEAD = 4;
    public static final byte P2_HEAD = 5;
    public static final byte P3_HEAD = 6;
    public static final byte P4_HEAD = 7;
    public static final byte FRUIT = 8;
    public static final byte FRUIT_SPECIAL = 9;
    public static final byte EMPTY = 10;

    /* score status */
    public static final int DEAD = -1;

    /* round status */
    public static final byte END_FINAL = -3;
    public static final byte ROUND_END = -2;
    public static final byte ROUND_STARTED = -1;
    public static final byte NOT_STARTED = 0;
}