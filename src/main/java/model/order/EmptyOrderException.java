package model.order;

public class EmptyOrderException extends IllegalArgumentException {
    public EmptyOrderException() {
        super("An order must contain at least one meal.");
    }
}
