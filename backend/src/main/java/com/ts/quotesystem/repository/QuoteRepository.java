package com.ts.quotesystem.repository;

import com.ts.quotesystem.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {
    Optional<Quote> findByQuoteNo(String quoteNo);
    
    // 見積番号の連番生成用に、指定日の見積数を取得する
    long countByQuoteNoStartingWith(String prefix);
}
