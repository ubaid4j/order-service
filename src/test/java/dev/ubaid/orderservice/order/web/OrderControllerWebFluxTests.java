package dev.ubaid.orderservice.order.web;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;

import dev.ubaid.orderservice.domain.Order;
import dev.ubaid.orderservice.domain.OrderService;
import dev.ubaid.orderservice.domain.OrderStatus;
import dev.ubaid.orderservice.web.OrderController;
import dev.ubaid.orderservice.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
public class OrderControllerWebFluxTests {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void whenBookNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("1234567890", 3);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
            .willReturn(Mono.just(expectedOrder));
        
        webTestClient
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(Order.class)
            .value(actualOrder -> {
                assertThat(actualOrder).isNotNull();
                assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
            });
    }
}
