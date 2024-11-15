package com.service.runnersmap.repository;

import com.service.runnersmap.entity.Post;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(value =
      "SELECT * " +
          "FROM post p " +
          // 사용자의 위치(lat, lng)를 기준으로 2km 내 존재하는 Post 조회
          "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(p.lat)) " +
          "* cos(radians(p.lng) - radians(:lng)) " +
          "+ sin(radians(:lat)) * sin(radians(p.lat)))) < 2 " +

          // 현재시각보다 미래에 러닝을 할 게시글 or 러닝 끝난 지 3일 내의 모집글(인증샷도 보여주려고)
          "AND ( (p.start_date_time >= CURRENT_TIMESTAMP AND p.arrive_yn = false) " +
          "   OR ( p.start_date_time >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 DAY) "
          + " AND p.start_date_time <= CURRENT_TIMESTAMP "
          + " AND p.arrive_yn = true) ) " +
          // 젠더가 null 이면 성별 상관 없이, 젠더 지정되면 해당 성별에 맞는 모집글만
          "AND (:gender IS NULL OR p.gender = :gender) " +
          // 사용자가 지정한 페이스 범위에 맞는 모집글 (null이면 페이스 제한 없이 모든 모집글 조회)
          "AND (:paceMinStart IS NULL OR (p.pace_min + p.pace_sec / 60) >= :paceMinStart) " +
          "AND (:paceMinEnd IS NULL OR (p.pace_min + p.pace_sec / 60) <= :paceMinEnd) " +
          // 사용자가 지정한 거리 범위에 맞는 모집글 (null이면 거리 제한 없이 모든 모집글 조회)
          "AND (:distanceStart IS NULL OR p.distance >= :distanceStart) " +
          "AND (:distanceEnd IS NULL OR p.distance <= :distanceEnd) " +

          // 사용자가 지정한 특정 날짜와 시간에 해당하는 모집글만 조회 (수정 예정)
//          "AND (:startDate IS NULL OR (p.start_date_time BETWEEN :startDate AND DATE_ADD(:startDate, INTERVAL 1 DAY))) " +
//          "AND (:startTime IS NULL OR TIME_FORMAT(p.start_date_time, '%H%i') = :startTime) " +

          // 날짜/시간 필터
          "AND (:firstStartDate IS NULL OR :firstStartTime IS NULL OR " +
          "p.start_date_time >= CONCAT(:firstStartDate, ' ', :firstStartTime)) " +
          "AND (:secondStartDate IS NULL OR :secondStartTime IS NULL OR " +
          "p.start_date_time <= CONCAT(:secondStartDate, ' ', :secondStartTime)) " +

          // 인원수 필터
          "AND (:limitMemberCntStart IS NULL OR p.limit_member_cnt >= :limitMemberCntStart)" +
          "AND (:limitMemberCntEnd IS NULL OR p.limit_member_cnt <= :limitMemberCntEnd)" +
          // 시작 시간 기준 오름차순 (최대 20개 제한)
          "ORDER BY p.start_date_time ASC " +
          "LIMIT 20",
      nativeQuery = true)
  List<Post> findAllWithin2Km(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("gender") String gender,
      @Param("paceMinStart") Integer paceMinStart,
      @Param("paceMinEnd") Integer paceMinEnd,
      @Param("distanceStart") Long distanceStart,
      @Param("distanceEnd") Long distanceEnd,
      @Param("firstStartDate") LocalDate firstStartDate,
      @Param("firstStartTime") LocalTime firstStartTime,
      @Param("secondStartDate") LocalDate secondStartDate,
      @Param("secondStartTime") LocalTime secondStartTime,
      @Param("limitMemberCntStart") Integer limitMemberCntStart,
      @Param("limitMemberCntEnd") Integer limitMemberCntEnd
  );


  boolean existsByAdminIdAndArriveYnIsFalse(Long adminId);

}
