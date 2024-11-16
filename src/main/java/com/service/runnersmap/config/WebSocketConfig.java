package com.service.runnersmap.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 웹소켓 통신 설정
@Slf4j
@Configuration
@EnableWebSocketMessageBroker   // stomp 사용을 위해 선언
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  // WebSocketConfigurer : 1대1 채팅방
  // WebSocketMessageBrokerConfigurer : 단톡방 - 메시징 시스템 필요

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat")  // 클라이언트가 WebSocket 연결을 위해 사용할 STOMP 엔드포인트(URL)
        .setAllowedOriginPatterns("*") //일단은 모든 출처에서 연결 허용 (추후 수정 해야 할 듯)
        .withSockJS();

    log.info("STOMP 엔드포인트 : /ws/chat");

  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {

    // 현재는 메모리 내 브로커를 사용하여 시도해보고 있습니다.
    // 이후에 외부 메시지 브로커로 변경 고려해야 할 듯합니다.
    registry.enableSimpleBroker("/sub");  // (서버 -> 클라이언트) 메시지 받기
    registry.setApplicationDestinationPrefixes("/pub");    // (클라이언트 -> 서버) 메시지 보내기

    log.info("메시지 브로커 설정 완료 : '브로커 /sub', '목적지 /pub'로 설정");

  }

}