package dev.ubaid.orderservice.book;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
public class BookClient {
    private static final String BOOKS_ROOT_API = "/api/books/";
    private final WebClient webClient;
    
    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient
            .get()
            .uri(BOOKS_ROOT_API + isbn)
            .retrieve()
            .bodyToMono(Book.class)
            .timeout(Duration.ofSeconds(3), Mono.empty())
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)));
    }
}
