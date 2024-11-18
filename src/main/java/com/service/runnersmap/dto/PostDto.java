package com.service.runnersmap.dto;

import com.service.runnersmap.entity.Path;
import com.service.runnersmap.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {

  private Long postId; //메이트모집글ID

  private Long adminId; //그룹장ID ( User랑 조인 )

  private String title; //제목

  private String content; //내용

  private Integer limitMemberCnt; //제한인원

  private String gender; //모집성별

  private LocalDateTime startDateTime; //출발일시

  private String startPosition;  //출발장소

  private Double distance; // 달릴거리

  private Integer paceMin; //예상 페이스분

  private Integer paceSec;  //예상 페이스초

  private List<Path> path; //경로

  private Boolean departureYn; //출발여부

  private Boolean arriveYn; //도착여부

  private Long chatRoomId; // 채팅방 ID

  private Double centerLat;

  private Double centerLng;

  private Long fileId; // 인증샷 Id

  private String afterRunPictureUrl;  // 인증샷

  private int likeCount;  // 좋아요 수

  private List<PostUserDto> postUsers;

  public static PostDto fromEntity(Post post) {

    if (post == null) {
      System.out.println("Post object is null before calling fromEntity.");
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
        .chatRoomId(post.getChatRoom() != null ? post.getChatRoom().getId() : null)
        .centerLat(post.getLat())
        .centerLng(post.getLng())
        .departureYn(post.getDepartureYn())
        .arriveYn(post.getArriveYn())
        .build();
  }
}
