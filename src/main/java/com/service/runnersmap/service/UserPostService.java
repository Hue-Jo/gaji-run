package com.service.runnersmap.service;

import com.service.runnersmap.dto.PostDto;
import com.service.runnersmap.dto.UserPostDto;
import com.service.runnersmap.dto.UserPostSearchDto;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserPostService {

  private final PostRepository postRepository;

  private final UserPostRepository userPostRepository;

  private final UserRepository userRepository;

  /**
   * 사용자별 러닝 참여 리스트 조회
   */
  @Transactional(readOnly = true)
  public List<PostDto> listParticipatePost(Long userId) throws Exception {

    // 사용자가 참여 중인 유효한 모집글(러닝이 종료되지 않은 것)을 조회
    return userPostRepository.findAllByUser_IdAndValidYnIsTrueAndActualEndTimeIsNull(userId)
        .stream()
        .map(userPost -> {
          // 참여 중인 각 모집글의 ID를 통해 모집글 상세 정보 조회
          Post post = postRepository.findById(userPost.getPost().getPostId())
              .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));
          return PostDto.fromEntity(post);

        })
        .collect(Collectors.toList());

  }

  /**
   * 특정 모집글 러닝 참가하기
   */
  @Transactional
  public void participate(Long postId, Long userId) throws Exception {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    Post newPost = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    // 이미 출발/도착한 상태의 모집글인 경우, 참여할 수 없도록 함
    if (newPost.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_DEPARTURE_POST_DATA);
    }
    if (newPost.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }

    // 동일 사용자가 같은 게시글에 중복 참여되지 않도록 함
    boolean existYn = userPostRepository.existsByUser_IdAndPost_PostIdAndValidYnIsTrue(userId,
        postId);
    if (existYn) {
      throw new RunnersMapException(ErrorCode.ALREADY_PARTICIPATE_USER);
    }


    // 새로운 모집글에 참여할 때 기존 모집글의 시작일 이후 날짜로 제한  (다음날부터 새로운 모집글에 참여 가능)
    List<UserPost> existingUserPosts = userPostRepository.findByUser_IdAndValidYnIsTrue(userId);

    for (UserPost existingPost : existingUserPosts) {
      LocalDate existingPostStartDate = existingPost.getPost().getStartDateTime().toLocalDate();
      LocalDate newPostSTartDate = newPost.getStartDateTime().toLocalDate();

      if (!newPostSTartDate.isAfter(existingPostStartDate)) {
        throw new RunnersMapException(ErrorCode.OVERLAPPING_POST_DATE);
      }
    }

    // 참여자 정보 저장
    UserPost newUserPost = new UserPost();
    newUserPost.setUser(user);
    newUserPost.setPost(newPost);
    newUserPost.setValidYn(true);
    newUserPost.setTotalDistance(newPost.getDistance());
    newUserPost.setYear(newPost.getStartDateTime().getYear());
    newUserPost.setMonth(newPost.getStartDateTime().getMonthValue());
    userPostRepository.save(newUserPost);


  }

  /**
   * 러닝 나가기
   */
  @Transactional
  public void participateOut(Long postId, Long userId) throws Exception {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    // 참여 정보 조회
    Optional<UserPost> optionalUserPost = userPostRepository.findByUser_IdAndPost_PostIdAndValidYnIsTrue(
        userId, postId);
    if (optionalUserPost.isPresent()) {
      UserPost userPost = optionalUserPost.get();
      userPost.setActualEndTime(null); // 실제 종료시간 초기화
      userPost.setValidYn(false); // 유효여부 false 처리
      userPostRepository.save(userPost); // 변경 사항 저장
    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_USER_POST_DATA);
    }
  }


  /**
   * 러닝기록 - 시작 버튼
   * 1. post 테이블에 출발 업데이트 처리한다. (1명이라도 출발했다면?)
   * 2. userPost 테이블에 실제 출발 시간을 업데이트 처리한다.
   */
  @Transactional
  public void startRecord(Long postId, Long userId) throws Exception {

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    // 이미 도착한 러닝은 시작 불가
    if (post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }

    if (!post.getDepartureYn()) {
      // 첫번째로 사용자가 출발 눌렀을 때 post 테이블의 출발여부를 true로 변경
      post.setDepartureYn(true); // 출발여부
      postRepository.save(post);
    }

    // 사용자별 실제 출발 시간 업데이트
    Optional<UserPost> optionalUserPost = userPostRepository.findByUser_IdAndPost_PostIdAndValidYnIsTrue(
        userId, postId);
    if (optionalUserPost.isPresent()) {
      UserPost userPost = optionalUserPost.get();
      if (userPost.getActualStartTime() != null) {
        throw new RunnersMapException(ErrorCode.ALREADY_START_POST_DATA);
      }
      userPost.setActualStartTime(LocalDateTime.now());
      userPostRepository.save(userPost);
    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_USER_POST_DATA);
    }

  }

  /**
   * 러닝기록 - 메이트들의 각각 러닝 정보 저장(사용자가 각각 도착할때마다 호출된다)
   * 1. post 테이블에 도착완료 업데이트 처리한다.
   * 2. userPost 테이블에 최종 도착 시간, 실제 달린 시간, 최종 달린 거리를 업데이트 처리 한다.
   * 사용자가 도착할 때마다 호출되어 각 사용자의 도착 정보를 업데이트하고, 모든 사용자가 도착한 경우 러닝 완료 처리
   */
  @Transactional
  public void completeRecord(Long postId, Long userId) throws Exception {

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

//    if (!post.getDepartureYn()) {
//      throw new RunnersMapException(ErrorCode.NOT_DEPARTURE_POST_DATA);
//    }
//    if (post.getArriveYn()) {
//      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
//    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // userPost 테이블 update (러닝 종료시간, 거리, 시간 업데이트)
    Optional<UserPost> optionalUserPost =
        userPostRepository.findByUser_IdAndPost_PostIdAndValidYnIsTrue(userId, postId);
    if (optionalUserPost.isPresent()) {
      UserPost userPost = optionalUserPost.get();
      if (userPost.getActualEndTime() != null) {
        throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
      }
      userPost.setActualEndTime(LocalDateTime.now());
      userPost.setRunningDuration(
          Duration.between(userPost.getActualStartTime(), LocalDateTime.now()));
      userPostRepository.save(userPost);

      // 미완료 러너 존재여부  -> true : 미도착, false : 도착
      boolean existsIncompleteUser =
          userPostRepository.existsByPost_PostIdAndValidYnIsTrueAndActualEndTimeIsNull(postId);
      if (!existsIncompleteUser) {
        // 모든 사용자가 도착하면 도착 처리(post 테이블 도착처리)
        // 만약에 사용자가 모두 도착하지 않았는데 비정상 종료처리가 되어야 한다면 그룹장이 모집글 방삭제를 해야한다.
        post.setArriveYn(true); // 도착여부
        postRepository.save(post);
      }

    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_USER_POST_DATA);
    }
  }


  /**
   * 러닝기록 - 조회
   * 1. ALL   : 누적달린 거리
   * 2. MONTH : 입력받은 월의 총 달린 거리
   * 3. DAY   : 입력받은 월의 달린거리
   */
  @Transactional(readOnly = true)
  public List<UserPostSearchDto> searchRunningData(Long userId, int year, int month)
      throws Exception {

    List<UserPostSearchDto> result = new ArrayList<>();

    // 전체 누적 거리 조회
    result.add(new UserPostSearchDto("ALL", userPostRepository.findTotalDistanceByUserId(userId)));

    // 월별 총 달린 거리 계산
    double sumMonthTotaldistance = 0;
    List<UserPost> userPosts = userPostRepository.findAllByUserIdAndYearAndMonth(userId, year, month);
    for (UserPost item : userPosts) {
      sumMonthTotaldistance += item.getTotalDistance() == null ? 0 : item.getTotalDistance();
    }
    result.add(new UserPostSearchDto("MONTH", sumMonthTotaldistance));

    // 일별 달린 거리, 시간 계산
    List<UserPostDto> runningMonths =
        userPostRepository.findAllByUser_IdAndValidYnIsTrueAndYearAndMonthAndActualEndTimeIsNotNull(userId, year, month)
        .stream()
        .collect(Collectors.groupingBy(
            // 사용자별 일자 기준 groupBy
            up -> Map.entry(up.getUser().getId(), up.getActualEndTime().toLocalDate()),
            Collectors.collectingAndThen(
                Collectors.toList(),
                group -> {
                  double totalDistanceSum = group.stream()
                      .mapToDouble(UserPost::getTotalDistance)
                      .sum();

                  long totalRunningDuration = group.stream()
                      .mapToLong(up -> up.getRunningDuration().getSeconds())
                      .sum();

                  return UserPostDto.builder()
                      .distance(totalDistanceSum)
                      .runningTime(DurationToStringConverter.convert(
                          Duration.ofSeconds(totalRunningDuration)))
                      .day(group.get(0).getActualEndTime().getDayOfMonth())
                      .build();
                }
            )
        ))
        .values().stream()
        .collect(Collectors.toList());

    result.add(new UserPostSearchDto("DAY", runningMonths));

    return result;
  }


  /**
   * Duration 객체를 시간, 분, 초 형식의 문자열로 변환
   */
  public class DurationToStringConverter {

    public static String convert(Duration duration) {
      if (duration == null) {
        return "00:00:00";
      }
      long seconds = duration.getSeconds();
      return String.format("%02d:%02d:%02d",
          (seconds / 3600), // 시간
          (seconds % 3600) / 60, // 분
          (seconds % 60)); // 초
    }
  }

  /**
   * 해당 사용자가 특정 러닝 모집글에 참여한 상태 조회 메서드
   */
  @Transactional(readOnly = true)
  public String userPostState(Long postId, Long userId) {

    Optional<UserPost> optionalUserPost =
        userPostRepository.findByUser_IdAndPost_PostIdAndValidYnIsTrue(userId, postId);
    if (optionalUserPost.isPresent()) {

      UserPost userPost = optionalUserPost.get();
      if (!userPost.getValidYn()) {
        throw new RunnersMapException(ErrorCode.NOT_VALID_USER);
      }
      // 실제 시작 시간이 null이면 아직 러닝을 시작하지 않은 상태이므로 "START" 반환
      // 그렇지 않으면, 이미 러닝이 시작된 상태이므로 "COMPLETE"를 반환
      return userPost.getActualStartTime() != null ? "COMPLETE" : "START";
    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_USER_POST_DATA);
    }
  }

}
