package model.order;

import model.restaurant.Restaurant;

import java.time.LocalDate;

public interface Order {
    LocalDate getDate();

    Double getPrice();

    boolean involvesRestaurant(Restaurant restaurant);
}
