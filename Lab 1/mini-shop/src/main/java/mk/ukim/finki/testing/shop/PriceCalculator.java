package mk.ukim.finki.testing.shop;

/** Calculates the total for an order containing one product type. All amounts are cents. */
public class PriceCalculator {

    public static final int MAX_UNIT_PRICE =  1_000_000;
    public static final int MAX_QUANTITY = 100;
    public static final long PREMIUM_DISCOUNT = 90L;
    public static final int PERCENTAGE = 100;
    public static final int SHIPPING = 500;
    public static final int FREE_DELIVERY = 5_000;

    public int calculateTotal(int unitPriceCents, int quantity, boolean premiumCustomer) {
        if (unitPriceCents < 1 || unitPriceCents > MAX_UNIT_PRICE) {
            throw new IllegalArgumentException("Unit price must be between 1 and 1000000 cents");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and 100");
        }

        int subtotal = unitPriceCents * quantity;
        int discountedSubtotal;
        if (premiumCustomer) {
            discountedSubtotal = (int) (subtotal * PREMIUM_DISCOUNT / PERCENTAGE);
        } else {
            discountedSubtotal = subtotal;
        }
        if (discountedSubtotal >= FREE_DELIVERY) {
            return discountedSubtotal;
        }
        else {
            return discountedSubtotal + SHIPPING;
        }
    }
}