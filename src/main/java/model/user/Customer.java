package model.user;

import lombok.Getter;
import model.order.MultiRestaurantOrder;
import model.order.Order;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static model.order.Order.RETENTION_THRESHOLD;

public class Customer implements User {
    @Getter
    private final String firstName;

    @Getter
    private final String lastName;

    @Getter
    private final Type type;

    @Getter
    private final List<Order> orders;

    public Customer(String firstName, String lastName, Type type) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.type = type;
        this.orders = new ArrayList<>();
    }

    public boolean hasOrderedInTheRetentionPeriod(Order toExclude) {
        return orders.stream()
                .filter(order -> order != toExclude)
                .map(Order::getDate)
                .max(Comparator.naturalOrder())
                .map(lastDate -> ChronoUnit.DAYS.between(lastDate, LocalDate.now()) < RETENTION_THRESHOLD)
                .orElse(false);
    }

    public void makeOrder(Restaurant restaurant, List<String> meals) {
        orders.add(new SingleRestaurantOrder(restaurant, this, meals));
    }

    public void makeOrder(Map<Restaurant, List<String>> mealsByRestaurant) {
        orders.add(new MultiRestaurantOrder(mealsByRestaurant, this));
    }

    public enum Type {
        CHILD(new BigDecimal("0.50")),
        STUDENT(new BigDecimal("0.25")),
        OTHER(BigDecimal.ZERO);

        @Getter
        private final BigDecimal discount;

        Type(BigDecimal discount) {
            this.discount = discount;
        }
    }
}
