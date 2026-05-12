package com.kevin.poker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.kevin.poker.network.PokerClientEventHandler;

import java.util.*;

public class PokerGUI extends Application implements PokerClientEventHandler.Listener {

    public enum LogType {
        INFO,
        CHECK,
        CALL,
        RAISE,
        FOLD,
        WIN,
        TURN
    }

    public static class LogEntry {
        private final String message;
        private final LogType type;

        public LogEntry(String message, LogType type) {
            this.message = message;
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public LogType getType() {
            return type;
        }
    }
    
    // Game state
    protected PokerGame game;
    protected int playerId;  // 0 = human, 1,2,3 = AI opponents
    protected List<Player> players;
    
    // UI Components
    protected VBox root;
    protected HBox tableArea;
    protected VBox playerArea;
    protected HBox communityArea;
    protected HBox buttonArea;
    
    // Player-specific elements
    protected HBox playerHand;
    protected Text potText;
    protected Text playerChipsText;
    protected Text gameStatusText;
    protected Text currentTurnText;
    private ListView<LogEntry> actionLogView;
    private int displayedPot;
    private List<Card> communityCards;
    
    // Street banner and opponent boxes
    protected Text streetBannerText;
    protected List<VBox> opponentBoxes;
    private final String basePlayerAreaStyle = "-fx-background-color: #2d6a4f; -fx-border-color: #d4a373; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;";
    private final String glowingPlayerAreaStyle = "-fx-background-color: #2d6a4f; -fx-border-color: #f4a261; -fx-border-width: 4; -fx-border-radius: 20; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, #f4a261, 25, 0.75, 0, 0);";
    
    // Betting controls
    protected Button checkCallButton;
    protected Button foldButton;
    protected Button raiseButton;
    protected Slider raiseSlider;
    private Label raiseAmountLabel;
    private Label currentBetLabel;

    // Network mode flag    private boolean isNetworkMode = false;
    private boolean isNetworkMode = false;
    private com.kevin.poker.network.PokerClient pokerClient;
    private String pendingHostName = null;
    private List<Card> pendingCommunityCards;
    private int pendingPlayerChips = -1;

    public void setPokerClient(com.kevin.poker.network.PokerClient client) {
        this.pokerClient = client;
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize game
        initializeGame();
        
        // Build UI
        buildUI();
        
        // Set up the scene
        Scene scene = new Scene(root, 1200, 800);
        scene.getRoot().setStyle("-fx-background-color: #1a472a;");
        
        primaryStage.setTitle("Texas Hold'em Poker");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start the first hand
        startNewHand();
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        System.out.println("PokerGUI: Player ID set to " + playerId);
    }

    // Also add this method to store player ID before GUI is ready
    public void setPendingPlayerId(int playerId) {
        this.playerId = playerId;
        System.out.println("PokerGUI: Pending player ID set to " + playerId);
    }

    public void updateHoleCards(List<Card> cards) {
        System.out.println("Updating hole cards: " + cards);
        if (playerHand == null) {
            System.out.println("playerHand is null, storing cards for later");
            return;
        }
        if (cards == null || cards.isEmpty()) {
            return;
        }
        Platform.runLater(() -> {
            playerHand.getChildren().clear();
            for (Card card : cards) {
                playerHand.getChildren().add(createCardNode(card));
            }
            System.out.println("Hole cards displayed: " + playerHand.getChildren().size() + " cards");
        });
    }
    
    public void updateCommunityCards() {
        System.out.println("updateCommunityCards() called - no args");
        if (!isNetworkMode && game != null) {
            updateCommunityCards(new ArrayList<>(game.getCommunityCards()));
            return;
        }
        updateCommunityCards(communityCards);
    }

    public void updateCommunityCards(List<Card> cards) {
        System.out.println("updateCommunityCards called with " + (cards != null ? cards.size() : 0) + " cards");
        this.communityCards = (cards == null) ? new ArrayList<>() : new ArrayList<>(cards);

        // Store for later if UI not ready
        if (communityArea == null) {
            System.out.println("communityArea is null, storing for later");
            pendingCommunityCards = new ArrayList<>(this.communityCards);
            return;
        }

        // Update UI
        Platform.runLater(() -> {
            communityArea.getChildren().clear();
            for (Card card : this.communityCards) {
                System.out.println("Adding card to community area: " + card);
                communityArea.getChildren().add(createCardNode(card));
            }
            System.out.println("Community cards now: " + communityArea.getChildren().size());
        });
    }

