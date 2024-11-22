package com.service.runnersmap.service;

import com.service.runnersmap.component.JwtTokenProvider;
import com.service.runnersmap.dto.LoginResponse;
import com.service.runnersmap.dto.UserDto.CompleteSignUpDto;
import com.service.runnersmap.entity.CustomOAuth2User;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
// OAuth2 사용자 정보 로드 & 해당 사용자가 DB에 없는 경우, 새 사용자 생성하는 서비스코드

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oAuth2User = super.loadUser(userRequest);

    String email = oAuth2User.getAttribute("email");

    User user = userRepository.findByEmail(email)
        .orElseGet(() -> createNewUser(oAuth2User));

    return new CustomOAuth2User(user, oAuth2User.getAttributes());

  }

  public boolean isNewUser(User user) {
    return user.getNickname() == null || user.getGender() == null;
  }

  private User createNewUser(OAuth2User oAuth2User) {
    String email = oAuth2User.getAttribute("email");

    User newUser = User.builder()
        .email(email)
        .password("") // 소셜로그인은 비번 필요 없음
        .nickname(null) // 닉네임 나중에 설정
        .gender(null) // 성별 나중에 설정
        .createdAt(LocalDateTime.now())
        .build();

    log.info("소셜 사용자 회원가입 : {}", email);
    return userRepository.save(newUser);
  }

  // 추가 정보 설정 & 토큰 발급
  @Transactional
  public LoginResponse completeSignUp(String email, CompleteSignUpDto completeSignUpDto) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RunnersMapException(ErrorCode.NOT_FOUND_USER));

    // 닉네임, 성별 신규 설정
    user.setNickname(completeSignUpDto.getNickname());
    user.setGender(completeSignUpDto.getGender());
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    // JWT 토큰 발급
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

    return new LoginResponse(
        accessToken,
        refreshToken,
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getGender(),
        "", // 마지막 위치
        "" ); // 프사url
  }
}
