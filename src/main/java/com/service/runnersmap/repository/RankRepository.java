package com.service.runnersmap.repository;

import com.service.runnersmap.entity.Rank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RankRepository extends JpaRepository<Rank, Long> {

//  List<Rank> findAllByRankPositionBetweenAndYearAndMonthOrderByRankPosition(
//      Integer startRankPosition, Integer endRankPosition, Integer year, Integer month);

  Page<Rank> findAllByRankPositionBetweenAndYearAndMonthOrderByRankPosition(
      Integer startRankPosition,
      Integer endRankPosition,
      Integer year,
      Integer month,
      Pageable pageable
  );

  @Transactional
  @Modifying
  void deleteByYearAndMonth(Integer year, Integer month);
}
