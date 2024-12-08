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
