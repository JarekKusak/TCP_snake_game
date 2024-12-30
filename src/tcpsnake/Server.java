package tcpsnake;

import java.io.*;
import java.net.*;

public class Server {

    private static final int MATRIX_SIZE = Common.MATRIX_SIZE;
    private static final int MAX_PLAYERS = Common.MAX_PLAYERS;
    private byte[][] matrix = new byte[MATRIX_SIZE][MATRIX_SIZE];
    //private Player[] players = new Player[MAX_PLAYERS];
    private String[] playerNames = new String[MAX_PLAYERS];
    private int[] scores = new int[MAX_PLAYERS];
    private int connectedPlayers = 0;

    private ServerSocket serverSocket;
    private Socket[] clientSockets = new Socket[MAX_PLAYERS];
    private DataOutputStream[] outputStreams = new DataOutputStream[MAX_PLAYERS];
    private BufferedReader[] inputStreams = new BufferedReader[MAX_PLAYERS];

    private int rounds;

    public Server(int playerCount, int port, int rounds) throws IOException {
        this.rounds = rounds;
        serverSocket = new ServerSocket(port);
        System.out.println("Server is running on port " + port + " and waiting for " + playerCount + " players...");

        // initialize matrix
        for (int i = 0; i < MATRIX_SIZE; i++)
            for (int j = 0; j < MATRIX_SIZE; j++)
                matrix[i][j] = Common.EMPTY;

        // connect players
        for (int i = 0; i < playerCount; i++) {
            Socket clientSocket = serverSocket.accept();
            clientSockets[i] = clientSocket;
            outputStreams[i] = new DataOutputStream(clientSocket.getOutputStream());
            inputStreams[i] = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            playerNames[i] = inputStreams[i].readLine();
            System.out.println("Player " + playerNames[i] + " connected.");
            //players[i] = new Player(i, new Position(2 + i, 2 + i), new Position(0, 1)); // Simple positions
            connectedPlayers++;
        }

        System.out.println("All players connected. Starting the game...");
    }

    public void startGame() {
        try {
            for (int round = 1; round <= rounds; round++) {
                System.out.println("Round " + round + " started.");
                playRound();
                System.out.println("Round " + round + " ended.");
                Thread.sleep(2000);
            }
            System.out.println("Game over!");
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playRound() throws IOException, InterruptedException {
        boolean roundActive = true;
        while (roundActive) {
            // update players' positions
            for (int i = 0; i < connectedPlayers; i++) {
                String input = inputStreams[i].readLine();
                if (input != null)
                    players[i].changeDirection(input.charAt(0));
                players[i].move(matrix);
            }

            // check for collisions or win conditions
            roundActive = checkRoundStatus();

            // send game state to players
            sendGameState();
            Thread.sleep(500);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java tcpsnake.Server <player_count> <port> <rounds>");
            return;
        }

        int playerCount = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        int rounds = Integer.parseInt(args[2]);

        try {
            Server server = new Server(playerCount, port, rounds);
            server.startGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}