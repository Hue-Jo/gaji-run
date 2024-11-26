package com.service.runnersmap.service;

import com.service.runnersmap.dto.RankSaveDto;
import com.service.runnersmap.entity.Rank;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.RankRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RankService {

  private final UserPostRepository userPostRepository;
  private final RankRepository rankRepository;
  private final UserRepository userRepository;


  /**
   * 조회 연도,월 랭킹 조회
   * 페이징 처리를 통해 100위까지 보여주는 것으로 수정 (한 페이지당 20)
   */
  @Transactional(readOnly = true)
  public Page<Rank> searchRankByMonth(Integer year, Integer month, Pageable pageable) throws Exception {

    // 랭킹 조회 범위 설정 (1~100위)
    int startRankPosition = 1;
    int endRankPosition = 100;

    // 특정 연,월에 해당하는 랭킹 조회 & 페이징처리
    return rankRepository.findAllByRankPositionBetweenAndYearAndMonthOrderByRankPosition(
        startRankPosition,
        endRankPosition,
        year,
        month,
        pageable);
  }

  /*
   * < 랭킹 집계 > : rankJob 에서 호출
   * - 월별 러닝 기록 데이터를 통해 점수를 계산하여 순위별로 사용자 정보를 저장한다. (러닝 유효 Y, 현재 유효한 사용자)
   * - 점수 계산 방식 (좀 더 고민 필요)
   * 1) 거리와 시간을 모두 고려하여 ( 거리 / 시간 ) 비율을 구한다.
   * 2) 특정 거리 이상의 사용자는 1) 번 비율에서 가중치를 부여한다.
   *  5km 미만   : 1.0
   *  5km 이상   : 1.2
   *  10km 이상  : 1.3
   *  21.0975km 이상 : 1.4  (하프마라톤)
   *  42.195km 이상  : 1.5  (마라톤)
   */

  /**
   * 월별 러닝 기록을 바탕으로 랭킹 계산/저장하는 메서드
   */
  @Transactional
  public void saveRankingByMonth(Integer year, Integer month, LocalDate batchExecutedDate) throws Exception {

    // 기존의 랭킹 데이터 삭제 (매일 초기화)
    rankRepository.deleteByYearAndMonth(year, month);

    // 조회년월의 유효한 러너 기록 (실제 종료 시간이 있는 기록만)
    List<UserPost> monthRunRecords = userPostRepository.findAllByValidYnIsTrueAndYearAndMonthAndActualEndTimeIsNotNull(
        year, month);
    if (monthRunRecords.isEmpty()) {
      log.info("유효한 러닝 기록이 존재하지 않습니다.");
      return;
    }

    // 조회한 기록(러닝기록)을 바탕으로 점수 계산
    List<RankSaveDto> monthSortRank = monthRunRecords.stream()
        .map(userPost -> {
          // 거리, 시간 기반 점수 계산
          double baseScore = calDistanceWeight(userPost);
          return RankSaveDto.builder()
              .userId(userPost.getUser().getId())
              .totalDistance(userPost.getTotalDistance())
              .actualStartTime(userPost.getActualStartTime())
              .actualEndTime(userPost.getActualEndTime())
              .runningDuration(userPost.getRunningDuration())
              .score(baseScore)
              .build();
        })
        // 사용자 Id별로 그룹화하여 거리, 시간, 점수 합산
        .collect(Collectors.groupingBy(RankSaveDto::getUserId,
            Collectors.reducing((dto1, dto2) -> RankSaveDto.builder()
                .userId(dto1.getUserId())
                .totalDistance(dto1.getTotalDistance() + dto2.getTotalDistance()) // 거리 합산
                .runningDuration(
                    dto1.getRunningDuration().plus(dto2.getRunningDuration())) // 러닝 시간 합산
                .score(dto1.getScore() + dto2.getScore()) // 점수 합산
                .build())
        ))
        .values().stream()
        .map(Optional::orElseThrow)
        .sorted(Comparator.comparingDouble(RankSaveDto::getScore).reversed()) // 점수 기준 내림차순 정렬
        .collect(Collectors.toList());

    // 정렬된 목록 순회하며 랭킹 데이터 저장
    int rank = 0;
    for (RankSaveDto rankItem : monthSortRank) {
      User user = userRepository.findById(rankItem.getUserId())
          .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

      // 랭크 객체 생성, 저장
      Rank newRank = Rank.builder()
          .user(user)
          .year(year)
          .month(month)
          .rankPosition(++rank)
          .totalDistance(rankItem.getTotalDistance())
          .totalTime(rankItem.getRunningDuration())
          .batchExecutedDate(batchExecutedDate)
          .build();

      rankRepository.save(newRank);
    }

  }


  /**
   * 거리 & 시간 바탕 점수 계산 메서드 거리에 따라 가중치 부여하여 점수 계산
   */
  private double calDistanceWeight(UserPost userPost) {
    double totalTimeInSeconds = userPost.getRunningDuration().toMillis() / 1000.0;
    if (totalTimeInSeconds == 0) {
      return 0;
    }

    // 기본점수 = 거리 / 시간
    double baseScore = userPost.getTotalDistance() / totalTimeInSeconds;

    // 거리에 따라 가중치 적용
    if (userPost.getTotalDistance() >= 42195) {
      baseScore *= 1.5; // 마라톤
    } else if (userPost.getTotalDistance() >= 21097.5) {
      baseScore *= 1.4; // 하프
    } else if (userPost.getTotalDistance() >= 10000) {
      baseScore *= 1.3; // 10km ~
    } else if (userPost.getTotalDistance() >= 5000) {
      baseScore *= 1.2; // 5km ~
    } else {
      baseScore *= 1.0; // ~ 5km
    }
    return baseScore;
  }
}
