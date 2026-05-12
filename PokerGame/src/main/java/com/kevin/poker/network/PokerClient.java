package com.kevin.poker.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class PokerClient implements Closeable, PokerClientEventHandler.Listener {
    private final String host;
    private final int port;
    private final String playerName;
    private final PokerClientEventHandler.Listener customListener;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private PokerClientEventHandler eventHandler;
    private Thread listenerThread;
    private volatile boolean running;

    public PokerClient(String host, int port, String playerName) {
        this(host, port, playerName, null);
    }

    public PokerClient(String host, int port, String playerName, PokerClientEventHandler.Listener customListener) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.customListener = customListener;
    }

    public synchronized void connect() throws IOException {
        if (running) {
            return;
        }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
        eventHandler = new PokerClientEventHandler(reader, this);
        running = true;
        listenerThread = new Thread(() -> eventHandler.listen(), "poker-client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        writer.println("JOIN " + playerName);
    }

    public synchronized void sendCommand(String command) {
        if (writer != null && running) {
            writer.println(command);
        }
    }

    // Remove or comment out runConsole() - we don't want console mode
    // public void runConsole() throws IOException {
    //     connect();
    //     try (Scanner scanner = new Scanner(System.in)) {
    //         System.out.println("Type /call, /fold, /raise 50, /chat hello, or /quit");
    //         while (running) {
    //             String line = scanner.nextLine().trim();
    //             if (line.isEmpty()) {
    //                 continue;
    //             }
    //             if (line.equalsIgnoreCase("/quit")) {
    //                 sendCommand("QUIT");
    //                 break;
    //             }
    //             if (line.startsWith("/call")) {
    //                 sendCommand("ACTION CALL");
    //                 continue;
    //             }
    //             if (line.startsWith("/fold")) {
    //                 sendCommand("ACTION FOLD");
    //                 continue;
    //             }
    //             if (line.startsWith("/raise")) {
    //                 String[] parts = line.split("\\s+");
    //                 if (parts.length < 2) {
    //                     System.out.println("Usage: /raise <amount>");
    //                     continue;
    //                 }
    //                 sendCommand("ACTION RAISE " + parts[1]);
    //                 continue;
    //             }
    //             if (line.startsWith("/chat ")) {
    //                 sendCommand("CHAT " + line.substring(6));
    //                 continue;
    //             }
    //             sendCommand("CHAT " + line);
    //         }
    //     }
    // }

    @Override
    public void onJoined(int playerId, String playerName) {
        System.out.println("✓ Joined as player " + playerId + " (" + playerName + ")");
        if (customListener != null) {
            customListener.onJoined(playerId, playerName);
        }
    }

    @Override
    public void onPlayerJoined(int playerId, String playerName) {
        System.out.println("→ " + playerName + " (player " + playerId + ") joined the table");
        if (customListener != null) {
            customListener.onPlayerJoined(playerId, playerName);
        }
    }

    @Override
    public void onPlayerLeft(int playerId) {
        System.out.println("← Player " + playerId + " left the table");
        if (customListener != null) {
            customListener.onPlayerLeft(playerId);
        }
    }

    @Override
    public void onStreet(String street) {
        // FORWARD to custom listener instead of printing
        if (customListener != null) {
            customListener.onStreet(street);
        }
    }

    @Override
    public void onPot(int pot) {
        // FORWARD to custom listener instead of printing
        if (customListener != null) {
            customListener.onPot(pot);
        }
    }

    @Override
    public void onBoard(List<String> cards) {
        // FORWARD to custom listener instead of printing
        if (customListener != null) {
            customListener.onBoard(cards);
        }
    }

    @Override
    public void onTurn(int currentBet, int pot, List<String> communityCards) {
        // FORWARD to custom listener - DO NOT print console prompt!
        if (customListener != null) {
            customListener.onTurn(currentBet, pot, communityCards);
        }
    }

    @Override
    public void onInfo(String message) {
        System.out.println("→ " + message);
        if (customListener != null) {
            customListener.onInfo(message);
        }
    }

    @Override
    public void onChat(int playerId, String message) {
        System.out.println("Player " + playerId + ": " + message);
        if (customListener != null) {
            customListener.onChat(playerId, message);
        }
    }

    @Override
    public void onError(String message) {
        System.out.println("✗ ERROR: " + message);
        if (customListener != null) {
            customListener.onError(message);
        }
    }

    // Add missing methods that the server might send
    @Override
    public void onGameInfo(int playerId, String playerName, int chips) {
        if (customListener != null) {
            customListener.onGameInfo(playerId, playerName, chips);
        }
    }

    @Override
    public void onHoleCards(List<com.kevin.poker.Card> cards) {
        System.out.println("PokerClient.onHoleCards: " + cards);
        if (customListener != null) {
            customListener.onHoleCards(cards);
        }
    }

    @Override
    public void onCommunityCards(List<com.kevin.poker.Card> cards) {
        if (customListener != null) {
            customListener.onCommunityCards(cards);
        }
    }

    @Override
    public void onGameStart() {
        System.out.println("GAME_START received in PokerClient, forwarding...");
        if (customListener != null) {
            customListener.onGameStart();
        }
    }

    @Override
    public void onHandEnded() {
        System.out.println("Hand ended");
        if (customListener != null) {
            customListener.onHandEnded();
        }
    }
    
    @Override
    public void onChipsUpdate(int playerId, int chips) {
        System.out.println("PokerClient.onChipsUpdate: Player " + playerId + " -> " + chips);
        if (customListener != null) {
            customListener.onChipsUpdate(playerId, chips);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        running = false;
        if (eventHandler != null) {
            eventHandler.stop();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}