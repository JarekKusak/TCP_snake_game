package tcpsnake;

import java.io.*;
import java.net.*;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.util.Scanner;

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
    }

    /**
     * Receives the current game state from the server and updates the local matrix.
     *
     * @throws IOException if there is an I/O error while reading from the server
     */
    private void receiveGameState() throws IOException {
        for (int y = 0; y < Common.MATRIX_SIZE; y++) {
            for (int x = 0; x < Common.MATRIX_SIZE; x++) {
                matrix[y][x] = in.readByte();
            }
        }
        roundStatus = in.readByte();
    }

    /**
     * Renders the current state of the game matrix to the console.
     */
    private void renderGame() {
        for (int y = 0; y < Common.MATRIX_SIZE; y++) {
            for (int x = 0; x < Common.MATRIX_SIZE; x++) {
                char displayChar = switch (matrix[y][x]) {
                    case Common.EMPTY -> '.';
                    case Common.FRUIT -> 'O';
                    case Common.P1_HEAD -> 'X';
                    case Common.P2_HEAD -> 'Y';
                    case Common.P1_BODY -> 'x';
                    case Common.P2_BODY -> 'y';
                    default -> '?';
                };
                System.out.print(" " + displayChar + " ");
            }
            System.out.println();
        }
        System.out.println("Round Status: " + roundStatus);
    }

    /**
     * Sends a command (e.g., a direction) to the server.
     *
     * @param input the command to send
     */
    private void sendInput(String input) {
        out.println(input);
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

            // get reader (character stream)
            java.io.Reader reader = terminal.reader();

            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    System.out.println("EOF? Končíme...");
                    break;
                }

                if (ch == 27) { // ESC
                    System.out.println("Ukončuji program");
                    break;
                }

                char c = (char) ch;
                char direction = Character.toUpperCase(c);
                if (direction == 'W' || direction == 'A' || direction == 'S' || direction == 'D') {
                    sendInput(String.valueOf(direction));
                }
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