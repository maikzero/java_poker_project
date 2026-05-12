package com.kevin.poker;

import java.util.*;
import java.util.stream.Collectors;

public class BettingRound {
    private List<Player> activePlayers;
    private int currentBet;
    private int pot;
    private int lastRaiserIndex = -1;

    public BettingRound(List<Player> players, int startingBet, int currentPot) {
        this.activePlayers = new ArrayList<>(players);
        this.currentBet = startingBet;
        this.pot = currentPot; // Use the passed pot, don't reset to 0
    }

    public boolean runBettingRound(int firstPlayerIndex, PokerGame game) {
        List<Player> playersInRound = activePlayers.stream()
                .filter(p -> !p.isFolded())
                .collect(Collectors.toList());

        if (playersInRound.size() <= 1) {
            return false;
        }

        int currentIndex = firstPlayerIndex % playersInRound.size();
        Set<Player> playersNeedingAction = new LinkedHashSet<>(playersInRound);
        int lastRaiserIndex = -1; // Add this to track raises

        game.logAction("Betting round started. Current bet: " + currentBet, PokerGUI.LogType.INFO);
        game.updatePotDisplay(pot);

        while (!playersNeedingAction.isEmpty()) {
            Player currentPlayer = playersInRound.get(currentIndex);

            // Find the actual player index from the main game players list for highlighting
            int actualPlayerIndex = -1;
            for (int i = 0; i < activePlayers.size(); i++) {
                if (activePlayers.get(i) == currentPlayer) {
                    actualPlayerIndex = i;
                    break;
                }
            }
            if (actualPlayerIndex >= 0 && actualPlayerIndex > 0) {
                game.highlightCurrentPlayer(actualPlayerIndex);
            }

            game.updateTurnDisplay(currentPlayer.getName() + " to act");
            game.logAction(currentPlayer.getName() + " to act (bet to match: " + currentBet + ")",
                    PokerGUI.LogType.TURN);

            if (!currentPlayer.isFolded()) {
                Action action = game.getPlayerAction(currentPlayer, currentBet);

                switch (action.getType()) {
                    case "FOLD":
                        currentPlayer.fold();
                        System.out.println(currentPlayer.getName() + " folds");
                        game.logAction(currentPlayer.getName() + " folds", PokerGUI.LogType.FOLD);
                        playersNeedingAction.remove(currentPlayer);
                        playersInRound.remove(currentPlayer);
                        if (playersInRound.size() <= 1) {
                            return false;
                        }
                        // Adjust index
                        if (currentIndex >= playersInRound.size()) {
                            currentIndex = 0;
                        }
                        continue;
                        
                    case "CALL":
                        int toCall = currentBet - currentPlayer.getCurrentBet();
                        if (toCall > 0) {
                            int actualCall = Math.min(toCall, currentPlayer.getChips());
                            pot += actualCall;
                            currentPlayer.call(currentBet);
                            System.out.println(currentPlayer.getName() + " calls " + actualCall);
                            game.logAction(currentPlayer.getName() + " calls " + actualCall, PokerGUI.LogType.CALL);
                            game.updatePotDisplay(pot);
                            // Add this - broadcast chip update
                            game.broadcastChipUpdate(currentPlayer);
                        } else {
                            System.out.println(currentPlayer.getName() + " checks");
                            game.logAction(currentPlayer.getName() + " checks", PokerGUI.LogType.CHECK);
                        }
                        playersNeedingAction.remove(currentPlayer);
                        break;

                    case "RAISE":
                        int raiseAmount = action.getAmount();
                        int newBet = currentBet + raiseAmount;
                        int toAdd = newBet - currentPlayer.getCurrentBet();
                        if (toAdd <= currentPlayer.getChips()) {
                            pot += toAdd;
                            currentPlayer.call(newBet);
                            currentBet = newBet;
                            System.out.println(currentPlayer.getName() + " raises to " + currentBet);
                            game.logAction(currentPlayer.getName() + " raises to " + currentBet,
                                    PokerGUI.LogType.RAISE);
                            game.updatePotDisplay(pot);
                            // Add this - broadcast chip update
                            game.broadcastChipUpdate(currentPlayer);
                            // ... rest of raise logic
                        }
                        break;
                }
            }

            currentIndex = (currentIndex + 1) % playersInRound.size();
        }

        for (Player p : activePlayers) {
            if (!p.isFolded()) {
                p.clearCurrentBet();
            }
        }

        game.updatePotDisplay(pot);

        System.out.println("\n💰 Pot: " + pot + " chips 💰\n");
        game.logAction("Betting round ended. Pot now " + pot, PokerGUI.LogType.INFO);
        return playersInRound.size() > 1;
    }

    public int getPot() {
        return pot;
    }

    
}