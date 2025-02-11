package tcpsnake;

import java.io.*;
import java.net.*;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Handles the client-side of the TCP Snake game.
 * Connects to the server, sends user input, and receives game state updates to render the game.
 */
public class Client {
    private Socket socket;
    private PrintWriter out;
    private DataInputStream in;

    private byte[][] matrix = new byte[Common.MATRIX_SIZE][Common.MATRIX_SIZE];
    private byte roundStatus;
    private boolean invalidMove = false; // Flag to track invalid inputs

    private int playerId;
    private int connectedPlayers;
    private byte currentRound;
    private int[] scores;
    private String[] playerNames;
    private boolean[] playerBlindness;
    private Position[] playerPositions;

    /**
     * Constructs the client, establishing a connection to the server and sending the nickname.
     *
     * @param nickname the player's nickname
     * @param hostname the hostname (or IP) of the server
     * @param port the port on which the server is running
     */
    public Client(String nickname, String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new DataInputStream(socket.getInputStream());
            out.println(nickname);
        } catch (IOException e) {
            System.err.println("Unable to connect to server.");
            System.exit(1);
        }
        System.out.println("Connected to server.");

        scores = new int[Common.MAX_PLAYERS];
        playerNames = new String[Common.MAX_PLAYERS];
        playerBlindness = new boolean[Common.MAX_PLAYERS];
        playerPositions = new Position[Common.MAX_PLAYERS];
        for (int i = 0; i < Common.MAX_PLAYERS; i++) {
            playerPositions[i] = new Position(0, 0); // Defaultně nastavíme všechny hráče na (0,0)
        }
    }

    /**
     * Receives the current game state from the server, including player positions and blindness states.
     *
     * @throws IOException if there is an I/O error while reading from the server
     */
    private void receiveGameState() throws IOException {
        // first, get the player's own ID
        playerId = in.readInt();

        for (int y = 0; y < Common.MATRIX_SIZE; y++) {
            for (int x = 0; x < Common.MATRIX_SIZE; x++) {
                matrix[y][x] = in.readByte();
            }
        }
        connectedPlayers = in.readInt();
        roundStatus = in.readByte();
        currentRound = in.readByte();

        for (int i = 0; i < connectedPlayers; i++) {
            playerNames[i] = in.readUTF();
        }

        for (int i = 0; i < connectedPlayers; i++) {
            scores[i] = in.readInt();
        }

        // receive player positions
        for (int i = 0; i < connectedPlayers; i++) {
            int x = in.readInt();
            int y = in.readInt();
            playerPositions[i] = new Position(x, y);
        }

        // receive player blindness status
        for (int i = 0; i < connectedPlayers; i++) {
            playerBlindness[i] = in.readBoolean();
        }
    }

    /**
     * Renders the scoreboard next to the game matrix.
     */
    private void renderScoreboard() {
        // top border
        System.out.print("╔");
        for (int i = 0; i < connectedPlayers; i++) {
            System.out.print("══════════");
            if (i < connectedPlayers - 1) System.out.print("╦");
        }
        System.out.println("╗");

        // player names
        System.out.print("║");
        for (int i = 0; i < connectedPlayers; i++) {
            String name = playerNames[i];
            System.out.printf(" %-8s ║", name);
        }
        System.out.println();

        // middle border
        System.out.print("╠");
        for (int i = 0; i < connectedPlayers; i++) {
            System.out.print("══════════");
            if (i < connectedPlayers - 1) System.out.print("╬");
        }
        System.out.println("╣");

        // player scores
        System.out.print("║");
        for (int i = 0; i < connectedPlayers; i++) {
            System.out.printf(" %8d ║", scores[i]);
        }
        System.out.println();

        // bottom border
        System.out.print("╚");
        for (int i = 0; i < connectedPlayers; i++) {
            System.out.print("══════════");
            if (i < connectedPlayers - 1) System.out.print("╩");
        }
        System.out.println("╝");
    }

    /**
     * Renders the current state of the game matrix to the console, including borders and round information.
     */
    private void renderGame() {
        // clear the console
        System.out.print("\033[H\033[2J");
        System.out.flush();

        // display error message if last move was invalid
        if (invalidMove) {
            System.out.println("Invalid move. Use W, A, S, or D.");
        }

        // display current round number in the format "Round X/Y"
        System.out.println("\nRound " + currentRound);

        // print top border
        System.out.print("╔");
        for (int i = 0; i < Common.MATRIX_SIZE * 2; i++) {
            System.out.print("═");
        }
        System.out.println("╗");

        // get player's position
        Position playerHead = playerPositions[playerId];

        // print game matrix with left and right borders
        for (int y = 0; y < Common.MATRIX_SIZE; y++) {
            System.out.print("║"); // left border
            for (int x = 0; x < Common.MATRIX_SIZE; x++) {
                String color = Common.RESET_COLOR;
                char cell = (char) matrix[y][x];

                // if the player is blinded, restrict their vision
                if (playerBlindness[playerId] && !isNearPlayer(x, y, playerHead)) {
                    System.out.print("? ");
                    continue;
                }

                // assign colors to different players
                switch (cell) {
                    case 'X', 'x' -> color = Common.P1_COLOR; // Player 1 (red)
                    case 'Y', 'y' -> color = Common.P2_COLOR; // Player 2 (blue)
                    case 'Z', 'z' -> color = Common.P3_COLOR; // Player 3 (green)
                    case 'W', 'w' -> color = Common.P4_COLOR; // Player 4 (yellow)
                    case 'O' -> color = Common.FRUIT_COLOR; // yellow for normal fruit
                    case '*' -> color = Common.SPECIAL_FRUIT_COLOR; // purple for special fruit
                    case '&' -> color = Common.POWERUP_COLOR; // cyan for blindness power-up
                }

                System.out.print(color + cell + " " + Common.RESET_COLOR);
            }
            System.out.println("║"); // right border
        }

        // print bottom border
        System.out.print("╚");
        for (int i = 0; i < Common.MATRIX_SIZE * 2; i++) {
            System.out.print("═");
        }
        System.out.println("╝");

        renderScoreboard();
    }

    /**
     * Determines if a given coordinate is within the player's visible area when blinded.
     *
     * @param x The x coordinate of the cell
     * @param y The y coordinate of the cell
     * @param playerHead The position of the player's head
     * @return true if the cell is within the visible area, false otherwise
     */
    private boolean isNearPlayer(int x, int y, Position playerHead) {
        int px = playerHead.x;
        int py = playerHead.y;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (px + dx == x && py + dy == y) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sends a command (e.g., a direction) to the server.
     *
     * @param input the command to send
     */
    private void sendInput(char input) {
        char direction = Character.toUpperCase(input);

        if (direction == 'W' || direction == 'A' || direction == 'S' || direction == 'D') {
            out.println(direction);
            invalidMove = false; // valid move, reset error flag
        } else {
            invalidMove = true; // invalid move detected
        }
    }

    /**
     * Starts the main client loop. Receives game states in a separate thread while reading user input in raw mode.
     */
    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    receiveGameState();
                    renderGame();
                    if (roundStatus == Common.END_FINAL) {
                        System.out.println("Game over.");
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Disconnected from server.");
            }
        }).start();

        System.out.println("Use W, A, S, D to move. Press ESC to quit.");

        try {
            // create terminal (system(true) => runs on system console)
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // switch to raw mode keys go directly without enter
            terminal.enterRawMode();
            terminal.echo(false);

            // get reader (character stream)
            java.io.Reader reader = terminal.reader();

            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    System.out.println("EOF? Exiting...");
                    break;
                }

                if (ch == 27) { // ESC key
                    if (reader.ready()) { // if the next char is available, could be escape sequence
                        int next1 = reader.read();
                        if (next1 == '[') { // escape sequence (např. arrows)
                            int next2 = reader.read();
                            switch (next2) {
                                case 'A': ch = 'W'; break; // up arrow → W
                                case 'B': ch = 'S'; break; // down arrow → S
                                case 'C': ch = 'D'; break; // right arrow → D
                                case 'D': ch = 'A'; break; // left arrow → A
                                default: continue; // not an arrow -> ignore
                            }
                        }
                    } else { // ESC -> end of the game
                        System.out.println("Exiting game.");
                        try {
                            out.println("EXIT");
                            socket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing connection.");
                        }
                        System.exit(0);
                    }
                }

                sendInput((char) ch);
            }

            terminal.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main method to start the client.
     * Usage: java tcpsnake.Client &lt;nickname&gt; &lt;hostname&gt; &lt;port&gt;
     *
     * @param args command-line arguments for the nickname, hostname, and port
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java tcpsnake.Client <nickname> <hostname> <port>");
            return;
        }

        String nickname = args[0];
        String hostname = args[1];
        int port = Integer.parseInt(args[2]);

        Client client = new Client(nickname, hostname, port);
        client.start();
    }
}