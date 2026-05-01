package model.user;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.lang.String.format;
import static java.time.LocalDate.now;

public class Order implements Entity {
    public static final int PLATFORM_LOYALTY_THRESHOLD = 10;
    public static final int RESTAURANT_LOYALTY_THRESHOLD = 5;
    public static final int RETENTION_THRESHOLD = 7;
    public static final double PLATFORM_LOYALTY_DISCOUNT = 0.10;
    public static final double RESTAURANT_LOYALTY_DISCOUNT = 0.15;
    public static final int FREE_MEAL_POSITION = 2;

    @Getter
    private final LocalDate date;

    @Getter
    private final Restaurant restaurant;

    @Getter
    private final Customer customer;

    @Getter
    private final List<Meal> meals;

    Order(Restaurant restaurant, Customer customer, List<String> mealNames) {
        this(restaurant, customer, mealNames, Clock.systemDefaultZone());
    }

    Order(Restaurant restaurant, Customer customer, List<String> mealNames, Clock clock) {
        this.date = now(clock);
        this.restaurant = restaurant.withReceivedOrder(this);
        this.customer = customer;
        this.meals = mealNames.stream().map(restaurant::getMealByName).toList();
    }

    public String getName() {
        return format("From %s - in %s", customer.getName(), restaurant.getName());
    }

    public Double getPrice() {
        double totalAmount = 0D;
        boolean hasOrderedInThePastWeek = false;
        for (Order order : customer.getOrders()) {
            if (order != this && ChronoUnit.DAYS.between(order.date, now()) < RETENTION_THRESHOLD)
                hasOrderedInThePastWeek = true;
        }

        double discount = customer.getType().getDiscount();
        if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
            discount += PLATFORM_LOYALTY_DISCOUNT;
        if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
            discount += RESTAURANT_LOYALTY_DISCOUNT;

        for (int mealindex = 0; mealindex < meals.size(); mealindex++) {
            if (hasOrderedInThePastWeek && mealindex == FREE_MEAL_POSITION - 1)
                continue;
            totalAmount += meals.get(mealindex).getPrice() * (1 - discount);
        }

        return totalAmount;
    }
}
