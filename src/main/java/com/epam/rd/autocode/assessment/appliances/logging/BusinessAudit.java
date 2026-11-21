package com.epam.rd.autocode.assessment.appliances.logging;

import com.epam.rd.autocode.assessment.appliances.security.logging.SecurityLogContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessAudit {
    private final ApplicationEventPublisher events;

    public void record(BusinessAction action, Long entityId) {
        events.publishEvent(new BusinessChange(action, SecurityLogContext.currentAccountId(), entityId,
                MDC.get(RequestLogContext.MDC_KEY)));
    }
}
