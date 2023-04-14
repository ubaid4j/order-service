package dev.ubaid.orderservice.event;

public record OrderDispatchedMessage(
    Long orderId
) {
}
