package model.order;

import lombok.Getter;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class MultiRestaurantOrder extends Order {

    @Getter
    private final List<SingleRestaurantOrder> subOrders;

    public MultiRestaurantOrder(Map<Restaurant, List<String>> mealsByRestaurant, Customer customer) {
        this(mealsByRestaurant, customer, Clock.systemDefaultZone());
    }

    public MultiRestaurantOrder(Map<Restaurant, List<String>> mealsByRestaurant, Customer customer, Clock clock) {
        super(customer, clock);
        if (mealsByRestaurant == null || mealsByRestaurant.isEmpty())
            throw new EmptyOrderException();
        mealsByRestaurant.forEach((restaurant, mealNames) -> validateMeals(mealNames));
        mealsByRestaurant.keySet().stream()
                .collect(Collectors.groupingBy(Restaurant::getName))
                .forEach((name, restaurants) -> {
                    if (restaurants.size() > 1)
                        throw new IllegalArgumentException("Restaurant : " + name + " is duplicated in multi-restaurant order");
                });
        this.subOrders = mealsByRestaurant.entrySet().stream()
                .map(e -> new SingleRestaurantOrder(e.getKey(), customer, e.getValue(), clock))
                .toList();
    }

    @Override
    public String getName() {
        String restaurants = subOrders.stream()
                .map(order -> order.getRestaurant().getName())
                .collect(joining(", "));
        return format("From %s - in [%s]", getCustomer().getName(), restaurants);
    }

    @Override
    public BigDecimal getPrice() {
        List<Meal> allMeals = this.subOrders
                .stream()
                .flatMap(order -> order.getMeals().stream())
                .toList();

        BigDecimal discount = computeCustomerTypeDiscount().add(computePlatformLoyaltyDiscount());

        return freeMealAmong(allMeals)
                .map(freeMeal -> subOrders.stream()
                        .map(order -> computePrice(order.getMeals(), discount.add(computeRestaurantLoyaltyDiscount(order.getRestaurant())), freeMeal))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElseGet(() -> subOrders.stream()
                        .map(order -> computePrice(order.getMeals(), discount.add(computeRestaurantLoyaltyDiscount(order.getRestaurant()))))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

    }

    @Override
    public boolean involvesRestaurant(Restaurant restaurant) {
        return subOrders.stream().anyMatch(order -> order.involvesRestaurant(restaurant));
    }
}
