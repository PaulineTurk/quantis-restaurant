package model.order;

import lombok.Getter;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.time.Clock;
import java.util.List;

import static java.lang.String.format;

public class SingleRestaurantOrder extends AbstractOrder {
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
    public Double getPrice() {
        double discount = computeDiscount(restaurant);
        return freeMealAmong(meals)
                .map(freeMeal -> computePrice(meals, discount, freeMeal))
                .orElseGet(() -> computePrice(meals, discount));
    }

    @Override
    public boolean involvesRestaurant(Restaurant restaurant) {
        return this.restaurant.equals(restaurant);
    }

    private double computeDiscount(Restaurant restaurant) {
        double discount = computeCustomerTypeDiscount();
        discount += computePlatformLoyaltyDiscount();
        discount += computeRestaurantLoyaltyDiscount(restaurant);
        return discount;

    }
}
