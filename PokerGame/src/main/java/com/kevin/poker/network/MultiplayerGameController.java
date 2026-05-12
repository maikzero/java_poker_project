package com.kevin.poker.network;

import com.kevin.poker.PokerGame;

import java.io.IOException;

/**
 * MultiplayerGameController bridges the PokerGame with the network layer.
 * The host runs the game loop and broadcasts updates to all connected clients.
 * Clients receive updates and send their actions back through the network.
 */
public class MultiplayerGameController {
    private final PokerGame game;
    private final PokerServer server;

    public MultiplayerGameController(int port, int smallBlind, int bigBlind) throws IOException {
        this.game = new PokerGame(smallBlind, bigBlind);
        this.server = new PokerServer(port, game);
    }

    public void startServer() throws IOException {
        server.start();
        System.out.println("Multiplayer server ready on port " + server.getPort());
    }

    public void stopServer() throws IOException {
        server.stop();
    }

    public PokerGame getGame() {
        return game;
    }

    public PokerServer getServer() {
        return server;
    }

    public void addHostPlayer(String playerName) {
        boolean hostExists = game.getPlayers().stream().anyMatch(player -> player.getId() == 0);
        if (!hostExists) {
            game.addPlayer(new com.kevin.poker.Player(0, playerName, 1000));
        }
    }

    public void waitForPlayers(int expectedPlayerCount, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while (game.getPlayers().size() < expectedPlayerCount) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new InterruptedException("Timeout waiting for " + expectedPlayerCount + " players.");
            }
            Thread.sleep(100);
        }
        System.out.println("All " + expectedPlayerCount + " players joined.");
        Thread.sleep(500); // Give clients time to settle
    }

    public void startGameWithGUI() {
        server.notifyGameStart(); // Tell all clients game is starting
        Thread gameThread = new Thread(() -> {
            try {
                game.startGame();
            } catch (Exception e) {
                System.out.println("Game error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "poker-game-loop");
        gameThread.setDaemon(false);
        gameThread.start();
    }
}
