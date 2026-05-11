package com.kevin.poker;

public class Poker {
	public static void main(String[] args) {
        Deck deck = new Deck();
        deck.shuffle();
        
        // Draw 5 cards to see if it works
        for (int i = 0; i < 5; i++) {
            System.out.println(deck.drawCard());
        }
        System.out.println("Remaining: " + deck.remainingCards());
    }
}