    public void updatePot(int pot) {
        this.displayedPot = pot;
        if (potText != null) {
            Platform.runLater(() -> potText.setText("Pot: " + pot));
        }
    }

    public void initializeWithGame(PokerGame externalGame) {
        this.game = externalGame;
        this.game.setUI(this);
        this.players = externalGame.getPlayers();
        this.playerId = 0;
        this.opponentBoxes = new ArrayList<>();
    }

    public void initializeForNetwork() {
        this.isNetworkMode = true;
        this.players = new ArrayList<>(); // Empty, will be populated by network
        this.playerId = -1;
        this.communityCards = new ArrayList<>(); // Initialize community cards list
        this.opponentBoxes = new ArrayList<>(); // IMPORTANT: Initialize the list!
        this.pendingCommunityCards = new ArrayList<>(); // <-- ADD THIS 
        this.game = null; // Game will be managed by server
        this.playerId = -1; // Will be set when joining
        this.pokerClient = null; // Will be set later
    }
    
    public void initializeGame() {
        // Create players (for demo: 1 human, 3 AI)
        players = new ArrayList<>();
        players.add(new Player(0, "You", 1000));
        players.add(new Player(1, "AI Bot 1", 1000));
        players.add(new Player(2, "AI Bot 2", 1000));
        players.add(new Player(3, "AI Bot 3", 1000));
        
        game = new PokerGame(10, 20);
        for (Player p : players) {
            game.addPlayer(p);
        }
        // Register this GUI with the game so it can notify UI events
        game.setUI(this);
        
        playerId = 0;  // Human is player 0
        opponentBoxes = new ArrayList<>();
        this.communityCards = new ArrayList<>();
        this.pendingCommunityCards = new ArrayList<>(); 
    }
    
