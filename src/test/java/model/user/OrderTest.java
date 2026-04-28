package model.user;

import java.util.List;

import org.junit.jupiter.api.Test;

import model.restaurant.Restaurant;
import static model.user.Customer.Type.CHILD;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals((15.0+10.0)*0.5, order.getPrice());
    }
}