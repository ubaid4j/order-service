package dev.ubaid.orderservice.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class ClientConfig {

    @Bean
    public boolean isWebClientDebug(
        @Value("#{environment.getProperty('webclient.debug') != null && environment.getProperty('webclient.debug') != 'false'}")
        boolean isDebug) {
        return isDebug;
    }

    @Bean
    @ConditionalOnExpression("#{isWebClientDebug == true}")
    public ReactorClientHttpConnector wiretappedConnector() {
        HttpClient httpClient =
            HttpClient.create()
                .wiretap(
                    this.getClass().getCanonicalName(), LogLevel.DEBUG,
                    AdvancedByteBufFormat.TEXTUAL);
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    WebClient webClient(
        ClientProperties props,
        WebClient.Builder builder,
        ReactorClientHttpConnector reactorClientHttpConnector
    ) {
        return builder
            .clientConnector(reactorClientHttpConnector)
            .baseUrl(props.catalogServiceUri().toString())
            .build();
    }
}
