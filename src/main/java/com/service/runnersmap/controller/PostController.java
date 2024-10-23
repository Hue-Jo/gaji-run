package com.service.runnersmap.controller;

import com.service.runnersmap.dto.PostDto;
import com.service.runnersmap.dto.PostInDto;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.service.PostService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

  private final PostService postService;

  /*
   * 러닝모집글/인증샷 내역 조회
   * - 지도에 표시될 데이터를 표시한다.
   */
  @GetMapping("/map-posts")
  public ResponseEntity<List<PostDto>> searchMapPost(
      @RequestParam(value = "centerLat") Double centerLat,
      @RequestParam(value = "centerLng") Double centerLng,
      @RequestParam(value = "gender", required = false) String gender,
      @RequestParam(value = "paceMinStart", required = false) Integer paceMinStart,
      @RequestParam(value = "paceMinEnd", required = false) Integer paceMinEnd,
      @RequestParam(value = "distanceStart", required = false) Long distanceStart,
      @RequestParam(value = "distanceEnd", required = false) Long distanceEnd,
      @RequestParam(value = "startDate", required = false) LocalDate startDate,
      @RequestParam(value = "startTime", required = false) String startTime,
      @RequestParam(value = "limitMemberCnt", required = false) Integer limitMemberCnt
  ) throws Exception {

    PostInDto inDto = PostInDto.builder()
        .lat(centerLat)
        .lng(centerLng)
        .gender(gender)
        .paceMinStart(paceMinStart)
        .paceMinEnd(paceMinEnd)
        .distanceStart(distanceStart)
        .distanceEnd(distanceEnd)
        .startDate(startDate)
        .startTime(startTime)
        .limitMemberCnt(limitMemberCnt)
        .build();

    List<PostDto> posts = postService.searchPost(inDto);

    if (posts.isEmpty()) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.ok(posts);
    }
  }

  /*
   * 러닝모집글 상세조회
   */
  @GetMapping
  public ResponseEntity<PostDto> searchDetailPost(
      @RequestParam(value = "postId") Long postId
  ) throws Exception {
    Optional<Post> post = postService.searchDetailPost(postId);
    if (post.isPresent()) {
      PostDto postDto = PostDto.fromEntity(post.get());
      return ResponseEntity.ok(postDto);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(null);
    }
  }

  /*
   * 러닝모집글 신규 등록
   */
  @PostMapping
  public ResponseEntity<Post> registerPost(@RequestBody PostDto postDto
  ) throws Exception {
    return ResponseEntity.ok(postService.registerPost(postDto));
  }

  /*
   * 러닝모집글 수정
   */
  @PutMapping
  public ResponseEntity<Void> modifyPost(@RequestBody PostDto postDto
  ) throws Exception {
    postService.modifyPost(postDto);
    return ResponseEntity.ok().build();
  }

  /*
   * 러닝모집글 삭제
   */
  @DeleteMapping
  public ResponseEntity<Void> deletePost(
      @RequestParam(value = "postId") Long postId
  ) throws Exception {
    postService.deletePost(postId);
    return ResponseEntity.ok().build();
  }

}
