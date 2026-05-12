package com.kevin.poker.network;

import java.util.ArrayList;
import java.util.List;

import com.kevin.poker.Card;
import com.kevin.poker.PokerGUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MultiplayerGUILauncher extends Application {
    private Stage primaryStage;
    private Stage waitingStage; // Waiting dialog stage

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Texas Hold'em Poker - Multiplayer");
        showModeSelection();
    }

    private void showModeSelection() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1b4332;");

        Label title = new Label("Texas Hold'em Poker");
        title.setStyle("-fx-font-size: 28; -fx-text-fill: #f4a261; -fx-font-weight: bold;");

        Label subtitle = new Label("Choose Game Mode");
        subtitle.setStyle("-fx-font-size: 16; -fx-text-fill: #d4a373;");

        Button hostBtn = new Button("Host a Game");
        hostBtn.setPrefWidth(200);
        hostBtn.setPrefHeight(50);
        hostBtn.setStyle(
                "-fx-font-size: 14; -fx-background-color: #f4a261; -fx-text-fill: #1b4332; -fx-font-weight: bold;");
        hostBtn.setOnAction(e -> showHostSetup());

        Button joinBtn = new Button("Join a Game");
        joinBtn.setPrefWidth(200);
        joinBtn.setPrefHeight(50);
        joinBtn.setStyle(
                "-fx-font-size: 14; -fx-background-color: #2d6a4f; -fx-text-fill: #f4a261; -fx-border-color: #f4a261; -fx-border-width: 2; -fx-font-weight: bold;");
        joinBtn.setOnAction(e -> showJoinSetup());

        root.getChildren().addAll(title, subtitle, new Separator(), hostBtn, joinBtn);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showHostSetup() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1b4332;");

        Label title = new Label("Host Game Setup");
        title.setStyle("-fx-font-size: 20; -fx-text-fill: #f4a261; -fx-font-weight: bold;");

        // Add name input field
        HBox nameBox = createInputBox("Your Name:", "Host");
        HBox portBox = createInputBox("Port:", "5000");
        HBox sbBox = createInputBox("Small Blind:", "10");
        HBox bbBox = createInputBox("Big Blind:", "20");
        HBox playerBox = createInputBox("Number of Players:", "2");

        TextField nameField = (TextField) nameBox.getChildren().get(1);
        TextField portField = (TextField) portBox.getChildren().get(1);
        TextField sbField = (TextField) sbBox.getChildren().get(1);
        TextField bbField = (TextField) bbBox.getChildren().get(1);
        TextField playerField = (TextField) playerBox.getChildren().get(1);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button startBtn = new Button("Start Server");
        startBtn.setPrefWidth(120);
        startBtn.setStyle("-fx-font-size: 12; -fx-background-color: #f4a261; -fx-text-fill: #1b4332;");
        startBtn.setOnAction(e -> {
            try {
                String hostName = nameField.getText().trim();
                if (hostName.isEmpty()) {
                    hostName = "Host";
                }
                int port = Integer.parseInt(portField.getText());
                int sb = Integer.parseInt(sbField.getText());
                int bb = Integer.parseInt(bbField.getText());
                int players = Integer.parseInt(playerField.getText());
                startHostMode(port, sb, bb, players, hostName); // Pass the name
            } catch (NumberFormatException ex) {
                showError("Invalid input: " + ex.getMessage());
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(120);
        backBtn.setStyle(
                "-fx-font-size: 12; -fx-background-color: #2d6a4f; -fx-text-fill: #f4a261; -fx-border-color: #f4a261; -fx-border-width: 1;");
        backBtn.setOnAction(e -> showModeSelection());

        buttonBox.getChildren().addAll(startBtn, backBtn);

        ScrollPane scrollPane = new ScrollPane(new VBox(10, nameBox, portBox, sbBox, bbBox, playerBox));
        scrollPane.setStyle("-fx-control-inner-background: #1b4332; -fx-text-fill: #f4a261;");
        scrollPane.setFitToWidth(true);

        root.getChildren().addAll(title, scrollPane, buttonBox);

        Scene scene = new Scene(root, 500, 450);
        primaryStage.setScene(scene);
    }

    private void showJoinSetup() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1b4332;");

        Label title = new Label("Join Game");
        title.setStyle("-fx-font-size: 20; -fx-text-fill: #f4a261; -fx-font-weight: bold;");

        HBox hostBox = createInputBox("Server Host:", "127.0.0.1");
        HBox portBox = createInputBox("Server Port:", "5000");
        HBox nameBox = createInputBox("Player Name:", "Player");

        TextField hostField = (TextField) hostBox.getChildren().get(1);
        TextField portField = (TextField) portBox.getChildren().get(1);
        TextField nameField = (TextField) nameBox.getChildren().get(1);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button joinBtn = new Button("Join Game");
        joinBtn.setPrefWidth(120);
        joinBtn.setStyle("-fx-font-size: 12; -fx-background-color: #f4a261; -fx-text-fill: #1b4332;");
        joinBtn.setOnAction(e -> {
            try {
                String host = hostField.getText();
                int port = Integer.parseInt(portField.getText());
                String name = nameField.getText();
                startClientMode(host, port, name);
            } catch (NumberFormatException ex) {
                showError("Invalid input: " + ex.getMessage());
            }
        });

        Button backBtn = new Button("Back");
        backBtn.setPrefWidth(120);
        backBtn.setStyle(
                "-fx-font-size: 12; -fx-background-color: #2d6a4f; -fx-text-fill: #f4a261; -fx-border-color: #f4a261; -fx-border-width: 1;");
        backBtn.setOnAction(e -> showModeSelection());

        buttonBox.getChildren().addAll(joinBtn, backBtn);

        ScrollPane scrollPane = new ScrollPane(new VBox(10, hostBox, portBox, nameBox));
        scrollPane.setStyle("-fx-control-inner-background: #1b4332; -fx-text-fill: #f4a261;");
        scrollPane.setFitToWidth(true);

        root.getChildren().addAll(title, scrollPane, buttonBox);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
    }

    private HBox createInputBox(String label, String defaultValue) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(150);
        lbl.setStyle("-fx-text-fill: #f4a261; -fx-font-size: 12;");

        TextField field = new TextField(defaultValue);
        field.setPrefWidth(150);
        field.setStyle("-fx-font-size: 12; -fx-control-inner-background: #2d6a4f; -fx-text-fill: #f4a261;");

        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void startHostMode(int port, int sb, int bb, int numPlayers, String hostName) {
        try {
            // Create and start the server
            MultiplayerGameController controller = new MultiplayerGameController(port, sb, bb);
            controller.addHostPlayer(hostName); // Add host player with the provided name
            controller.startServer();

            // Close launcher and show waiting dialog
            primaryStage.close();
            showWaitingDialog("Hosting game on port " + port, "Waiting for " + numPlayers + " players to join...");

            // Wait for players in a separate thread
            Thread waitThread = new Thread(() -> {
                try {
                    controller.waitForPlayers(numPlayers, 120000); // 2 minutes timeout

                    // All players joined! Now open the game GUI on the JavaFX thread
                    Platform.runLater(() -> {
                        try {
                            closeWaitingDialog();

                            PokerGUI gui = new PokerGUI();
                            gui.initializeWithGame(controller.getGame());
                            gui.buildUI();

                            Stage gameStage = new Stage();
                            Scene gameScene = new Scene(gui.getRoot(), 1200, 800);
                            gameStage.setTitle("Texas Hold'em Poker - Host");
                            gameStage.setScene(gameScene);
                            gameStage.show();

                            // Start the game
                            controller.startGameWithGUI();
                        } catch (Exception ex) {
                            showError("GUI Error: " + ex.getMessage());
                        }
                    });

                } catch (InterruptedException ex) {
                    Platform.runLater(() -> {
                        closeWaitingDialog();
                        showError("Timeout waiting for players. Please try again.");
                        showModeSelection();
                    });
                }
            });
            waitThread.setDaemon(false);
            waitThread.start();

        } catch (Exception ex) {
            showError("Server Error: " + ex.getMessage());
        }
    }

    private class OpponentInfo {
        int id;
        String name;

        OpponentInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private void startClientMode(String host, int port, String name) {
        try {
            primaryStage.close();
            showWaitingDialog("Connecting to " + host + ":" + port, "Waiting for game to start...");

            // Create GUI
            PokerGUI gui = new PokerGUI();
            gui.initializeForNetwork();

            // Store reference for inner class
            PokerGUI finalGui = gui;

            // Create client with listener
            PokerClient client = new PokerClient(host, port, name, new PokerClientEventHandler.Listener() {

                private boolean guiReady = false;
                private List<Card> pendingHoleCards = null;
                private List<Card> pendingCommunityCards = null;
                private int pendingCurrentBet = 0;
                private int pendingPot = 0;
                private String pendingStreet = "PRE-FLOP";
                private int pendingPlayerId = -1;
                private int pendingChips = 0;
                private List<String> pendingMessages = new ArrayList<>();
                // Store pending opponent chip updates: playerId -> chips
                private java.util.Map<Integer, Integer> pendingOpponentChips = new java.util.HashMap<>();
                private List<OpponentInfo> pendingOpponents = new ArrayList<>();

                

                @Override
                public void onGameInfo(int playerId, String playerName, int chips) {
                    System.out.println("Game info - Player ID: " + playerId + ", Chips: " + chips);
                    pendingPlayerId = playerId;
                    pendingChips = chips;

                    // Set playerId in GUI IMMEDIATELY (even before GUI is built)
                    finalGui.setPlayerId(playerId);

                    // Also store chips
                    finalGui.onChips(chips);
                }

                @Override
                public void onHoleCards(List<Card> cards) {
                    System.out.println("Received hole cards: " + cards);
                    if (cards != null && !cards.isEmpty()) {
                        if (guiReady) {
                            // GUI is ready, update immediately
                            javafx.application.Platform.runLater(() -> {
                                finalGui.updateHoleCards(cards);
                            });
                        } else {
                            // GUI not ready yet, store for later
                            pendingHoleCards = cards;
                            System.out.println("Stored hole cards for later: " + cards);
                        }
                    }
                }

                @Override
                public void onCommunityCards(List<Card> cards) {
                    System.out.println("Received community cards: " + cards);
                    if (guiReady) {
                        finalGui.updateCommunityCards(cards);
                    } else {
                        pendingCommunityCards = cards;
                    }
                }

                @Override
                public void onHandEnded() {
                    System.out.println("Hand ended - waiting for next hand");
                    // You might want to clear the board, but keep player chips
                    finalGui.appendActionLog("--- Hand ended ---", PokerGUI.LogType.INFO);
                }

                @Override
                public void onGameStart() {
                    System.out.println("GAME_START received! Opening GUI...");
                    guiReady = true;

                    javafx.application.Platform.runLater(() -> {
                        try {
                            closeWaitingDialog();

                            // Build UI FIRST
                            finalGui.buildUI();
                            System.out.println("UI built");

                            // Add host (player 0) as an opponent
                            // finalGui.addOpponent(0, "Host");
                            // System.out.println("Added host as opponent");

                            // Add any pending opponents that joined before GUI was ready
                            if (pendingOpponents != null && !pendingOpponents.isEmpty()) {
                                System.out.println("Adding " + pendingOpponents.size() + " pending opponents...");
                                for (OpponentInfo opponent : pendingOpponents) {
                                    // Don't filter here - add all opponents
                                    System.out.println(
                                            "Adding opponent: " + opponent.name + " (ID: " + opponent.id + ")");
                                    finalGui.addOpponent(opponent.id, opponent.name);
                                }
                                pendingOpponents.clear();
                            }

                            // Apply player info
                            if (pendingPlayerId != -1) {
                                finalGui.setPlayerId(pendingPlayerId);
                                System.out.println("Set player ID: " + pendingPlayerId);
                            }

                            // Apply THE LATEST chips (from onChipsUpdate, not the initial 1000)
                            // pendingChips should have been updated by onChipsUpdate to 990
                            if (pendingChips > 0 && pendingChips != 1000) {
                                finalGui.updateMyChips(pendingChips);
                                System.out.println("Applied pending chips: " + pendingChips);
                            } else if (pendingChips == 1000) {
                                // If still 1000, maybe no CHIPS_UPDATE arrived yet
                                System.out.println("Waiting for chip updates, not applying initial 1000");
                            }

                            // Apply any pending opponent chip updates
                            if (!pendingOpponentChips.isEmpty()) {
                                for (java.util.Map.Entry<Integer, Integer> entry : pendingOpponentChips.entrySet()) {
                                    int oppId = entry.getKey();
                                    int oppChips = entry.getValue();
                                    System.out.println("Applying pending opponent chips: playerId=" + oppId + ", chips=" + oppChips);
                                    finalGui.updateOpponentChips(oppId, oppChips);
                                }
                                pendingOpponentChips.clear();
                            }

                            // Apply hole cards after UI is built
                            if (pendingHoleCards != null && !pendingHoleCards.isEmpty()) {
                                System.out.println("Applying hole cards: " + pendingHoleCards);
                                finalGui.updateHoleCards(pendingHoleCards);
                            }

                            // Apply community cards
                            if (pendingCommunityCards != null && !pendingCommunityCards.isEmpty()) {
                                System.out.println("Applying community cards: " + pendingCommunityCards);
                                finalGui.updateCommunityCards(pendingCommunityCards);
                            }

                            if (pendingCurrentBet > 0) {
                                finalGui.enablePlayerTurn(pendingCurrentBet);
                            }

                            Stage gameStage = new Stage();
                            Scene gameScene = new Scene(finalGui.getRoot(), 1200, 800);
                            gameStage.setTitle("Texas Hold'em Poker - " + name);
                            gameStage.setScene(gameScene);
                            gameStage.show();

                            System.out.println("Game stage shown for " + name);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showError("GUI Error: " + ex.getMessage());
                        }
                    });
                }

                @Override
                public void onJoined(int playerId, String playerName) {
                    System.out.println("Joined as player " + playerId);
                    pendingPlayerId = playerId; // ADD THIS LINE
                    finalGui.setPlayerId(playerId);
                    finalGui.appendActionLog("You joined as " + playerName, PokerGUI.LogType.INFO);
                }

                @Override
                public void onPlayerJoined(int playerId, String playerName) {
                    System.out.println("Player joined: " + playerName + " (ID: " + playerId + ")");

                    // If this is the first player we hear about (pendingPlayerId not set yet),
                    // assume it's us and set it
                    if (pendingPlayerId == -1 && playerId > 0) {
                        System.out.println("Setting pendingPlayerId from onPlayerJoined: " + playerId);
                        pendingPlayerId = playerId;
                    }

                    // Skip adding yourself as an opponent
                    if (playerId == pendingPlayerId) {
                        System.out.println("Skipping adding self as opponent: " + playerName);
                        return;
                    }

                    finalGui.appendActionLog(playerName + " joined the table", PokerGUI.LogType.INFO);

                    if (guiReady) {
                        javafx.application.Platform.runLater(() -> {
                            finalGui.addOpponent(playerId, playerName);
                        });
                    } else {
                        pendingOpponents.add(new OpponentInfo(playerId, playerName));
                        System.out.println("Stored pending opponent: " + playerName + " (ID: " + playerId + ")");
                    }
                }

                @Override
                public void onPlayerLeft(int playerId) {
                    String msg = "Player " + playerId + " left the table";
                    System.out.println(msg);
                    if (guiReady) {
                        finalGui.appendActionLog(msg, PokerGUI.LogType.INFO);
                    } else {
                        pendingMessages.add(msg);
                    }
                }

                @Override
                public void onStreet(String street) {
                    System.out.println("Street: " + street);
                    pendingStreet = street;
                    if (guiReady) {
                        finalGui.displayStreet(street);
                        finalGui.appendActionLog("--- " + street + " ---", PokerGUI.LogType.TURN);
                    }
                }

                @Override
                public void onPot(int pot) {
                    System.out.println("Pot: " + pot);
                    pendingPot = pot;
                    if (guiReady) {
                        finalGui.updatePot(pot);
                    }
                }

                @Override
                public void onChipsUpdate(int playerId, int chips) {
                    System.out.println("=== onChipsUpdate CALLED ===");
                    System.out.println("[onChipsUpdate] Player " + playerId + " chips: " + chips);
                    System.out.println("[onChipsUpdate] pendingPlayerId: " + pendingPlayerId);
                    System.out.println("[onChipsUpdate] guiReady: " + guiReady);

                    if (playerId == pendingPlayerId) {
                        System.out.println("[onChipsUpdate] Updating MY chips to: " + chips);
                        pendingChips = chips;

                        if (guiReady) {
                            javafx.application.Platform.runLater(() -> {
                                System.out.println("[onChipsUpdate] Calling finalGui.updateMyChips(" + chips + ")");
                                finalGui.updateMyChips(chips);
                            });
                        } else {
                            System.out.println("[onChipsUpdate] GUI not ready, storing chips for later");
                        }
                    } else {
                        System.out.println("[onChipsUpdate] Updating OPPONENT " + playerId + " chips to: " + chips);
                        if (guiReady) {
                            javafx.application.Platform.runLater(() -> {
                                System.out.println("[onChipsUpdate] Calling finalGui.updateOpponentChips(" + playerId + ", " + chips + ")");
                                finalGui.updateOpponentChips(playerId, chips);
                            });
                        } else {
                            System.out.println("[onChipsUpdate] GUI not ready, storing opponent chips for later");
                            pendingOpponentChips.put(playerId, chips);
                        }
                    }
                }

                @Override
                public void onBoard(List<String> cards) {
                    System.out.println("=== onBoard CALLED ===");
                    System.out.println("Board raw: " + cards);

                    List<Card> cardObjects = new ArrayList<>();

                    // The cards list may have the whole board as one string or multiple strings
                    for (String cardStr : cards) {
                        // Find all card patterns in the string
                        // Look for "rank=" and extract until the next comma or bracket
                        int index = 0;
                        while (index < cardStr.length()) {
                            int rankStart = cardStr.indexOf("rank=", index);
                            if (rankStart == -1)
                                break;
                            rankStart += 5; // skip "rank="

                            int rankEnd = cardStr.indexOf(",", rankStart);
                            if (rankEnd == -1)
                                rankEnd = cardStr.indexOf("]", rankStart);
                            if (rankEnd == -1)
                                rankEnd = cardStr.length();
                            String rankStr = cardStr.substring(rankStart, rankEnd).trim();
                            rankStr = rankStr.replace("[", "").replace("]", "").trim();

                            int suitStart = cardStr.indexOf("suit=", rankEnd);
                            if (suitStart == -1)
                                break;
                            suitStart += 5; // skip "suit="

                            int suitEnd = cardStr.indexOf(",", suitStart);
                            if (suitEnd == -1)
                                suitEnd = cardStr.indexOf("]", suitStart);
                            if (suitEnd == -1)
                                suitEnd = cardStr.length();
                            String suitStr = cardStr.substring(suitStart, suitEnd).trim();
                            suitStr = suitStr.replace("[", "").replace("]", "").trim();

                            try {
                                Card.Rank rank = Card.Rank.valueOf(rankStr.toUpperCase());
                                Card.Suit suit = Card.Suit.valueOf(suitStr.toUpperCase());
                                cardObjects.add(new Card(rank, suit));
                                System.out.println("Parsed card: " + rank + " of " + suit);
                            } catch (IllegalArgumentException e) {
                                System.err.println("Failed to parse: rank=" + rankStr + ", suit=" + suitStr);
                            }

                            index = suitEnd;
                        }
                    }

                    if (!cardObjects.isEmpty()) {
                        System.out.println(
                                "Updating community cards with " + cardObjects.size() + " cards: " + cardObjects);
                        finalGui.onCommunityCards(cardObjects);
                    } else {
                        System.out.println("No cards were parsed!");
                    }
                }

                private String extractValue(String text, String key) {
                    int start = text.indexOf(key);
                    if (start == -1)
                        return null;
                    start += key.length();
                    int end = text.indexOf(",", start);
                    if (end == -1)
                        end = text.indexOf("]", start);
                    if (end == -1)
                        end = text.length();
                    String value = text.substring(start, end).trim();
                    // Remove any brackets or extra spaces
                    value = value.replace("[", "").replace("]", "").trim();
                    return value.isEmpty() ? null : value;
                }

                @Override
                public void onTurn(int currentBet, int pot, List<String> communityCards) {
                    System.out.println("Your turn! Current bet: " + currentBet);
                    pendingCurrentBet = currentBet;
                    pendingPot = pot;

                    if (guiReady) {
                        finalGui.enablePlayerTurn(currentBet);
                        finalGui.appendActionLog("Your turn - Current bet: " + currentBet, PokerGUI.LogType.TURN);
                    }
                }

                @Override
                public void onInfo(String message) {
                    System.out.println("Info: " + message);

                    // Check if this is a chips message
                    if (message.startsWith("Your chips: ")) {
                        String chipsStr = message.substring("Your chips: ".length());
                        try {
                            int chips = Integer.parseInt(chipsStr);
                            finalGui.updateMyChips(chips);
                        } catch (NumberFormatException e) {
                            // Not a number, just log normally
                            finalGui.appendActionLog(message, PokerGUI.LogType.INFO);
                        }
                    } else {
                        finalGui.appendActionLog(message, PokerGUI.LogType.INFO);
                    }
                }

                

                @Override
                public void onChat(int playerId, String message) {
                    String msg = "Player " + playerId + ": " + message;
                    System.out.println(msg);
                    if (guiReady) {
                        finalGui.appendActionLog(msg, PokerGUI.LogType.INFO);
                    } else {
                        pendingMessages.add(msg);
                    }
                }

                @Override
                public void onError(String message) {
                    System.err.println("Error: " + message);
                    if (guiReady) {
                        finalGui.appendActionLog("ERROR: " + message, PokerGUI.LogType.INFO);
                    } else {
                        pendingMessages.add("ERROR: " + message);
                    }
                    javafx.application.Platform.runLater(() -> {
                        closeWaitingDialog();
                        showError("Connection Error: " + message);
                        showModeSelection();
                    });
                }

                private Card parseCardFromString(String cardStr) {
                    try {
                        if (cardStr == null)
                            return null;

                        // Handle format: "Card [rank=TWO, suit=SPADES]"
                        int rankStart = cardStr.indexOf("rank=");
                        if (rankStart == -1)
                            return null;
                        rankStart += 5;
                        int rankEnd = cardStr.indexOf(",", rankStart);
                        if (rankEnd == -1)
                            rankEnd = cardStr.indexOf("]", rankStart);
                        if (rankEnd == -1)
                            return null;
                        String rankStr = cardStr.substring(rankStart, rankEnd).trim();

                        int suitStart = cardStr.indexOf("suit=");
                        if (suitStart == -1)
                            return null;
                        suitStart += 5;
                        int suitEnd = cardStr.indexOf("]", suitStart);
                        if (suitEnd == -1)
                            suitEnd = cardStr.indexOf(",", suitStart);
                        if (suitEnd == -1)
                            return null;
                        String suitStr = cardStr.substring(suitStart, suitEnd).trim();

                        rankStr = rankStr.replace("[", "").replace("]", "").trim().toUpperCase();
                        suitStr = suitStr.replace("[", "").replace("]", "").trim().toUpperCase();

                        Card.Rank rank = Card.Rank.valueOf(rankStr);
                        Card.Suit suit = Card.Suit.valueOf(suitStr);
                        return new Card(rank, suit);
                    } catch (Exception e) {
                        System.err.println("Failed to parse card: " + cardStr + " - " + e.getMessage());
                        return null;
                    }
                }
            });

            client.connect();
            finalGui.setPokerClient(client);
            System.out.println("Client connected to " + host + ":" + port);

        } catch (Exception ex) {
            ex.printStackTrace();
            closeWaitingDialog();
            showError("Connection Error: " + ex.getMessage());
            showModeSelection();
        }
    }



    private void showWaitingDialog(String title, String message) {
        Platform.runLater(() -> {
            waitingStage = new Stage();
            waitingStage.setTitle(title);
            waitingStage.setResizable(false);

            VBox root = new VBox(20);
            root.setPadding(new Insets(30));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #1b4332;");

            Label waitingLabel = new Label(message);
            waitingLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #f4a261;");

            ProgressIndicator progress = new ProgressIndicator();
            progress.setStyle("-fx-progress-color: #f4a261;");

            root.getChildren().addAll(progress, waitingLabel);

            Scene scene = new Scene(root, 350, 200);
            waitingStage.setScene(scene);
            waitingStage.show();
        });
    }

    private void closeWaitingDialog() {
        Platform.runLater(() -> {
            if (waitingStage != null) {
                waitingStage.close();
                waitingStage = null;
            }
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}