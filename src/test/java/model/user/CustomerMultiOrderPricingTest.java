package model.user;

import model.order.MultiRestaurantOrder;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;
import static model.order.Order.PLATFORM_LOYALTY_DISCOUNT;
import static model.order.Order.RESTAURANT_LOYALTY_DISCOUNT;
import static model.user.Customer.Type.CHILD;
import static model.user.Customer.Type.OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomerMultiOrderPricingTest {
    private final String MEAL_1 = "Meal 1";
    private final String MEAL_2 = "Meal 2";
    private final String MEAL_4 = "Meal 4";
    private final String MEAL_NEUTRAL = "Meal neutral";

    private final double MEAL_1_PRICE = 10.0;
    private final double MEAL_2_PRICE = 20.0;
    private final double MEAL_4_PRICE = 40.0;

    private Restaurant restaurant;
    private Restaurant otherRestaurant;
    private Restaurant neutralRestaurant;

    private Customer customer;
    private Clock eightDaysAgo;


    @BeforeEach
    void setUp() {
        customer = new Customer("A", "A", OTHER);
        restaurant = new Restaurant("Le Ticino");
        otherRestaurant = new Restaurant("L'étoile");
        neutralRestaurant = new Restaurant("Le Neutre");
        var owner = new RestaurantOwner("Robert", "Dupont", restaurant);
        var otherOwner = new RestaurantOwner("Magali", "Noel", otherRestaurant);
        var neutralOwner = new RestaurantOwner("X", "X", neutralRestaurant);
        owner.addMeal(MEAL_1, MEAL_1_PRICE);
        owner.addMeal(MEAL_2, MEAL_2_PRICE);
        String MEAL_3 = "Meal 3";
        double MEAL_3_PRICE = 30.0;
        owner.addMeal(MEAL_3, MEAL_3_PRICE);
        otherOwner.addMeal(MEAL_4, MEAL_4_PRICE);
        double MEAL_NEUTRAL_PRICE = 40.0;
        neutralOwner.addMeal(MEAL_NEUTRAL, MEAL_NEUTRAL_PRICE);

        eightDaysAgo = Clock.fixed(Instant.now().minus(8, DAYS), ZoneId.systemDefault());
    }

    private Double lastOrderPrice(Customer customer) {
        return customer.getOrders().getLast().getPrice();
    }

    private void warmup(Customer customer, Restaurant r, String meal, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new SingleRestaurantOrder(r, customer, List.of(meal), eightDaysAgo));
        }
    }

    private void warmupMulti(Customer customer, Map<Restaurant, List<String>> mealByRestaurant, int times) {
        for (int i = 0; i < times; i++) {
            customer.getOrders().add(new MultiRestaurantOrder(mealByRestaurant, customer, eightDaysAgo));
        }
    }


    @Nested
    class BasePrice {

        @Test
        void multiOrderPriceIsSumOfSubOrders() {
            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_4_PRICE);
        }

        @Test
        void multiOrderCountsAsOneOrderForCustomer() {
            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));
            assertThat(customer.getOrders()).hasSize(1);
        }
    }

    @Nested
    class PlatformLoyalty {

        @Test
        void multiOrderCountsAsOneForPlatformLoyalty() {
            warmup(customer, neutralRestaurant, MEAL_NEUTRAL, 6);

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(customer.getOrders()).hasSize(7);
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_4_PRICE);
        }

        @Test
        void platformLoyaltyAppliedToAllSubOrdersWhenTriggered() {
            warmup(customer, neutralRestaurant, MEAL_NEUTRAL, 9);

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_4_PRICE) * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void firstMultiOrderShouldNotTriggerPlatformLoyalty() {
            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_4_PRICE);
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void restaurantLoyaltyAppliedOnlyToItsSubOrder() {
            warmup(customer, restaurant, MEAL_1, 4);

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    MEAL_1_PRICE * (1 - RESTAURANT_LOYALTY_DISCOUNT) + MEAL_4_PRICE
            );
        }

        @Test
        void restaurantLoyaltyAppliedToEachSubOrderIndependently() {
            warmupMulti(customer, Map.of(
                            restaurant, List.of(MEAL_1),
                            otherRestaurant, List.of(MEAL_4)),
                    4);

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_4_PRICE) * (1 - RESTAURANT_LOYALTY_DISCOUNT)
            );
        }

        @Test
        void restaurantLoyaltyDoesNotCrossSubOrders() {
            warmup(customer, restaurant, MEAL_1, 4);

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    MEAL_1_PRICE * (1 - RESTAURANT_LOYALTY_DISCOUNT) + MEAL_4_PRICE
            );
        }
    }

    @Nested
    class RetentionDiscount {

        @Test
        void retentionAppliedOnTheCheapestMealWhenOrderedInRetentionPeriod() {
            customer.makeOrder(restaurant, List.of(MEAL_1));

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1, MEAL_2),
                    otherRestaurant, List.of(MEAL_4, MEAL_4)
            ));

            var cheapestPrice = Collections.min(List.of(MEAL_1_PRICE, MEAL_2_PRICE, MEAL_4_PRICE));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_4_PRICE + MEAL_4_PRICE - cheapestPrice);
        }

        @Test
        void noRetentionOnFirstMultiOrder() {
            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1, MEAL_2),
                    otherRestaurant, List.of(MEAL_4, MEAL_4)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_4_PRICE + MEAL_4_PRICE);
        }

        @Test
        void retentionNotTriggeredAfterSevenDays() {
            Customer customer = new Customer("A", "A", OTHER);
            Clock sevenDaysAgo = Clock.fixed(Instant.now().minus(7, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(restaurant, customer, List.of(MEAL_1), sevenDaysAgo));

            customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1, MEAL_2),
                    otherRestaurant, List.of(MEAL_4, MEAL_4)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_4_PRICE + MEAL_4_PRICE);
        }
    }


    @Nested
    class EdgeCases {

        @Test
        void duplicateRestaurantInMultiOrderThrowsException() {
            Restaurant duplicate = new Restaurant("Le Ticino");

            assertThatThrownBy(() -> customer.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    duplicate, List.of(MEAL_2)
            ))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Restaurant : Le Ticino is duplicated in multi-restaurant order");
        }

        @Test
        void childDiscountAppliedAcrossAllSubOrders() {
            Customer child = new Customer("A", "A", CHILD);

            child.makeOrder(Map.of(
                    restaurant, List.of(MEAL_1),
                    otherRestaurant, List.of(MEAL_4)
            ));

            assertThat(lastOrderPrice(child)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_4_PRICE) * (1 - CHILD.getDiscount())
            );
        }
    }
}
