package com.kevin.poker.network;

import com.kevin.poker.Card;
import com.kevin.poker.PokerGUI;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Network-aware GUI for poker clients. Receives game state updates via network messages
 * and displays them in real-time.
 */
public class PokerClientGUI extends PokerGUI {
        @Override
        public void onChipsUpdate(int playerId, int chips) {
            Platform.runLater(() -> {
                playerChips.put(playerId, chips);
                updateUIState();
            });
        }
    private final PokerClient client;
    private int myPlayerId = -1;
    private final Map<Integer, String> playerNames = new HashMap<>();
    private final Map<Integer, Integer> playerChips = new HashMap<>();
    private final Map<Integer, Integer> playerBets = new HashMap<>();
    private final List<Card> myHoleCards = new ArrayList<>();
    private final List<Card> communityCards = new ArrayList<>();
    private int potAmount = 0;
    private String currentStreet = "WAITING";

    public PokerClientGUI(PokerClient client) {
        this.client = client;
        // Don't call initializeGame() - we'll get game state from network
        // Instead, set playerId to a default value
        this.playerId = -1; // Will be set when onJoined is called
        this.opponentBoxes = new ArrayList<>();
    }

    @Override
    public void buildUI() {
        // Build a client-specific UI that doesn't rely on a local game instance
        javafx.geometry.Insets insets = new javafx.geometry.Insets(20);
        this.root = new javafx.scene.layout.VBox(10);
        root.setPadding(insets);
        root.setStyle("-fx-background-color: #1a472a;");

        // Status bar
        javafx.scene.layout.HBox statusBar = new javafx.scene.layout.HBox(20);
        statusBar.setAlignment(javafx.geometry.Pos.CENTER);
        gameStatusText = new javafx.scene.text.Text("Connecting to game...");
        gameStatusText.setFill(javafx.scene.paint.Color.WHITE);
        gameStatusText.setFont(javafx.scene.text.Font.font(18));
        potText = new javafx.scene.text.Text("Pot: 0");
        potText.setFill(javafx.scene.paint.Color.WHITE);
        potText.setFont(javafx.scene.text.Font.font(16));
        statusBar.getChildren().addAll(gameStatusText, potText);

        // Street banner
        streetBannerText = new javafx.scene.text.Text("WAITING");
        streetBannerText.setFill(javafx.scene.paint.Color.WHITE);
        streetBannerText.setFont(javafx.scene.text.Font.font(28));
        javafx.scene.layout.HBox bannerBox = new javafx.scene.layout.HBox();
        bannerBox.setAlignment(javafx.geometry.Pos.CENTER);
        bannerBox.getChildren().add(streetBannerText);

        // Table area with opponents
        tableArea = new javafx.scene.layout.HBox(20);
        tableArea.setAlignment(javafx.geometry.Pos.CENTER);
        tableArea.setStyle("-fx-background-color: #2d5a3d; -fx-padding: 20; -fx-border-radius: 10;");

        // Add opponent boxes
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.VBox opponentBox = new javafx.scene.layout.VBox();
            opponentBox.setStyle("-fx-background-color: #1b4332; -fx-border-color: #d4a373; -fx-border-width: 2; -fx-padding: 10;");
            opponentBox.setPrefWidth(120);
            opponentBox.setPrefHeight(100);
            opponentBox.setAlignment(javafx.geometry.Pos.CENTER);
            opponentBoxes.add(opponentBox);
        }
        tableArea.getChildren().addAll(opponentBoxes);

        // Community cards
        communityArea = new javafx.scene.layout.HBox(10);
        communityArea.setAlignment(javafx.geometry.Pos.CENTER);
        communityArea.setStyle("-fx-padding: 10;");

        // Player area
        playerArea = new javafx.scene.layout.VBox(10);
        playerArea.setStyle("-fx-background-color: #2d6a4f; -fx-border-color: #d4a373; -fx-border-width: 2;");
        playerArea.setPadding(insets);
        playerArea.setAlignment(javafx.geometry.Pos.CENTER);

