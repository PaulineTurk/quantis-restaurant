package model.user;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import model.restaurant.Restaurant;

public class Customer implements User
{
    @Getter
    private final String firstName;

    @Getter
    private final String lastName;

    @Getter
    private final Type type;

    @Getter
    private final List<Order> orders;

    public Customer(String firstName, String lastName,Type type)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.type = type;
        this.orders = new ArrayList<>();
    }

    public void makeOrder(Restaurant restaurant, List<String> meals)
    {
        orders.add(new Order(restaurant, this, meals));
    }

    public enum Type {
        CHILD,
        STUDENT,
        OTHER
    }
}
