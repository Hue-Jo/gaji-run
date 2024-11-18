package com.service.runnersmap.service;

import com.service.runnersmap.component.JwtTokenProvider;
import com.service.runnersmap.dto.LoginResponse;
import com.service.runnersmap.dto.UserDto.AccountDeleteDto;
import com.service.runnersmap.dto.UserDto.AccountInfoDto;
import com.service.runnersmap.dto.UserDto.AccountUpdateDto;
import com.service.runnersmap.dto.UserDto.LoginDto;
import com.service.runnersmap.dto.UserDto.SignUpDto;
import com.service.runnersmap.entity.RefreshToken;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.RefreshTokenRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final FileStorageService fileStorageService;


  /**
   * 회원가입 이메일, 비밀번호, 닉네임, 성별 입력
   */
  @Transactional
  public void signUp(SignUpDto signUpDto) {

    log.info("회원가입 시도: {} ", signUpDto.getEmail());
    // 중복 회원가입 불가
    if (userRepository.findByEmail(signUpDto.getEmail()).isPresent()) {
      throw new RunnersMapException(ErrorCode.ALREADY_EXISTS_USER);
    }
    // 비밀번호 확인 로직 추가
    if (!signUpDto.getPassword().equals(signUpDto.getConfirmPassword())) {
      throw new RunnersMapException(ErrorCode.NOT_VALID_PASSWORD);
    }
    // 비밀번호 복잡성 검증 추가
    if (!isValidPassword(signUpDto.getPassword())) {
      throw new RunnersMapException(ErrorCode.NEED_MORE_PW_CONDITION);
    }
    // 동일 닉네임 사용 불가
    if (userRepository.findByNickname(signUpDto.getNickname()).isPresent()) {
      throw new RunnersMapException(ErrorCode.ALREADY_EXISTS_NICKNAME);
    }

    User user = User.builder()
        .email(signUpDto.getEmail())
        .password(passwordEncoder.encode(signUpDto.getPassword()))
        .nickname(signUpDto.getNickname())
        .gender(signUpDto.getGender())
        .profileImageUrl("")
        .createdAt(LocalDateTime.now())
        .build();
    userRepository.save(user);
    log.info("회원가입 완료");
  }

  /**
   * 로그인 이메일, 비밀번호 입력
   */
  @Transactional
  public LoginResponse login(LoginDto loginDto) {
    log.info("로그인 요청: {} ", loginDto.getEmail());

    User user = userRepository.findByEmail(loginDto.getEmail())
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
      throw new RunnersMapException(ErrorCode.NOT_VALID_PASSWORD);
    }

    //토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

    // 기존 리프레시 토큰이 있으면 업데이트, 없으면 새로 저장
    RefreshToken refreshTokenEntity = refreshTokenRepository.findByUser(user)
        .map(existingToken -> {
          existingToken.setToken(refreshToken);
          return existingToken;
        })
        .orElse(RefreshToken.builder()
            .token(refreshToken)
            .user(user)
            .build());
    refreshTokenRepository.save(refreshTokenEntity);

    log.info("로그인 성공");
    return new LoginResponse(accessToken, refreshToken,
        user.getId(), user.getNickname(), user.getEmail(), user.getGender(), user.getLastPosition(),
        user.getProfileImageUrl());
  }

  /**
   * 리프레시 토큰으로 엑세스 토큰 갱신
   */
  @Transactional
  public LoginResponse refreshAccessToken(String refreshToken) {
    log.info("리프레시 토큰으로 엑세스 토큰 갱신 요청");

    RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.INVALID_REFRESH_TOKEN));

    if (jwtTokenProvider.isTokenExpired(refreshToken)) {
      throw new RunnersMapException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    String email = storedRefreshToken.getUser().getEmail();

    String newAccessToken = jwtTokenProvider.generateAccessToken(email);
    log.info("새로운 엑세스토큰 생성 완료");

    return new LoginResponse(newAccessToken, refreshToken,
        storedRefreshToken.getUser().getId(),
        storedRefreshToken.getUser().getNickname(),
        storedRefreshToken.getUser().getEmail(),
        storedRefreshToken.getUser().getGender(),
        storedRefreshToken.getUser().getLastPosition(),
        storedRefreshToken.getUser().getProfileImageUrl()
    );
  }

  /**
   * 회원탈퇴 로그인 된 상태에서 비밀번호 재입력 후 계정삭제
   */
  @Transactional
  public void deleteAccount(String email, AccountDeleteDto accountDeleteDto) {
    log.info("회원탈퇴 요청: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // 입력한 비밀번호와 저장된 비밀번호가 일치하는지 확인
    if (!passwordEncoder.matches(accountDeleteDto.getPassword(), user.getPassword())) {
      throw new RunnersMapException(ErrorCode.NOT_VALID_PASSWORD);
    }

    // 리프레시 토큰 삭제
    refreshTokenRepository.deleteByUser(user);

    // 사용자 삭제
    userRepository.delete(user);
    log.info("회원탈퇴 완료");
  }

  /**
   * 로그아웃
   */
  @Transactional
  public void logout(String email) {
    log.info("로그아웃 요청: {} ", email);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));
    // 리프레시 토큰 삭제
    refreshTokenRepository.deleteByUser(user);
    // SecurityContext 초기화
    SecurityContextHolder.clearContext();
    log.info("로그아웃 완료");
  }

  /**
   * 회원정보 조회
   */
  @Transactional(readOnly = true)
  public AccountInfoDto getAccountInfo(String email) {
    log.info("회원정보 조회 요청: {}", email);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    return AccountInfoDto.builder()
        .nickname(user.getNickname())
        .email(user.getEmail())
        .gender(user.getGender())
        .profileImageUrl(user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "")
        .build();
  }

  /**
   * 회원정보 수정 (닉네임, 비밀번호)
   */
  @Transactional
  public void updateAccount(String email, AccountUpdateDto accountUpdateDto) {

    log.info("회원정보 수정 요청 : {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // 닉네임 수정(중복 방지)
    if (accountUpdateDto.getNewNickname() != null && !accountUpdateDto.getNewNickname().isEmpty()) {
      if (userRepository.findByNickname(accountUpdateDto.getNewNickname()).isPresent()) {
        throw new RunnersMapException(ErrorCode.ALREADY_EXISTS_NICKNAME);
      }
      user.setNickname(accountUpdateDto.getNewNickname());
    }

    // 비밀번호 수정
    if (accountUpdateDto.getNewPassword() != null && !accountUpdateDto.getNewPassword().isEmpty()) {
      if (!accountUpdateDto.getNewPassword().equals(accountUpdateDto.getNewConfirmPassword())) {
        throw new RunnersMapException(ErrorCode.NOT_VALID_PASSWORD);
      }
      // 비밀번호 복잡성 검증
      if (!isValidPassword(accountUpdateDto.getNewPassword())) {
        throw new RunnersMapException(ErrorCode.NEED_MORE_PW_CONDITION);
      }
      user.setPassword(passwordEncoder.encode(accountUpdateDto.getNewPassword()));
    }

    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    log.info("회원정보 수정 완료");
  }


  /**
   * 프로필 등록/수정
   */
  @Transactional
  public String updateProfileImage(String email, MultipartFile profileImage) throws IOException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // 파일 크기 제한 체크 (예: 5MB)
    long maxFileSize = 5 * 1024 * 1024; // 5MB
    if (profileImage.getSize() > maxFileSize) {
      throw new IOException("파일 크기는 5MB를 초과할 수 없습니다.");
    }

    // 기존 프사가 있는 경우, 삭제 후 새 이미지로 대체
    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
      String existingProfileImageUrl = user.getProfileImageUrl();
      String existingProfileImageName = existingProfileImageUrl.substring(
          existingProfileImageUrl.lastIndexOf("/") + 1);
      try {
        fileStorageService.deleteFile(existingProfileImageName);
      } catch (IOException e) {
        log.error("기존 프로필 사진 삭제 실패 {}", e.getMessage());
        throw new IOException("기존 프로필 사진 삭제 중 문제 발생");
      }
    }

    // 새 프로필사진 업로드
    String newProfileImageUrl;

    try {
      newProfileImageUrl = fileStorageService.uploadFile(profileImage);
    } catch (IOException e) {
      log.error("새 프로필 사진 업로드 실패 : {}", e.getMessage());
      throw new IOException("새 프로필 사진 업로드 중 문제 발생");
    }

    user.setProfileImageUrl(newProfileImageUrl);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    log.info("프로필 이미지 등록/수정 완료");

    return newProfileImageUrl;
  }


  /**
   * 프로필 사진 삭제
   */
  @Transactional
  public void deleteProfileImage(String email) throws IOException {

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    if (user.getProfileImageUrl() != null) {
      fileStorageService.deleteFile(user.getProfileImageUrl());
      user.setProfileImageUrl("");
      user.setUpdatedAt(LocalDateTime.now());
      userRepository.save(user);
      log.info("프로필 이미지가 삭제되었습니다.");
    }

  }

  /**
   * 마지막 위치 업데이트
   */
  @Transactional
  public void updateLastPosition(String email, String lastPosition) {
    log.info("마지막 위치 업데이트 요청");

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    user.setLastPosition(lastPosition);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    log.info("마지막 위치 업데이트 완료");

  }


  /**
   * 비밀번호 검증 최소 1개의 소문자, 숫자, 특수문자 8자 이상
   */
  public boolean isValidPassword(String password) {
    String regex = "^(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])[a-z\\d!@#$%^&*]{8,}$";
    return Pattern.compile(regex).matcher(password).matches();
  }

}
