package com.kevin.poker.network;

public class PokerNetworkLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "server": {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
                try (PokerServer server = new PokerServer(port)) {
                    server.start();
                    System.out.println("Press Ctrl+C to stop the server.");
                    Thread.currentThread().join();
                }
                break;
            }
            case "client": {
                String host = args.length > 1 ? args[1] : "127.0.0.1";
                int port = args.length > 2 ? Integer.parseInt(args[2]) : 5000;
                String name = args.length > 3 ? args[3] : "Player";
                try (PokerClient client = new PokerClient(host, port, name)) {
                    client.runConsole();
                }
                break;
            }
            default:
                printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  server [port]");
        System.out.println("  client [host] [port] [name]");
    }
}
