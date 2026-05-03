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
import static model.order.AbstractOrder.PLATFORM_LOYALTY_DISCOUNT;
import static model.order.AbstractOrder.RESTAURANT_LOYALTY_DISCOUNT;
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
        void multiOrderPriceIsSumOfSubOrders() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_OTHER_PRICE);
        }

        @Test
        void multiOrderCountsAsOneOrderForCustomer() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));
            assertThat(customer.getOrders()).hasSize(1);
        }
    }

    @Nested
    class OrderWithDiscountBasedOnCustomerType {

        @Test
        void childDiscountAppliedAcrossAllSubOrders() {
            Customer child = new Customer("A", "A", CHILD);

            child.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(child)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_OTHER_PRICE) * (1 - CHILD.getDiscount())
            );
        }
    }

    @Nested
    class PlatformLoyalty {

        @Test
        void multiOrderCountsAsOneForPlatformLoyalty() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 6);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(customer.getOrders()).hasSize(7);
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_OTHER_PRICE);
        }

        @Test
        void platformLoyaltyAppliedToAllSubOrdersWhenTriggered() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 9);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_OTHER_PRICE) * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void firstMultiOrderShouldNotTriggerPlatformLoyalty() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_OTHER_PRICE);
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void restaurantLoyaltyDoesNotCrossSubOrders() {
            warmup(customer, RESTAURANT, MEAL_1, 4);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    MEAL_1_PRICE * (1 - RESTAURANT_LOYALTY_DISCOUNT) + MEAL_OTHER_PRICE
            );
        }

        @Test
        void restaurantLoyaltyAppliedToEachSubOrderIndependently() {
            warmupMulti(customer, Map.of(
                            RESTAURANT, List.of(MEAL_1),
                            OTHER_RESTAURANT, List.of(MEAL_OTHER)),
                    4);

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer)).isEqualTo(
                    (MEAL_1_PRICE + MEAL_OTHER_PRICE) * (1 - RESTAURANT_LOYALTY_DISCOUNT)
            );
        }
    }

    @Nested
    class RetentionDiscount {

        @Test
        void retentionAppliedOnTheCheapestMealWhenOrderedInRetentionPeriod() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_OTHER_PRICE + MEAL_OTHER_PRICE - MEAL_1_PRICE);
        }

        @Test
        void noRetentionOnFirstMultiOrder() {
            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_OTHER_PRICE + MEAL_OTHER_PRICE);
        }

        @Test
        void retentionNotTriggeredAfterSevenDays() {
            Clock sevenDaysAgo = Clock.fixed(Instant.now().minus(7, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), sevenDaysAgo));

            customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1, MEAL_2),
                    OTHER_RESTAURANT, List.of(MEAL_OTHER, MEAL_OTHER)
            ));

            assertThat(lastOrderPrice(customer))
                    .isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_OTHER_PRICE + MEAL_OTHER_PRICE);
        }
    }

    @Nested
    class InvalidMeals {
        @Test
        void duplicateRestaurantInMultiOrderThrowsException() {
            Restaurant duplicate = new Restaurant("Le Ticino");

            assertThatThrownBy(() -> customer.makeOrder(Map.of(
                    RESTAURANT, List.of(MEAL_1),
                    duplicate, List.of(MEAL_2)
            ))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Restaurant : Le Ticino is duplicated in multi-restaurant order");
        }

        @Test
        void nullMapShouldThrow() {
            assertThatThrownBy(() -> customer.makeOrder((Map<Restaurant, List<String>>) null))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void emptyMapShouldThrow() {
            assertThatThrownBy(() -> customer.makeOrder(Map.of()))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void emptyMealListForOneRestaurantShouldThrow() {
            Map<Restaurant, List<String>> meals = new java.util.HashMap<>();
            meals.put(RESTAURANT, List.of(MEAL_1));
            meals.put(OTHER_RESTAURANT, List.of());

            assertThatThrownBy(() -> customer.makeOrder(meals))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void nullMealListForOneRestaurantShouldThrow() {
            Map<Restaurant, List<String>> meals = new java.util.HashMap<>();
            meals.put(RESTAURANT, List.of(MEAL_1));
            meals.put(OTHER_RESTAURANT, null);

            assertThatThrownBy(() -> customer.makeOrder(meals))
                    .isInstanceOf(EmptyOrderException.class)
                    .hasMessage("An order must contain at least one meal.");
        }
    }
}
