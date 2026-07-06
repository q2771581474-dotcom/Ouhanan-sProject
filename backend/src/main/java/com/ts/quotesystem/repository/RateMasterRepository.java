package com.ts.quotesystem.repository;

import com.ts.quotesystem.entity.RateMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RateMasterRepository extends JpaRepository<RateMaster, Long> {
    List<RateMaster> findByActiveTrue();
    Optional<RateMaster> findByCategoryAndItemCodeAndActiveTrue(String category, String itemCode);
    List<RateMaster> findByCategoryAndActiveTrue(String category);
}
