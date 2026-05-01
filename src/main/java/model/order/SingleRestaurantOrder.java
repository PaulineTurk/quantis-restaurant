package model.order;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static java.lang.String.format;
import static java.time.LocalDate.now;

public class SingleRestaurantOrder implements Order, Entity {

    @Getter
    private final LocalDate date;

    @Getter
    private final Restaurant restaurant;

    @Getter
    private final Customer customer;

    @Getter
    private final List<Meal> meals;

    public SingleRestaurantOrder(Restaurant restaurant, Customer customer, List<String> mealNames) {
        this(restaurant, customer, mealNames, Clock.systemDefaultZone());
    }

    public SingleRestaurantOrder(Restaurant restaurant, Customer customer, List<String> mealNames, Clock clock) {
        this.date = now(clock);
        this.restaurant = restaurant.withReceivedOrder(this);
        this.customer = customer;
        this.meals = mealNames.stream().map(restaurant::getMealByName).toList();
    }

    public String getName() {
        return format("From %s - in %s", customer.getName(), restaurant.getName());
    }

    @Override
    public Double getPrice() {
        double discount = computeDiscount();
        if (customer.hasOrderedInTheRetentionPeriod(this) && meals.size() >= FREE_MEAL_POSITION) {
            return computePrice(discount, meals.get(FREE_MEAL_POSITION - 1));
        }
        return computePrice(discount);
    }

    protected double computeDiscount() {
        double discount = customer.getType().getDiscount();
        if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
            discount += PLATFORM_LOYALTY_DISCOUNT;
        if (customer.getOrders().stream().filter(order -> order.involvesRestaurant(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
            discount += RESTAURANT_LOYALTY_DISCOUNT;
        return discount;
    }

    protected double computePrice(double discount) {
        return meals.stream()
                .mapToDouble(meal -> meal.getPrice() * (1 - discount))
                .sum();
    }

    protected double computePrice(double discount, Meal freeMeal) {
        return meals.stream()
                .filter(meal -> !meal.equals(freeMeal))
                .mapToDouble(meal -> meal.getPrice() * (1 - discount))
                .sum();
    }

    @Override
    public boolean involvesRestaurant(Restaurant restaurant) {
        return this.restaurant.equals(restaurant);
    }
}
