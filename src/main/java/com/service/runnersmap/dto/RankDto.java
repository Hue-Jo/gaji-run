package com.service.runnersmap.dto;

import com.service.runnersmap.entity.Rank;
import com.service.runnersmap.service.UserPostService.DurationToStringConverter;
import java.time.Duration;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RankDto {

  private Integer rankPosition;

  private Long userId;

  private String nickName;

  private double totalDistance;

  private String totalTime;

  private String profileImageUrl;

  private LocalDate batchExecutedDate;


  public static RankDto fromEntity(Rank rank) {
    return RankDto.builder()
        .rankPosition(rank.getRankPosition())
        .userId(rank.getUser().getId())
        .nickName(rank.getUser().getNickname())
        .totalDistance(rank.getTotalDistance())
        .totalTime(DurationToStringConverter.convert(rank.getTotalTime()))
        .profileImageUrl(rank.getUser().getProfileImageUrl())
        .batchExecutedDate(rank.getBatchExecutedDate())
        .build();
  }

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
}
