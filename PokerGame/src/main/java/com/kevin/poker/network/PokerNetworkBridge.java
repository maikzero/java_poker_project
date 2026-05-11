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
}
