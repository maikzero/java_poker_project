package com.kevin.poker;

public class Card {
	public final Rank rank;
	public final Suit suit;
	public enum Rank{
		ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
		TEN, JACK, QUEEN, KING
	}
	public enum Suit{
		SPADES, HEARTS, CLUBS, DIAMONDS
	}
	public Card(Rank rank, Suit suit) {
		super();
		this.rank = rank;
		this.suit = suit;
	}
	
	
	public Rank getRank() {
		return rank;
	}

	public Suit getSuit() {
		return suit;
	}


	@Override
	public String toString() {
		return "Card [rank=" + rank + ", suit=" + suit + "]";
	}
	
	
}

