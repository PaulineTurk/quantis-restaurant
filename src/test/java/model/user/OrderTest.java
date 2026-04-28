package model.user;

import model.restaurant.Restaurant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.user.Customer.Type.CHILD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderTest
{
    @Test
    void getPrice_whenChild_when1stOrder_then50PercentDiscount()
    {
        // Given
        Customer customer = new Customer("Ba", "Bar", CHILD);
        Restaurant restaurant = new Restaurant("The restaurant");
        restaurant.addMeal("Meal 1", 15.0);
        restaurant.addMeal("Meal 2", 10.0);
        customer.makeOrder(restaurant, List.of("Meal 1", "Meal 2"));

        // When
        Order order = customer.getOrders().get(0);

        // Then
        assertThat((15.0+10.0)*0.5).isEqualTo(order.getPrice());
    }
}