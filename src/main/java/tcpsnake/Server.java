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
            // Zpracování příkazů hráčů
            for (int i = 0; i < connectedPlayers; i++) {
                String command = playerCommands[i].poll();
                if (command != null) {
                    players[i].changeDirection(command.charAt(0));
                }
                players[i].move(matrix, players);
            }

            if (isRoundOver()) {
                break;
            }

            broadcastGameState();

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

        for (int i = 0; i < connectedPlayers; i++) {
            players[i] = new Player(i, getPlayerStartingPosition(i), getPlayerStartingDirection(i));
        }

        generateApple();
    }

    private void generateApple() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(MATRIX_SIZE);
            y = rand.nextInt(MATRIX_SIZE);
        } while (matrix[y][x] != Common.EMPTY);
        matrix[y][x] = Common.FRUIT;
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
    private LinkedList<Position> body; // Uchovává celý had
    private Position direction;
    private boolean alive = true;
    private boolean grew = false; // Indikátor růstu při sežrání jablka

    public Player(int id, Position position, Position direction) {
        this.id = id;
        this.body = new LinkedList<>();
        this.body.add(new Position(position.x, position.y)); // Had začíná jen s hlavou
        this.direction = direction;
    }

    public void move(byte[][] matrix, Player[] players) {
        if (!alive) return;

        Position head = body.getFirst();
        Position newHead = new Position(head.x + direction.x, head.y + direction.y);

        // Zajištění průchodu hranicemi (had vyjede z jedné strany a objeví se na druhé)
        if (newHead.x < 0) newHead.x = matrix.length - 1;
        else if (newHead.x >= matrix.length) newHead.x = 0;
        if (newHead.y < 0) newHead.y = matrix[0].length - 1;
        else if (newHead.y >= matrix[0].length) newHead.y = 0;


        for (Player other : players) {
            if (other != null && other != this) {
                if (other.body.contains(newHead)) {
                    alive = false;
                    return;
                }
            }
        }

        for (Player other : players) {
            if (other != null && other != this) {
                Position otherHead = other.body.getFirst();
                if (otherHead.x == newHead.x && otherHead.y == newHead.y) {
                    this.alive = false;
                    other.alive = false;
                    return;
                }
            }
        }

        // Kolize se sebou samým
        if (body.contains(newHead)) {
            alive = false;
            return;
        }

        // Sežrání jablka
        boolean ateApple = matrix[newHead.y][newHead.x] == Common.FRUIT;
        if (ateApple) {
            grew = true;
            generateNewApple(matrix); // Vygenerování nového jablka
        }

        // Pokud had nesnědl jablko, odstraníme jeho ocas
        if (!grew) {
            Position tail = body.removeLast();
            matrix[tail.y][tail.x] = Common.EMPTY;
        } else {
            grew = false;
        }

        // Přidání nové hlavy
        body.addFirst(newHead);

        // Aktualizace matice: Hlava → velké písmeno, Tělo → malé písmeno
        matrix[newHead.y][newHead.x] = id == 0 ? Common.P1_HEAD : Common.P2_HEAD;
        for (int i = 1; i < body.size(); i++) {
            Position segment = body.get(i);
            matrix[segment.y][segment.x] = id == 0 ? Common.P1_BODY : Common.P2_BODY;
        }
    }

    public void changeDirection(char input) {
        Position newDirection = switch (input) {
            case 'W' -> new Position(0, -1);
            case 'S' -> new Position(0, 1);
            case 'A' -> new Position(-1, 0);
            case 'D' -> new Position(1, 0);
            default -> direction;
        };

        // Zabránění otočení o 180°
        if (body.size() == 1 || !(newDirection.x == -direction.x && newDirection.y == -direction.y)) {
            direction = newDirection;
        }
    }

    private void generateNewApple(byte[][] matrix) {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(matrix.length);
            y = rand.nextInt(matrix[0].length);
        } while (matrix[y][x] != Common.EMPTY);
        matrix[y][x] = Common.FRUIT;
    }

    public boolean isAlive() {
        return alive;
    }
}