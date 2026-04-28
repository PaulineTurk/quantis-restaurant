package model.restaurant;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import model.Entity;
import model.user.Order;
import static java.lang.String.format;

public class Restaurant implements Entity
{
    @Getter
    private final String name;

    @Getter
    private final List<Meal> meals;

    @Getter
    private final List<Order> orders;

    public Restaurant(String name)
    {
        this.name = name;
        this.meals = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    public void addMeal(String mealName, Double price)
    {
        if (meals.stream().map(Meal::getName).anyMatch(n -> n.equals(name)))
            throw new IllegalArgumentException(format("Meal %s already exists in %s", mealName, name));
        meals.add(new Meal(this, mealName, price));
    }

    public Restaurant withReceivedOrder(Order order)
    {
        orders.add(order);
        return this;
    }

    public Meal getMealByName(String mealName)
    {
        return meals.stream()
                    .filter(meal -> meal.getName().equals(mealName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(format("No meal named %s in %s", mealName, name)));
    }
}
