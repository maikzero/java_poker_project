package com.kevin.poker;
import java.util.*;
import java.util.stream.Collectors;

import com.kevin.poker.HandRank.HandType;


public class HandEvaluator {
	public static HandRank evaluateBestHand(List<Card> sevenCards) {
	    if (sevenCards.size() != 7) {
	        throw new IllegalArgumentException("Must have exactly 7 cards");
	    }
	    
	    // Generate all 21 combinations of 5 cards
	    List<List<Card>> fiveCardCombos = getAllCombinations(sevenCards, 5);
	    
	    // Find the best hand among all combinations
	    HandRank bestHand = null;
	    for (List<Card> fiveCards : fiveCardCombos) {
	        HandRank currentRank = evaluateFiveCardHand(fiveCards);
	        if (bestHand == null || currentRank.compareTo(bestHand) > 0) {
	            bestHand = currentRank;
	        }
	    }
	    
	    return bestHand;
	}

	// Generate all combinations of k cards from a list
	private static List<List<Card>> getAllCombinations(List<Card> cards, int k) {
	    List<List<Card>> result = new ArrayList<>();
	    combine(cards, k, 0, new ArrayList<>(), result);
	    return result;
	}

	private static void combine(List<Card> cards, int k, int start, 
	                             List<Card> current, List<List<Card>> result) {
	    if (current.size() == k) {
	        result.add(new ArrayList<>(current));
	        return;
	    }
	    for (int i = start; i < cards.size(); i++) {
	        current.add(cards.get(i));
	        combine(cards, k, i + 1, current, result);
	        current.remove(current.size() - 1);
	    }
	}
    
    public static HandRank evaluateFiveCardHand(List<Card> hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("Hand must have exactly 5 cards");
        }
        
