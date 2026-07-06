package com.ts.quotesystem.repository;

import com.ts.quotesystem.entity.QuoteBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuoteBreakdownRepository extends JpaRepository<QuoteBreakdown, Long> {
    List<QuoteBreakdown> findByQuoteIdOrderByDisplayOrderAsc(Long quoteId);
}
