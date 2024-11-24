package com.service.runnersmap.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

  private final User user;
  // OAuth2 인증 과정에서 제공된 사용자의 데이터 담긴 맵 (JSON 형식)
  // 구글이 제공하는 사용자 데이터를 담고 있음.
  private final Map<String, Object> attributes;

  // 사용자 속성 정보 반환
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 우리 서비스에서는 ROLE_USER를 사용하지 않기 때문에 권한 없이 반환
    return Collections.emptyList();
  }

  // 사용자 식별자 반환 (유일식별자로 이메일 반환)
  // 인터페이스 구현을 위해 오버라이드 했으나 식별자가 이메일인 게 꺼림칙.
  // id로 바꾸자니 반환형이 String이라 일단 email로.
  @Override
  public String getName() {
    return user.getEmail();
  }

// 사실상 상단의 메서드와 같은 역할이지만 명확한 역할구분을 위해 따로 만듦
  public String getEmail() {
    return user.getEmail();
  }
}
