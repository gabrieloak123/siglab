package br.ufrn.dimap.siglab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientConfig {
    @Bean
    RestClient externalRestClient(@Value("${EXTERNAL_CONNECT_TIMEOUT_MS:5000}") int connectTimeout,
                                  @Value("${EXTERNAL_READ_TIMEOUT_MS:20000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory).build();
    }
}
