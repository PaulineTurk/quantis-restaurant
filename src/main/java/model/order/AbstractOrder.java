package model.order;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;

public abstract class AbstractOrder implements Order, Entity {
    public static final int RETENTION_THRESHOLD = 7;
    public static final double PLATFORM_LOYALTY_DISCOUNT = 0.10;
    public static final double RESTAURANT_LOYALTY_DISCOUNT = 0.15;
    protected static final int PLATFORM_LOYALTY_THRESHOLD = 10;
    protected static final int RESTAURANT_LOYALTY_THRESHOLD = 5;
    protected static final int MIN_MEALS_FOR_FREE_CHEAPEST = 2;

    @Getter
    private final LocalDate date;
    @Getter
    private final Customer customer;

    protected AbstractOrder(Customer customer, Clock clock) {
        this.date = now(clock);
        this.customer = customer;
    }

    protected double computePrice(List<Meal> meals, double discount) {
        return meals.stream()
                .mapToDouble(meal -> meal.getPrice() * (1 - discount))
                .sum();
    }

    protected double computePrice(List<Meal> meals, double discount, Meal freeMeal) {
        return meals.stream()
                .filter(meal -> !meal.equals(freeMeal))
                .mapToDouble(meal -> meal.getPrice() * (1 - discount))
                .sum();
    }

    protected Optional<Meal> freeMealAmong(List<Meal> meals) {
        if (!customer.hasOrderedInTheRetentionPeriod(this) || meals.size() < MIN_MEALS_FOR_FREE_CHEAPEST)
            return Optional.empty();
        return meals.stream().min(Comparator.comparingDouble(Meal::getPrice));
    }


    protected double computeCustomerTypeDiscount() {
        return customer.getType().getDiscount();
    }

    protected double computePlatformLoyaltyDiscount() {
        if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
            return PLATFORM_LOYALTY_DISCOUNT;
        return 0.0;
    }

    protected double computeRestaurantLoyaltyDiscount(Restaurant restaurant) {
        if (customer.getOrders().stream().filter(order -> order.involvesRestaurant(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
            return RESTAURANT_LOYALTY_DISCOUNT;
        return 0.0;
    }
}
