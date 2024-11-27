package com.service.runnersmap.service;

import com.service.runnersmap.dto.LocationRequest;
import com.service.runnersmap.dto.LocationResponse;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserLocation;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.LocationRepository;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final LocationRepository realTimeLocationRepository;
  private final LocationRepository locationRepository;

  /**
   * 좌표 저장 로직 FE에서 받은 좌표 저장
   */
  public void updateLocation(LocationRequest request) {
    User user = userRepository.findById(request.getUserId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));
    Post post = postRepository.findById(request.getPostId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    UserLocation location = UserLocation.builder()
        .user(user)
        .post(post)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .timeStamp(LocalDateTime.now())
        .build();

    realTimeLocationRepository.save(location);
  }

  /**
   * 특정 러닝그룹의 참여자들 위치 조회
   */
  public List<LocationResponse> getGroupLocations(Long postId) {
    List<UserLocation> recentLocations = locationRepository.findLatestLocationsByPostId(postId);

    return recentLocations.stream()
        .map(location -> new LocationResponse(
            location.getUser().getId(),
            location.getUser().getNickname(),
            location.getLatitude(),
            location.getLongitude()
        ))
        .collect(Collectors.toList());
  }
}
