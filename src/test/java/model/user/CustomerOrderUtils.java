package model.user;

import model.order.MultiRestaurantOrder;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;
import static model.user.Customer.RETENTION_THRESHOLD;

public class CustomerOrderUtils {
    protected static final String MEAL_1 = "Meal 1";
    protected static final String MEAL_2 = "Meal 2";
    protected static final String MEAL_OTHER = "Meal other";
    protected static final String MEAL_NEUTRAL = "Meal neutral";

    protected static final BigDecimal MEAL_1_PRICE = new BigDecimal("10.00");
    protected static final BigDecimal MEAL_2_PRICE = new BigDecimal("20.00");
    protected static final BigDecimal MEAL_OTHER_PRICE = new BigDecimal("30.00");
    protected static final BigDecimal MEAL_NEUTRAL_PRICE = new BigDecimal("40.00");
    protected static final Clock AFTER_RETENTION_PERIOD = Clock.fixed(Instant.now().minus(RETENTION_THRESHOLD + 1, DAYS), ZoneId.systemDefault());

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

    protected static BigDecimal lastOrderPrice(Customer customer) {
        return customer.getOrders().getLast().getPrice();
    }

    protected static void addPastOrder(Customer customer, Restaurant r, String meal, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new SingleRestaurantOrder(r, customer, List.of(meal), AFTER_RETENTION_PERIOD));
        }
    }

    protected static void addPastMultiOrder(Customer customer, Map<Restaurant, List<String>> mealByRestaurant, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new MultiRestaurantOrder(mealByRestaurant, customer, AFTER_RETENTION_PERIOD));
        }
    }

    protected static BigDecimal priceAfterDiscount(BigDecimal price, BigDecimal... discounts) {
        BigDecimal totalDiscount = Arrays.stream(discounts)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return price.multiply(BigDecimal.ONE.subtract(totalDiscount));
    }
}
