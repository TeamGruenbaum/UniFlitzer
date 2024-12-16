package de.uniflitzer.backend.applicationservices.livestream

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
@EnableWebSocketMessageBroker
class LiveStreamingConfigurator: WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(stompEndpointRegistry: StompEndpointRegistry) {
        stompEndpointRegistry.addEndpoint("")
    }

    @Bean
    fun createWebSocketContainer():ServletServerContainerFactoryBean {
        var container = ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(1000 * 180);
        return container;
    }
}