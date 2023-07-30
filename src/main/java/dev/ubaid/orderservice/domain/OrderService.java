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
        Order order = Order.of(
            book.isbn(),
            book.title() + "-" + book.author(),
            book.price(),
            quantity,
            OrderStatus.ACCEPTED
        );
        log.info("Accepted Order: {}", order);
        return order;
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        Order order = Order.of(
            bookIsbn,
            null,
            null,
            quantity,
            OrderStatus.REJECTED
        );
        log.info("Rejected Order: {}", order);
        return order;
    }

    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
        return flux
            .doOnNext(orderDispatchedMessage -> log.info("order {} is dispatched. Updating order in order-service", orderDispatchedMessage))
            .doOnNext(orderDispatchedMessage -> log.info("finding order by order id: {}", orderDispatchedMessage.orderId()))
            .flatMap(message -> orderRepository.findById(message.orderId()))
            .doOnNext(order -> log.info("order found: {}", order))
            .map(this::buildDispatchedOrder)
            .flatMap(orderRepository::save);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        log.info("updating order {} status to {}", existingOrder, OrderStatus.DISPATCHED);
        Order order = new Order(
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
        log.info("Updated Order: {}", order);
        return order;
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
