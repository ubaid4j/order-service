package dev.ubaid.orderservice.web;

import dev.ubaid.orderservice.config.ClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {
    
    private final ClientProperties clientProps;
    
    @GetMapping
    public Mono<String> index() {
        return Mono.just(clientProps.homeMessage());
    }
}
