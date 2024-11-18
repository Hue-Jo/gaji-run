package com.service.runnersmap.entity;

import com.service.runnersmap.converter.PathListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "post")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Long postId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "users_id")
  private User admin;

  @OneToOne(mappedBy = "post")
  private ChatRoom chatRoom; // 연결된 채팅방

  @Column(nullable = false)
  private String title; //제목

  @Column(nullable = false)
  private String content; //내용

  @Column(nullable = false)
  private Integer limitMemberCnt; //제한인원

  @Column(nullable = true)
  private String gender; //모집성별

  @Column(nullable = false)
  private LocalDateTime startDateTime; //출발일시

  @Column(nullable = false)
  private String startPosition;  //출발장소

  @Column(nullable = false)
  private Double distance; // 달릴거리

  @Column(nullable = false)
  private Integer paceMin; //예상 페이스분

  @Column(nullable = false)
  private Integer paceSec;  //예상 페이스초

  @Convert(converter = PathListConverter.class)
//  @Column(columnDefinition = "json")
  @Column(columnDefinition = "LONGTEXT", nullable = false)
  private List<Path> path;

  @Column(nullable = true)
  private Boolean departureYn; //출발여부

  @Column(nullable = true)
  private Boolean arriveYn; //도착여부

  @Column(nullable = false)
  private Double lat;  //위도

  @Column(nullable = false)
  private Double lng;  //경도

  @CreatedDate
  private LocalDateTime createdDateTime;
  @LastModifiedDate
  private LocalDateTime updatedDateTime;


}
