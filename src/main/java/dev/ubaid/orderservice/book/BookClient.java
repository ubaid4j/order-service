package dev.ubaid.orderservice.book;

import java.time.Duration;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookClient {
    private static final String BOOKS_ROOT_API = "/api/books/";
    private static final Function<Exception, Mono<Book>> LOG_ERROR = exp -> {
        log.error("error during getting book from catalog service", exp);
        return Mono.empty();
    };
    private final WebClient webClient;

    public Mono<Book> getBookByIsbn(String isbn) {
        return webClient
            .get()
            .uri(BOOKS_ROOT_API + isbn)
            .retrieve()
            .bodyToMono(Book.class)
            .timeout(Duration.ofSeconds(3), Mono.empty())
            .onErrorResume(WebClientResponseException.class, LOG_ERROR)
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
            .onErrorResume(Exception.class, LOG_ERROR);
    }
}
