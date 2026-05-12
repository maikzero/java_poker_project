package com.kevin.poker;

import com.kevin.poker.network.PokerNetworkBridge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PokerGame {
    private List<Player> players;
    private Deck deck;
    private List<Card> communityCards;
    private int pot;
    private int dealerIndex;
    private int smallBlind;
    private int bigBlind;
    private Scanner scanner;
    private PokerGUI ui; // optional GUI callback
    private PokerNetworkBridge networkBridge;
    private final Map<Integer, BlockingQueue<Action>> networkActionQueues = new ConcurrentHashMap<>();
    private final BlockingQueue<Action> guiActionQueue = new LinkedBlockingQueue<>();
    private boolean networkMode = false;
    private String currentStreet = "PRE-FLOP";
    private int currentBet = 0;
    private int playerId = -1; // For networked clients to identify themselves
    
    public PokerGame(int smallBlind, int bigBlind) {
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.deck = new Deck();
        this.communityCards = new ArrayList<>();
        this.players = new ArrayList<>();
        this.dealerIndex = 0;
        this.scanner = new Scanner(System.in);
    }
    
    public void addPlayer(Player player) {
        players.add(player);
    }

    public void setUI(PokerGUI ui) {
        this.ui = ui;
    }

    public void setNetworkBridge(PokerNetworkBridge networkBridge) {
        this.networkBridge = networkBridge;
        this.networkMode = (networkBridge != null);
    }

    public void registerNetworkActionQueue(int playerId, BlockingQueue<Action> actionQueue) {
        if (actionQueue == null) {
            networkActionQueues.remove(playerId);
        } else {
            networkActionQueues.put(playerId, actionQueue);
        }
    }

    public void unregisterNetworkActionQueue(int playerId) {
        networkActionQueues.remove(playerId);
    }

    public void logAction(String message) {
        if (ui != null) {
            ui.appendActionLog(message);
        }
        if (networkBridge != null) {
            networkBridge.onLog(message);
        }
    }

    public void logAction(String message, PokerGUI.LogType type) {
        if (ui != null) {
            ui.appendActionLog(message, type);
        }
        if (networkBridge != null) {
            networkBridge.onLog(message);
        }
    }

    public void updatePotDisplay(int potValue) {
        if (ui != null) {
            ui.updatePot(potValue);
        }
        if (networkBridge != null) {
            networkBridge.onPotUpdated(potValue);
        }
    }

    public void updateTurnDisplay(String status) {
        if (ui != null) {
            ui.updateTurnStatus(status);
        }
    }
    
    public void highlightCurrentPlayer(int playerIndex) {
        if (ui != null) {
            ui.highlightCurrentPlayer(playerIndex);
        }
    }
    
    public void displayStreet(String street) {
        this.currentStreet = street; // ADD THIS LINE
        if (ui != null) {
            ui.displayStreet(street);
        }
        if (networkBridge != null) {
            networkBridge.onStreetChanged(street);
        }
    }

    public String getStreet() {
        return currentStreet;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(int bet) {
        this.currentBet = bet;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        System.out.println("PokerGUI: Player ID set to " + playerId);
    }
    
    public void startGame() {
        System.out.println("=== Starting Poker Game ===");
        boolean gameRunning = true;
        
        while (gameRunning) {
            playHand();
            
            // Check if game should continue
            long activePlayers = players.stream().filter(p -> p.getChips() > 0).count();
            if (activePlayers <= 1) {
                gameRunning = false;
                Optional<Player> winner = players.stream().filter(p -> p.getChips() > 0).findFirst();
                if (winner.isPresent()) {
                    System.out.println("\n🏆 GAME OVER! Winner: " + winner.get().getName() + " 🏆");
                }
            }
            
            // Rotate dealer
            dealerIndex = (dealerIndex + 1) % players.size();
            
            // Wait for user to continue (skip in network mode)
            if (!networkMode) {
                System.out.print("\nPress Enter to continue to next hand...");
                scanner.nextLine();
            }
        }
        if (!networkMode) {
            scanner.close();
        }
    }
    
    private void playHand() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("NEW HAND - Dealer: " + players.get(dealerIndex).getName());
        System.out.println("=".repeat(50));

        // Reset for new hand
        deck.reset();
        deck.shuffle();
        communityCards.clear();
        pot = 0;
        players.forEach(Player::resetForNewHand);

        // Collect blinds
        int smallBlindIndex = (dealerIndex + 1) % players.size();
        int bigBlindIndex = (dealerIndex + 2) % players.size();

        Player smallBlindPlayer = players.get(smallBlindIndex);
        Player bigBlindPlayer = players.get(bigBlindIndex);

        smallBlindPlayer.placeBet(smallBlind);
        bigBlindPlayer.placeBet(bigBlind);
        // Broadcast chip updates
        broadcastChipUpdate(smallBlindPlayer);
        broadcastChipUpdate(bigBlindPlayer);
        pot = smallBlind + bigBlind;

        System.out.println(smallBlindPlayer.getName() + " posts small blind: " + smallBlind);
        System.out.println(bigBlindPlayer.getName() + " posts big blind: " + bigBlind);

        // Deal hole cards
        for (int i = 0; i < 2; i++) {
            for (Player player : players) {
                player.receiveCard(deck.drawCard());
            }
        }

        // Broadcast hole cards to all players
        if (networkBridge != null) {
            for (Player player : players) {
                networkBridge.onHoleCardsDealt(player.getId(), player.getHoleCards());
            }
        }

        // Show hole cards (only for human players - here all are human for testing)
        for (Player player : players) {
            System.out.println(player.getName() + " hole cards: " + player.getHoleCards());
        }

        // Pre-flop betting - Pass current pot
        displayStreet("PRE-FLOP");
        int firstToAct = (bigBlindIndex + 1) % players.size();
        BettingRound preFlop = new BettingRound(players, bigBlind, pot); // Pass current pot
        boolean moreThanOne = preFlop.runBettingRound(firstToAct, this);
        pot = preFlop.getPot(); // Get updated pot
        updatePotDisplay(pot);

        if (!moreThanOne) {
            awardPotToRemainingPlayer();
            return;
        }

        // Flop - FIXED: Pass current pot
        displayStreet("FLOP");
        System.out.println("\n" + "─".repeat(30));
        System.out.println("FLOP");
        System.out.println("─".repeat(30));
        communityCards.add(deck.drawCard());
        communityCards.add(deck.drawCard());
        communityCards.add(deck.drawCard());
        displayCommunityCards();
        updateCommunityCardsDisplay();

        BettingRound flop = new BettingRound(players, 0, pot); // Pass current pot
        moreThanOne = flop.runBettingRound(smallBlindIndex, this);
        pot = flop.getPot(); // Get updated pot
        updatePotDisplay(pot);

        if (!moreThanOne) {
            awardPotToRemainingPlayer();
            return;
        }

        // Turn - FIXED: Pass current pot
        displayStreet("TURN");
        System.out.println("\n" + "─".repeat(30));
        System.out.println("TURN");
        System.out.println("─".repeat(30));
        communityCards.add(deck.drawCard());
        displayCommunityCards();
        updateCommunityCardsDisplay();

        BettingRound turn = new BettingRound(players, 0, pot); // Pass current pot
        moreThanOne = turn.runBettingRound(smallBlindIndex, this);
        pot = turn.getPot(); // Get updated pot
        updatePotDisplay(pot);

        if (!moreThanOne) {
            awardPotToRemainingPlayer();
            return;
        }

        // River - FIXED: Pass current pot
        displayStreet("RIVER");
        System.out.println("\n" + "─".repeat(30));
        System.out.println("RIVER");
        System.out.println("─".repeat(30));
        communityCards.add(deck.drawCard());
        displayCommunityCards();
        updateCommunityCardsDisplay();

        BettingRound river = new BettingRound(players, 0, pot); // Pass current pot
        moreThanOne = river.runBettingRound(smallBlindIndex, this);
        pot = river.getPot(); // Get updated pot
        updatePotDisplay(pot);

        if (!moreThanOne) {
            awardPotToRemainingPlayer();
            return;
        }

        // Showdown
        showdown();
    }
    
    private void displayCommunityCards() {
        System.out.print("Community cards: ");
        for (Card card : communityCards) {
            System.out.print(card + " ");
        }
        System.out.println();
    }
    

    private void updateCommunityCardsDisplay() {
        System.out.println("=== updateCommunityCardsDisplay called ===");
        System.out.println("Current community cards: " + communityCards.size() + " cards - " + communityCards);

        if (ui != null) {
            ui.updateCommunityCards();
        }
        if (networkBridge != null) {
            networkBridge.onCommunityCardsUpdated(getCommunityCards());
        }
    }
    
    private void showdown() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SHOWDOWN");
        System.out.println("=".repeat(50));
        logAction("SHOWDOWN", PokerGUI.LogType.WIN);
        
        List<Player> activePlayers = players.stream()
            .filter(p -> !p.isFolded())
            .collect(Collectors.toList());
        
        // Evaluate each player's best hand
        Map<Player, HandRank> playerRanks = new HashMap<>();
        for (Player player : activePlayers) {
            List<Card> allCards = new ArrayList<>();
            allCards.addAll(player.getHoleCards());
            allCards.addAll(communityCards);
            HandRank rank = HandEvaluator.evaluateBestHand(allCards);
            playerRanks.put(player, rank);
            System.out.println(player.getName() + ": " + rank.getType() + " - " + rank);
        }
        
        // Find winner(s)
        HandRank bestRank = playerRanks.get(activePlayers.get(0));
        for (HandRank rank : playerRanks.values()) {
            if (rank.compareTo(bestRank) > 0) {
                bestRank = rank;
            }
        }
        
        // Find all players with best rank
        List<Player> winners = new ArrayList<>();
        for (Map.Entry<Player, HandRank> entry : playerRanks.entrySet()) {
            if (entry.getValue().compareTo(bestRank) == 0) {
                winners.add(entry.getKey());
            }
        }
        
        // Award pot
        int splitPot = pot / winners.size();
        for (Player w : winners) {
            w.addChips(splitPot);
            broadcastChipUpdate(w); // Broadcast chip update for winner(s)
            System.out.println("\n🎉 " + w.getName() + " wins " + splitPot + " chips with " + playerRanks.get(w).getType() + " 🎉");
            logAction(w.getName() + " wins " + splitPot + " chips", PokerGUI.LogType.WIN);
        }
        
        System.out.println("\nTotal pot: " + pot + " chips");
        
        // Show chip counts
        System.out.println("\nCurrent chip counts:");
        for (Player p : players) {
            System.out.println("  " + p.getName() + ": " + p.getChips() + " chips");
        }
        // Notify GUI of showdown results (if attached)
        if (ui != null) {
            ui.showShowdown(playerRanks, winners);
        }
    }
    
    private void awardPotToRemainingPlayer() {
        Optional<Player> winner = players.stream().filter(p -> !p.isFolded()).findFirst();
        if (winner.isPresent()) {
            Player winningPlayer = winner.get();
            winningPlayer.addChips(pot);
            System.out.println("\n🏆 " + winningPlayer.getName() + " wins " + pot + " chips (everyone else folded) 🏆");
            logAction(winningPlayer.getName() + " wins " + pot + " chips (everyone else folded)", PokerGUI.LogType.WIN);

            // Broadcast chips update to all clients
            broadcastChipUpdate(winningPlayer); // Add this
            if (networkBridge != null) {
                // Also broadcast that the hand ended so clients can reset
                networkBridge.onHandEnded();
                networkBridge.onCommunityCardsUpdated(new ArrayList<>()); // Clear board
            }

            // Show chip counts
            System.out.println("\nCurrent chip counts:");
            for (Player p : players) {
                System.out.println("  " + p.getName() + ": " + p.getChips() + " chips");
                if (networkBridge != null) {
                    networkBridge.onChipsUpdated(p.getId(), p.getChips());
                }
            }
            
            
        }
    }
    
    public Action getPlayerAction(Player player, int currentBet) {
        int toCall = currentBet - player.getCurrentBet();

        // If GUI is attached and it's the human player's turn, wait for GUI action
        if (ui != null && player.getId() == 0) {
            ui.enablePlayerTurn(currentBet);
            try {
                Action guiAction = guiActionQueue.poll(5, TimeUnit.MINUTES);
                if (guiAction != null) {
                    return guiAction;
                }
                System.out.println("GUI action timeout. Defaulting to call/check.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for GUI action. Defaulting to call/check.");
            }
        }

        BlockingQueue<Action> networkQueue = networkActionQueues.get(player.getId());
        if (networkQueue != null) {
            if (networkBridge != null) {
                networkBridge.promptPlayer(player, currentBet, pot, getCommunityCards());
            }
            try {
                Action networkAction = networkQueue.poll(5, TimeUnit.MINUTES);
                if (networkAction != null) {
                    return networkAction;
                }
                System.out.println("Network action timeout. Defaulting to call/check.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for network action. Defaulting to call/check.");
            }
        }
        
        System.out.println("\n" + "▶".repeat(20));
        System.out.println(player.getName() + "'s TURN");
        System.out.println("Your chips: " + player.getChips()); 
        System.out.println("Current bet to match: " + currentBet);
        System.out.println("You have already bet: " + player.getCurrentBet());
        
        if (toCall > 0) {
            System.out.println("To call: " + toCall + " chips");
        }
        
        System.out.println("\nOptions:");
        if (toCall == 0) {
            System.out.println("  [1] Check");
        } else {
            System.out.println("  [1] Call (" + toCall + " chips)");
        }
        System.out.println("  [2] Fold");
        System.out.println("  [3] Raise");
        System.out.print("\nYour choice: ");
        
        String input = scanner.nextLine().trim();
        
        switch (input) {
            case "2":
                System.out.println(player.getName() + " folds");
                return new Action("FOLD", 0);
            case "3":
                System.out.print("Enter raise amount (min " + bigBlind + "): ");
                try {
                    int amount = Integer.parseInt(scanner.nextLine().trim());
                    if (amount < bigBlind) {
                        System.out.println("Raise must be at least " + bigBlind + ". Calling instead.");
                        return new Action("CALL", 0);
                    }
                    System.out.println(player.getName() + " raises by " + amount);
                    return new Action("RAISE", amount);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid amount. Calling instead.");
                    return new Action("CALL", 0);
                }
            default:
                if (toCall == 0) {
                    System.out.println(player.getName() + " checks");
                } else {
                    System.out.println(player.getName() + " calls " + toCall);
                }
                return new Action("CALL", 0);
        }
    }
    
    // Methods for GUI integration
    public int getPot() {
        return pot;
    }
    
    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public void receivePlayerAction(int playerId, Action action) {
        // This would integrate with the GUI to handle player actions
        if (playerId < 0 || playerId >= players.size()) {
            throw new IllegalArgumentException("Invalid player ID");
        }
        if (playerId == 0 && action != null) {
            guiActionQueue.offer(action);
        }
    }
    
    public void startHand() {
        playHand();
        // If the GUI or external launcher called startHand directly, rotate dealer to next player
        if (players.size() > 0) {
            dealerIndex = (dealerIndex + 1) % players.size();
        }

        // broadcast hole cards to all players
        if (networkBridge != null) {
            for (Player player : players) {
                networkBridge.onHoleCardsDealt(player.getId(), player.getHoleCards());
            }
        }
    }

    public void broadcastChipUpdate(Player player) {
        if (networkBridge != null) {
            networkBridge.onChipsUpdated(player.getId(), player.getChips());
            System.out.println("Broadcasting chip update - Player " + player.getId() + ": " + player.getChips());
        }
    }

    private void broadcastAllChips() {
        if (networkBridge != null) {
            for (Player p : players) {
                networkBridge.onChipsUpdated(p.getId(), p.getChips());
            }
        }
    }

    

    
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("WELCOME TO TEXAS HOLD'EM POKER");
        System.out.println("=".repeat(50));
        
        PokerGame game = new PokerGame(10, 20);
        game.addPlayer(new Player(1, "Kevin", 1000));
        game.addPlayer(new Player(2, "Alice", 1000));
        game.addPlayer(new Player(3, "Bob", 1000));
        
        System.out.println("\nPlayers:");
        for (Player p : game.players) {
            System.out.println("  " + p.getName() + " - " + p.getChips() + " chips");
        }
        
        game.startGame();
    }
}