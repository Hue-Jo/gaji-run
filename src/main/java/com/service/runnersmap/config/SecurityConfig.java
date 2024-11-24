package com.service.runnersmap.config;

import com.service.runnersmap.component.JwtFilter;
import com.service.runnersmap.component.OAuth2AuthSuccessHandler;
import com.service.runnersmap.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthSuccessHandler oAuth2AuthSuccessHandler;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
      throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
        // HTTP 요청에 대한 권한
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/user/sign-up",
                "/api/user/login",
                "/api/user/refresh")
            .permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/oauth2/authorization/google")
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2AuthSuccessHandler)
        )
        // 폼 로그인 기능을 비활성화
        .formLogin(form -> form.disable())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // jwt 필터 추가
    return http.build();
  }

//  @Bean
//  public UrlBasedCorsConfigurationSource corsConfigurationSource() {
//    CorsConfiguration configuration = new CorsConfiguration();
//    configuration.setAllowCredentials(false); // 인증 관련 설정
//    configuration.addAllowedOriginPattern("http://");
//    configuration.addAllowedHeader("*"); // 모든 헤더 허용
//    configuration.addAllowedMethod("*"); // 모든 메서드 허용 (GET, POST, PUT, DELETE 등)
//
//    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정 적용
//    return source;
//  }

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true); // 쿠키 및 인증 정보를 허용
    config.addAllowedOriginPattern("*"); // 모든 도메인을 허용 (cors관련)
    config.addAllowedHeader("*"); // 모든 헤더를 허용
    config.addAllowedMethod("*"); // 모든 HTTP 메서드를 허용
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }

  private UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true); // 인증 관련 설정
    configuration.addAllowedOriginPattern("*"); // 모든 도메인을 허용 (cors관련)
    configuration.addAllowedHeader("*"); // 모든 헤더 허용
    configuration.addAllowedMethod("*"); // 모든 메서드 허용 (GET, POST, PUT, DELETE 등)

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 CORS 설정 적용
    return source;
  }
}