package tcpsnake;

/**
 * Represents a position with x and y coordinates.
 */
public class Position {
    int x, y;

    /**
     * Constructs a Position with the given x and y coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Updates this position by adding the given direction's x and y values to this position's coordinates.
     *
     * @param direction the direction to update this position with
     */
    void update(Position direction) {
        this.x += direction.x;
        this.y += direction.y;
    }
}
