package dev.ubaid.orderservice.web;

import dev.ubaid.orderservice.domain.Order;
import dev.ubaid.orderservice.domain.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Flux<Order> getAllOrders(@AuthenticationPrincipal Authentication currentUser) {
        log.info("REST request to find all orders created by: {}", currentUser.getName());
        return orderService.findAllCreatedBy(currentUser.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> submitOrder(
        @RequestBody @Valid OrderRequest orderRequest
    ) {
        log.info("REST request to submit order: {}", orderRequest);
        return orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity());
    }
}