        // Player hand
        playerHand = new javafx.scene.layout.HBox(10);
        playerHand.setAlignment(javafx.geometry.Pos.CENTER);

        // Player chips text
        playerChipsText = new javafx.scene.text.Text("Chips: 0");
        playerChipsText.setFill(javafx.scene.paint.Color.WHITE);

        // Button area
        buttonArea = new javafx.scene.layout.HBox(15);
        buttonArea.setAlignment(javafx.geometry.Pos.CENTER);

        checkCallButton = new javafx.scene.control.Button("Check/Call");
        checkCallButton.setStyle("-fx-font-size: 14; -fx-min-width: 100;");
        checkCallButton.setOnAction(e -> onCheckCall());
        checkCallButton.setDisable(true);

        foldButton = new javafx.scene.control.Button("Fold");
        foldButton.setStyle("-fx-font-size: 14; -fx-min-width: 100; -fx-background-color: #dc2f02; -fx-text-fill: white;");
        foldButton.setOnAction(e -> onFold());
        foldButton.setDisable(true);

        raiseButton = new javafx.scene.control.Button("Raise");
        raiseButton.setStyle("-fx-font-size: 14; -fx-min-width: 100; -fx-background-color: #f4a261;");
        raiseButton.setOnAction(e -> onRaise());
        raiseButton.setDisable(true);

        raiseSlider = new javafx.scene.control.Slider(20, 500, 100);
        raiseSlider.setDisable(true);

        buttonArea.getChildren().addAll(checkCallButton, foldButton, raiseButton, raiseSlider);

        playerArea.getChildren().addAll(playerChipsText, playerHand, buttonArea);

