package tcpsnake;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents the server for the Snake game played over TCP.
 * It handles player connections, game state updates, and communication with clients.
 */
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

    @SuppressWarnings("unchecked")
    private Queue<String>[] playerCommands = new ConcurrentLinkedQueue[MAX_PLAYERS];
    private byte roundStatus = Common.NOT_STARTED;

    private int rounds;
    private byte currentRound = 1;

    private static final String SCORE_FILE = "highscores.txt";
    private static Map<String, Integer> highScores = new HashMap<>();
    private int[] totalScores = new int[MAX_PLAYERS]; // stores total scores across all rounds

    /**
     * Constructs a new Server instance.
     *
     * @param playerCount the number of players who will join the game
     * @param port the port on which the server will listen
     * @param rounds the number of rounds to be played
     * @throws IOException if an I/O error occurs when opening the socket
     */
    public Server(int playerCount, int port, int rounds) throws IOException {
        this.rounds = rounds;
        this.serverSocket = new ServerSocket(port);

        // load scores of players
        loadHighScores();

        showHighScores();

        // initialize game matrix
        resetGameState();

        // wait for players to connect
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

            // start a thread to handle player input
            new ClientHandler(this, i, inputStreams[i]).start();
        }
        System.out.println("All players connected. Starting the game...");
    }

    /**
     * Saves the results of the completed game into a history file.
     */
    private void saveGameHistory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("game_history.txt", true))) {
            writer.write("Game Finished - " + java.time.LocalDateTime.now());
            writer.newLine();
            for (int i = 0; i < connectedPlayers; i++) {
                writer.write(playerNames[i] + " - Score: " + totalScores[i]);
                writer.newLine();
            }
            writer.write("---------------------------");
            writer.newLine();
            System.out.println("Game history saved.");
        } catch (IOException e) {
            System.err.println("Error saving game history.");
        }
    }

    /**
     * Displays the top high scores stored in the file.
     * If there are no scores recorded yet, it informs the players.
     */
    private static void showHighScores() {
        System.out.println("=== 🏆 High Scores 🏆 ===");
        if (highScores.isEmpty()) {
            System.out.println("No high scores recorded yet.");
        } else {
            highScores.entrySet()
                    .stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Descending order
                    .limit(5) // Show only top 5
                    .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue() + " points"));
        }
        System.out.println("=========================");
    }

    /**
     * Loads high scores from a file into the `highScores` map.
     * If the file does not exist or cannot be read, initializes an empty high score list.
     *
     * The high scores are stored in a text file where each line follows the format:
     * <pre>
     * playerName:score
     * </pre>
     * Example:
     * <pre>
     * Alice:1500
     * Bob:1200
     * </pre>
     */
    private static void loadHighScores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SCORE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    highScores.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
            System.out.println("High scores loaded.");
        } catch (IOException e) {
            System.out.println("No high scores found. Starting fresh.");
        }
    }

    /**
     * Saves a player's high score to the `highScores` map and writes it to a file.
     * If the player already has a recorded score, it will be updated with the new value.
     *
     * The file format remains consistent, storing scores in the format:
     * <pre>
     * playerName:score
     * </pre>
     *
     * @param playerName The name of the player whose score is being saved.
     * @param score The score achieved by the player.
     */
    public static void saveHighScore(String playerName, int score) {
        highScores.put(playerName, score);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SCORE_FILE))) {
            for (Map.Entry<String, Integer> entry : highScores.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
            System.out.println("High scores saved.");
        } catch (IOException e) {
            System.err.println("Error saving high scores.");
        }
    }

    /**
     * Starts the game, including multiple rounds if specified.
     * Manages the main game loop and handles round transitions.
     */
    public void startGame() {
        try {
            for (currentRound = 1; currentRound <= rounds; currentRound++) {
                roundStatus = Common.ROUND_STARTED;
                System.out.println("Round " + currentRound + " started.");
                resetGameState();
                playRound();
                roundStatus = Common.ROUND_END;
                System.out.println("Round " + currentRound + " ended.");
                Thread.sleep(2000);
            }
            roundStatus = Common.END_FINAL;

            // find the player with the highest score
            int maxScore = Integer.MIN_VALUE;
            String winner = "No one";
            for (int i = 0; i < connectedPlayers; i++) {
                if (totalScores[i] > maxScore) {
                    maxScore = totalScores[i];
                    winner = playerNames[i];
                }
            }

            System.out.println("🏆 Winner: " + winner + " with " + maxScore + " points!");

            // send final game state to all players (now including the winner)
            broadcastGameState();

            for (int i = 0; i < connectedPlayers; i++) {
                saveHighScore(playerNames[i], totalScores[i]); // save score of each player
            }

            System.out.println("Game over!");

            saveGameHistory();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    /**
     * Manages a single round of the game. Handles player commands, movement, and detects round completion.
     *
     * @throws IOException if an I/O error occurs during communication
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void playRound() throws IOException, InterruptedException {
        while (!isRoundOver()) { // continue until only one player is left
            for (int i = 0; i < connectedPlayers; i++) {
                String command = playerCommands[i].poll();
                if (command != null) {
                    players[i].changeDirection(command.charAt(0));
                }
                players[i].move(matrix, players, totalScores);
            }

            broadcastGameState();
            Thread.sleep(250);
        }

        // last player standing gets a small bonus
        for (Player player : players) {
            if (player != null && player.isAlive()) {
                totalScores[player.getId()] += 10; // bonus for survival
                System.out.println("🏅 Player " + playerNames[player.getId()] + " survived and earned +10 points!");
                break;
            }
        }

        // final rendering before ending the round
        broadcastGameState();
        Thread.sleep(2000);
    }

    /**
     * Checks if the round is over by counting how many players are still alive.
     *
     * @return true if only one player is left alive, false otherwise
     */
    private boolean isRoundOver() {
        int alivePlayers = 0;
        for (Player player : players) {
            if (player != null && player.isAlive()) {
                alivePlayers++;
            }
        }
        return alivePlayers <= 1; // round ends when only one player is left
    }

    /**
     * Sends the current game state (matrix, player positions, scores, and blindness status) to all connected players.
     *
     * @throws IOException if an I/O error occurs while sending data
     */
    private void broadcastGameState() throws IOException {
        for (int i = 0; i < connectedPlayers; i++) {
            if (outputStreams[i] == null) { // player does exist
                continue; // skip if not
            }

            try {
                outputStreams[i].writeInt(i); // Sending player's own ID

                // send game matrix
                for (int y = 0; y < MATRIX_SIZE; y++) {
                    for (int x = 0; x < MATRIX_SIZE; x++) {
                        outputStreams[i].writeByte(matrix[y][x]);
                    }
                }

                // send game metadata
                outputStreams[i].writeInt(connectedPlayers);
                outputStreams[i].writeByte(roundStatus);
                outputStreams[i].writeByte(currentRound);

                // send player names
                for (int j = 0; j < connectedPlayers; j++) {
                    outputStreams[i].writeUTF(playerNames[j]);
                }

                // send player scores
                for (int j = 0; j < connectedPlayers; j++) {
                    outputStreams[i].writeInt(totalScores[j]);
                }

                // send player positions (x, y)
                for (int j = 0; j < connectedPlayers; j++) {
                    if (players[j] != null) {
                        Position head = players[j].getHeadPosition();
                        outputStreams[i].writeInt(head.x);
                        outputStreams[i].writeInt(head.y);
                    } else {
                        outputStreams[i].writeInt(-1); // if player doesn't exist
                        outputStreams[i].writeInt(-1);
                    }
                }

                // send player blindness status
                for (int j = 0; j < connectedPlayers; j++) {
                    if (players[j] != null) {
                        outputStreams[i].writeBoolean(players[j].isBlind());
                    } else {
                        outputStreams[i].writeBoolean(false); // default false for disconnected player
                    }
                }

                outputStreams[i].flush();
            } catch (IOException e) {
                System.out.println("Player " + i + " disconnected while sending game state.");
                removePlayer(i);
            }
        }
    }

    /**
     * Resets the game state by clearing the matrix, resetting players, and generating a new apple.
     */
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

    /**
     * Generates a new apple (fruit) in a random empty position on the matrix.
     */
    private void generateApple() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(MATRIX_SIZE);
            y = rand.nextInt(MATRIX_SIZE);
        } while (matrix[y][x] != Common.EMPTY);

        double chance = rand.nextDouble();
        if (chance < 0.10) {
            matrix[y][x] = Common.POWERUP_BLIND;
        } else if (chance < 0.30) {
            matrix[y][x] = Common.SPECIAL_FRUIT;
        } else {
            matrix[y][x] = Common.FRUIT;
        }
    }

    /**
     * Determines the starting position for a given player ID.
     *
     * @param playerId the ID of the player
     * @return the starting Position for the player
     */
    private Position getPlayerStartingPosition(int playerId) {
        return switch (playerId) {
            case 0 -> new Position(1, 1);
            case 1 -> new Position(MATRIX_SIZE - 2, MATRIX_SIZE - 2);
            case 2 -> new Position(1, MATRIX_SIZE - 2);
            case 3 -> new Position(MATRIX_SIZE - 2, 1);
            default -> new Position(0, 0);
        };
    }

    /**
     * Determines the starting direction for a given player ID.
     *
     * @param playerId the ID of the player
     * @return the starting direction as a Position
     */
    private Position getPlayerStartingDirection(int playerId) {
        return switch (playerId) {
            case 0 -> new Position(0, 1);
            case 1 -> new Position(0, -1);
            case 2 -> new Position(1, 0);
            case 3 -> new Position(-1, 0);
            default -> new Position(0, 0);
        };
    }

    /**
     * Closes all client connections and the server socket.
     */
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

    /**
     * Processes input from a specific player by adding the command to the player's queue.
     *
     * @param playerId the ID of the player
     * @param input the command received from the player
     */
    public synchronized void processPlayerInput(int playerId, String input) {
        playerCommands[playerId].add(input);
    }

    /**
     * The main method to start the server.
     * Usage: java tcpsnake.Server &lt;player_count&gt; &lt;port&gt; &lt;rounds&gt;
     *
     * @param args command-line arguments specifying the number of players, port, and rounds
     */
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

    private void removePlayer(int playerId) {
        if (outputStreams[playerId] != null) {
            try {
                outputStreams[playerId].close();
            } catch (IOException ignored) {}
        }
        outputStreams[playerId] = null;
        clientSockets[playerId] = null;
        players[playerId] = null;

        connectedPlayers--;

        System.out.println("Player " + playerId + " disconnected. " + connectedPlayers + " players remaining.");

        // Pokud zůstane méně než 2 hráči, vypni server
        if (connectedPlayers < 2) {
            System.out.println("Not enough players to continue. Shutting down server.");
            System.exit(0);
        }
    }


    /**
     * A thread that continuously listens for commands from a specific player and forwards them to the server.
     */
    private static class ClientHandler extends Thread {
        private Server server;
        private int playerId;
        private BufferedReader input;

        /**
         * Constructs a ClientHandler for the specified server and player.
         *
         * @param server the Server instance to which this handler belongs
         * @param playerId the ID of the player this handler manages
         * @param input the input stream from the player's socket
         */
        public ClientHandler(Server server, int playerId, BufferedReader input) {
            this.server = server;
            this.playerId = playerId;
            this.input = input;
        }

        /**
         * Runs the thread, reading commands from the player and passing them to the server.
         */
        public void run() {
            try {
                String command;
                while ((command = input.readLine()) != null) {
                    if (command.equals("DISCONNECT")) { // Pokud hráč poslal odpojení
                        System.out.println("Player " + playerId + " disconnected.");
                        break; // Ukončí smyčku
                    }
                    server.processPlayerInput(playerId, command);
                }
            } catch (IOException e) {
                System.out.println("Player " + playerId + " unexpectedly disconnected.");
            } finally {
                server.removePlayer(playerId); // Odebrání hráče ze hry
            }
        }
    }
}

