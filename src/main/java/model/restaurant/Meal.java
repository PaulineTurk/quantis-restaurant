package model.restaurant;

import java.util.Objects;

import lombok.Getter;
import model.Entity;

public class Meal implements Entity
{
    @Getter
    private final Restaurant restaurant;

    @Getter
    private final String name;

    @Getter
    private final Double price;

    Meal(Restaurant restaurant, String name, Double price)
    {
        this.restaurant = restaurant;
        this.name = name;
        this.price = price;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Meal meal = (Meal) o;
        return Objects.equals(name, meal.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }
}