        root.getChildren().addAll(statusBar, bannerBox, tableArea, communityArea, playerArea);
    }

    @Override
    public void onJoined(int playerId, String playerName) {
        myPlayerId = playerId;
        this.playerId = playerId; // Set this GUI's playerId
        Platform.runLater(() -> {
            playerNames.put(playerId, playerName);
            gameStatusText.setText("Connected as " + playerName);
            updateUIState();
        });
    }

    @Override
    public void onPlayerJoined(int playerId, String playerName) {
        Platform.runLater(() -> {
            playerNames.put(playerId, playerName);
            playerChips.put(playerId, 0);
            updateUIState();
        });
    }

    @Override
    public void onPlayerLeft(int playerId) {
        Platform.runLater(() -> {
            playerNames.remove(playerId);
            playerChips.remove(playerId);
            playerBets.remove(playerId);
            updateUIState();
        });
    }

    public void onGameInfo(int playerId, String playerName, int chips) {
        Platform.runLater(() -> {
            playerNames.put(playerId, playerName);
            playerChips.put(playerId, chips);
            updateUIState();
        });
    }

    public void onHoleCards(List<Card> cards) {
        Platform.runLater(() -> {
            myHoleCards.clear();
            if (cards != null) {
                myHoleCards.addAll(cards);
            }
            updateUIState();
        });
    }

    public void onTurnToAct(int playerId, int currentBet) {
        Platform.runLater(() -> {
            if (playerId == myPlayerId) {
                enablePlayerTurn(currentBet);
            }
            updateUIState();
        });
    }


    @Override
    public void onPot(int amount) {
        Platform.runLater(() -> {
            potAmount = amount;
            updatePot(amount);
        });
    }

    public void onBet(int playerId, int amount) {
        Platform.runLater(() -> {
            playerBets.put(playerId, amount);
            updateUIState();
        });
    }

    @Override
    public void onStreet(String street) {
        Platform.runLater(() -> {
            currentStreet = street;
            displayStreet(street);
        });
    }

    public void onFold(int playerId) {
        Platform.runLater(this::updateUIState);
    }

    @Override
    public void onBoard(List<String> cards) {
        // Not used in GUI mode
    }

    @Override
    public void onTurn(int currentBet, int pot, List<String> communityCards) {
        // Not used in GUI mode
    }

    @Override
    public void onInfo(String message) {
        System.out.println("Info: " + message);
    }

    @Override
    public void onChat(int playerId, String message) {
        Platform.runLater(() -> {
            String player = playerNames.getOrDefault(playerId, "Player " + playerId);
            System.out.println("[" + player + "]: " + message);
        });
    }

    @Override
    public void onError(String message) {
        System.err.println("Error: " + message);
    }

    @Override
    public void enablePlayerTurn(int currentBet) {
        Platform.runLater(() -> {
            checkCallButton.setDisable(false);
            foldButton.setDisable(false);
            raiseButton.setDisable(false);
            raiseSlider.setDisable(false);
            highlightHumanPlayer();
        });
    }

    protected void updateUIState() {
        // Update opponent info
        if (opponentBoxes != null && opponentBoxes.size() > 0) {
            int seatIndex = 0;
            for (Map.Entry<Integer, String> entry : playerNames.entrySet()) {
                int playerId = entry.getKey();
                String playerName = entry.getValue();
                if (playerId != myPlayerId && seatIndex < opponentBoxes.size()) {
                    VBox opponentBox = opponentBoxes.get(seatIndex);
                    opponentBox.getChildren().clear();
                    VBox opponentInfo = new VBox(5);
                    opponentInfo.setAlignment(Pos.CENTER);
                    opponentInfo.setPadding(new Insets(10));
                    Text nameText = new Text(playerName);
                    nameText.setStyle("-fx-font-size: 12; -fx-fill: #f4a261;");
                    int chips = playerChips.getOrDefault(playerId, 0);
                    Text chipsText = new Text("Chips: " + chips);
                    chipsText.setStyle("-fx-font-size: 10; -fx-fill: #d4a373;");
                    int bet = playerBets.getOrDefault(playerId, 0);
                    Text betText = new Text("Bet: " + bet);
                    betText.setStyle("-fx-font-size: 10; -fx-fill: #e76f51;");
                    opponentInfo.getChildren().addAll(nameText, chipsText, betText);
                    opponentBox.getChildren().add(opponentInfo);
                    seatIndex++;
                }
            }
        }

        // Update human player's chip display
        if (playerChipsText != null && myPlayerId != -1) {
            int myChips = playerChips.getOrDefault(myPlayerId, 0);
            playerChipsText.setText("Chips: " + myChips);
        }

        // Update player hand (hole cards)
        if (playerHand != null && myHoleCards.size() > 0) {
            playerHand.getChildren().clear();
            for (Card card : myHoleCards) {
                Node cardNode = createCardNode(card);
                playerHand.getChildren().add(cardNode);
            }
        }

        // Update community cards
        if (communityArea != null) {
            communityArea.getChildren().clear();
            for (Card card : communityCards) {
                Node cardNode = createCardNode(card);
                communityArea.getChildren().add(cardNode);
            }
        }

        // Update pot
        if (potText != null) {
            potText.setText("Pot: " + potAmount);
        }

        // Update street
        if (streetBannerText != null) {
            streetBannerText.setText(currentStreet);
        }
    }

    private Node createCardNode(Card card) {
        HBox cardBox = new HBox();
        cardBox.setStyle("-fx-border-color: #f4a261; -fx-border-width: 2; -fx-padding: 5;");
        cardBox.setPrefWidth(60);
        cardBox.setPrefHeight(90);
        cardBox.setAlignment(Pos.CENTER);

        Text cardText = new Text(card.getRank() + "\n" + card.getSuit());
        cardText.setStyle("-fx-font-size: 12; -fx-fill: #f4a261;");
        cardBox.getChildren().add(cardText);
        return cardBox;
    }

    @Override
    protected void onCheckCall() {
        client.sendCommand("ACTION CALL");
        disableButtons(true);
        clearHumanHighlight();
    }

    @Override
    protected void onFold() {
        client.sendCommand("ACTION FOLD");
        disableButtons(true);
        clearHumanHighlight();
    }

    @Override
    protected void onRaise() {
        int amount = (int) raiseSlider.getValue();
        client.sendCommand("ACTION RAISE " + amount);
        disableButtons(true);
        clearHumanHighlight();
    }
}
