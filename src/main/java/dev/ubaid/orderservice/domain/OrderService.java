package dev.ubaid.orderservice.domain;

import dev.ubaid.orderservice.book.Book;
import dev.ubaid.orderservice.book.BookClient;
import dev.ubaid.orderservice.event.OrderDispatchedMessage;
import dev.ubaid.orderservice.order.event.OrderAcceptedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final BookClient bookClient;
    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public Flux<Order> findAllCreatedBy(String userName) {
        return orderRepository.findAllByCreatedBy(userName);
    }

    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
            .map(book -> buildAcceptedOrder(book, quantity))
            .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
            .flatMap(orderRepository::save)
            .doOnNext(this::publishOrderAcceptedEvent);
    }

    private static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(
            book.isbn(),
            book.title() + "-" + book.author(),
            book.price(),
            quantity,
            OrderStatus.ACCEPTED
        );
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(
            bookIsbn,
            null,
            null,
            quantity,
            OrderStatus.REJECTED
        );
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux
            .flatMap(message -> orderRepository.findById(message.orderId()))
            .map(this::buildDispatchedOrder)
            .flatMap(orderRepository::save);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
            existingOrder.id(),
            existingOrder.bookIsbn(),
            existingOrder.bookName(),
            existingOrder.bookPrice(),
            existingOrder.quantity(),
            OrderStatus.DISPATCHED,
            existingOrder.createdDate(),
            existingOrder.lastModifiedDate(), 
                existingOrder.createdBy(),
                existingOrder.lastModifiedBy(), 
            existingOrder.version()
        );
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!OrderStatus.ACCEPTED.equals(order.status())) {
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id {}", order.id());
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending data for order with id {}: {}", order.id(), result);
    }
}
