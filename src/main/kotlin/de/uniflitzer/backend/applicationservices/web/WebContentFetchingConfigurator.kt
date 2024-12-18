package de.uniflitzer.backend.applicationservices.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

@Configuration
class WebContentFetchingConfigurator {
    @Bean
    fun httpClient(): HttpClient = HttpClient.newHttpClient();
}