package com.kevin.poker.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MultiplayerNetworkLauncher {
    public static void main(String[] args) throws Exception {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=".repeat(50));
        System.out.println("TEXAS HOLD'EM POKER - MULTIPLAYER");
        System.out.println("=".repeat(50));
        System.out.println("\n[1] Host a game (server mode)");
        System.out.println("[2] Join a game (client mode)");
        System.out.print("\nChoose mode (1 or 2): ");

        String choice = console.readLine().trim();

        if ("1".equals(choice)) {
            hostGame(console);
        } else if ("2".equals(choice)) {
            joinGame(console);
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void hostGame(BufferedReader console) throws Exception {
        System.out.print("\nEnter server port (default 5000): ");
        String portInput = console.readLine().trim();
        int port = portInput.isEmpty() ? 5000 : Integer.parseInt(portInput);

        System.out.print("Enter small blind (default 10): ");
        String sbInput = console.readLine().trim();
        int smallBlind = sbInput.isEmpty() ? 10 : Integer.parseInt(sbInput);

        System.out.print("Enter big blind (default 20): ");
        String bbInput = console.readLine().trim();
        int bigBlind = bbInput.isEmpty() ? 20 : Integer.parseInt(bbInput);

        System.out.print("Enter total players at the table including host (default 3): ");
        String npInput = console.readLine().trim();
        int numPlayers = npInput.isEmpty() ? 3 : Integer.parseInt(npInput);

        MultiplayerGameController controller = new MultiplayerGameController(port, smallBlind, bigBlind);
        controller.addHostPlayer("Host");
        controller.startServer();

        System.out.println("\nWaiting for " + numPlayers + " total players to join...");
        try {
            controller.waitForPlayers(numPlayers, 60000);
        } catch (InterruptedException e) {
            System.out.println("Timeout or error while waiting for players: " + e.getMessage());
            controller.stopServer();
            return;
        }

        System.out.println("Starting game...");
        controller.startGameWithGUI();

        System.out.println("\nServer running. Press Ctrl+C to stop.");
        Thread.currentThread().join();
        controller.stopServer();
    }

    private static void joinGame(BufferedReader console) throws Exception {
        System.out.print("\nEnter server host (default 127.0.0.1): ");
        String host = console.readLine().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }

        System.out.print("Enter server port (default 5000): ");
        String portInput = console.readLine().trim();
        int port = portInput.isEmpty() ? 5000 : Integer.parseInt(portInput);

        System.out.print("Enter your player name: ");
        String name = console.readLine().trim();
        if (name.isEmpty()) {
            name = "Player";
        }

        System.out.println("\nConnecting to " + host + ":" + port + " as " + name + "...");

        try (PokerClient client = new PokerClient(host, port, name)) {
            // client.runConsole();
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
