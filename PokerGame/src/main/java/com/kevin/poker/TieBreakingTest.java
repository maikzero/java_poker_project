package com.kevin.poker;

import java.util.*;

import com.kevin.poker.HandRank.HandType;

public class TieBreakingTest {
    public static void main(String[] args) {
        
        // Test 1: One Pair - higher pair wins
        HandRank pairOfAces = makeOnePair(14, List.of(13, 12, 11)); // A-A-K-Q-J
        HandRank pairOfKings = makeOnePair(13, List.of(14, 12, 11)); // K-K-A-Q-J
        System.out.println("AA vs KK: " + pairOfAces.compareTo(pairOfKings)); // Positive (AA wins)
        
        // Test 2: One Pair - same pair, higher kicker wins
        HandRank pairAcesKickerK = makeOnePair(14, List.of(13, 12, 5)); // A-A-K-Q-5
        HandRank pairAcesKickerQ = makeOnePair(14, List.of(12, 10, 9)); // A-A-Q-10-9
        System.out.println("Same pair, K vs Q kicker: " + 
            pairAcesKickerK.compareTo(pairAcesKickerQ)); // Positive (K beats Q)
        
        // Test 3: Two Pair - higher top pair wins
        HandRank twoPairAcesAndKings = makeTwoPair(14, 13, 12); // A-A-K-K-Q
        HandRank twoPairKingsAndQueens = makeTwoPair(13, 12, 14); // K-K-Q-Q-A
        System.out.println("AAKK vs KKQQ: " + 
            twoPairAcesAndKings.compareTo(twoPairKingsAndQueens)); // Positive (AA beats KK)
        
        // Test 4: Two Pair - same top pair, higher second pair wins
        HandRank twoPairAcesKings = makeTwoPair(14, 13, 12); // A-A-K-K-Q
        HandRank twoPairAcesQueens = makeTwoPair(14, 12, 13); // A-A-Q-Q-K
        System.out.println("AAKK vs AAQQ: " + 
            twoPairAcesKings.compareTo(twoPairAcesQueens)); // Positive (KK beats QQ)
        
        // Test 5: Straight - higher straight wins
        HandRank straight10High = makeStraight(10); // 10-9-8-7-6
        HandRank straight9High = makeStraight(9);   // 9-8-7-6-5
        System.out.println("10-high vs 9-high straight: " + 
            straight10High.compareTo(straight9High)); // Positive
        
        // Test 6: Flush - compare card by card
        HandRank flushAKQJ9 = makeFlush(List.of(14, 13, 12, 11, 9));
        HandRank flushAKQJ8 = makeFlush(List.of(14, 13, 12, 11, 8));
        System.out.println("Flush AKQJ9 vs AKQJ8: " + 
            flushAKQJ9.compareTo(flushAKQJ8)); // Positive (9 beats 8)
        
        // Test 7: Full House - compare trips first
        HandRank fullHouseAAA88 = makeFullHouse(14, 8);  // A-A-A-8-8
        HandRank fullHouseKKKAA = makeFullHouse(13, 14); // K-K-K-A-A
        System.out.println("AAA88 vs KKKAA: " + 
            fullHouseAAA88.compareTo(fullHouseKKKAA)); // Positive (AAA beats KKK)
        
        // Test 8: Perfect tie
        HandRank hand1 = makeOnePair(10, List.of(9, 8, 7));
        HandRank hand2 = makeOnePair(10, List.of(9, 8, 7));
        System.out.println("Perfect tie: " + hand1.compareTo(hand2)); // 0
    }
    
    // Helper methods for testing
    private static HandRank makeOnePair(int pairRank, List<Integer> kickers) {
        return new HandRank(HandType.ONE_PAIR, List.of(pairRank), kickers);
    }
    
    private static HandRank makeTwoPair(int highPair, int lowPair, int kicker) {
        return new HandRank(HandType.TWO_PAIR, List.of(highPair, lowPair), List.of(kicker));
    }
    
    private static HandRank makeStraight(int highCard) {
        return new HandRank(HandType.STRAIGHT, List.of(highCard), new ArrayList<>());
    }
    
    private static HandRank makeFlush(List<Integer> cards) {
        return new HandRank(HandType.FLUSH, cards, new ArrayList<>());
    }
    
    private static HandRank makeFullHouse(int trips, int pair) {
        return new HandRank(HandType.FULL_HOUSE, List.of(trips, pair), new ArrayList<>());
    }
}