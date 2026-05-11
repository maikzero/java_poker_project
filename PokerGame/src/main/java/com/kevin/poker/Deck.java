package com.kevin.poker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
	public List<Card> cards;
	
	public Deck() {
		cards = new ArrayList<>();
		for (Card.Rank rank: Card.Rank.values()) {
			for (Card.Suit suit: Card.Suit.values()) {
				cards.add(new Card(rank, suit));
			}
		}
	}
	public void shuffle() {
		Collections.shuffle(cards);
		System.out.println("Shuffled "+cards.size()  +" card deck");
	}
	public Card drawCard() {
		return cards.remove(0);
	}
	public int remainingCards() {
		return cards.size();
	}
	public void reset() {
		// TODO Auto-generated method stub
		cards = new ArrayList<>();
		for (Card.Rank rank: Card.Rank.values()) {
			for (Card.Suit suit: Card.Suit.values()) {
				cards.add(new Card(rank, suit));
			}
		}
	}
	
}
