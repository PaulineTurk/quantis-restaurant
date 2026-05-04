package model.order;

import model.restaurant.Restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface Order {
    LocalDate getDate();

    BigDecimal getPrice();

    boolean involvesRestaurant(Restaurant restaurant);
}
