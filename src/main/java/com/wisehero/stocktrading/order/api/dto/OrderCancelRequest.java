package com.wisehero.stocktrading.order.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Cancel request bound to the requester account.
 */
public record OrderCancelRequest(
        @NotNull @Positive Long accountId
) {
}
