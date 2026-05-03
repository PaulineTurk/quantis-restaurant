package model.user;

import model.order.SingleRestaurantOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static model.order.AbstractOrder.PLATFORM_LOYALTY_DISCOUNT;
import static model.order.AbstractOrder.RESTAURANT_LOYALTY_DISCOUNT;
import static model.user.Customer.Type.*;
import static model.user.CustomerOrderUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerSingleOrderPricingTest {
    private Customer customer;

    @BeforeEach
    void createCustomer() {
        customer = new Customer("A", "A", OTHER);
    }


    @Nested
    class BasePriceWithoutDiscount {

        @Test
        void singleMealNoDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void multipleMealsNoDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE);
        }
    }

    @Nested
    class OrderWithDiscountBasedOnCustomerType {

        @Test
        void childDiscountSingleMeal() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_1_PRICE * (1 - CHILD.getDiscount()));
        }

        @Test
        void childDiscountMultipleMeals() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(child)).isEqualTo((MEAL_1_PRICE + MEAL_2_PRICE) * (1 - CHILD.getDiscount()));
        }

        @Test
        void studentDiscountSingleMeal() {
            Customer student = new Customer("A", "A", STUDENT);
            student.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(student)).isEqualTo(MEAL_2_PRICE * (1 - STUDENT.getDiscount()));
        }
    }


    @Nested
    class PlatformLoyalty {

        @Test
        void firstOrderShouldNotTriggerPlatformLoyalty() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void tenthOrderTriggersPlatformLoyalty() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 9);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void eleventhOrderNoLongerTriggersPlatformLoyalty() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 10);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void twentiethOrderTriggersPlatformLoyaltyAgain() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 19);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void firstOrderAtRestaurantShouldNotTriggerLoyalty() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void fifthOrderAtSameRestaurantTriggersLoyalty() {
            warmup(customer, RESTAURANT, MEAL_1, 4);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE * (1 - RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void restaurantLoyaltyDoesNotCrossRestaurants() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 4);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void sixthOrderNoLongerTriggersRestaurantLoyalty() {
            warmup(customer, RESTAURANT, MEAL_1, 5);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }
    }


    @Nested
    class RetentionDiscount {

        @Test
        void firstOrderNoRetentionDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE);
        }

        @Test
        void secondOrderSameDayCheapestMealFree() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }

        @Test
        void retentionWithOnlyOneMealNoEffect() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void orderExactlySevenDaysAgoShouldNotTriggerRetention() {
            Clock sevenDaysAgo = Clock.fixed(Instant.now().minus(7, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), sevenDaysAgo));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE);
        }

        @Test
        void orderSixDaysAgoShouldTriggerRetention() {
            Clock sixDaysAgo = Clock.fixed(Instant.now().minus(6, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), sixDaysAgo));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }
    }


    @Nested
    class CombinedDiscounts {

        @Test
        void childWithRestaurantLoyalty() {
            Customer child = new Customer("A", "A", CHILD);
            warmup(child, RESTAURANT, MEAL_1, 4);
            child.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_2_PRICE * (1 - CHILD.getDiscount() - RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void studentWithPlatformLoyalty() {
            Customer student = new Customer("A", "A", STUDENT);
            warmup(student, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 9);
            student.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(student)).isEqualTo(MEAL_1_PRICE * (1 - STUDENT.getDiscount() - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void customerWithPlatformAndRestaurantLoyalty() {
            warmup(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, 5);
            warmup(customer, RESTAURANT, MEAL_1, 4);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT - RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void retentionAndChildDiscount() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(RESTAURANT, List.of(MEAL_1));
            child.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_2_PRICE * (1 - CHILD.getDiscount()));
        }
    }

    @Nested
    class InvalidMeals {

        @Test
        void nullMealListShouldThrow() {
            assertThatThrownBy(() -> customer.makeOrder(RESTAURANT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void emptyMealListShouldThrow() {
            assertThatThrownBy(() -> customer.makeOrder(RESTAURANT, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An order must contain at least one meal.");
        }
    }
}