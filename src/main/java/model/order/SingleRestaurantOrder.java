package model.order;

import lombok.Getter;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import static java.lang.String.format;

public class SingleRestaurantOrder extends Order {
    @Getter
    private final Restaurant restaurant;

    @Getter
    private final List<Meal> meals;

    public SingleRestaurantOrder(Restaurant restaurant, Customer customer, List<String> mealNames) {
        this(restaurant, customer, mealNames, Clock.systemDefaultZone());
    }

    public SingleRestaurantOrder(Restaurant restaurant, Customer customer, List<String> mealNames, Clock clock) {
        super(customer, clock);
        validateMeals(mealNames);
        this.restaurant = restaurant.withReceivedOrder(this);
        this.meals = mealNames.stream().map(restaurant::getMealByName).toList();
    }

    @Override
    public String getName() {
        return format("From %s - in %s", getCustomer().getName(), restaurant.getName());
    }

    @Override
    public BigDecimal getPrice() {
        BigDecimal discount = computeDiscount(restaurant);
        return freeMealAmong(meals)
                .map(freeMeal -> computePrice(meals, discount, freeMeal))
                .orElseGet(() -> computePrice(meals, discount));
    }

    @Override
    public boolean involvesRestaurant(Restaurant restaurant) {
        return this.restaurant.equals(restaurant);
    }

    private BigDecimal computeDiscount(Restaurant restaurant) {
        BigDecimal discount = computeCustomerTypeDiscount();
        discount = discount.add(computePlatformLoyaltyDiscount());
        discount = discount.add(computeRestaurantLoyaltyDiscount(restaurant));
        return discount;

    }
}
