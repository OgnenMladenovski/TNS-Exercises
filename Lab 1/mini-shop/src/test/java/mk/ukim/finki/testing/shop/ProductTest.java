package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductTest {

    @Test
    void createsProductWithGivenValues() {
        Product product = new Product("ValidCode", 30, 10);

        assertEquals("ValidCode", product.code());
        assertEquals(30, product.unitPriceCents());
        assertEquals(10, product.availableQuantity());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t"})
    void rejectsBlankCode(String code) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Product(code, 35, 15));

        assertEquals("Product code must not be blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1_000_001})
    void rejectsOutOfRangePrice(int price) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Product("ValidCode", price, 20));

        assertEquals("Unit price must be between 1 and 1000000 cents", exception.getMessage());
    }

    @Test
    void rejectsNegativeValueQuantity(){
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Product("ValidCode", 45, -1));

        assertEquals("Available quantity must not be negative", exception.getMessage());
    }
}