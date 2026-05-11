package com.kevin.poker;

import java.util.List;

public class HandRank implements Comparable<HandRank> {
	
	
	public enum HandType{
		HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT, FLUSH, FULL_HOUSE,
		FOUR_OF_A_KIND, STRAIGHT_FLUSH, ROYAL_FLUSH
	}
    private final HandType type;
    private final List<Integer> primaryRanks;
    private final List<Integer> kickers;
    
    public HandRank(HandType type, List<Integer> primaryRanks, List<Integer> kickers) {
        this.type = type;
        this.primaryRanks = primaryRanks;
        this.kickers = kickers;
    }
    
    public HandType getType() { return type; }
    public List<Integer> getPrimaryRanks() { return primaryRanks; }
    public List<Integer> getKickers() { return kickers; }
    
    @Override
    public int compareTo(HandRank other) {
        // Compare hand type first
        if (this.type != other.type) {
            return Integer.compare(this.type.ordinal(), other.type.ordinal());
        }
        
        // Compare primary ranks
        for (int i = 0; i < this.primaryRanks.size(); i++) {
            if (i >= other.primaryRanks.size()) return 1;
            int cmp = Integer.compare(this.primaryRanks.get(i), other.primaryRanks.get(i));
            if (cmp != 0) return cmp;
        }
        
        // Compare kickers
        for (int i = 0; i < this.kickers.size(); i++) {
            if (i >= other.kickers.size()) return 1;
            int cmp = Integer.compare(this.kickers.get(i), other.kickers.get(i));
            if (cmp != 0) return cmp;
        }
        
        return 0; // Perfect tie - split pot
    }
    
    @Override
    public String toString() {
        return type + " " + primaryRanks + " (kickers: " + kickers + ")";
    }
}
	

