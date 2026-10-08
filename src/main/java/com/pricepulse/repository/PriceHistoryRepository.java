package com.pricepulse.repository;

import com.pricepulse.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    // Newest first; id breaks ties so "latest" is deterministic when two records share a timestamp.
    List<PriceHistory> findByProductIdOrderByRecordedAtDescIdDesc(Long productId);
}
