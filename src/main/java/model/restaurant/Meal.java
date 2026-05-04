package model.restaurant;

import lombok.Getter;
import model.Entity;

import java.math.BigDecimal;
import java.util.Objects;

public class Meal implements Entity {
    @Getter
    private final Restaurant restaurant;

    @Getter
    private final String name;

    @Getter
    private final BigDecimal price;

    Meal(Restaurant restaurant, String name, BigDecimal price) {
        this.restaurant = restaurant;
        this.name = name;
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Meal meal = (Meal) o;
        return Objects.equals(name, meal.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}