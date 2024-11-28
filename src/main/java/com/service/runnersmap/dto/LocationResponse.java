package com.service.runnersmap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationResponse {
  private Long userId;
  private String nickname;
  private Double latitude;
  private Double longitude;
}
