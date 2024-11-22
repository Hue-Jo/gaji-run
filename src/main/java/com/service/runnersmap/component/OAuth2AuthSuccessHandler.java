package com.service.runnersmap.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.runnersmap.dto.LoginResponse;
import com.service.runnersmap.entity.CustomOAuth2User;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.service.CustomOAuth2UserService;
import com.service.runnersmap.type.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthSuccessHandler implements AuthenticationSuccessHandler {
// OAuth2 인증이 된 후 호출되는 핸들러 => 사용자에게 JWT 토큰 발급 / 추가정보 입력 페이지로 리디렉션

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final CustomOAuth2UserService customOAuth2UserService;


  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 인증완료된 사용자의 정보 가져오기
    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    // 이메일 추출
    String email = oAuth2User.getEmail();

    log.info("Google 로그인 성공, email: {}", email);

    // DB에서 사용자 조회
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("사용자가 존재하지 않습니다. email: {} ", email);
          return new RunnersMapException(ErrorCode.NOT_FOUND_USER);
        });

    // 신규 사용자 - 추가 정보 입력 페이지로 리디렉션
    if (customOAuth2UserService.isNewUser(user)) {
      log.info("신규 사용자의 상세 정보를 얻기 위해 /complete-sign-up 페이지로 리디렉션.");
      response.sendRedirect("/complete-sign-up?email=" + email);

    } else {
      // 기존 사용자 - JWT 토큰 발급
      log.info("기존 사용자를 위한 JWT 토큰 발급");
      String accessToken = jwtTokenProvider.generateAccessToken(email);
      String refreshToken = jwtTokenProvider.generateRefreshToken(email);

      LoginResponse loginResponse = new LoginResponse(
          accessToken,
          refreshToken,
          user.getId(),
          user.getNickname(),
          user.getEmail(),
          user.getGender(),
          user.getLastPosition(),
          user.getProfileImageUrl());

      // JSON 응답으로 반환 or 특정 페이지로 리디렉션하는 방법도 있음
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter()
          .write(new ObjectMapper().writeValueAsString(loginResponse));
      log.info("JWT 토큰 발급 완료");

    }

  }

}
