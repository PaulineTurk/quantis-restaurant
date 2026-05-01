package model.user;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static model.user.Customer.Type.CHILD;
import static model.user.Customer.Type.STUDENT;

public class Order implements Entity {
    public static final int PLATFORM_LOYALTY_THRESHOLD = 10;
    public static final int RESTAURANT_LOYALTY_THRESHOLD = 5;
    public static final int RETENTION_THRESHOLD = 7;
    public static final double PLATFORM_LOYALTY_DISCOUNT = 0.10;
    public static final double RESTAURANT_LOYALTY_DISCOUNT = 0.15;
    public static final int FREE_MEAL_POSITION = 2;

    @Getter
    private final LocalDate date;

    @Getter
    private final Restaurant restaurant;

    @Getter
    private final Customer customer;

    @Getter
    private final List<Meal> meals;

    Order(Restaurant restaurant, Customer customer, List<String> mealNames) {
        this.date = now();
        this.restaurant = restaurant.withReceivedOrder(this);
        this.customer = customer;
        this.meals = mealNames.stream().map(restaurant::getMealByName).toList();
    }

    public String getName() {
        return format("From %s - in %s", customer.getName(), restaurant.getName());
    }

    public Double getPrice() {
        Double totalAmount = 0D;
        int mealNumber = 0;
        Iterator mealIterator = meals.iterator();
        while (mealIterator.hasNext()) {
            Meal each = (Meal) mealIterator.next();
            mealNumber += 1;

            boolean hasOrderedInThePastWeek = false;
            for (Order order : customer.getOrders()) {
                if (order != this && ChronoUnit.DAYS.between(order.date, now()) <= RETENTION_THRESHOLD)
                    hasOrderedInThePastWeek = true;
            }
            if (hasOrderedInThePastWeek && mealNumber == FREE_MEAL_POSITION)
                continue;

            switch (customer.getType()) {
                case CHILD:
                    if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - CHILD.getDiscount() - PLATFORM_LOYALTY_DISCOUNT - RESTAURANT_LOYALTY_DISCOUNT);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - CHILD.getDiscount() - PLATFORM_LOYALTY_DISCOUNT);
                    else totalAmount += each.getPrice() * (1 - CHILD.getDiscount());
                    break;
                case STUDENT:
                    if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - STUDENT.getDiscount() - PLATFORM_LOYALTY_DISCOUNT - RESTAURANT_LOYALTY_DISCOUNT);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - STUDENT.getDiscount() - PLATFORM_LOYALTY_DISCOUNT);
                    else totalAmount += each.getPrice() * (1 - STUDENT.getDiscount());
                    break;
                default:
                    if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - PLATFORM_LOYALTY_DISCOUNT - RESTAURANT_LOYALTY_DISCOUNT);
                    else if (customer.getOrders().stream().filter(o -> o.getRestaurant().equals(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
                        totalAmount += each.getPrice() * (1 - PLATFORM_LOYALTY_DISCOUNT);
                    else totalAmount += each.getPrice();
            }
        }
        return totalAmount;
    }
}
