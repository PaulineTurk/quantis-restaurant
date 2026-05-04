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
import static model.order.Order.*;
import static model.user.Customer.RETENTION_THRESHOLD;
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
        void getPrice_whenSingleMeal_thenFullPrice() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenMultipleMeals_thenSumOfPrices() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_2_PRICE));
        }
    }

    @Nested
    class OrderWithDiscountBasedOnCustomerType {

        @Test
        void getPrice_whenChild_thenDiscountAppliedToAllMeals() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(child))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE.add(MEAL_2_PRICE), CHILD.getDiscount()));
        }
    }

    @Nested
    class PlatformLoyalty {

        @Test
        void getPrice_whenFirstOrder_thenNoPlatformLoyaltyDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenLoyalToPlatform_thenPlatformLoyaltyDiscount() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD - 1);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE, PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenPlatformLoyaltyExceeded_thenNoPlatformLoyaltyDiscount() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenLoyaltyToPlatformReachedAgain_thenPlatformLoyaltyDiscount() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD * 2 - 1);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE, PLATFORM_LOYALTY_DISCOUNT));
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void getPrice_whenFirstOrderAtRestaurant_thenNoRestaurantLoyaltyDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenLoyaltyToRestaurant_thenRestaurantLoyaltyDiscount() {
            addPastOrder(customer, RESTAURANT, MEAL_1, RESTAURANT_LOYALTY_THRESHOLD - 1);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_2_PRICE, RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenLoyaltyOnRestaurantA_thenNoDiscountOnRestaurantB() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, RESTAURANT_LOYALTY_THRESHOLD - 1);
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenRestaurantLoyaltyExceeded_thenNoRestaurantLoyaltyDiscount() {
            addPastOrder(customer, RESTAURANT, MEAL_1, RESTAURANT_LOYALTY_THRESHOLD);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }
    }

    @Nested
    class RetentionDiscount {

        @Test
        void getPrice_whenFirstOrder_thenNoRetentionDiscount() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_2_PRICE));
        }

        @Test
        void getPrice_whenSecondOrderSameDay_thenCheapestMealFree() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }

        @Test
        void getPrice_whenRetentionWithOnlyOneMeal_thenNoFreeMeal() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void getPrice_whenLastOrderExactlyAfterRetentionPeriod_thenNoRetentionDiscount() {
            Clock afterRetentionPeriod = Clock.fixed(Instant.now().minus(RETENTION_THRESHOLD, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), afterRetentionPeriod));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_2_PRICE));
        }

        @Test
        void getPrice_whenLastOrderDuringRetentionPeriod_thenCheapestMealFree() {
            Clock duringRetentionPeriod = Clock.fixed(Instant.now().minus(RETENTION_THRESHOLD - 1, DAYS), ZoneId.systemDefault());
            customer.getOrders().add(new SingleRestaurantOrder(RESTAURANT, customer, List.of(MEAL_1), duringRetentionPeriod));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }

        @Test
        void getPrice_whenCheapestMealDuplicated_thenOnlyOneFree() {
            customer.makeOrder(RESTAURANT, List.of(MEAL_1));
            customer.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_1, MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE.add(MEAL_1_PRICE));
        }
    }

    @Nested
    class CombinedDiscounts {

        @Test
        void getPrice_whenChildAndRestaurantLoyalty_thenBothDiscountsApplied() {
            Customer child = new Customer("A", "A", CHILD);
            addPastOrder(child, RESTAURANT, MEAL_1, RESTAURANT_LOYALTY_THRESHOLD - 1);
            child.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(child))
                    .isEqualTo(priceAfterDiscount(MEAL_2_PRICE, CHILD.getDiscount(), RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenStudentAndPlatformLoyalty_thenBothDiscountsApplied() {
            Customer student = new Customer("A", "A", STUDENT);
            addPastOrder(student, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD - 1);
            student.makeOrder(RESTAURANT, List.of(MEAL_1));
            assertThat(lastOrderPrice(student))
                    .isEqualTo(priceAfterDiscount(MEAL_1_PRICE, STUDENT.getDiscount(), PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenPlatformAndRestaurantLoyalty_thenBothDiscountsApplied() {
            addPastOrder(customer, NEUTRAL_RESTAURANT, MEAL_NEUTRAL, PLATFORM_LOYALTY_THRESHOLD - RESTAURANT_LOYALTY_THRESHOLD);
            addPastOrder(customer, RESTAURANT, MEAL_1, RESTAURANT_LOYALTY_THRESHOLD - 1);
            customer.makeOrder(RESTAURANT, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer))
                    .isEqualTo(priceAfterDiscount(MEAL_2_PRICE, PLATFORM_LOYALTY_DISCOUNT, RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void getPrice_whenChildAndRetention_thenBothApplied() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(RESTAURANT, List.of(MEAL_1));
            child.makeOrder(RESTAURANT, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(child))
                    .isEqualTo(priceAfterDiscount(MEAL_2_PRICE, CHILD.getDiscount()));
        }
    }

    @Nested
    class InvalidMeals {

        @Test
        void makeOrder_whenNullMealList_thenThrowsEmptyOrderException() {
            assertThatThrownBy(() -> customer.makeOrder(RESTAURANT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An order must contain at least one meal.");
        }

        @Test
        void makeOrder_whenEmptyMealList_thenThrowsEmptyOrderException() {
            assertThatThrownBy(() -> customer.makeOrder(RESTAURANT, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("An order must contain at least one meal.");
        }
    }
}