package com.kevin.poker;

import java.util.*;

public class Player {
    private final int id;
    private final String name;
    private int chips;
    private List<Card> holeCards;
    private boolean isFolded;
    private int currentBet;
    
    public Player(int id, String name, int startingChips) {
        this.id = id;
        this.name = name;
        this.chips = startingChips;
        this.holeCards = new ArrayList<>();
        this.isFolded = false;
        this.currentBet = 0;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public int getChips() { return chips; }
    public List<Card> getHoleCards() { return holeCards; }
    public boolean isFolded() { return isFolded; }
    public int getCurrentBet() { return currentBet; }
    
    public void addChips(int amount) {
        chips += amount;
    }
    
    public boolean subtractChips(int amount) {
        if (amount > chips) return false;
        chips -= amount;
        return true;
    }
    
    public void receiveCard(Card card) {
        holeCards.add(card);
    }
    
    public void clearHoleCards() {
        holeCards.clear();
    }
    
    public void fold() {
        isFolded = true;
    }
    
    public void resetForNewHand() {
        isFolded = false;
        currentBet = 0;
        holeCards.clear();
    }
    
    public void placeBet(int amount) {
        if (amount > chips) {
            throw new IllegalArgumentException("Not enough chips");
        }
        chips -= amount;
        currentBet += amount;
    }
    
    public void call(int callAmount) {
        int toCall = callAmount - currentBet;
        if (toCall > chips) {
            // All-in
            chips = 0;
            currentBet += chips;
        } else {
            chips -= toCall;
            currentBet = callAmount;
        }
    }
    
    public void clearCurrentBet() {
        this.currentBet = 0;
    }
    
    @Override
    public String toString() {
        return name + " (" + chips + " chips)";
    }
}
