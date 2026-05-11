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

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private PokerClientEventHandler eventHandler;
    private Thread listenerThread;
    private volatile boolean running;

    public PokerClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    public synchronized void connect() throws IOException {
        if (running) {
            return;
        }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true);
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

    public void runConsole() throws IOException {
        connect();
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Type /call, /fold, /raise 50, /chat hello, or /quit");
            while (running) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equalsIgnoreCase("/quit")) {
                    sendCommand("QUIT");
                    break;
                }
                if (line.startsWith("/call")) {
                    sendCommand("ACTION CALL");
                    continue;
                }
                if (line.startsWith("/fold")) {
                    sendCommand("ACTION FOLD");
                    continue;
                }
                if (line.startsWith("/raise")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) {
                        System.out.println("Usage: /raise <amount>");
                        continue;
                    }
                    sendCommand("ACTION RAISE " + parts[1]);
                    continue;
                }
                if (line.startsWith("/chat ")) {
                    sendCommand("CHAT " + line.substring(6));
                    continue;
                }
                sendCommand("CHAT " + line);
            }
        }
    }

    @Override
    public void onJoined(int playerId, String playerName) {
        System.out.println("✓ Joined as player " + playerId + " (" + playerName + ")");
    }

    @Override
    public void onPlayerJoined(int playerId, String playerName) {
        System.out.println("→ " + playerName + " (player " + playerId + ") joined the table");
    }

    @Override
    public void onPlayerLeft(int playerId) {
        System.out.println("← Player " + playerId + " left the table");
    }

    @Override
    public void onStreet(String street) {
        System.out.println("\n[" + street + "]");
    }

    @Override
    public void onPot(int pot) {
        System.out.println("Pot: " + pot);
    }

    @Override
    public void onBoard(List<String> cards) {
        System.out.print("Board: ");
        for (String card : cards) {
            System.out.print(card + " ");
        }
        System.out.println();
    }

    @Override
    public void onTurn(int currentBet, int pot, List<String> communityCards) {
        System.out.println("\nYour turn!");
        System.out.println("  Current bet: " + currentBet);
        System.out.println("  Pot: " + pot);
        System.out.print("  Board: ");
        for (String card : communityCards) {
            System.out.print(card + " ");
        }
        System.out.println();
        System.out.println("Enter action: /call, /fold, or /raise <amount>");
    }

    @Override
    public void onInfo(String message) {
        System.out.println("→ " + message);
    }

    @Override
    public void onChat(int playerId, String message) {
        System.out.println("Player " + playerId + ": " + message);
    }

    @Override
    public void onError(String message) {
        System.out.println("✗ ERROR: " + message);
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