    public void buildUI() {
        root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a472a;");

        // Create playerChipsText EARLY (KEEP THIS)
        playerChipsText = new Text("Chips: 0");
        playerChipsText.setFill(Color.WHITE);
        playerChipsText.setFont(Font.font(16));

        // Apply pending chips if any
        if (pendingPlayerChips >= 0) {
            playerChipsText.setText("Chips: " + pendingPlayerChips);
            pendingPlayerChips = -1;
        }

        // Ensure opponentBoxes exists
        if (opponentBoxes == null) {
            opponentBoxes = new ArrayList<>();
        }
        
        // Game status bar
        HBox statusBar = new HBox(20);
        statusBar.setAlignment(Pos.CENTER);
        gameStatusText = new Text("Game in progress");
        gameStatusText.setFill(Color.WHITE);
        gameStatusText.setFont(Font.font(18));
        currentTurnText = new Text("Turn: Waiting for hand to start");
        currentTurnText.setFill(Color.WHITE);
        currentTurnText.setFont(Font.font(16));
        potText = new Text("Pot: 0");
        potText.setFill(Color.WHITE);
        potText.setFont(Font.font(16));
        statusBar.getChildren().addAll(gameStatusText, currentTurnText, potText);
        
        // Table area with street banner
        VBox tableContainer = new VBox(10);
        tableContainer.setAlignment(Pos.TOP_CENTER);
        
        // Street banner
        HBox bannerBox = new HBox();
        bannerBox.setAlignment(Pos.CENTER);
        streetBannerText = new Text("PRE-FLOP");
        streetBannerText.setFill(Color.web("#f4a261"));
        streetBannerText.setFont(Font.font("Arial", 42));
        streetBannerText.setStyle("-fx-font-weight: bold;");
        bannerBox.getChildren().add(streetBannerText);
        
        // Table area (community cards + opponents)
        tableArea = new HBox(20);
        tableArea.setAlignment(Pos.CENTER);
        tableArea.setStyle("-fx-background-color: #2d6a4f; -fx-border-color: #d4a373; -fx-border-width: 3; -fx-border-radius: 200; -fx-background-radius: 200;");
        tableArea.setPrefHeight(400);
        
        // Community cards area
        communityArea = new HBox(10);
        communityArea.setAlignment(Pos.CENTER);
        communityArea.setPrefWidth(400);

        if (pendingCommunityCards != null && !pendingCommunityCards.isEmpty()) {
            System.out.println("Applying pending community cards: " + pendingCommunityCards);
            for (Card card : pendingCommunityCards) {
                communityArea.getChildren().add(createCardNode(card));
            }
            pendingCommunityCards = null;
        }
        
        // Opponents (top, left, right positions - simplified for now)
        if (players != null) {
            for (int i = 1; i <= 3 && i < players.size(); i++) {
                VBox opponentBox = createOpponentBox(players.get(i), i);
                opponentBoxes.add(opponentBox);
                tableArea.getChildren().add(opponentBox);
            }
        } else {
            // Network mode: create empty opponent boxes that will be populated dynamically
            for (int i = 0; i < 3; i++) {
                VBox opponentBox = new VBox();
                opponentBox.setStyle("-fx-background-color: #1b4332; -fx-border-color: #d4a373; -fx-border-width: 2; -fx-padding: 10;");
                opponentBox.setPrefWidth(120);
                opponentBox.setPrefHeight(100);
                opponentBox.setAlignment(Pos.CENTER);
                opponentBoxes.add(opponentBox);
                tableArea.getChildren().add(opponentBox);
            }
        }
        tableArea.getChildren().add(communityArea);
        
        tableContainer.getChildren().addAll(bannerBox, tableArea);
        
        // Player area (bottom)
        playerArea = new VBox(10);
        playerArea.setAlignment(Pos.CENTER);
        playerArea.setStyle(basePlayerAreaStyle);
        playerArea.setPadding(new Insets(15));
        
        // Player hand
        playerHand = new HBox(10);
        playerHand.setAlignment(Pos.CENTER);
        
        // Player info
        HBox playerInfo = new HBox(20);
        playerInfo.setAlignment(Pos.CENTER);
        playerChipsText.setFill(Color.WHITE);
        playerChipsText.setFont(Font.font(16));
        currentBetLabel = new Label("Current bet: 0");
        currentBetLabel.setTextFill(Color.WHITE);
        playerInfo.getChildren().addAll(playerChipsText, currentBetLabel);
        
        // Betting buttons
        buttonArea = new HBox(15);
        buttonArea.setAlignment(Pos.CENTER);
        
        checkCallButton = new Button("Check/Call");
        checkCallButton.setStyle("-fx-font-size: 14; -fx-min-width: 100;");
        checkCallButton.setOnAction(e -> onCheckCall());
        
        foldButton = new Button("Fold");
        foldButton.setStyle("-fx-font-size: 14; -fx-min-width: 100; -fx-background-color: #dc2f02; -fx-text-fill: white;");
        foldButton.setOnAction(e -> onFold());
        
        raiseButton = new Button("Raise");
        raiseButton.setStyle("-fx-font-size: 14; -fx-min-width: 100; -fx-background-color: #f4a261;");
        raiseButton.setOnAction(e -> onRaise());
        
        raiseSlider = new Slider(20, 500, 100);
        raiseSlider.setShowTickLabels(true);
        raiseSlider.setMajorTickUnit(100);
        raiseSlider.setBlockIncrement(20);
        
        raiseAmountLabel = new Label("Raise to: 100");
        raiseAmountLabel.setTextFill(Color.WHITE);
        raiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            raiseAmountLabel.setText("Raise to: " + newVal.intValue())
        );
        
        VBox raiseBox = new VBox(5, raiseAmountLabel, raiseSlider);
        raiseBox.setAlignment(Pos.CENTER);
        
        buttonArea.getChildren().addAll(checkCallButton, foldButton, raiseButton, raiseBox);
        
        playerArea.getChildren().addAll(playerInfo, playerHand, buttonArea);

