package com.service.runnersmap.controller;

import com.service.runnersmap.dto.RankDto;
import com.service.runnersmap.entity.Rank;
import com.service.runnersmap.service.RankService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking")
public class RankController {

  private final RankService rankService;

  /*
   * 랭킹 조회
   * - 월별로 집계된 데이터를 순위별로 조회한다.
   */
  @GetMapping
  public ResponseEntity<Page<RankDto>> searchRankByMonth(
      @RequestParam(value = "year") Integer year,
      @RequestParam(value = "month") Integer month,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) throws Exception {

    Pageable pageable = PageRequest.of(page,size);
    Page<Rank> rankPage = rankService.searchRankByMonth(year, month, pageable);

    if (rankPage.isEmpty()) {
      return ResponseEntity.noContent().build();
    } else {
      Page<RankDto> rankDtoPage = rankPage.map(RankDto::fromEntity);
      return ResponseEntity.ok(rankDtoPage);
//      return ResponseEntity.ok(
//          rank.stream().map(p -> RankDto.fromEntity(p)).collect(Collectors.toList())

    }
  }

}
