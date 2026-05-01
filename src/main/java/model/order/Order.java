package model.order;

import model.restaurant.Restaurant;

import java.time.LocalDate;

public interface Order {
    int PLATFORM_LOYALTY_THRESHOLD = 10;
    int RESTAURANT_LOYALTY_THRESHOLD = 5;
    int RETENTION_THRESHOLD = 7;
    double PLATFORM_LOYALTY_DISCOUNT = 0.10;
    double RESTAURANT_LOYALTY_DISCOUNT = 0.15;
    int MIN_MEALS_FOR_FREE_CHEAPEST = 2;

    LocalDate getDate();

    Double getPrice();

    boolean involvesRestaurant(Restaurant restaurant);
}
