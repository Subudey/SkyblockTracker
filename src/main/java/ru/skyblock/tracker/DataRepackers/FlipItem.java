package ru.skyblock.tracker.DataRepackers;

public interface FlipItem {
    String itemName();

    String itemId();

    double buyOrderPrice();

    double sellOrderPrice();

    double profitPerUnit();

    double percentProfit();

    double volume();

    double hourlyProfit();
}
