package main.java.tcpsnake;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private static final int MATRIX_SIZE = Common.MATRIX_SIZE;
    private static final int MAX_PLAYERS = Common.MAX_PLAYERS;

    private byte[][] matrix = new byte[MATRIX_SIZE][MATRIX_SIZE];
    private Player[] players = new Player[MAX_PLAYERS];
    private String[] playerNames = new String[MAX_PLAYERS];
    private int[] scores = new int[MAX_PLAYERS];
    private int connectedPlayers = 0;

    private ServerSocket serverSocket;
    private Socket[] clientSockets = new Socket[MAX_PLAYERS];
    private DataOutputStream[] outputStreams = new DataOutputStream[MAX_PLAYERS];
    private BufferedReader[] inputStreams = new BufferedReader[MAX_PLAYERS];

    private Queue<String>[] playerCommands = new ConcurrentLinkedQueue[MAX_PLAYERS];
    private byte roundStatus = Common.NOT_STARTED;

    private int rounds;

    public Server(int playerCount, int port, int rounds) throws IOException {
        this.rounds = rounds;
        this.serverSocket = new ServerSocket(port);

        // Initialize game matrix
        resetGameState();

        // Wait for players to connect
        System.out.println("Server is running on port " + port + " and waiting for " + playerCount + " players...");
        for (int i = 0; i < playerCount; i++) {
            Socket clientSocket = serverSocket.accept();
            clientSockets[i] = clientSocket;

            outputStreams[i] = new DataOutputStream(clientSocket.getOutputStream());
            inputStreams[i] = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String playerName = inputStreams[i].readLine();
            playerNames[i] = playerName;
            System.out.println("Player " + playerName + " connected.");

            players[i] = new Player(i, getPlayerStartingPosition(i), getPlayerStartingDirection(i));
            playerCommands[i] = new ConcurrentLinkedQueue<>();
            connectedPlayers++;

            // Start a thread to handle player input
            new ClientHandler(this, i, inputStreams[i]).start();
        }
        System.out.println("All players connected. Starting the game...");
    }

    public void startGame() {
        try {
            for (int round = 1; round <= rounds; round++) {
                roundStatus = Common.ROUND_STARTED;
                System.out.println("Round " + round + " started.");
                resetGameState();
                playRound();
                roundStatus = Common.ROUND_END;
                System.out.println("Round " + round + " ended.");
                Thread.sleep(2000);
            }
            roundStatus = Common.END_FINAL;
            System.out.println("Game over!");
            broadcastGameState();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private void playRound() throws IOException, InterruptedException {
        while (true) {
            // Process player commands
            for (int i = 0; i < connectedPlayers; i++) {
                String command = playerCommands[i].poll();
                if (command != null) {
                    players[i].changeDirection(command.charAt(0));
                }
                players[i].move(matrix);
            }

            // Check round status
            if (isRoundOver()) {
                break;
            }

            // Broadcast game state
            broadcastGameState();

            // Slow down game loop
            Thread.sleep(500);
        }
    }

    private boolean isRoundOver() {
        int alivePlayers = 0;
        for (Player player : players) {
            if (player != null && player.isAlive()) {
                alivePlayers++;
            }
        }
        return alivePlayers < 2;
    }

    private void broadcastGameState() throws IOException {
        for (int i = 0; i < connectedPlayers; i++) {
            for (int y = 0; y < MATRIX_SIZE; y++) {
                for (int x = 0; x < MATRIX_SIZE; x++) {
                    outputStreams[i].writeByte(matrix[y][x]);
                }
            }
            outputStreams[i].writeByte(roundStatus);
            outputStreams[i].flush();
        }
    }

    private void resetGameState() {
        for (int y = 0; y < MATRIX_SIZE; y++) {
            for (int x = 0; x < MATRIX_SIZE; x++) {
                matrix[y][x] = Common.EMPTY;
            }
        }
        Arrays.fill(scores, 0);
    }

    private Position getPlayerStartingPosition(int playerId) {
        return switch (playerId) {
            case 0 -> new Position(1, 1);
            case 1 -> new Position(MATRIX_SIZE - 2, MATRIX_SIZE - 2);
            case 2 -> new Position(1, MATRIX_SIZE - 2);
            case 3 -> new Position(MATRIX_SIZE - 2, 1);
            default -> new Position(0, 0);
        };
    }

    private Position getPlayerStartingDirection(int playerId) {
        return switch (playerId) {
            case 0 -> new Position(0, 1);
            case 1 -> new Position(0, -1);
            case 2 -> new Position(1, 0);
            case 3 -> new Position(-1, 0);
            default -> new Position(0, 0);
        };
    }

    private void closeConnections() {
        try {
            for (Socket socket : clientSockets) {
                if (socket != null) {
                    socket.close();
                }
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void processPlayerInput(int playerId, String input) {
        playerCommands[playerId].add(input);
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

    private static class ClientHandler extends Thread {
        private Server server;
        private int playerId;
        private BufferedReader input;

        public ClientHandler(Server server, int playerId, BufferedReader input) {
            this.server = server;
            this.playerId = playerId;
            this.input = input;
        }

        public void run() {
            try {
                String command;
                while ((command = input.readLine()) != null) {
                    server.processPlayerInput(playerId, command);
                }
            } catch (IOException e) {
                System.out.println("Player " + playerId + " disconnected.");
            }
        }
    }
}

class Position {
    int x, y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void update(Position direction) {
        this.x += direction.x;
        this.y += direction.y;
    }
}

class Player {
    private int id;
    private Position position;
    private Position direction;
    private boolean alive = true;

    public Player(int id, Position position, Position direction) {
        this.id = id;
        this.position = position;
        this.direction = direction;
    }

    public void move(byte[][] matrix) {
        if (!alive) return;
        position.update(direction);
        matrix[position.y][position.x] = (byte) id;
    }

    public void changeDirection(char input) {
        switch (input) {
            case 'W':
                direction = new Position(0, -1);
                break;
            case 'S':
                direction = new Position(0, 1);
                break;
            case 'A':
                direction = new Position(-1, 0);
                break;
            case 'D':
                direction = new Position(1, 0);
                break;
        }
    }

    public boolean isAlive() {
        return alive;
    }
}