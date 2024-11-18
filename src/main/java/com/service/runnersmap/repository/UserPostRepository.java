package com.service.runnersmap.repository;

import com.service.runnersmap.entity.UserPost;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserPostRepository extends JpaRepository<UserPost, Long> {

  // 사용자가 참여한 모든 모집글 리스트 조회 (유효한 모집글만)
  List<UserPost> findByUser_IdAndValidYnIsTrue(Long userId);

  @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END " +
      "FROM UserPost u " +
      "WHERE u.user.id = :userId " +
      "AND FUNCTION('DATE', u.post.startDateTime) = :startDate " +
      "AND u.validYn = TRUE")
  boolean existsByUser_IdAndPost_StartDateTime_DateAndValidYnIsTrue(@Param("userId")  Long userId, @Param("startDate") LocalDate startDate);
  // 유저별 러닝리스트
  List<UserPost> findAllByUser_IdAndValidYnIsTrueAndActualEndTimeIsNull(Long userId);

  boolean existsByUser_IdAndPost_PostIdAndValidYnIsTrue(Long userId, Long postId);

  // 모집글내에 유효한 유저는 무조건 1개.
  Optional<UserPost> findByUser_IdAndPost_PostIdAndValidYnIsTrue(Long userId, Long postId);

  List<UserPost> findAllByPost_PostIdAndValidYnIsTrue(Long postId);

  @Transactional
  int deleteByPost_PostId(Long postId);

  boolean existsByPost_PostIdAndValidYnIsTrueAndActualEndTimeIsNull(Long postId);

  @Query("SELECT SUM(up.totalDistance) FROM UserPost up WHERE up.user.Id = :userId AND up.validYn = true")
  Double findTotalDistanceByUserId(@Param("userId") Long userId);

  List<UserPost> findAllByUserIdAndYearAndMonth(Long userId, int year, int month);

  List<UserPost> findAllByUser_IdAndValidYnIsTrueAndYearAndMonthAndActualEndTimeIsNotNull(Long userId, int year, int month);

  List<UserPost> findAllByValidYnIsTrueAndYearAndMonthAndActualEndTimeIsNotNull(int year, int month);

  Optional<UserPost> findByUserIdAndPostPostId(Long userId, Long postId);

}
