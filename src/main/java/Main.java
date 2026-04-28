import java.util.List;

import model.restaurant.Restaurant;
import model.user.Customer;
import model.user.RestaurantOwner;
import static model.user.Customer.Type.OTHER;

public class Main
{
    public static void main(String[] args)
    {
        // *******************
        // *** RESTAURANTS ***
        // *******************

        Restaurant ticino = new Restaurant("Le Ticino");
        Restaurant etoile = new Restaurant("L'étoile");
        Restaurant texan = new Restaurant("Le texan");

        // *************
        // *** USERS ***
        // *************

        RestaurantOwner robertDupont = new RestaurantOwner("Robert", "Dupont", ticino);
        RestaurantOwner magaliNoel = new RestaurantOwner("Magali", "Noel", etoile);
        RestaurantOwner nicolasBenoit = new RestaurantOwner("Nicolas", "Benoit", texan);

        Customer catherine = new Customer("Catherine", "Zwahlen", OTHER);
        Customer clementine = new Customer("Clementine", "Delerce", OTHER);

        // *************
        // *** MEALS ***
        // *************

        robertDupont.addMeal("Pizza tonno", 20.50);
        robertDupont.addMeal("Pasta bolognese", 18.30);
        robertDupont.addMeal("Tiramisu", 12.80);

        magaliNoel.addMeal("Cassoulet", 22.60);
        magaliNoel.addMeal("Risotto", 19.15);
        magaliNoel.addMeal("Banana split", 14.90);

        nicolasBenoit.addMeal("Burger vege", 21.10);
        nicolasBenoit.addMeal("Fajitas", 24.0);

        // **************
        // *** ORDERS ***
        // **************

        catherine.makeOrder(ticino, List.of("Pizza tonno", "Tiramisu"));
        clementine.makeOrder(etoile, List.of("Risotto", "Banana split"));

        System.out.println("done");
    }
}