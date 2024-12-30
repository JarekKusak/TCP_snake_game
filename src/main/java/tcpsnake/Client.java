package main.java.tcpsnake;

import java.io.*;
import java.net.*;
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
                System.out.print(matrix[y][x] == Common.EMPTY ? "." : "X");
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

        Scanner scanner = new Scanner(System.in);
        System.out.println("Use W, A, S, D to move. Press Q to quit.");
        while (true) {
            String input = scanner.nextLine().toUpperCase();
            if (input.equals("Q")) {
                System.exit(0);
            } else if (input.matches("[WASD]")) {
                sendInput(input);
            }
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