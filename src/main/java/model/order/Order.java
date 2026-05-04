package model.order;

import lombok.Getter;
import model.Entity;
import model.restaurant.Meal;
import model.restaurant.Restaurant;
import model.user.Customer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;

public abstract class Order implements Entity {
    public static final BigDecimal PLATFORM_LOYALTY_DISCOUNT = new BigDecimal("0.10");
    public static final BigDecimal RESTAURANT_LOYALTY_DISCOUNT = new BigDecimal("0.15");
    public static final int PLATFORM_LOYALTY_THRESHOLD = 10;
    public static final int RESTAURANT_LOYALTY_THRESHOLD = 5;
    protected static final int MIN_MEALS_TO_GET_CHEAPEST_FREE = 2;

    @Getter
    private final LocalDate date;
    @Getter
    private final Customer customer;

    protected Order(Customer customer, Clock clock) {
        this.date = now(clock);
        this.customer = customer;
    }

    protected static void validateMeals(List<String> mealNames) {
        if (mealNames == null || mealNames.isEmpty())
            throw new EmptyOrderException();
    }

    public abstract BigDecimal getPrice();

    protected abstract boolean involvesRestaurant(Restaurant restaurant);

    protected BigDecimal computePrice(List<Meal> meals, BigDecimal discount) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discount);
        return meals.stream()
                .map(meal -> meal.getPrice().multiply(discountMultiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected BigDecimal computePrice(List<Meal> meals, BigDecimal discount, Meal freeMeal) {
        List<Meal> remainingMeals = new ArrayList<>(meals);
        remainingMeals.remove(freeMeal);
        return computePrice(remainingMeals, discount);
    }

    protected Optional<Meal> freeMealAmong(List<Meal> meals) {
        if (!customer.hasOrderedInTheRetentionPeriod(this) || meals.size() < MIN_MEALS_TO_GET_CHEAPEST_FREE)
            return Optional.empty();
        return meals.stream().min(Comparator.comparing(Meal::getPrice));
    }

    protected BigDecimal computeCustomerTypeDiscount() {
        return customer.getType().getDiscount();
    }

    protected BigDecimal computePlatformLoyaltyDiscount() {
        if (customer.getOrders().size() % PLATFORM_LOYALTY_THRESHOLD == 0)
            return PLATFORM_LOYALTY_DISCOUNT;
        return BigDecimal.ZERO;
    }

    protected BigDecimal computeRestaurantLoyaltyDiscount(Restaurant restaurant) {
        if (customer.getOrders().stream().filter(order -> order.involvesRestaurant(restaurant)).count() % RESTAURANT_LOYALTY_THRESHOLD == 0)
            return RESTAURANT_LOYALTY_DISCOUNT;
        return BigDecimal.ZERO;
    }
}
