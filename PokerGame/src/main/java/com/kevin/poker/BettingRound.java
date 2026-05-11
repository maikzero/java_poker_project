package com.kevin.poker;

import java.util.*;
import java.util.stream.Collectors;

public class BettingRound {
    private List<Player> activePlayers;
    private int currentBet;
    private int pot;
    
    public BettingRound(List<Player> players, int startingBet) {
        this.activePlayers = new ArrayList<>(players);
        this.currentBet = startingBet;
        this.pot = 0;
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
            game.logAction(currentPlayer.getName() + " to act (bet to match: " + currentBet + ")", PokerGUI.LogType.TURN);
            
            if (!currentPlayer.isFolded()) {
                Action action = game.getPlayerAction(currentPlayer, currentBet);
                
                switch (action.getType()) {
                    case "FOLD":
                        currentPlayer.fold();
                        System.out.println(currentPlayer.getName() + " folds");
                        game.logAction(currentPlayer.getName() + " folds", PokerGUI.LogType.FOLD);
                        playersNeedingAction.remove(currentPlayer);
                        if (playersInRound.size() <= 1) {
                            return false;
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
                            game.logAction(currentPlayer.getName() + " raises to " + currentBet, PokerGUI.LogType.RAISE);
                            game.updatePotDisplay(pot);
                            playersNeedingAction.clear();
                            for (Player p : playersInRound) {
                                if (p != currentPlayer && !p.isFolded()) {
                                    playersNeedingAction.add(p);
                                }
                            }
                        } else {
                            System.out.println("Not enough chips for raise! Calling instead.");
                            int toCallRaise = currentBet - currentPlayer.getCurrentBet();
                            if (toCallRaise > 0) {
                                pot += Math.min(toCallRaise, currentPlayer.getChips());
                                currentPlayer.call(currentBet);
                                game.logAction(currentPlayer.getName() + " calls " + Math.min(toCallRaise, currentPlayer.getChips()), PokerGUI.LogType.CALL);
                                game.updatePotDisplay(pot);
                            }
                            playersNeedingAction.remove(currentPlayer);
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
        return true;
    }
    
    public int getPot() {
        return pot;
    }
}