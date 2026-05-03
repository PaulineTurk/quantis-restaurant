package model.user;

import model.order.MultiRestaurantOrder;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

public class CustomerOrderUtils {
    protected static final String MEAL_1 = "Meal 1";
    protected static final String MEAL_2 = "Meal 2";
    protected static final String MEAL_OTHER = "Meal other";
    protected static final String MEAL_NEUTRAL = "Meal neutral";

    protected static final double MEAL_1_PRICE = 10.0;
    protected static final double MEAL_2_PRICE = 20.0;
    protected static final double MEAL_OTHER_PRICE = 30.0;
    protected static final double MEAL_NEUTRAL_PRICE = 40.0;
    protected static final Clock EIGHT_DAYS_AGO = Clock.fixed(Instant.now().minus(8, DAYS), ZoneId.systemDefault());

    protected static final Restaurant RESTAURANT = new Restaurant("Le Ticino");
    protected static final Restaurant OTHER_RESTAURANT = new Restaurant("L'étoile");
    protected static final Restaurant NEUTRAL_RESTAURANT = new Restaurant("Le Neutre");

    protected static final RestaurantOwner OWNER = new RestaurantOwner("Robert", "Dupont", RESTAURANT);
    protected static final RestaurantOwner OTHER_OWNER = new RestaurantOwner("Magali", "Noel", OTHER_RESTAURANT);
    protected static final RestaurantOwner NEUTRAL_OWNER = new RestaurantOwner("X", "X", NEUTRAL_RESTAURANT);

    static {
        OWNER.addMeal(MEAL_1, MEAL_1_PRICE);
        OWNER.addMeal(MEAL_2, MEAL_2_PRICE);
        OTHER_OWNER.addMeal(MEAL_OTHER, MEAL_OTHER_PRICE);
        NEUTRAL_OWNER.addMeal(MEAL_NEUTRAL, MEAL_NEUTRAL_PRICE);
    }

    protected static Double lastOrderPrice(Customer customer) {
        return customer.getOrders().getLast().getPrice();
    }

    protected static void warmup(Customer customer, Restaurant r, String meal, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new SingleRestaurantOrder(r, customer, List.of(meal), EIGHT_DAYS_AGO));
        }
    }

    protected static void warmupMulti(Customer customer, Map<Restaurant, List<String>> mealByRestaurant, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new MultiRestaurantOrder(mealByRestaurant, customer, EIGHT_DAYS_AGO));
        }
    }
}
