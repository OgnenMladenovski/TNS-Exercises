package mk.ukim.finki.testing.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private OrderRepository orders;

    @Mock
    private OrderNotifier notifier;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(products, orders, notifier, new PriceCalculator());
    }

    @Test
    void validatesCustomerIdFirst() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder(" ", " ", 0, false));

        assertEquals("Customer ID must not be blank", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @Test
    void validatesProductCodeSecond() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder("Customer1", " ", 0, false));

        assertEquals("Product code must not be blank", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @Test
    void validatesQuantityLast() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder("Customer1", "ProductCode1", 0, false));

        assertEquals("Quantity must be between 1 and 100", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t"})
    void rejectsBlankCustomerId(String customerId) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder(customerId, "ProductCode1", 1, false));

        assertEquals("Customer ID must not be blank", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t"})
    void rejectsBlankProductCode(String productCode) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder("Customer1", productCode, 1, false));

        assertEquals("Product code must not be blank", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsOutOfRangeQuantity(int quantity) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.placeOrder("Customer1", "ProductCode1", quantity, false));

        assertEquals("Quantity must be between 1 and 100", exception.getMessage());
        verifyNoInteractions(products, orders, notifier);
    }

    @Test
    void looksUpProductByExactCodeOnlyOnce() {
        when(products.findByCode(" ProductCode1 ")).thenReturn(Optional.of(new Product(" ProductCode1 ", 1000, 10)));

        Order order = service.placeOrder("Customer1", " ProductCode1 ", 1, false);

        verify(products, times(1)).findByCode(" ProductCode1 ");
        assertEquals("Customer1", order.customerId());
        assertEquals(" ProductCode1 ", order.productCode());
    }

    @Test
    void rejectsUnknownProduct() {
        when(products.findByCode("ProductCode1")).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () -> service.placeOrder("Customer1", "ProductCode1", 1, false));

        assertEquals("Product not found: ProductCode1", exception.getMessage());
        verifyNoInteractions(orders, notifier);
    }

    @Test
    void rejectsQuantityAboveStock() {
        when(products.findByCode("ProductCode1")).thenReturn(Optional.of(new Product("ProductCode1", 1000, 5)));

        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> service.placeOrder("Customer1", "ProductCode1", 6, false));

        assertEquals("Requested quantity 6 exceeds available quantity 5", exception.getMessage());
        verifyNoInteractions(orders, notifier);
    }

    @Test
    void placesOrder() {
        when(products.findByCode("ProductCode1")).thenReturn(Optional.of(new Product("ProductCode1", 1000, 5)));

        Order order = service.placeOrder("Customer1", "ProductCode1", 5, false);

        assertEquals("Customer1", order.customerId());
        assertEquals("ProductCode1", order.productCode());
        assertEquals(5, order.quantity());
        assertEquals(5000, order.totalCents());

        InOrder inOrder = inOrder(orders, notifier);
        inOrder.verify(orders).save(same(order));
        inOrder.verify(notifier).sendConfirmation(same(order));
        verifyNoMoreInteractions(orders, notifier);
    }

    @Test
    void checksSaveFailure() {
        when(products.findByCode("ProductCode1")).thenReturn(Optional.of(new Product("ProductCode1", 1000, 5)));
        IllegalStateException failure = new IllegalStateException("Save failed");
        doThrow(failure).when(orders).save(any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.placeOrder("Customer1", "ProductCode1", 1, false));

        assertSame(failure, exception);
        verifyNoInteractions(notifier);
    }

    @Test
    void checksNotificationFailure() {
        when(products.findByCode("ProductCode1")).thenReturn(Optional.of(new Product("ProductCode1", 1000, 5)));
        IllegalStateException failure = new IllegalStateException("Notification failed");
        doThrow(failure).when(notifier).sendConfirmation(any());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.placeOrder("Customer1", "ProductCode1", 1, false));

        assertSame(failure, exception);
        verify(orders).save(any());
    }
}