package model.user;


import lombok.Getter;
import model.restaurant.Restaurant;

public class RestaurantOwner implements User
{
    @Getter
    private final String firstName;

    @Getter
    private final String lastName;

    @Getter
    private final Restaurant restaurant;

    public RestaurantOwner(String firstName, String lastName, Restaurant restaurant)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.restaurant = restaurant;
    }

    public void addMeal(String mealName, Double price)
    {
        restaurant.addMeal(mealName, price);
    }
}