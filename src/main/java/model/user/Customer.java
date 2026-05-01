package model.user;

import lombok.Getter;
import model.restaurant.Restaurant;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static model.user.IOrder.RETENTION_THRESHOLD;

public class Customer implements User {
    @Getter
    private final String firstName;

    @Getter
    private final String lastName;

    @Getter
    private final Type type;

    @Getter
    private final List<IOrder> orders;

    public Customer(String firstName, String lastName, Type type) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.type = type;
        this.orders = new ArrayList<>();
    }

    public boolean hasOrderedInTheRetentionPeriod(IOrder toExclude) {
        return orders.stream()
                .filter(order -> order != toExclude)
                .map(IOrder::getDate)
                .max(Comparator.naturalOrder())
                .map(lastDate -> ChronoUnit.DAYS.between(lastDate, LocalDate.now()) < RETENTION_THRESHOLD)
                .orElse(false);
    }

    public void makeOrder(Restaurant restaurant, List<String> meals) {
        orders.add(new Order(restaurant, this, meals));
    }

    public void makeOrder(Map<Restaurant, List<String>> mealsByRestaurant) {
        orders.add(new MultiRestaurantOrder(mealsByRestaurant, this));
    }

    public enum Type {
        CHILD(0.5),
        STUDENT(0.25),
        OTHER(0.0);

        @Getter
        private final double discount;

        Type(double discount) {
            this.discount = discount;
        }
    }
}
