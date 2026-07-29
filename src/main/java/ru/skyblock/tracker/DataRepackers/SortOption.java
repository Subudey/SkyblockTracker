package ru.skyblock.tracker.DataRepackers;

import java.util.Comparator;

public enum SortOption {
    HOURLY_PROFIT("Профит в час", Comparator.comparingDouble(FlipItem::hourlyProfit).reversed()),
    PROFIT_PER_UNIT("Профит / шт", Comparator.comparingDouble(FlipItem::profitPerUnit).reversed()),
    PERCENT_PROFIT("Процент профита", Comparator.comparingDouble(FlipItem::percentProfit).reversed()),
    BUY_PRICE_LOW("Цена покупки (сначала дешёвые)", Comparator.comparingDouble(FlipItem::buyOrderPrice)),
    BUY_PRICE_HIGH("Цена покупки (сначала дорогие)", Comparator.comparingDouble(FlipItem::buyOrderPrice).reversed()),
    VOLUME("Объём в час", Comparator.comparingDouble(FlipItem::volume).reversed());

    private final String displayName;
    private final Comparator<FlipItem> comparator;

    SortOption(String displayName, Comparator<FlipItem> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public Comparator<FlipItem> getComparator() {
        return comparator;
    }

    @Override
    public String toString() {
        return displayName;
    }
}