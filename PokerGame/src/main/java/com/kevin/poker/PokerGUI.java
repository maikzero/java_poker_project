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

import java.util.*;

public class PokerGUI extends Application {

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
    private PokerGame game;
    private int playerId;  // 0 = human, 1,2,3 = AI opponents
    private List<Player> players;
    
    // UI Components
    private VBox root;
    private HBox tableArea;
    private VBox playerArea;
    private HBox communityArea;
    private HBox buttonArea;
    
    // Player-specific elements
    private HBox playerHand;
    private Text potText;
    private Text playerChipsText;
    private Text gameStatusText;
    private Text currentTurnText;
    private ListView<LogEntry> actionLogView;
    private int displayedPot;
    
    // Street banner and opponent boxes
    private Text streetBannerText;
    private List<VBox> opponentBoxes;
    private int currentPlayerIndex;
    private final String basePlayerAreaStyle = "-fx-background-color: #2d6a4f; -fx-border-color: #d4a373; -fx-border-width: 2; -fx-border-radius: 20; -fx-background-radius: 20;";
    private final String glowingPlayerAreaStyle = "-fx-background-color: #2d6a4f; -fx-border-color: #f4a261; -fx-border-width: 4; -fx-border-radius: 20; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, #f4a261, 25, 0.75, 0, 0);";
    
    // Betting controls
    private Button checkCallButton;
    private Button foldButton;
    private Button raiseButton;
    private Slider raiseSlider;
    private Label raiseAmountLabel;
    private Label currentBetLabel;
    
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
    
    private void initializeGame() {
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
        currentPlayerIndex = -1;
    }
    
    private void buildUI() {
        root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a472a;");
        
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
        
        // Opponents (top, left, right positions - simplified for now)
        for (int i = 1; i <= 3; i++) {
            VBox opponentBox = createOpponentBox(players.get(i), i);
            opponentBoxes.add(opponentBox);
            tableArea.getChildren().add(opponentBox);
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
        playerChipsText = new Text("Chips: " + players.get(playerId).getChips());
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
    
    private VBox createOpponentBox(Player opponent, int index) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10;");
        box.setUserData(index);  // Store index for highlighting
        
        Text nameText = new Text(opponent.getName());
        nameText.setFill(Color.WHITE);
        
        Text chipsText = new Text(opponent.getChips() + " chips");
        chipsText.setFill(Color.YELLOW);
        
        HBox cards = new HBox(5);
        // Show card backs for opponents
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
            // Update player chips
            playerChipsText.setText("Chips: " + players.get(playerId).getChips());
            
            // Update pot
            potText.setText("Pot: " + displayedPot);
            
            // Update player's hand
            playerHand.getChildren().clear();
            for (Card card : players.get(playerId).getHoleCards()) {
                playerHand.getChildren().add(createCardNode(card));
            }
            
            // Update community cards
            communityArea.getChildren().clear();
            for (Card card : game.getCommunityCards()) {
                communityArea.getChildren().add(createCardNode(card));
            }
            
            // Update opponent displays
            for (int i = 1; i <= 3; i++) {
                VBox opponentBox = (VBox) tableArea.getChildren().get(i - 1);
                Text chipsText = (Text) opponentBox.getChildren().get(1);
                chipsText.setText(players.get(i).getChips() + " chips");
            }
        });
    }
    
    private StackPane createCardNode(Card card) {
        StackPane cardNode = new StackPane();
        cardNode.setPrefSize(60, 85);
        cardNode.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        String rankStr = card.getRank().toString();
        String suitStr = card.getSuit().toString().substring(0, 1);
        Color suitColor = (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS) 
                          ? Color.RED : Color.BLACK;
        
        Text rankText = new Text(rankStr + suitStr);
        rankText.setFont(Font.font(20));
        rankText.setFill(suitColor);
        
        cardNode.getChildren().add(rankText);
        return cardNode;
    }
    
    private void disableButtons(boolean disabled) {
        Platform.runLater(() -> {
            checkCallButton.setDisable(disabled);
            foldButton.setDisable(disabled);
            raiseButton.setDisable(disabled);
            raiseSlider.setDisable(disabled);
        });
    }
    
    private void onCheckCall() {
        // Send CALL action
        game.receivePlayerAction(playerId, new Action("CALL", 0));
        clearHumanHighlight();
        disableButtons(true);
    }
    
    private void onFold() {
        // Send FOLD action
        game.receivePlayerAction(playerId, new Action("FOLD", 0));
        clearHumanHighlight();
        disableButtons(true);
    }
    
    private void onRaise() {
        int amount = (int) raiseSlider.getValue();
        // Send RAISE action
        game.receivePlayerAction(playerId, new Action("RAISE", amount));
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
            
            // Highlight player 0 (human) by clearing highlights to show human is active
            clearPlayerHighlight();
            highlightHumanPlayer();
            
            // Update check/call button text
            int toCall = currentBet - players.get(playerId).getCurrentBet();
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
            
            currentPlayerIndex = playerIndex;
        });
    }
    
    public void clearPlayerHighlight() {
        Platform.runLater(() -> {
            for (VBox box : opponentBoxes) {
                box.setStyle("-fx-background-color: #1b4332; -fx-border-radius: 10; -fx-background-radius: 10;");
            }
            currentPlayerIndex = -1;
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
        });
    }

    public void updatePot(int pot) {
        Platform.runLater(() -> {
            displayedPot = pot;
            potText.setText("Pot: " + pot);
        });
    }

    public void updateCommunityCards() {
        Platform.runLater(() -> {
            List<Card> cards = game.getCommunityCards();
            int currentCardCount = communityArea.getChildren().size();
            
            // Only animate new cards that haven't been added yet
            for (int i = currentCardCount; i < cards.size(); i++) {
                Card card = cards.get(i);
                javafx.scene.Node cardNode = createCardNode(card);
                cardNode.setOpacity(0); // Start invisible
                communityArea.getChildren().add(cardNode);
                
                // Stagger animation: each card starts after the previous one
                int delay = i * 300; // 300ms per card
                Timeline timeline = new Timeline(
                    new KeyFrame(
                        Duration.millis(delay),
                        event -> {
                            FadeTransition fade = new FadeTransition(
                                Duration.millis(500),
                                cardNode
                            );
                            fade.setFromValue(0);
                            fade.setToValue(1);
                            fade.play();
                        }
                    )
                );
                timeline.play();
            }
        });
    }

    public void appendActionLog(String message) {
        appendActionLog(message, LogType.INFO);
    }

    public void appendActionLog(String message, LogType type) {
        Platform.runLater(() -> actionLogView.getItems().add(new LogEntry(message, type)));
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
    
    public static void main(String[] args) {
        launch(args);
    }
}
