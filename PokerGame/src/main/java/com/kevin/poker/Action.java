package com.kevin.poker;

public class Action {
    private String type;  // "FOLD", "CALL", "RAISE"
    private int amount;
    
    public Action(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }
    
    public String getType() { return type; }
    public int getAmount() { return amount; }
}