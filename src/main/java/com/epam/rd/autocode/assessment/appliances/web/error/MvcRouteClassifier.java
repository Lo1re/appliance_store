package com.epam.rd.autocode.assessment.appliances.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Classifies handler mappings only; never checks whether a database resource exists. */
@Slf4j
@Component
public class MvcRouteClassifier {
    private final RequestMappingHandlerMapping routes;

    public MvcRouteClassifier(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping routes) {
        this.routes = routes;
    }

    public boolean isUnmappedRead(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            return false;
        }
        try {
            return routes.getHandler(request) == null;
        } catch (Exception exception) {
            log.debug("Could not classify rejected route (type={})", exception.getClass().getSimpleName());
            return false;
        }
    }
}
