package com.kevin.poker.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PokerClientEventHandler {
    private final BufferedReader reader;
    private final Listener listener;
    private volatile boolean running;

    public interface Listener {
        void onJoined(int playerId, String playerName);

        void onPlayerJoined(int playerId, String playerName);

        void onPlayerLeft(int playerId);

        void onStreet(String street);

        void onPot(int pot);

        void onBoard(List<String> cards);

        void onTurn(int currentBet, int pot, List<String> communityCards);

        void onInfo(String message);

        void onChat(int playerId, String message);

        void onError(String message);
    }

    public PokerClientEventHandler(BufferedReader reader, Listener listener) {
        this.reader = reader;
        this.listener = listener;
    }

    public void listen() {
        running = true;
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            if (running) {
                listener.onError("Connection lost: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
    }

    private void handleMessage(String line) {
        if (line.isEmpty()) {
            return;
        }

        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toUpperCase();
        String payload = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "WELCOME":
                listener.onInfo(payload);
                break;
            case "JOINED":
                handleJoined(payload);
                break;
            case "JOINED_PLAYER":
                handlePlayerJoined(payload);
                break;
            case "LEFT":
                handlePlayerLeft(payload);
                break;
            case "STREET":
                listener.onStreet(payload);
                break;
            case "POT":
                try {
                    listener.onPot(Integer.parseInt(payload));
                } catch (NumberFormatException ignored) {
                }
                break;
            case "BOARD":
                handleBoard(payload);
                break;
            case "TURN":
                handleTurn(payload);
                break;
            case "INFO":
                listener.onInfo(payload);
                break;
            case "CHAT":
                handleChat(payload);
                break;
            case "ERROR":
                listener.onError(payload);
                break;
            default:
                listener.onInfo("Unknown command: " + command);
        }
    }

    private void handleJoined(String payload) {
        String[] parts = payload.split("\\s+", 2);
        if (parts.length == 2) {
            try {
                int playerId = Integer.parseInt(parts[0]);
                String name = parts[1];
                listener.onJoined(playerId, name);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void handlePlayerJoined(String payload) {
        String[] parts = payload.split("\\s+", 2);
        if (parts.length == 2) {
            try {
                int playerId = Integer.parseInt(parts[0]);
                String name = parts[1];
                listener.onPlayerJoined(playerId, name);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void handlePlayerLeft(String payload) {
        try {
            int playerId = Integer.parseInt(payload);
            listener.onPlayerLeft(playerId);
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleBoard(String payload) {
        List<String> cards = new ArrayList<>();
        if (!payload.isEmpty()) {
            for (String card : payload.split("\\s+")) {
                cards.add(card);
            }
        }
        listener.onBoard(cards);
    }

    private void handleTurn(String payload) {
        String[] parts = payload.split("\\s+", 3);
        if (parts.length >= 2) {
            try {
                int currentBet = Integer.parseInt(parts[0]);
                int pot = Integer.parseInt(parts[1]);
                List<String> communityCards = new ArrayList<>();
                if (parts.length > 2) {
                    for (String card : parts[2].split("\\s+")) {
                        communityCards.add(card);
                    }
                }
                listener.onTurn(currentBet, pot, communityCards);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void handleChat(String payload) {
        String[] parts = payload.split("\\s+", 2);
        if (parts.length == 2) {
            try {
                int playerId = Integer.parseInt(parts[0]);
                String message = parts[1];
                listener.onChat(playerId, message);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
