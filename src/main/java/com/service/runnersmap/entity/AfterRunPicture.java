package com.service.runnersmap.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AfterRunPicture {
// 인증샷 엔티티
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "users_id")
  private User user;  // 업로더 (한 사용자는 여러 모집글에 대한 인증샷 업로드 가능)

  @OneToOne
  @JoinColumn(name = "post_id")
  private Post post;  // 인증샷과 연관된 모집글 (한 모집글에는 하나의 인증샷만)

  private String afterRunPictureUrl;
  private LocalDateTime createdAt;

  @Builder.Default
  private Integer likeCount = 0;  // 좋아요 수, 기본값 0

  @Version
  private Long version;

}
