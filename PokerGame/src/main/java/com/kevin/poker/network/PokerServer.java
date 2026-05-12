package com.kevin.poker.network;

import com.kevin.poker.Action;
import com.kevin.poker.Card;
import com.kevin.poker.Player;
import com.kevin.poker.PokerGame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PokerServer implements PokerNetworkBridge, Closeable {
    private final int port;
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private final Map<Integer, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Map<Integer, BlockingQueue<Action>> actionQueues = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private volatile boolean running;
    private ServerSocket serverSocket;
    private PokerGame game;

    public PokerServer(int port) {
        this.port = port;
    }

    public PokerServer(int port, PokerGame game) {
        this(port);
        bindGame(game);
    }

    public synchronized void bindGame(PokerGame game) {
        this.game = game;
        if (this.game != null) {
            this.game.setNetworkBridge(this);
            for (Map.Entry<Integer, BlockingQueue<Action>> entry : actionQueues.entrySet()) {
                this.game.registerNetworkActionQueue(entry.getKey(), entry.getValue());
            }
        }
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "poker-accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("Poker server listening on port " + port);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                connectionPool.submit(new ClientSession(socket));
            } catch (IOException e) {
                if (running) {
                    System.out.println("Accept loop stopped: " + e.getMessage());
                }
            }
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    public synchronized void stop() throws IOException {
        running = false;
        for (ClientSession session : sessions.values()) {
            session.close();
        }
        sessions.clear();
        actionQueues.clear();
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        connectionPool.shutdownNow();
        if (game != null) {
            game.setNetworkBridge(null);
        }
    }

    private int registerPlayer(String playerName, ClientSession session) {
        int playerId = nextPlayerId.getAndIncrement();
        sessions.put(playerId, session);
        BlockingQueue<Action> actionQueue = new LinkedBlockingQueue<>();
        actionQueues.put(playerId, actionQueue);

        if (game != null) {
            game.addPlayer(new Player(playerId, playerName, 1000));
            game.registerNetworkActionQueue(playerId, actionQueue);
        }

        onPlayerJoined(new Player(playerId, playerName, 1000));
        return playerId;
    }

    private void unregisterPlayer(int playerId) {
        sessions.remove(playerId);
        actionQueues.remove(playerId);
        if (game != null) {
            game.unregisterNetworkActionQueue(playerId);
        }
        onPlayerLeft(playerId);
    }

    private void broadcast(String message) {
        for (ClientSession session : sessions.values()) {
            session.sendLine(message);
        }
    }

    private void sendToPlayer(int playerId, String message) {
        ClientSession session = sessions.get(playerId);
        if (session != null) {
            session.sendLine(message);
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String formatCards(List<Card> cards) {
        return cards.stream()
            .map(Card::toString)
            .collect(Collectors.joining(" "));
    }

    @Override
    public void onLog(String message) {
        broadcast("INFO " + sanitize(message));
    }

    @Override
    public void onStreetChanged(String street) {
        broadcast("STREET " + sanitize(street));
    }

    @Override
    public void onPotUpdated(int pot) {
        broadcast("POT " + pot);
    }

    @Override
    public void onCommunityCardsUpdated(List<Card> communityCards) {
        String payload = communityCards.stream()
            .map(card -> card.getRank() + ":" + card.getSuit())
            .collect(Collectors.joining(","));
        broadcast("COMMUNITY_CARDS " + payload);
        broadcast("BOARD " + payload);
    }

    @Override
    public void promptPlayer(Player player, int currentBet, int pot, List<Card> communityCards) {
        sendToPlayer(player.getId(), "TURN " + currentBet + " " + pot + " " + formatCards(communityCards));
    }

    @Override
    public void onPlayerJoined(Player player) {
        broadcast("JOINED_PLAYER " + player.getId() + " " + sanitize(player.getName()));
    }

    @Override
    public void onPlayerLeft(int playerId) {
        broadcast("LEFT " + playerId);
    }

    @Override
    public void onHoleCardsDealt(int playerId, List<Card> holeCards) {
        System.out.println("Sending HOLE_CARDS to player " + playerId + ": " + holeCards);
        StringBuilder sb = new StringBuilder();
        for (Card card : holeCards) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(card.getRank()).append(":").append(card.getSuit());
        }
        sendToPlayer(playerId, "HOLE_CARDS " + sb.toString());
    }

    // Add to PokerServer class - Send hole cards to a specific player
    private void sendHoleCardsToPlayer(int playerId, List<Card> holeCards) {
        StringBuilder sb = new StringBuilder();
        for (Card card : holeCards) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(card.getRank()).append(":").append(card.getSuit());
        }
        sendToPlayer(playerId, "HOLE_CARDS " + sb.toString());
    }

    // Add to PokerServer class - Tell a player it's their turn
    private void sendYourTurn(int playerId, int currentBet, int pot, List<Card> communityCards) {
        String communityStr = formatCards(communityCards);
        sendToPlayer(playerId, "YOUR_TURN " + currentBet);
        sendToPlayer(playerId, "CURRENT_BET " + currentBet);
        sendToPlayer(playerId, "POT " + pot);
        sendToPlayer(playerId, "BOARD " + communityStr);
    }

    // Add to PokerServer class - Send game start info to all players
    // private void broadcastGameStart() {
    //     for (Map.Entry<Integer, ClientSession> entry : sessions.entrySet()) {
    //         int playerId = entry.getKey();
    //         Player player = game.getPlayers().stream()
    //                 .filter(p -> p.getId() == playerId)
    //                 .findFirst()
    //                 .orElse(null);
    //         if (player != null) {
    //             sendToPlayer(playerId, "PLAYER_ID " + playerId);
    //             sendToPlayer(playerId, "CHIPS " + player.getChips());
    //             sendHoleCardsToPlayer(playerId, player.getHoleCards());
    //         }
    //     }
    //     broadcastCommunityCards();
    // }

    /// In PokerServer.java - Add this method
    private void syncFullGameStateToPlayer(int playerId) {
        Player player = null;
        for (Player p : game.getPlayers()) {
            if (p.getId() == playerId) {
                player = p;
                break;
            }
        }

        if (player == null) {
            System.err.println("Could not find player " + playerId);
            return;
        }

        System.out.println("Syncing game state to player " + playerId);
        System.out.println("  Player chips: " + player.getChips()); // Debug

        // Send PLAYER_ID with name and chips
        sendToPlayer(playerId, "PLAYER_ID " + playerId + " " + player.getName() + " " + player.getChips());

        // Send CHIPS separately as well
        sendToPlayer(playerId, "CHIPS " + player.getChips());

        // Send hole cards
        StringBuilder holeCards = new StringBuilder();
        for (Card card : player.getHoleCards()) {
            if (holeCards.length() > 0)
                holeCards.append(",");
            holeCards.append(card.getRank()).append(":").append(card.getSuit());
        }
        sendToPlayer(playerId, "HOLE_CARDS " + holeCards.toString());

        // Send rest of state...
        sendToPlayer(playerId, "STREET " + game.getStreet());
        sendToPlayer(playerId, "POT " + game.getPot());
        sendToPlayer(playerId, "CURRENT_BET " + game.getCurrentBet());

        // Send community cards
        StringBuilder community = new StringBuilder();
        for (Card card : game.getCommunityCards()) {
            if (community.length() > 0)
                community.append(",");
            community.append(card.getRank()).append(":").append(card.getSuit());
        }
        sendToPlayer(playerId, "COMMUNITY_CARDS " + community.toString());

        for (Player existingPlayer : game.getPlayers()) {
            if (existingPlayer.getId() != playerId) { // Skip the player themselves
                sendToPlayer(playerId, "JOINED_PLAYER " + existingPlayer.getId() + " " + existingPlayer.getName());
                System.out.println("  Sending existing player: " + existingPlayer.getName() + " (ID: "
                        + existingPlayer.getId() + ")");

                // Also send their chips
                sendToPlayer(playerId, "CHIPS_UPDATE " + existingPlayer.getId() + " " + existingPlayer.getChips());
            }
        }
    }
    
    // Add to PokerServer class - Broadcast updated community cards to all players
    public void notifyGameStart() {
        broadcast("GAME_START");
        System.out.println("Broadcasted GAME_START to all clients");
    }

    public void broadcastHoleCards() {
        for (Player player : game.getPlayers()) {
            StringBuilder sb = new StringBuilder();
            for (Card card : player.getHoleCards()) {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(card.getRank()).append(":").append(card.getSuit());
            }
            sendToPlayer(player.getId(), "HOLE_CARDS " + sb.toString());
        }
    }
    
    @Override
    public void onChipsUpdated(int playerId, int chips) {
        String message = "CHIPS_UPDATE " + playerId + " " + chips;
        System.out.println("Broadcasting: " + message);
        broadcast(message);
    }
    
    @Override
    public void onHandEnded() {
        broadcast("HAND_ENDED");
    }

    private final class ClientSession implements Runnable, Closeable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private volatile int playerId = -1;
        private volatile boolean closed;

        private ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        }

        @Override
        public void run() {
            try {
                writer.println("WELCOME Send JOIN <name> to enter the table.");
                String line;
                while ((line = reader.readLine()) != null && !closed) {
                    handleLine(line.trim());
                }
            } catch (IOException e) {
                if (!closed) {
                    System.out.println("Client session ended: " + e.getMessage());
                }
            } finally {
                try {
                    close();
                } catch (IOException ignored) {
                }
            }
        }

        private void handleLine(String line) {
            if (line.isEmpty()) {
                return;
            }

            String upper = line.toUpperCase();
            if (upper.startsWith("JOIN ")) {
                if (playerId != -1) {
                    writer.println("ERROR You already joined.");
                    return;
                }
                String name = sanitize(line.substring(5));
                if (name.isEmpty()) {
                    writer.println("ERROR Name cannot be empty.");
                    return;
                }
                playerId = registerPlayer(name, this);
                writer.println("JOINED " + playerId + " " + name);

                // Send full game state to the new player
                PokerServer.this.syncFullGameStateToPlayer(playerId);
                return;
            }

            if (playerId == -1) {
                writer.println("ERROR Join first with: JOIN <name>");
                return;
            }

            if (upper.startsWith("ACTION ")) {
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    writer.println("ERROR Usage: ACTION FOLD|CALL|RAISE [amount]");
                    return;
                }
                String type = parts[1].toUpperCase();
                int amount = 0;
                if ("RAISE".equals(type)) {
                    if (parts.length < 3) {
                        writer.println("ERROR Usage: ACTION RAISE <amount>");
                        return;
                    }
                    try {
                        amount = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        writer.println("ERROR Raise amount must be a number.");
                        return;
                    }
                }
                BlockingQueue<Action> queue = actionQueues.get(playerId);
                if (queue != null) {
                    queue.offer(new Action(type, amount));
                }
                return;
            }

            if (upper.startsWith("CHAT ")) {
                broadcast("CHAT " + playerId + " " + sanitize(line.substring(5)));
                return;
            }

            if (upper.equals("QUIT")) {
                try {
                    close();
                } catch (IOException ignored) {
                }
                return;
            }

            writer.println("ERROR Unknown command.");
        }

        private void sendLine(String message) {
            if (!closed) {
                writer.println(message);
            }
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (playerId != -1) {
                unregisterPlayer(playerId);
            }
            socket.close();
        }
    }
}
