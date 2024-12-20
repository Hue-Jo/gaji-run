package com.service.runnersmap.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostInDto {

  private Double lat;
  private Double lng;
  private String gender;
  private Integer paceMinStart;
  private Integer paceMinEnd;
  private Long distanceStart;
  private Long distanceEnd;

  private LocalDateTime startDateTime;
  private LocalDateTime endDateTime;

  private Integer limitMemberCntStart;
  private Integer limitMemberCntEnd;

}