        List<Integer> ranks = getRankValues(hand);
        Collections.sort(ranks, Collections.reverseOrder()); // High to low
        
        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);
        Map<Integer, Integer> rankCounts = getRankCountsMap(hand);
        
        // Royal Flush
        if (isStraight && isFlush && ranks.get(0) == 14 && ranks.get(4) == 10) {
            return new HandRank(HandType.ROYAL_FLUSH, new ArrayList<>(), new ArrayList<>());
        }
        
        // Straight Flush
        if (isStraight && isFlush) {
            int highCard = getStraightHighCard(hand);
            return new HandRank(HandType.STRAIGHT_FLUSH, 
                List.of(highCard), new ArrayList<>());
        }
        
        // Four of a Kind
        if (hasCount(rankCounts, 4)) {
            int quadRank = getRankWithCount(rankCounts, 4);
            int kicker = getRankWithCount(rankCounts, 1);
            return new HandRank(HandType.FOUR_OF_A_KIND, 
                List.of(quadRank), List.of(kicker));
        }
        
        // Full House
        if (hasCount(rankCounts, 3) && hasCount(rankCounts, 2)) {
            int tripleRank = getRankWithCount(rankCounts, 3);
            int pairRank = getRankWithCount(rankCounts, 2);
            return new HandRank(HandType.FULL_HOUSE, 
                List.of(tripleRank, pairRank), new ArrayList<>());
        }
        
        // Flush
        if (isFlush) {
            return new HandRank(HandType.FLUSH, 
                ranks, new ArrayList<>());
        }
        
        // Straight
        if (isStraight) {
            int highCard = getStraightHighCard(hand);
            return new HandRank(HandType.STRAIGHT, 
                List.of(highCard), new ArrayList<>());
        }
        
        // Three of a Kind
        if (hasCount(rankCounts, 3)) {
            int tripleRank = getRankWithCount(rankCounts, 3);
            List<Integer> kickers = getRanksWithCount(rankCounts, 1);
            Collections.sort(kickers, Collections.reverseOrder());
            return new HandRank(HandType.THREE_OF_A_KIND, 
                List.of(tripleRank), kickers);
        }
        
        // Two Pair
        if (getCountOfCount(rankCounts, 2) == 2) {
            List<Integer> pairs = getRanksWithCount(rankCounts, 2);
            Collections.sort(pairs, Collections.reverseOrder());
            int kicker = getRankWithCount(rankCounts, 1);
            return new HandRank(HandType.TWO_PAIR, 
                List.of(pairs.get(0), pairs.get(1)), List.of(kicker));
        }
        
        // One Pair
        if (hasCount(rankCounts, 2)) {
            int pairRank = getRankWithCount(rankCounts, 2);
            List<Integer> kickers = getRanksWithCount(rankCounts, 1);
            Collections.sort(kickers, Collections.reverseOrder());
            return new HandRank(HandType.ONE_PAIR, 
                List.of(pairRank), kickers);
        }
        
        // High Card
        return new HandRank(HandType.HIGH_CARD, 
            ranks, new ArrayList<>());
    }
    
    // Helper methods
    private static List<Integer> getRankValues(List<Card> hand) {
        return hand.stream()
            .map(c -> rankToInt(c.getRank()))
            .collect(Collectors.toList());
    }
    
    private static Map<Integer, Integer> getRankCountsMap(List<Card> hand) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card card : hand) {
            int rank = rankToInt(card.getRank());
            counts.put(rank, counts.getOrDefault(rank, 0) + 1);
        }
        return counts;
    }
    
    private static boolean hasCount(Map<Integer, Integer> counts, int target) {
        return counts.values().stream().anyMatch(c -> c == target);
    }
    
    private static int getRankWithCount(Map<Integer, Integer> counts, int target) {
        return counts.entrySet().stream()
            .filter(e -> e.getValue() == target)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(0);
    }
    
    private static List<Integer> getRanksWithCount(Map<Integer, Integer> counts, int target) {
        return counts.entrySet().stream()
            .filter(e -> e.getValue() == target)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private static int getCountOfCount(Map<Integer, Integer> counts, int target) {
        return (int) counts.values().stream().filter(c -> c == target).count();
    }
    
    private static int getStraightHighCard(List<Card> hand) {
        List<Integer> ranks = getRankValues(hand);
        Collections.sort(ranks);
        
        // Check for Ace-low straight (A-2-3-4-5)
        if (ranks.contains(14) && ranks.contains(2) && 
            ranks.contains(3) && ranks.contains(4) && ranks.contains(5)) {
            return 5;  // 5 is the high card of an Ace-low straight
        }
        
        // Normal straight - return the highest rank
        return Collections.max(ranks);
    }
    
    private static boolean isFlush(List<Card> hand) {
        Card.Suit firstSuit = hand.get(0).getSuit();
        return hand.stream().allMatch(c -> c.getSuit() == firstSuit);
    }
    
    private static boolean isStraight(List<Card> hand) {
        List<Integer> ranks = getRankValues(hand);
        Set<Integer> uniqueRanks = new TreeSet<>(ranks);
        
        if (uniqueRanks.size() < 5) return false;
        
        List<Integer> sorted = new ArrayList<>(uniqueRanks);
        Collections.sort(sorted);
        
        // Check Ace-low
        if (sorted.contains(14) && sorted.contains(2) && 
            sorted.contains(3) && sorted.contains(4) && sorted.contains(5)) {
            return true;
        }
        
        // Check normal straight
        for (int i = 0; i <= sorted.size() - 5; i++) {
            if (sorted.get(i + 4) == sorted.get(i) + 4) {
                return true;
            }
        }
        return false;
    }
    
    private static int rankToInt(Card.Rank rank) {
        switch (rank) {
            case TWO: return 2; case THREE: return 3; case FOUR: return 4;
            case FIVE: return 5; case SIX: return 6; case SEVEN: return 7;
            case EIGHT: return 8; case NINE: return 9; case TEN: return 10;
            case JACK: return 11; case QUEEN: return 12; case KING: return 13;
            case ACE: return 14;
            default: return 0;
        }
    }
}