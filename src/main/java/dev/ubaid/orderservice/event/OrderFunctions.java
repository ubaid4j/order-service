package dev.ubaid.orderservice.event;

import dev.ubaid.orderservice.domain.OrderService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@Slf4j
public class OrderFunctions {

    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return flux -> {
            orderService.consumeOrderDispatchedEvent(flux)
                .doOnNext(order -> log.info("The order with id {} is dispatched", order.id()))
                .subscribe();
        };
    }
}
