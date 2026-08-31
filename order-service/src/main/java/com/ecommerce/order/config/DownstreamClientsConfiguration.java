package com.ecommerce.order.config;

import java.net.http.HttpClient;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class DownstreamClientsConfiguration {

    @Bean
    JwtEncoder serviceJwtEncoder(@Qualifier("jwtSecretKey") SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).build();
    }

    @Bean
    @Qualifier("catalogRestClient")
    RestClient catalogRestClient(
            RestClient.Builder builder,
            DownstreamClientsProperties properties,
            DownstreamAuthenticationInterceptor downstreamAuthenticationInterceptor) {
        var service = properties.catalog();
        return builder.clone()
                .baseUrl(service.baseUrl().toString())
                .requestFactory(requestFactory(service))
                .requestInterceptor(downstreamAuthenticationInterceptor)
                .build();
    }

    @Bean
    @Qualifier("inventoryRestClient")
    RestClient inventoryRestClient(
            RestClient.Builder builder,
            DownstreamClientsProperties properties,
            DownstreamAuthenticationInterceptor downstreamAuthenticationInterceptor) {
        var service = properties.inventory();
        return builder.clone()
                .baseUrl(service.baseUrl().toString())
                .requestFactory(requestFactory(service))
                .requestInterceptor(downstreamAuthenticationInterceptor)
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory(DownstreamClientsProperties.Service service) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(service.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(service.readTimeout());
        return requestFactory;
    }
}
