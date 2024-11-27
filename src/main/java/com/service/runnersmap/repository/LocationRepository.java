package com.service.runnersmap.repository;

import com.service.runnersmap.entity.UserLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationRepository extends JpaRepository<UserLocation, Long> {

  @Query("SELECT ul FROM UserLocation ul " +
      "WHERE ul.post.postId = :postId " +
      " AND ul.timeStamp = (" +
      "SELECT MAX(ul2.timeStamp) " +
      "FROM UserLocation ul2 " +
      "WHERE ul2.user.id = ul.user.id AND ul2.post.postId = :postId)")
  List<UserLocation> findLatestLocationsByPostId(@Param("postId") Long postId);
}
