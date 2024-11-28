package com.service.runnersmap.controller;

import com.service.runnersmap.dto.LoginResponse;
import com.service.runnersmap.dto.UserDto;
import com.service.runnersmap.dto.UserDto.AccountDeleteDto;
import com.service.runnersmap.dto.UserDto.AccountInfoDto;
import com.service.runnersmap.dto.UserDto.AccountUpdateDto;
import com.service.runnersmap.dto.UserDto.LastPositionDto;
import com.service.runnersmap.dto.UserDto.LoginDto;
import com.service.runnersmap.dto.UserDto.SignUpDto;
import com.service.runnersmap.service.CustomOAuth2UserService;
import com.service.runnersmap.service.UserService;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final CustomOAuth2UserService  customOAuth2UserService;

  // 회원가입 API
  @PostMapping("/sign-up")
  public ResponseEntity<Void> signUp(@RequestBody SignUpDto signUpDto) {
    userService.signUp(signUpDto);
    return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
  }


  // 소셜 로그인 첫 시도시, 추가 정보 입력
  @PostMapping("/complete-sign-up")
  public ResponseEntity<LoginResponse> completeSignUp(
      @RequestParam String email,
      @RequestBody @Valid UserDto.CompleteSignUpDto completeSignUpDto) {
    LoginResponse loginResponse = customOAuth2UserService.completeSignUp(email,completeSignUpDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(loginResponse);
  }


  // 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginDto loginDto) {
    LoginResponse tokenResponse = userService.login(loginDto);
    return ResponseEntity.ok(tokenResponse); // 200 OK
  }


  // 로그아웃 API
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {

    String email = userDetails.getUsername();
    userService.logout(email);
    return ResponseEntity.ok().build(); // 200 OK
  }


  // 회원탈퇴 API
  @DeleteMapping("/my-page")
  public ResponseEntity<Void> deleteAccount(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountDeleteDto accountDeleteDto) {

    String email = userDetails.getUsername();
    userService.deleteAccount(email, accountDeleteDto);
    return ResponseEntity.noContent().build(); // 204 No Content
  }


  // 회원정보 조회 API
  @GetMapping("/my-page")
  public ResponseEntity<AccountInfoDto> getUserInfo(
      @AuthenticationPrincipal UserDetails userDetails) {

    String email = userDetails.getUsername();
    AccountInfoDto accountInfoDto = userService.getAccountInfo(email);
    return ResponseEntity.ok(accountInfoDto); // 200 OK
  }


  // 회원정보 수정 API
  @PutMapping("/my-page")
  public ResponseEntity<Void> updateAccount(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountUpdateDto accountUpdateDto) {

    String email = userDetails.getUsername();
    userService.updateAccount(email, accountUpdateDto);
    return ResponseEntity.ok().build(); // 200 OK

  }


  // 리프레시 토큰으로 엑세스 토큰 갱신
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refreshToken(@RequestBody LoginResponse refreshToken) {

    LoginResponse newTokenResponse = userService.refreshAccessToken(refreshToken.getRefreshToken());
    return ResponseEntity.ok(newTokenResponse);

  }


  // 프로필 사진 등록 API
  @PutMapping("/profile-image")
  public ResponseEntity<String> uploadProfileImage(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam("file") MultipartFile file) throws IOException {

    String email = userDetails.getUsername();
    String imageUrl = userService.updateProfileImage(email, file);
    return ResponseEntity.ok(imageUrl);  // 업로드된 프로필 이미지 URL 반환
  }


  // 프로필 사진 삭제 API
  @DeleteMapping("/profile-image")
  public ResponseEntity<Void> deleteProfileImage(
      @AuthenticationPrincipal UserDetails userDetails) throws IOException {
    String email = userDetails.getUsername();
    userService.deleteProfileImage(email);
    return ResponseEntity.ok().build();
  }

  // 마지막 위치 업데이트 API
  @PutMapping("/last-position")
  public ResponseEntity<Void> updateLastPosition(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody LastPositionDto lastPositionDto) {

    String email = userDetails.getUsername();
    userService.updateLastPosition(email, lastPositionDto.getLastPosition());
    return ResponseEntity.ok().build();

  }
}