/**
 * The Player class represents a snake player, tracking movement and interactions.
 */
class Player {
    private int id;
    private LinkedList<Position> body;
    private Position direction;
    private boolean alive = true;
    private boolean grew = false;
    private int score = 0;
    private boolean isBlind = false; // Indicates if player is blinded
    /**
     * Initializes a new player with a head position and movement direction.
     */
    public Player(int id, Position position, Position direction) {
        this.id = id;
        this.body = new LinkedList<>();
        this.body.add(new Position(position.x, position.y));
        this.direction = direction;
    }

    /**
     * Sets the blindness status for the player.
     *
     * @param blind true if the player should be blinded, false otherwise
     */
    public void setBlind(boolean blind) {
        this.isBlind = blind;
        if (blind) {
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Blindness lasts 3 seconds
                    this.isBlind = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Returns the current position of the player's head.
     *
     * @return Position object representing the head's coordinates.
     */
    public Position getHeadPosition() {
        return body.getFirst();
    }


    /**
     * Returns whether the player is currently blinded.
     *
     * @return true if the player is blinded, false otherwise
     */
    public boolean isBlind() {
        return isBlind;
    }

    /**
     * Moves the player forward, handling wall wrapping, collisions, and score updates.
     *
     * @param matrix the game matrix representing the playfield
     * @param players array of all players in the game
     * @param scores array tracking player scores across all rounds
     */
    public void move(byte[][] matrix, Player[] players, int[] scores) {
        if (!alive) return;

        Position head = body.getFirst();
        Position newHead = new Position(head.x + direction.x, head.y + direction.y);

        // wall wrapping logic
        if (newHead.x < 0) newHead.x = matrix.length - 1;
        else if (newHead.x >= matrix.length) newHead.x = 0;
        if (newHead.y < 0) newHead.y = matrix[0].length - 1;
        else if (newHead.y >= matrix[0].length) newHead.y = 0;

        byte cell = matrix[newHead.y][newHead.x];

        // check if the new position collides with another player's head
        boolean isHeadCollision = false;
        int otherPlayerId = -1;

        for (int i = 0; i < players.length; i++) {
            if (players[i] != null && players[i].isAlive() && players[i] != this) {
                if (cell == Common.PLAYER_HEADS[i]) { // Check if we collide with another head
                    isHeadCollision = true;
                    otherPlayerId = i;
                    break;
                }
            }
        }

        // if it's a head-on collision, both players die
        if (isHeadCollision) {
            alive = false;
            players[otherPlayerId].alive = false;
            scores[id] -= 20;
            scores[otherPlayerId] -= 20;
            clearBodyFromMatrix(matrix);
            players[otherPlayerId].clearBodyFromMatrix(matrix);
            return; // both players die immediately, exit method
        }

        // if the player hits any body segment (not head), they die alone
        if (cell != Common.EMPTY && cell != Common.FRUIT
                && cell != Common.SPECIAL_FRUIT && cell != Common.POWERUP_BLIND) {
            alive = false;
            scores[id] -= 30;
            clearBodyFromMatrix(matrix);
            return;
        }

        // fruit collection logic
        if (cell == Common.FRUIT) {
            grew = true;
            scores[id] += 10;
            generateApple(matrix, players);
        } else if (cell == Common.SPECIAL_FRUIT) {
            grew = true;
            scores[id] += 20;
            generateApple(matrix, players);
        } else if (cell == Common.POWERUP_BLIND) {
            scores[id] += 15; // Bonus za sebrání PowerUpu
            activateBlindness(players);
            generateApple(matrix, players);
        }

        // if player didn't eat fruit, remove tail (otherwise, keep growing)
        if (!grew) {
            Position tail = body.removeLast();
            matrix[tail.y][tail.x] = Common.EMPTY;
        } else {
            grew = false;
        }

        // move head forward
        body.addFirst(newHead);
        matrix[newHead.y][newHead.x] = Common.PLAYER_HEADS[id];

        // update the rest of the body in the matrix
        for (int i = 1; i < body.size(); i++) {
            Position segment = body.get(i);
            matrix[segment.y][segment.x] = Common.PLAYER_BODIES[id];
        }
    }


    /**
     * Removes the player's body from the matrix when they die.
     *
     * @param matrix the game matrix
     */
    private void clearBodyFromMatrix(byte[][] matrix) {
        for (Position segment : body) {
            matrix[segment.y][segment.x] = Common.EMPTY;
        }
    }

    /**
     * Activates the blindness effect for all other players in the game.
     * When a player collects a blinding PowerUp, this method sets the blindness
     * status for all other alive players, temporarily restricting their vision.
     *
     * The blindness effect is handled by the `setBlind(true)` method,
     * which starts a timer to disable blindness after a predefined duration.
     *
     * @param players Array of all players in the game.
     *                The effect is applied to every alive player except the one who activated it.
     */
    private void activateBlindness(Player[] players) {
        for (Player other : players) {
            if (other != null && other.isAlive() && other != this) {
                other.setBlind(true);
            }
        }
    }

    /**
     * Changes the direction of movement based on player input.
     */
    public void changeDirection(char input) {
        Position newDirection = switch (input) {
            case 'W' -> new Position(0, -1);
            case 'S' -> new Position(0, 1);
            case 'A' -> new Position(-1, 0);
            case 'D' -> new Position(1, 0);
            default -> direction;
        };

        if (body.size() == 1 || !(newDirection.x == -direction.x && newDirection.y == -direction.y)) {
            direction = newDirection;
        }
    }

    /**
     * Generates an apple (fruit) in a random empty position on the matrix.
     * The apple will not spawn on a player's body.
     *
     * There is a probability distribution for spawning different types of items:
     * - 30% chance to spawn a PowerUp that blinds other players.
     * - 10% chance to spawn a special golden apple.
     * - Otherwise, a normal fruit is spawned.
     *
     * @param matrix The game matrix representing the playfield.
     * @param players Array of all players in the game, to avoid spawning apples on them.
     */
    private void generateApple(byte[][] matrix, Player[] players) {
        Random rand = new Random();
        int x, y;
        boolean validPosition;

        do {
            x = rand.nextInt(matrix.length);
            y = rand.nextInt(matrix[0].length);
            validPosition = matrix[y][x] == Common.EMPTY;

            // Prevent apple from spawning on a snake
            for (Player player : players) {
                if (player != null && player.isAlive()) {
                    for (Position segment : player.body) {
                        if (segment.x == x && segment.y == y) {
                            validPosition = false;
                            break;
                        }
                    }
                }
            }
        } while (!validPosition);

        double chance = rand.nextDouble();
        if (chance < 0.10) {
            matrix[y][x] = Common.POWERUP_BLIND; // 10% chance for blinding PowerUp
        } else if (chance < 0.30) {
            matrix[y][x] = Common.SPECIAL_FRUIT; // 30% chance for golden apple
        } else {
            matrix[y][x] = Common.FRUIT; // Default case for normal apple
        }
    }

    /**
     * Checks if the player is still alive.
     *
     * @return {@code true} if the player is alive, {@code false} otherwise.
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Gets the current score of the player.
     *
     * @return The player's score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Gets the player's unique ID.
     *
     * @return The player's ID.
     */
    public int getId() {
        return id;
    }
}