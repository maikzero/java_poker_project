package com.kevin.poker.network;

import com.kevin.poker.Card;
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
        
        // Additional methods for GUI support
        default void onGameInfo(int playerId, String playerName, int chips) {}
        
        default void onHoleCards(List<Card> cards) {}
        
        default void onTurnToAct(int playerId, int currentBet) {}
        
        default void onCommunityCards(List<Card> cards) {}
        
        default void onBet(int playerId, int amount) {}
        
        default void onFold(int playerId) {}
        
        default void onGameStart() {}
        
        default void onCurrentBet(int currentBet) {
        }

        default void onChips(int chips) {
        }
        
        default void onChipsUpdate(int playerId, int chips) {
        }

        void onHandEnded();
        
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
            case "PLAYER_ID":
                handlePlayerId(payload);
                break;

            case "CHIPS":
                handleChips(payload);
                break;

            case "HOLE_CARDS":
                handleHoleCards(payload);
                break;

            case "CURRENT_BET":
                handleCurrentBet(payload);
                break;

            case "COMMUNITY_CARDS":
                System.out.println("Received COMMUNITY_CARDS message");
                handleCommunityCards(payload);
                break;
            case "GAME_START":
                System.out.println("Received GAME_START message");
                listener.onGameStart();
                break;
            case "CHIPS_UPDATE":
                handleChipsUpdate(payload);
                break;
            case "HAND_ENDED":
                listener.onHandEnded();
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
        if (payload != null && !payload.isEmpty()) {
            for (String token : payload.split("[\\s,]+")) {
                String card = token.trim();
                if (!card.isEmpty()) {
                    cards.add(card);
                }
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
    
    private void handlePlayerId(String payload) {
        String[] parts = payload.split("\\s+");
        try {
            int playerId = Integer.parseInt(parts[0]);
            String playerName = parts.length > 1 ? parts[1] : "";
            int chips = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            System.out.println("handlePlayerId: id=" + playerId + ", name=" + playerName + ", chips=" + chips);
            listener.onGameInfo(playerId, playerName, chips);
        } catch (NumberFormatException e) {
            listener.onError("Invalid PLAYER_ID: " + payload);
        }
    }

    private void handleChips(String payload) {
        try {
            int chips = Integer.parseInt(payload);
            System.out.println("Received CHIPS: " + chips);
            // Just forward to listener - let the listener figure out which player
            listener.onChipsUpdate(-1, chips); // -1 means unknown player, listener will handle
        } catch (NumberFormatException e) {
            listener.onError("Invalid CHIPS: " + payload);
        }
    }

    private void handleHoleCards(String payload) {
        List<Card> cards = parseCardList(payload);
        listener.onHoleCards(cards);
    }

    private void handleCurrentBet(String payload) {
        try {
            int currentBet = Integer.parseInt(payload);
            listener.onInfo("Current bet: " + currentBet);
        } catch (NumberFormatException e) {
            listener.onError("Invalid CURRENT_BET: " + payload);
        }
    }

    private List<Card> parseCardList(String payload) {
        List<Card> cards = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            return cards;
        }

        String[] cardTokens = payload.split("[\\s,]+");
        for (String token : cardTokens) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            String[] parts = token.split(":");
            if (parts.length == 2) {
                try {
                    Card.Rank rank = Card.Rank.valueOf(parts[0].trim().toUpperCase());
                    Card.Suit suit = Card.Suit.valueOf(parts[1].trim().toUpperCase());
                    cards.add(new Card(rank, suit));
                } catch (IllegalArgumentException e) {
                    System.err.println("Failed to parse card: " + token);
                }
            }
        }
        return cards;
    }

    private void handleChipsUpdate(String payload) {
        String[] parts = payload.split("\\s+");
        if (parts.length >= 2) {
            try {
                int playerId = Integer.parseInt(parts[0]);
                int chips = Integer.parseInt(parts[1]);
                System.out.println("CHIPS_UPDATE received - Player " + playerId + ": " + chips);
                listener.onChipsUpdate(playerId, chips);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse CHIPS_UPDATE: " + payload);
            }
        }
    }

    private void handleCommunityCards(String payload) {
        List<Card> cards = parseCardList(payload);
        listener.onCommunityCards(cards);
    }

    

    
    
}
