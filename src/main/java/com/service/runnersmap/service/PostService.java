package com.service.runnersmap.service;

import com.service.runnersmap.dto.AfterRunPictureDto;
import com.service.runnersmap.dto.PostDto;
import com.service.runnersmap.dto.PostInDto;
import com.service.runnersmap.dto.PostUserDto;
import com.service.runnersmap.entity.AfterRunPicture;
import com.service.runnersmap.entity.ChatRoom;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.entity.UserPost;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.AfterRunPictureRepository;
import com.service.runnersmap.repository.LikesRepository;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final UserPostRepository userPostRepository;
  private final UserRepository userRepository;
  private final AfterRunPictureRepository afterRunPictureRepository;
  private final LikesRepository likesRepository;
  private final ChatService chatService;


  /**
   * 모집글 조회 (다양한 필터 적용)
   * 러닝이 완료된 모집글은 인증샷 정보로 반환
   */
  @Transactional(readOnly = true)
  public List<PostDto> searchPost(PostInDto inDto) throws Exception {

    // 검색 조건에 맞는 모집글 리스트 가져오기
    List<Post> posts = postRepository.findAllWithin2Km(
        inDto.getLat(),
        inDto.getLng(),
        inDto.getGender(),
        inDto.getPaceMinStart(),
        inDto.getPaceMinEnd(),
        inDto.getDistanceStart(),
        inDto.getDistanceEnd(),
        inDto.getStartDateTime(),
        inDto.getEndDateTime(),
        inDto.getLimitMemberCntStart(),
        inDto.getLimitMemberCntEnd()
    );

    return posts.stream()
        .map(post -> {
              // 인증샷이 있는 경우 인증샷 정보를 조회하여 Dto에 담음
              AfterRunPictureDto afterRunPictureDto = new AfterRunPictureDto();
              if (post.getArriveYn()) {
                // 완료된 postId 기준으로 인증샷 조회
                afterRunPictureDto = viewAfterRunPictureForPost(post);

              }
              return PostDto.builder()
                  .postId(post.getPostId())
                  .adminId(post.getAdmin().getId())
                  .title(post.getTitle())
                  .content(post.getContent())
                  .limitMemberCnt(post.getLimitMemberCnt())
                  .gender(post.getGender())
                  .startDateTime(post.getStartDateTime())
                  .startPosition(post.getStartPosition())
                  .distance(post.getDistance())
                  .paceMin(post.getPaceMin())
                  .paceSec(post.getPaceSec())
                  .path(post.getPath())
                  .centerLat(post.getLat())
                  .centerLng(post.getLng())
                  .departureYn(post.getDepartureYn())
                  .arriveYn(post.getArriveYn())
                  .afterRunPictureUrl(afterRunPictureDto.getAfterRunPictureUrl())
                  .likeCount(afterRunPictureDto.getLikeCount())
                  .fileId(afterRunPictureDto.getFileId())
                  .build();
            }
            // 인증샷이 완료된 경우에만 리스트에 포함
        ).filter(post -> !post.getArriveYn() || (post.getArriveYn()
            && post.getAfterRunPictureUrl() != null))
        .collect(Collectors.toList());


  }


  /**
   * 인증샷 조회 메서드 (모집글 조회 메서드 내에서 사용됨)
   */
  public AfterRunPictureDto viewAfterRunPictureForPost(Post post) {
    log.info("인증샷 조회 요청 : 모집글Id = {}", post.getPostId());

    Optional<AfterRunPicture> afterRunPicture = afterRunPictureRepository.findByPost(post);
    if (afterRunPicture.isPresent()) {
      AfterRunPicture picture = afterRunPicture.get();
      int likeCount = likesRepository.countByAfterRunPicture(afterRunPicture.get());

      // 인증샷에 좋아요 누른 사용자 ID 리스트 조회
      List<Long> likeUserIds = likesRepository.findByAfterRunPicture(picture)
          .stream()
          .map(likes -> likes.getUser().getId())
          .toList();

      return AfterRunPictureDto.builder()
          .afterRunPictureUrl(picture.getAfterRunPictureUrl())
          .likeCount(likeCount)
          .likeUserIds(likeUserIds)
          .fileId(picture.getId())
          .build();
    } else {
      return AfterRunPictureDto.builder().build();
    }
  }


  /**
   * 특정 모집글의 상세 정보를 조회하는 메서드
   * 모집글 상세정보 & 함께 참가한 사용자들의 정보
   */
  @Transactional(readOnly = true)
  public PostDto searchDetailPost(Long postId) throws Exception {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA));

    // 모집글에 참가한 유효한 사용자 조회
    List<UserPost> userPosts = userPostRepository.findAllByPost_PostIdAndValidYnIsTrue(postId);
    PostDto postDto = PostDto.fromEntity(post);

    // 사용자 정보를 DTO에 담아 반환
    List<PostUserDto> postUserDtoList = userPosts.stream()
        .map(userPost -> {
          PostUserDto postUserDto = new PostUserDto();
          postUserDto.setUserId(userPost.getUser().getId());
          postUserDto.setNickname(userPost.getUser().getNickname());
          postUserDto.setProfileImageUrl(userPost.getUser().getProfileImageUrl());
          return postUserDto;
        })
        .collect(Collectors.toList());

    postDto.setPostUsers(postUserDtoList);
    return postDto;
  }


  /**
   * 모집글 등록 메서드
   */
  @Transactional
  public PostDto registerPost(PostDto postDto) throws Exception {

    // admin(그룹장)이 유효한 사용자인지 확인
    User user = userRepository.findById(postDto.getAdminId())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    LocalDate newPostDate = postDto.getStartDateTime().toLocalDate();
    boolean hasConflict =
        userPostRepository.existsByUser_IdAndPost_StartDateTime_DateAndValidYnIsTrue(user.getId(), newPostDate);

    if (hasConflict) {
      throw new RunnersMapException(ErrorCode.OVERLAPPING_POST_DATE);
    }

    // post 객체 생성 (chatRoom 연결없이 먼저 생성)
    Post post = postRepository.save(Post.builder()
        .admin(user)
        .title(postDto.getTitle())
        .content(postDto.getContent())
        .limitMemberCnt(postDto.getLimitMemberCnt())
        .gender(postDto.getGender())
        .startDateTime(postDto.getStartDateTime())
        .startPosition(postDto.getStartPosition())
        .distance(postDto.getDistance())
        .paceMin(postDto.getPaceMin())
        .paceSec(postDto.getPaceSec())
        .path(postDto.getPath())
        .lat(postDto.getCenterLat())
        .lng(postDto.getCenterLng())
        .departureYn(false)
        .arriveYn(false)
        .build());

    postRepository.save(post);
    log.info("[RUNNERS LOG] 모집글 작성 postId: {} ", post.getPostId());

    // chatRoom 생성 (Post와 연결)
    ChatRoom chatRoom = chatService.createChatRoom(user.getId(), post.getPostId());
    post.setChatRoom(chatRoom);
    postRepository.save(post); // Post와 ChatRoom 관계 저장

    log.info("[RUNNERS LOG] 모집글 작성 완료 postId: {} ", post.getPostId());


    // 그룹 사용자에 그룹장 추가
    UserPost userPost = new UserPost();
    userPost.setPost(post);
    userPost.setUser(user);
    userPost.setValidYn(true);
    userPost.setTotalDistance(postDto.getDistance());
    userPost.setYear(postDto.getStartDateTime().getYear());
    userPost.setMonth(postDto.getStartDateTime().getMonthValue());
    userPostRepository.save(userPost);

    log.info("[RUNNERS LOG] 그룹 사용자 추가 userId : {} ", user.getId());

    return PostDto.fromEntity(post);

  }


  /**
   * 모집글 수정 메서드
   */
  @Transactional
  public void modifyPost(PostDto postDto) throws Exception {
    Optional<Post> postItem = postRepository.findById(postDto.getPostId());
    if (postItem.isPresent()) {
      Post post = postItem.get();

      // 변경 가능 상태인지 체크
      validatePost(post);

      post.setTitle(postDto.getTitle());
      post.setContent(postDto.getContent());
      post.setLimitMemberCnt(postDto.getLimitMemberCnt());
      post.setGender(postDto.getGender());
      post.setStartDateTime(postDto.getStartDateTime());
      post.setStartPosition(postDto.getStartPosition());
      post.setDistance(postDto.getDistance());
      post.setPaceMin(postDto.getPaceMin());
      post.setPaceSec(postDto.getPaceSec());
      post.setPath(postDto.getPath());
      postRepository.save(post);

      log.info("[RUNNERS LOG] 모집글 수정 postId : {} ", post.getPostId());

    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA);
    }
  }


  /**
   * 모집글 삭제 메서드
   */
  @Transactional
  public void deletePost(Long postId) throws Exception {
    /*
     * 그룹장 권한의 방삭제 기능 처리시에는 실제 db delete 처리한다.
     * (그룹 방 생성만 하고 아무것도 하지 않은 상태일 때만 삭제 가능하기 때문)
     * userPost 관련 로직은 delete 처리 하지 않고 유효여부 false 처리 한다.
     */
    Optional<Post> postItem = postRepository.findById(postId);
    if (postItem.isPresent()) {
      Post post = postItem.get();

      // 변경 가능 상태인지 체크
      validatePost(post);

      // 1. userPost 데이터 삭제 (post에 참여한 모든 사용자 - 최소한 그룹장은 등록되어 있음 )
      // 모집글에 참여하기로 했던 사용자 데이터 삭제
      int delCnt = userPostRepository.deleteByPost_PostId(postId);
      if (delCnt < 0) {
        throw new RunnersMapException(ErrorCode.NOT_FOUND_USER);
      }
      log.info("[RUNNERS LOG] 삭제된 참여자 수 : {} ", delCnt);

      // 2. Post 데이터 삭제 (모집글 삭제)
      postRepository.deleteById(post.getPostId());
      log.info("[RUNNERS LOG] 모집글 삭제 postId : {} ", postId);

    } else {
      throw new RunnersMapException(ErrorCode.NOT_FOUND_POST_DATA);
    }
  }


  /**
   * 모집글 상태 검증 메서드
   */
  private void validatePost(Post post) {
    // 해당 채팅방의 그룹장 이외에는 불가
//    if (!post.getAdmin().getId().equals(post.getAdminId())) {
//      throw new RunnersMapException(ErrorCode.OWNER_ONLY_ACCESS_POST_DATA);
//    }

    // 시작 전의 러닝 글에 대해서만 가능
    if (post.getDepartureYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_START_POST_DATA);
    }
    // 완료 전의 러닝 글에 대해서만 가능
    if (post.getArriveYn()) {
      throw new RunnersMapException(ErrorCode.ALREADY_COMPLETE_POST_DATA);
    }
  }

}
