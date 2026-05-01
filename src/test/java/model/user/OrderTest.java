package model.user;

import model.restaurant.Restaurant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.user.Customer.Type.CHILD;
import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {
    @Test
    void getPrice_whenChild_when1stOrder_thenChildDiscount() {
        // Given
        Customer customer = new Customer("Ba", "Bar", CHILD);
        Restaurant restaurant = new Restaurant("The restaurant");
        final double MEAL_1_PRICE = 15.0;
        restaurant.addMeal("Meal 1", MEAL_1_PRICE);
        final double MEAL_2_PRICE = 10.0;
        restaurant.addMeal("Meal 2", MEAL_2_PRICE);
        customer.makeOrder(restaurant, List.of("Meal 1", "Meal 2"));

        // When
        Order order = customer.getOrders().getFirst();

        // Then
        assertThat(order.getPrice()).isEqualTo((MEAL_1_PRICE + MEAL_2_PRICE) * (1 - CHILD.getDiscount()));
    }
}