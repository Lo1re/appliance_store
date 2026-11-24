package com.epam.rd.autocode.assessment.appliances.config.transport;

import com.epam.rd.autocode.assessment.appliances.web.transport.HttpsRedirectFilter;

import jakarta.servlet.DispatcherType;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("${server.ssl.enabled:false} && ${store.https.redirect-enabled:false}")
public class HttpsRedirectConfiguration {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpRedirectConnector(
            @Value("${store.https.http-port:8080}") int httpPort) {
        if (httpPort < 0 || httpPort > 65535) {
            throw new IllegalArgumentException("Invalid HTTP redirect port");
        }
        return server -> {
            var connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(httpPort);
            connector.setScheme("http");
            connector.setSecure(false);
            connector.setProperty("address", "127.0.0.1");
            server.addAdditionalTomcatConnectors(connector);
        };
    }

    @Bean
    public FilterRegistrationBean<HttpsRedirectFilter> httpsRedirectFilter(
            @Value("${store.https.public-origin}") String origin) {
        var registration = new FilterRegistrationBean<>(new HttpsRedirectFilter(origin));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
