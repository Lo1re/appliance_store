package com.epam.rd.autocode.assessment.appliances.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class BusinessAuditLogger {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void committed(BusinessChange event) {
        log.info("Business change committed (action={}, actorId={}, entityId={}, requestId={})",
                event.action(), event.actorId() == null ? "-" : event.actorId(),
                event.entityId(), event.requestId());
    }
}