        // Action log
        actionLogView = new ListView<>();
        actionLogView.setPrefHeight(180);
        actionLogView.setStyle("-fx-control-inner-background: #102b20; -fx-background-color: #102b20;");
        actionLogView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item.getMessage());
                setTextFill(Color.WHITE);
                switch (item.getType()) {
                    case CHECK:
                        setStyle("-fx-background-color: #1b4332; -fx-text-fill: white;");
                        break;
                    case CALL:
                        setStyle("-fx-background-color: #264653; -fx-text-fill: white;");
                        break;
                    case RAISE:
                        setStyle("-fx-background-color: #b8860b; -fx-text-fill: black;");
                        break;
                    case FOLD:
                        setStyle("-fx-background-color: #8b1e3f; -fx-text-fill: white;");
                        break;
                    case WIN:
                        setStyle("-fx-background-color: #2a9d8f; -fx-text-fill: white;");
                        break;
                    case TURN:
                        setStyle("-fx-background-color: #3a506b; -fx-text-fill: white;");
                        break;
                    default:
                        setStyle("-fx-background-color: #102b20; -fx-text-fill: white;");
                        break;
                }
            }
        });
        
        root.getChildren().addAll(statusBar, tableContainer, playerArea, actionLogView);
        
        // Initially disable buttons until player's turn
        disableButtons(true);
    }

    public VBox getRoot() {
        return root;
    }
    
    private VBox createOpponentBox(Player opponent, int index) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10;");
        box.setUserData(opponent.getId()); // Set the player ID here

        Text nameText = new Text(opponent.getName());
        nameText.setFill(Color.WHITE);

        Text chipsText = new Text(opponent.getChips() + " chips");
        chipsText.setFill(Color.YELLOW);

        HBox cards = new HBox(5);
        for (int i = 0; i < 2; i++) {
            Rectangle cardBack = new Rectangle(50, 70);
            cardBack.setFill(Color.NAVY);
            cardBack.setStroke(Color.WHITE);
            cardBack.setArcWidth(10);
            cardBack.setArcHeight(10);
            cards.getChildren().add(cardBack);
        }

        box.getChildren().addAll(nameText, chipsText, cards);
        return box;
    }
    
    private void updateUI() {
        Platform.runLater(() -> {
            // Update player chips - only in local mode or if players list has data
            if (!isNetworkMode && players != null && playerId >= 0 && playerId < players.size()) {
                playerChipsText.setText("Chips: " + players.get(playerId).getChips());
            }

            // Update pot
            potText.setText("Pot: " + displayedPot);

            // Update player's hand - only in local mode or if we have hole cards from
            // network
            if (!isNetworkMode && players != null && playerId >= 0 && playerId < players.size()) {
                playerHand.getChildren().clear();
                for (Card card : players.get(playerId).getHoleCards()) {
                    playerHand.getChildren().add(createCardNode(card));
                }
            }

            // Update community cards - use stored communityCards for network mode
            if (isNetworkMode) {
                communityArea.getChildren().clear();
                if (communityCards != null) {
                    for (Card card : communityCards) {
                        communityArea.getChildren().add(createCardNode(card));
                    }
                }
            } else if (game != null) {
                communityArea.getChildren().clear();
                for (Card card : game.getCommunityCards()) {
                    communityArea.getChildren().add(createCardNode(card));
                }
            }

            // Update opponent displays - only in local mode
            if (!isNetworkMode && players != null && opponentBoxes != null) {
                for (int i = 0; i < opponentBoxes.size(); i++) {
                    int playerIndex = i + 1;
                    if (playerIndex < players.size()) {
                        VBox opponentBox = opponentBoxes.get(i);
                        if (opponentBox != null && opponentBox.getChildren().size() > 1) {
                            Text chipsText = (Text) opponentBox.getChildren().get(1);
                            chipsText.setText(players.get(playerIndex).getChips() + " chips");
                        }
                    }
                }
            }
        });
    }
    
    private StackPane createCardNode(Card card) {
        System.out.println("Creating card node for: " + card);
        StackPane cardNode = new StackPane();
        cardNode.setPrefSize(60, 85);
        cardNode.setStyle(
                "-fx-background-color: white; -fx-border-color: black; -fx-border-radius: 5; -fx-background-radius: 5;");

        String rankStr = card.getRank().toString();
        String suitStr = card.getSuit().toString().substring(0, 1);
        Color suitColor = (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS)
                ? Color.RED
                : Color.BLACK;

        Text rankText = new Text(rankStr + suitStr);
        rankText.setFont(Font.font(20));
        rankText.setFill(suitColor);

        cardNode.getChildren().add(rankText);
        return cardNode;
    }
    
    protected void disableButtons(boolean disabled) {
        Platform.runLater(() -> {
            checkCallButton.setDisable(disabled);
            foldButton.setDisable(disabled);
            raiseButton.setDisable(disabled);
            raiseSlider.setDisable(disabled);
        });
    }
    
    protected void onCheckCall() {
        if (isNetworkMode) {
            // Send to server via PokerClient (need reference)
            if (pokerClient != null) {
                pokerClient.sendCommand("ACTION CALL");
            }
        } else {
            // Local mode - send to local game
            game.receivePlayerAction(playerId, new Action("CALL", 0));
        }
        clearHumanHighlight();
        disableButtons(true);
    }

    protected void onFold() {
        if (isNetworkMode) {
            if (pokerClient != null) {
                pokerClient.sendCommand("ACTION FOLD");
            }
        } else {
            game.receivePlayerAction(playerId, new Action("FOLD", 0));
        }
        clearHumanHighlight();
        disableButtons(true);
    }

    protected void onRaise() {
        int amount = (int) raiseSlider.getValue();
        if (isNetworkMode) {
            if (pokerClient != null) {
                pokerClient.sendCommand("ACTION RAISE " + amount);
            }
        } else {
            game.receivePlayerAction(playerId, new Action("RAISE", amount));
        }
        clearHumanHighlight();
        disableButtons(true);
    }
    
    
    private void startNewHand() {
        updateUI();
        Platform.runLater(() -> {
            actionLogView.getItems().clear();
            appendActionLog("--- New hand started ---", LogType.INFO);
            currentTurnText.setText("Turn: Dealing cards...");
            clearPlayerHighlight();
            clearHumanHighlight();
            displayStreet("PRE-FLOP");
        });
        // Start the hand logic in a separate thread so UI stays responsive
        new Thread(() -> {
            game.startHand();
            runGameLoop();
        }).start();
    }
    
    private void runGameLoop() {
        // This is where the game waits for player input
        // For now, we'll let the game engine handle turns and call back to UI
        
        // Simplified: For demo, just run through betting rounds
        // In reality, the PokerGame class should call a method like waitForPlayerAction()
    }
    
    public void enablePlayerTurn(int currentBet) {
        Platform.runLater(() -> {
            currentBetLabel.setText("Current bet: " + currentBet);
            disableButtons(false);
            currentTurnText.setText("Turn: Your move");

            clearPlayerHighlight();
            highlightHumanPlayer();

            // In network mode, currentBet IS the amount to call
            int toCall = currentBet;
            if (!isNetworkMode && players != null && playerId >= 0 && playerId < players.size()) {
                toCall = currentBet - players.get(playerId).getCurrentBet();
            }

            if (toCall == 0) {
                checkCallButton.setText("Check");
            } else {
                checkCallButton.setText("Call (" + toCall + ")");
            }

            gameStatusText.setText("Your turn!");
        });
    }
    
    public void updateGameStatus(String status) {
        Platform.runLater(() -> gameStatusText.setText(status));
    }

    public void updateTurnStatus(String status) {
        Platform.runLater(() -> currentTurnText.setText("Turn: " + status));
    }
    
    public void highlightCurrentPlayer(int playerIndex) {
        Platform.runLater(() -> {
            if (opponentBoxes == null)
                return;
            // Clear all highlights
            for (VBox box : opponentBoxes) {
                box.setStyle("-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10;");
            }
            
            // Highlight current player (if not human player 0)
            if (playerIndex > 0 && playerIndex <= opponentBoxes.size()) {
                opponentBoxes.get(playerIndex - 1).setStyle(
                    "-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10; " +
                    "-fx-border-color: #f4a261; -fx-border-width: 4; -fx-effect: dropshadow(gaussian, #f4a261, 15, 0.8, 0, 0);"
                );
            }
        });
    }
    
    public void clearPlayerHighlight() {
        Platform.runLater(() -> {
            for (VBox box : opponentBoxes) {
                box.setStyle("-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10;");
            }
        });
    }

    public void highlightHumanPlayer() {
        Platform.runLater(() -> playerArea.setStyle(glowingPlayerAreaStyle));
    }

    public void clearHumanHighlight() {
        Platform.runLater(() -> playerArea.setStyle(basePlayerAreaStyle));
    }
    
    public void displayStreet(String street) {
        Platform.runLater(() -> {
            streetBannerText.setText(street.toUpperCase());
            streetBannerText.setOpacity(1.0);
            updateUI();
        });
    }


    public void appendActionLog(String message) {
        appendActionLog(message, LogType.INFO);
    }

    public void appendActionLog(String message, LogType type) {
    Platform.runLater(() -> {
        if (actionLogView != null) {
            actionLogView.getItems().add(new LogEntry(message, type));
        }
    });
}

    // Called by PokerGame after showdown to display results
    public void showShowdown(Map<Player, HandRank> playerRanks, List<Player> winners) {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Showdown results:\n\n");
            for (Map.Entry<Player, HandRank> e : playerRanks.entrySet()) {
                sb.append(e.getKey().getName())
                  .append(": ")
                  .append(e.getValue().getType())
                  .append(" - ")
                  .append(e.getValue().toString())
                  .append("\n");
            }
            sb.append("\nWinner(s): ");
            for (int i = 0; i < winners.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(winners.get(i).getName());
            }

            updateUI();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Showdown");
            alert.setHeaderText("Round Result");
            alert.setContentText(sb.toString());
            alert.showAndWait();
            // After the user dismisses the showdown alert, start a new hand automatically
            startNewHand();
        });
    }

    // ===== Network Listener Implementation =====
    @Override
    public void onJoined(int playerId, String playerName) {
        // Player joined game
        this.playerId = playerId;
        Platform.runLater(() -> gameStatusText.setText("You joined as " + playerName));
    }

    @Override
    public void onPlayerJoined(int playerId, String playerName) {
        // Another player joined
        Platform.runLater(() -> appendActionLog(playerName + " joined the table", LogType.INFO));
    }

    @Override
    public void onPlayerLeft(int playerId) {
        // A player left
        Platform.runLater(() -> appendActionLog("Player left the table", LogType.INFO));
    }

    @Override
    public void onStreet(String street) {
        // Street changed (pre-flop, flop, turn, river)
        displayStreet(street);
    }

    @Override
    public void onPot(int pot) {
        this.displayedPot = pot;
        Platform.runLater(() -> potText.setText("Pot: " + pot));
    }

    @Override
    public void onBoard(List<String> cards) {
        // Convert string cards to Card objects for network mode
        List<Card> cardObjects = new ArrayList<>();
        for (String cardStr : cards) {
            String[] parts = cardStr.split(":");
            if (parts.length == 2) {
                try {
                    Card.Rank rank = Card.Rank.valueOf(parts[0]);
                    Card.Suit suit = Card.Suit.valueOf(parts[1]);
                    cardObjects.add(new Card(rank, suit));
                } catch (IllegalArgumentException e) {
                    // Skip invalid card
                }
            }
        }
        updateCommunityCards(cardObjects);
    }

    @Override
    public void onChips(int chips) {
        System.out.println("PokerGUI.onChips called with: " + chips);
        // If UI not ready, store for later
        if (playerChipsText == null) {
            pendingPlayerChips = chips;
            System.out.println("Stored pending chips: " + chips);
            return;
        }
        Platform.runLater(() -> {
            playerChipsText.setText("Chips: " + chips);
            System.out.println("Updated chips display to: " + chips);
        });
    }

    @Override
    public void onChipsUpdate(int playerId, int chips) {
        System.out.println("PokerGUI.onChipsUpdate called for player " + playerId + " -> " + chips);
        System.out.println("Current this.playerId = " + this.playerId);

        // Always update the display for the current player if playerId matches OR if
        // this is the first chip update
        if (this.playerId == -1 && playerId > 0) {
            System.out.println("Setting playerId from chip update: " + playerId);
            this.playerId = playerId;
            onChips(chips);
        } else if (playerId == this.playerId) {
            onChips(chips);
        } else {
            updateOpponentChips(playerId, chips);
        }
    }
    @Override
    public void onTurn(int currentBet, int pot, List<String> communityCards) {
        this.displayedPot = pot;
        // on your turn, you should already have the community cards from onBoard, so we can ignore the communityCards parameter here
        enablePlayerTurn(currentBet);
    }

    @Override
    public void onInfo(String message) {
        // Info message
        Platform.runLater(() -> appendActionLog(message, LogType.INFO));
    }

    @Override
    public void onChat(int playerId, String message) {
        // Chat message
        Platform.runLater(() -> appendActionLog("Player " + playerId + ": " + message, LogType.INFO));
    }

    @Override
    public void onError(String message) {
        // Error message
        Platform.runLater(() -> appendActionLog("ERROR: " + message, LogType.INFO));
    }

    public void setPlayerName(String name) {
        // Optional: Update a label showing your name
        System.out.println("Player name set to: " + name);
        Platform.runLater(() -> {
            if (gameStatusText != null) {
                gameStatusText.setText("Playing as: " + name);
            }
        });
    }
    
    public void addOpponent(int opponentId, String opponentName) {
        System.out.println("[addOpponent] Adding opponent: " + opponentName + " (ID: " + opponentId + ")");

        if (tableArea == null) {
            System.out.println("[addOpponent] Table area is null, can't add opponent");
            return;
        }

        // Create a temporary Player object for the opponent
        Player tempPlayer = new Player(opponentId, opponentName, 1000);

        // Use the existing createOpponentBox method
        VBox opponentBox = createOpponentBox(tempPlayer, opponentId);

        // IMPORTANT: Set the user data to the player ID for later lookup
        opponentBox.setUserData(opponentId);
        System.out.println("[addOpponent] Set userData for opponentBox to: " + opponentId);

        opponentBoxes.add(opponentBox);
        System.out.println("[addOpponent] opponentBoxes size is now: " + opponentBoxes.size());

        // Add to table area before community cards
        if (communityArea != null) {
            int communityIndex = tableArea.getChildren().indexOf(communityArea);
            tableArea.getChildren().add(communityIndex, opponentBox);
            System.out.println("[addOpponent] Added opponentBox at index " + communityIndex + " before communityArea");
        } else {
            tableArea.getChildren().add(opponentBox);
            System.out.println("[addOpponent] Added opponentBox at end of tableArea");
        }

        System.out.println("[addOpponent] Opponent added to table with ID: " + opponentId);
    }
    
    public void updateOpponentChips(int playerId, int chips) {
        Platform.runLater(() -> {
            boolean found = false;
            System.out.println("updateOpponentChips called for playerId=" + playerId + ", chips=" + chips);
            if (opponentBoxes == null || opponentBoxes.isEmpty()) {
                System.out.println("[updateOpponentChips] opponentBoxes is null or empty!");
            }
            for (VBox box : opponentBoxes) {
                Integer boxPlayerId = (Integer) box.getUserData();
                System.out.println("  Checking box with playerId=" + boxPlayerId);
                if (boxPlayerId != null && boxPlayerId == playerId) {
                    // Update chips text (it's the second child)
                    if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Text) {
                        Text chipsText = (Text) box.getChildren().get(1);
                        chipsText.setText(chips + " chips");
                        System.out.println("  Updated opponent chips for playerId=" + playerId + " to " + chips);
                    } else {
                        System.out.println("  Could not update chips: box children missing or not Text");
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("  [updateOpponentChips] No opponent box found for playerId=" + playerId);
            }
        });
    }

    public void onCommunityCards(List<Card> cards) {
        System.out.println("PokerGUI.onCommunityCards called with: " + (cards != null ? cards.size() : 0) + " cards");
        updateCommunityCards(cards);
    }

    @Override
    public void onHandEnded() {
        System.out.println("PokerGUI: Hand ended");
        Platform.runLater(() -> {
            appendActionLog("--- Hand ended ---", LogType.INFO);

            // Clear community cards
            if (communityArea != null) {
                communityArea.getChildren().clear();
                communityCards.clear();
            }

            // Clear player's hand (will be redealt next hand)
            if (playerHand != null) {
                playerHand.getChildren().clear();
            }

            // Reset button states
            disableButtons(true);
            currentBetLabel.setText("Current bet: 0");
            currentTurnText.setText("Turn: Waiting for next hand");
        });
    }

    public void updateMyChips(int chips) {
        System.out.println(">>> updateMyChips ENTERED with chips: " + chips);
        System.out.println(">>> playerChipsText = " + playerChipsText);

        if (playerChipsText == null) {
            System.out.println(">>> ERROR: playerChipsText is NULL! Cannot update chips.");
            return;
        }

        Platform.runLater(() -> {
            System.out.println(">>> Platform.runLater: Setting chips text to: " + chips);
            playerChipsText.setText("Chips: " + chips);
        });
    }

    public void setPendingPlayerChips(int chips) {
        this.pendingPlayerChips = chips;
        if (playerChipsText != null) {
            Platform.runLater(() -> playerChipsText.setText("Chips: " + chips));
        }
    }
    

    
    public static void main(String[] args) {
        launch(args);
    }
}
