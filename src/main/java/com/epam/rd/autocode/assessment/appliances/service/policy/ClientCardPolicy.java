package com.epam.rd.autocode.assessment.appliances.service.policy;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;

import lombok.RequiredArgsConstructor;

// Checks optional loyalty-card uniqueness; the database constraint handles concurrent writes.
@RequiredArgsConstructor
public final class ClientCardPolicy {
    private final ClientRepository clients;

    public void requireAvailable(String card, Long currentClientId) {
        if (card == null) {
            return;
        }
        boolean taken =
                currentClientId == null
                        ? clients.existsByCard(card)
                        : clients.existsByCardAndIdNot(card, currentClientId);
        if (taken) {
            throw new AccountException(AccountError.CARD_DUPLICATE);
        }
    }
}
