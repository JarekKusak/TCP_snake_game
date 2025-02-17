package tcpsnake;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerTest {
    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(0, new Position(5, 5), new Position(1, 0));
    }

    @Test
    void testPlayerInitialization() {
        assertNotNull(player);
        assertEquals(0, player.getId());
    }

    @Test
    void testSetBlind() throws InterruptedException {
        player.setBlind(true);
        assertTrue(player.isBlind());

        Thread.sleep(3100);
        assertFalse(player.isBlind());
    }

    @Test
    void testInitialScore() {
        assertEquals(0, player.getScore());
    }

    @Test
    void testPlayerIsInitiallyAlive() {
        assertTrue(player.isAlive());
    }

    @Test
    void testPlayerHeadPosition() {
        assertEquals(5, player.getHeadPosition().x);
        assertEquals(5, player.getHeadPosition().y);
    }
}