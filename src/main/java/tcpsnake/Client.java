package main.java.tcpsnake;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private PrintWriter out;
    private DataInputStream in;

    private byte[][] matrix = new byte[Common.MATRIX_SIZE][Common.MATRIX_SIZE];
    private int id;
    private String nick;

    private byte round_status;
    private String[] player_names = new String[Common.MAX_PLAYERS];
    private int[] scores = new int[Common.MAX_PLAYERS];

    private int CURRENT_PLAYERS;

    public Client(String nick, String hostname, int server_port) {
        this.nick = nick;

        try {
            this.socket = new Socket(hostname, server_port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Unable to connect to the server. Exiting...");
            System.exit(1);
        }

        System.out.println("Connected to the server.");
        setup_game_connection();
    }

    private void setup_game_connection() {
        try {
            out.println(nick);
            CURRENT_PLAYERS = in.readInt();
            id = in.readInt();
            for (int i = 0; i < CURRENT_PLAYERS; i++) {
                player_names[i] = in.readUTF();
            }
        } catch (IOException e) {
            System.err.println("Server disconnected. Exiting...");
            System.exit(1);
        }
    }

    private void receive_game_status() {
        try {
            for (int i = 0; i < Common.MATRIX_SIZE; i++) {
                in.readFully(matrix[i]);
            }
            for (int i = 0; i < CURRENT_PLAYERS; i++) {
                scores[i] = in.readInt();
            }
            round_status = in.readByte();
        } catch (IOException e) {
            System.err.println("Server disconnected. Exiting...");
            System.exit(1);
        }
    }

    private void render_console() {
        for (int i = 0; i < Common.MATRIX_SIZE; i++) {
            for (int j = 0; j < Common.MATRIX_SIZE; j++) {
                switch (matrix[i][j]) {
                    case Common.EMPTY:
                        System.out.print(" . ");
                        break;
                    case Common.FRUIT:
                        System.out.print(" * ");
                        break;
                    case Common.FRUIT_SPECIAL:
                        System.out.print(" @ ");
                        break;
                    default:
                        // Players and their heads
                        if (matrix[i][j] < Common.P1_HEAD) {
                            System.out.print(" " + (matrix[i][j] + 1) + " ");
                        } else {
                            System.out.print(" H ");
                        }
                        break;
                }
            }
            System.out.println();
        }
        System.out.println("Scores:");
        for (int i = 0; i < CURRENT_PLAYERS; i++) {
            System.out.println(player_names[i] + ": " + scores[i]);
        }
    }

    private void handle_input() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().toUpperCase();
            if (input.equals("Q")) {
                close_and_exit();
            } else if (input.matches("[WASD]")) {
                out.println(input);
            }
        }
    }

    private void close_and_exit() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error while closing the client.");
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java tcpsnake.Client <nickname> <hostname> <port>");
            return;
        }

        String nick = args[0];
        String hostname = args[1];
        int port = Integer.parseInt(args[2]);

        Client client = new Client(nick, hostname, port);

        Thread inputThread = new Thread(client::handle_input);
        inputThread.start();

        while (true) {
            client.receive_game_status();
            client.render_console();

            if (client.round_status == Common.END_FINAL) {
                System.out.println("Game over! Thanks for playing!");
                break;
            }
        }

        client.close_and_exit();
    }
}
