package model.user;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import static java.lang.String.format;
import static java.time.LocalDate.now;

public class Order implements Entity
{
    @Getter
    private final LocalDate date;

    @Getter
    private final Restaurant restaurant;

    @Getter
    private final Customer customer;

    @Getter
    private final List<Meal> meals;

    Order(Restaurant restaurant, Customer customer, List<String> mealNames)
    {
        this.date = now();
        this.restaurant = restaurant.withReceivedOrder(this);
        this.customer = customer;
        this.meals = mealNames.stream().map(restaurant::getMealByName).toList();
    }

    public String getName()
    {
        return format("From %s - in %s", customer.getName(), restaurant.getName());
    }

    public Double getPrice()
    {
        Double totalAmount = 0D;
        int mealNumber = 0;
        Iterator mealIterator = meals.iterator();
        while(mealIterator.hasNext()) {
            Meal each = (Meal) mealIterator.next();
            mealNumber +=1;

            boolean hasOrderedInThePastWeek = false;
            for (Order order : customer.getOrders())
            {
                if (order != this && ChronoUnit.DAYS.between(order.date, now()) <= 7)
                    hasOrderedInThePastWeek = true;
            }
            if (hasOrderedInThePastWeek && mealNumber == 2)
                continue;

            switch (customer.getType()) {
                case CHILD:
                    if (customer.getOrders().size() % 10 == 0)
                        totalAmount += each.getPrice() * (1-0.5-0.10-0.15);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % 5 == 0)
                        totalAmount += each.getPrice() * (1-0.5-0.10);
                    else totalAmount += each.getPrice() * (1-0.5);
                    break;
                case STUDENT:
                    if (customer.getOrders().size() % 10 == 0)
                        totalAmount += each.getPrice() * (1-0.25-0.10-0.15);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % 5 == 0)
                        totalAmount += each.getPrice() * (1-0.25-0.10);
                    else totalAmount += each.getPrice() * (1-0.25);
                    break;
                default:
                    if (customer.getOrders().size() % 10 == 0)
                        totalAmount += each.getPrice() * (1-0.10-0.15);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % 5 == 0)
                        totalAmount += each.getPrice() * (1-0.10);
                    else totalAmount += each.getPrice();
            }
        }
        return totalAmount;
    }
}
