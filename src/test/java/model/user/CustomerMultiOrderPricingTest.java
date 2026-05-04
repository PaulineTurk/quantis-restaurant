package model.user;

import model.order.EmptyOrderException;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;
import static model.order.Order.*;
import static model.user.Customer.RETENTION_THRESHOLD;
import static model.user.Customer.Type.CHILD;
import static model.user.Customer.Type.OTHER;
import static model.user.CustomerOrderUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomerMultiOrderPricingTest {
    private Customer customer;

    @BeforeEach
    void createCustomer() {
        customer = new Customer("A", "A", OTHER);
    }

    @Nested
    class BasePriceWithoutDiscount {

        @Test
        void getPrice_whenMultiOrder_thenSumOfSubOrderPrices() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_OTHER_PRICE));
        }

        @Test
        void makeOrder_whenMultiOrder_thenCountsAsOneForCustomerAndOnePerRestaurant() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));
            assertThat(customer.getOrders()).hasSize(1);
            assertThat(RESTAURANT.getOrders().stream()
                    .filter(order -> order.getCustomer().equals(customer)).toList())
                    .hasSize(1);
            assertThat(OTHER_RESTAURANT.getOrders().stream()
                    .filter(order -> order.getCustomer().equals(customer)).toList())
                    .hasSize(1);
            assertThat(NEUTRAL_RESTAURANT.getOrders().stream()
                    .filter(order -> order.getCustomer().equals(customer)).toList())
                    .isEmpty();
        }

        @Test
        void getPrice_whenMultiOrderWithSingleRestaurant_thenSumOfMeals() {
            customer.makeOrder(Map.of(RESTAURANT, List.of(MEAL_1, MEAL_2)));
            assertThat(customer.getOrders()).hasSize(1);
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_2_PRICE));
        }
    }

    @Nested
    class OrderWithDiscountBasedOnCustomerType {

        @Test
        void getPrice_whenChild_thenDiscountAppliedAcrossAllSubOrders() {
            Customer child = new Customer("A", "A", CHILD);

            child.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(child))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE.add(MEAL_OTHER_PRICE), CHILD.getDiscount()));
        }
    }

    @Nested
    class PlatformLoyalty {

        @Test
        void getPrice_whenNoLoyaltyToPlatform_thenNoPlatformLoyaltyDiscount() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD - 2);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_OTHER_PRICE));
        }

        @Test
        void getPrice_whenLoyalToPlatform_thenPlatformLoyaltyAppliedToAllSubOrders() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD - 1);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE.add(MEAL_OTHER_PRICE), PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenFirstMultiOrder_thenNoPlatformLoyaltyDiscount() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_OTHER_PRICE));
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void getPrice_whenRestaurantLoyaltyOnOneSubOrder_thenDiscountOnlyOnThisSubOrder() {
            addPastOrder(customer, RESTAURANT, MEAL_1, RESTAURANT_LOYALTY_THRESHOLD - 1);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_2_PRICE, RESTAURANT_LOYALTY_DISCOUNT)
                            .add(MEAL_OTHER_PRICE));
        }

        @Test
        void getPrice_whenRestaurantLoyaltyOnBothSubOrders_thenDiscountAppliedToEach() {
            addPastMultiOrder(customer, Map.of(
                            RESTAURANT, List.of(MEAL_1),
                            OTHER_RESTAURANT, List.of(MEAL_OTHER)),
                    RESTAURANT_LOYALTY_THRESHOLD - 1);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE.add(MEAL_OTHER_PRICE), RESTAURANT_LOYALTY_DISCOUNT));
        }
    }

    @Nested
    class RetentionDiscount {

        @Test
        void getPrice_whenRetentionPeriod_thenCheapestMealFreeAcrossSubOrders() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE
                            .add(MEAL_2_PRICE)
                            .add(MEAL_OTHER_PRICE)
                            .add(MEAL_OTHER_PRICE)
                            .subtract(MEAL_1_PRICE));
        }

        @Test
        void getPrice_whenFirstMultiOrder_thenNoRetentionDiscount() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE
                            .add(MEAL_2_PRICE)
                            .add(MEAL_OTHER_PRICE)
                            .add(MEAL_OTHER_PRICE));
        }

        @Test
        void getPrice_whenOrderAfterRetentionPeriod_thenNoRetentionDiscount() {
            Clock afterRetentionPeriod = Clock.fixed(Instant.now().minus(RETENTION_THRESHOLD, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), afterRetentionPeriod));

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE
                            .add(MEAL_2_PRICE)
                            .add(MEAL_OTHER_PRICE)
                            .add(MEAL_OTHER_PRICE));
        }

        @Test
        void getPrice_whenCheapestMealDuplicatedAcrossSubOrders_thenOnlyOneFree() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_1, MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE
                    .add(MEAL_1_PRICE)
                    .add(MEAL_OTHER_PRICE));
        }
    }

    @Nested
    class InvalidMeals {

        @Test
        void makeOrder_whenDuplicateRestaurant_thenThrowsIllegalArgumentException() {
            Restaurant duplicate = new Restaurant("Le Ticino");

            assertThatThrownBy(() -> customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    duplicate, List.of(MEAL_2)
            ))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Restaurant : Le Ticino is duplicated in multi-restaurant order");
        }

        @Test
        void makeOrder_whenNullMap_thenThrowsEmptyOrderException() {
            assertThatThrownBy(() -> customer.makeOrder((Map<Restaurant, List<String>>) null))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void makeOrder_whenEmptyMap_thenThrowsEmptyOrderException() {
            assertThatThrownBy(() -> customer.makeOrder(Map.of()))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void makeOrder_whenEmptyMealListForOneRestaurant_thenThrowsEmptyOrderException() {
            Map<Restaurant, List<String>> meals = new java.util.HashMap<>();
            meals.put(RESTAURANT, List.of(MEAL_1));
            meals.put(OTHER_RESTAURANT, List.of());

            assertThatThrownBy(() -> customer.makeOrder(meals))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void makeOrder_whenNullMealListForOneRestaurant_thenThrowsEmptyOrderException() {
            Map<Restaurant, List<String>> meals = new java.util.HashMap<>();
            meals.put(RESTAURANT, List.of(MEAL_1));
            meals.put(OTHER_RESTAURANT, null);

            assertThatThrownBy(() -> customer.makeOrder(meals))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }
    }
}