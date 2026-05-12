package com.kevin.poker.network;

import com.kevin.poker.Card;
import com.kevin.poker.Player;

import java.util.List;

public interface PokerNetworkBridge {
    void onLog(String message);

    void onStreetChanged(String street);

    void onPotUpdated(int pot);

    void onCommunityCardsUpdated(List<Card> communityCards);

    void promptPlayer(Player player, int currentBet, int pot, List<Card> communityCards);

    void onPlayerJoined(Player player);

    void onPlayerLeft(int playerId);
    
    default void onHoleCardsDealt(int playerId, List<Card> holeCards) {
        // Override in PokerServer to send HOLE_CARDS message
    }
    
    default void onChipsUpdated(int playerId, int chips) {
    }
    
    default void onHandEnded() {
    }
    
    
}
