package com.service.runnersmap.controller;

import com.service.runnersmap.dto.LocationRequest;
import com.service.runnersmap.dto.LocationResponse;
import com.service.runnersmap.service.LocationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;


  /**
   * 사용자 위치 업데이트
   */
  @PostMapping("/update")
  public ResponseEntity<Void> updateLocation(@RequestBody LocationRequest request) {
    locationService.updateLocation(request);
    return ResponseEntity.ok().build();
  }


  /**
   * 같이 러닝 중인 사용자들의 위치 조회
   */
  @GetMapping("/group/{postId}")
  public ResponseEntity<List<LocationResponse>> getGroupLocations(@PathVariable Long postId) {
    List<LocationResponse> locations = locationService.getGroupLocations(postId);
    return ResponseEntity.ok(locations);
  }

}
