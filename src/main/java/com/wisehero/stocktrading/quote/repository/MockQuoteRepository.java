package com.wisehero.stocktrading.quote.repository;

import com.wisehero.stocktrading.quote.domain.MockQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockQuoteRepository extends JpaRepository<MockQuote, String> {
}
