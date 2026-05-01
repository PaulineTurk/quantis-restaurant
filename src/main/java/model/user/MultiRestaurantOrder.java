package model.user;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class MultiRestaurantOrder implements IOrder, Entity {

    @Getter
    private final LocalDate date;

    @Getter
    private final Customer customer;

    @Getter
    private final List<Order> subOrders;

    MultiRestaurantOrder(Map<Restaurant, List<String>> mealsByRestaurant, Customer customer) {
        this(mealsByRestaurant, customer, Clock.systemDefaultZone());
    }

    MultiRestaurantOrder(Map<Restaurant, List<String>> mealsByRestaurant, Customer customer, Clock clock) {
        this.date = LocalDate.now(clock);
        this.customer = customer;
        mealsByRestaurant.keySet().stream()
                .collect(Collectors.groupingBy(Restaurant::getName))
                .forEach((name, restaurants) -> {
                    if (restaurants.size() > 1)
                        throw new IllegalArgumentException("Restaurant : " + name + " is duplicated in multi-restaurant order");
                });
        this.subOrders = mealsByRestaurant.entrySet().stream()
                .map(e -> new Order(e.getKey(), customer, e.getValue()))
                .toList();
    }


    @Override
    public boolean involvesRestaurant(Restaurant restaurant) {
        return subOrders.stream().anyMatch(o -> o.involvesRestaurant(restaurant));
    }

    @Override
    public String getName() {
        String restaurants = subOrders.stream()
                .map(o -> o.getRestaurant().getName())
                .collect(joining(", "));
        return format("From %s - in [%s]", customer.getName(), restaurants);
    }

    @Override
    public Double getPrice() {
        if (!customer.hasOrderedInTheRetentionPeriod(this)) {
            return subOrders.stream()
                    .mapToDouble(order -> order.computePrice(order.computeDiscount()))
                    .sum();
        }

        Meal freeMeal = subOrders.stream()
                .flatMap(o -> o.getMeals().stream())
                .min(Comparator.comparingDouble(Meal::getPrice))
                .orElseThrow();

        return subOrders.stream()
                .mapToDouble(order -> order.computePrice(order.computeDiscount(), freeMeal))
                .sum();
    }
}
