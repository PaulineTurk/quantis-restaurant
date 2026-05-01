package model.user;

import model.order.IOrder;
import model.order.SingleRestaurantOrder;
import model.restaurant.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static model.order.IOrder.PLATFORM_LOYALTY_DISCOUNT;
import static model.order.IOrder.RESTAURANT_LOYALTY_DISCOUNT;
import static model.user.Customer.Type.*;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSingleOrderPricingTest {

    private final String MEAL_1 = "Meal 1";
    private final String MEAL_2 = "Meal 2";
    private final String MEAL_3 = "Meal 3";
    private final String MEAL_4 = "Meal 4";
    private final double MEAL_1_PRICE = 10.0;
    private final double MEAL_2_PRICE = 20.0;
    private final double MEAL_3_PRICE = 30.0;
    private Restaurant restaurant;
    private Restaurant otherRestaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant("Le Ticino");
        otherRestaurant = new Restaurant("L'étoile");
        var owner = new RestaurantOwner("Robert", "Dupont", restaurant);
        var otherOwner = new RestaurantOwner("Magali", "Noel", otherRestaurant);

        owner.addMeal(MEAL_1, MEAL_1_PRICE);
        owner.addMeal(MEAL_2, MEAL_2_PRICE);
        owner.addMeal(MEAL_3, MEAL_3_PRICE);

        double MEAL_4_PRICE = 40.0;
        otherOwner.addMeal(MEAL_4, MEAL_4_PRICE);
    }

    private Double lastOrderPrice(Customer customer) {
        List<IOrder> orders = customer.getOrders();
        return orders.getLast().getPrice();
    }

    @Nested
    class BasePriceWithoutDiscount {

        @Test
        void singleMealNoDiscount() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void multipleMealsNoDiscount() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1, MEAL_2, MEAL_3));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE + MEAL_3_PRICE);
        }
    }

    @Nested
    class OrderWithDiscountBasedOnCustomerType {

        @Test
        void childDiscountSingleMeal() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_1_PRICE * (1 - CHILD.getDiscount()));
        }

        @Test
        void childDiscountMultipleMeals() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(restaurant, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(child)).isEqualTo((MEAL_1_PRICE + MEAL_2_PRICE) * (1 - CHILD.getDiscount()));
        }

        @Test
        void studentDiscountSingleMeal() {
            Customer student = new Customer("A", "A", STUDENT);
            student.makeOrder(restaurant, List.of(MEAL_2));
            assertThat(lastOrderPrice(student)).isEqualTo(MEAL_2_PRICE * (1 - STUDENT.getDiscount()));
        }
    }


    @Nested
    class PlatformLoyalty {

        @Test
        void firstOrderShouldNotTriggerPlatformLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void tenthOrderTriggersPlatformLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 9; i++) {
                customer.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void eleventhOrderNoLongerTriggersPlatformLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 10; i++) {
                customer.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void twentiethOrderTriggersPlatformLoyaltyAgain() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 19; i++) {
                customer.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            customer.makeOrder(restaurant, List.of(MEAL_1));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT));
        }
    }

    @Nested
    class RestaurantLoyalty {

        @Test
        void firstOrderAtRestaurantShouldNotTriggerLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void fifthOrderAtSameRestaurantTriggersLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 4; i++) {
                customer.makeOrder(restaurant, List.of(MEAL_1));
            }
            customer.makeOrder(restaurant, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE * (1 - RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void restaurantLoyaltyDoesNotCrossRestaurants() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 4; i++) {
                customer.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void sixthOrderNoLongerTriggersRestaurantLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 5; i++) {
                customer.makeOrder(restaurant, List.of(MEAL_1));
            }
            customer.makeOrder(restaurant, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }
    }


    @Nested
    class RetentionDiscount {

        @Test
        void firstOrderNoRetentionDiscount() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE + MEAL_2_PRICE);
        }

        @Test
        void secondOrderSameDaySecondMealFree() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_3));
            customer.makeOrder(restaurant, List.of(MEAL_1, MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void retentionWithOnlyOneMealNoEffect() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_2));
            customer.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_1_PRICE);
        }

        @Test
        void secondMealIsOfferedNotCheapest() {
            Customer customer = new Customer("A", "A", OTHER);
            customer.makeOrder(restaurant, List.of(MEAL_1));
            customer.makeOrder(restaurant, List.of(MEAL_2, MEAL_3));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }

        @Test
        void orderExactlySevenDaysAgoShouldNotTriggerRetention() {
            Customer customer = new Customer("A", "A", OTHER);
            Clock sevenDaysAgo = Clock.fixed(Instant.now().minus(7, DAYS), ZoneId.systemDefault());

            customer.getOrders().add(new SingleRestaurantOrder(restaurant, customer, List.of(MEAL_1), sevenDaysAgo));
            customer.makeOrder(restaurant, List.of(MEAL_2, MEAL_3));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE + MEAL_3_PRICE);
        }

        @Test
        void orderSixDaysAgoShouldTriggerRetention() {
            Customer customer = new Customer("A", "A", OTHER);
            Clock sixDaysAgo = Clock.fixed(Instant.now().minus(6, DAYS), ZoneId.systemDefault());

            customer.getOrders().add(new SingleRestaurantOrder(restaurant, customer, List.of(MEAL_1), sixDaysAgo));
            customer.makeOrder(restaurant, List.of(MEAL_2, MEAL_3));

            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE);
        }
    }


    @Nested
    class CombinedDiscounts {

        @Test
        void childWithRestaurantLoyalty() {
            Customer child = new Customer("A", "A", CHILD);
            for (int i = 0; i < 4; i++) {
                child.makeOrder(restaurant, List.of(MEAL_1));
            }
            child.makeOrder(restaurant, List.of(MEAL_2));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_2_PRICE * (1 - CHILD.getDiscount() - RESTAURANT_LOYALTY_DISCOUNT));
        }

        @Test
        void studentWithPlatformLoyalty() {
            Customer student = new Customer("A", "A", STUDENT);
            for (int i = 0; i < 9; i++) {
                student.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            student.makeOrder(restaurant, List.of(MEAL_1));
            assertThat(lastOrderPrice(student)).isEqualTo(MEAL_1_PRICE * (1 - STUDENT.getDiscount() - PLATFORM_LOYALTY_DISCOUNT));
        }

        @Test
        void customerWithPlatformAndRestaurantLoyalty() {
            Customer customer = new Customer("A", "A", OTHER);
            for (int i = 0; i < 5; i++) {
                customer.makeOrder(otherRestaurant, List.of(MEAL_4));
            }
            for (int i = 0; i < 4; i++) {
                customer.makeOrder(restaurant, List.of(MEAL_1));
            }
            customer.makeOrder(restaurant, List.of(MEAL_2));
            assertThat(lastOrderPrice(customer)).isEqualTo(MEAL_2_PRICE * (1 - PLATFORM_LOYALTY_DISCOUNT - RESTAURANT_LOYALTY_DISCOUNT)); // specs : -10% seulement
        }

        @Test
        void retentionAndChildDiscount() {
            Customer child = new Customer("A", "A", CHILD);
            child.makeOrder(restaurant, List.of(MEAL_1));
            child.makeOrder(restaurant, List.of(MEAL_2, MEAL_3));
            assertThat(lastOrderPrice(child)).isEqualTo(MEAL_2_PRICE * (1 - CHILD.getDiscount()));
        }
    }
}