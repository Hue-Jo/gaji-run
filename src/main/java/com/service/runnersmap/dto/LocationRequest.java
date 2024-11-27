package com.service.runnersmap.dto;

import lombok.Getter;

@Getter
public class LocationRequest {

  private Long userId;
  private Long postId;
  private Double latitude;
  private Double longitude;

}
