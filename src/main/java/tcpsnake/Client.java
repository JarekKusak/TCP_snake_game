package tcpsnake;

import java.io.*;
import java.net.*;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.util.Scanner;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private DataInputStream in;

    private byte[][] matrix = new byte[Common.MATRIX_SIZE][Common.MATRIX_SIZE];
    private byte roundStatus;

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

    private void receiveGameState() throws IOException {
        for (int y = 0; y < Common.MATRIX_SIZE; y++) {
            for (int x = 0; x < Common.MATRIX_SIZE; x++) {
                matrix[y][x] = in.readByte();
            }
        }
        roundStatus = in.readByte();
    }

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

    private void sendInput(String input) {
        out.println(input);
    }

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
            // Vytvoříme terminál (system(true) => běží nad systémovou konzolí)
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            // Přepneme do raw módu: klávesy chodí rovnou, bez Enteru
            terminal.enterRawMode();

            // Získáme Reader (znakový stream)
            java.io.Reader reader = terminal.reader();

            whileg (true) {
                // read() vrací -1 při EOF nebo int kód znaku
                int ch = reader.read();
                if (ch == -1) {
                    System.out.println("EOF? Končíme...");
                    break;
                }

                if (ch == 27) { // ESC
                    System.out.println("Ukončuji program");
                    break;
                }

                // Pro W/A/S/D to bude prosté písmeno
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