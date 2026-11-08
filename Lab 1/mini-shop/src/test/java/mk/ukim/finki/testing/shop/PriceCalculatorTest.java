package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PriceCalculatorTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1_000_001})
    void rejectsOutOfRangePrice(int price) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calculator.calculateTotal(price, 10, false));

        assertEquals("Unit price must be between 1 and 1000000 cents", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsOutOfRangeQuantity(int quantity) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calculator.calculateTotal(100, quantity, false));

        assertEquals("Quantity must be between 1 and 100", exception.getMessage());
    }

    @Test
    void calculatesSubtotal() {  //Both Subtotal and Total
        int total = calculator.calculateTotal(1000, 2, true);

        assertEquals(2300, total);
    }

    @Test
    void calculatesStandardCustomer() {
        int total = calculator.calculateTotal(1000, 6, false);

        assertEquals(6000, total);
    }

    @Test
    void calculatesPremiumCustomer() {
        int total = calculator.calculateTotal(515, 1, true);

        assertEquals(963, total);
    }

    @Test
    void calculatesShipping() {
        int total = calculator.calculateTotal(5000, 1, true);

        assertEquals(5000, total);
    }

    @Test
    void calculatesFreeShipping()
    {
        int total = calculator.calculateTotal(5000, 1, false);

        assertEquals(5000, total);
    }
}