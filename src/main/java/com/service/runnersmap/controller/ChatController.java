package com.service.runnersmap.controller;

import com.service.runnersmap.dto.ChatMessageDto;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.service.ChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

  private final ChatService chatService;
  private final UserPostRepository userPostRepository;

  /**
   * 채팅방에 처음 접속할 때 입장 알림 메시지 전송
   */
  @MessageMapping(value = "/enter")
  public void handleUserEnter(@Payload ChatMessageDto chatMessageDto) {
    validateUserAccess(chatMessageDto.getSenderId(), chatMessageDto.getChatRoomId());
    log.info("사용자 {}가 채팅방 {}에 입장했습니다.", chatMessageDto.getSenderId(), chatMessageDto.getChatRoomId());
    chatService.handleUserEnter(chatMessageDto);
  }

  /**
   * 채팅방에서 퇴장할 때 퇴장 알림 메시지 전송
   */
  @MessageMapping(value = "/exit")
  public void handleUserExit(@Payload ChatMessageDto chatMessageDto) {
    validateUserAccess(chatMessageDto.getSenderId(), chatMessageDto.getChatRoomId());
    log.info("사용자 {}가 채팅방 {}에서 퇴장했습니다.", chatMessageDto.getSenderId(), chatMessageDto.getChatRoomId());
    chatService.handleUserExit(chatMessageDto);
  }

  /**
   * 메시지 전송
   */
  @MessageMapping(value = "/message")
  @SendTo("/sub/chat/room/{chatRoomId}")
  public void sendMessage(ChatMessageDto message) {
    validateUserAccess(message.getSenderId(), message.getChatRoomId());
    try {
      chatService.saveAndBroadcastMessage(message);
    } catch (RuntimeException e) {
      log.error("메시지 전송 중 오류 발생 : {} ", e.getMessage());
    }
  }

  /**
   * 특정 채팅방의 메시지를 조회
   */
  @GetMapping("/messages/{chatRoomId}")
  public ResponseEntity<List<ChatMessageDto>> getMessages(@PathVariable Long chatRoomId, @RequestParam Long userId) {
    validateUserAccess(userId, chatRoomId);
    List<ChatMessageDto> messageDtos = chatService.getMessages(chatRoomId);
    if (messageDtos.isEmpty()) {
      return ResponseEntity.notFound().build(); // 메시지가 없는 경우 404 반환
    }
    return ResponseEntity.ok(messageDtos); // 메시지 반환
  }


  /**
   * 사용자 권한 검증 메서드
   */
  private void validateUserAccess(Long userId, Long chatRoomId) {
    boolean isParticipating = userPostRepository.findByUserIdAndPostPostId(userId, chatRoomId).isPresent();
    if (!isParticipating) {
      throw new RuntimeException("해당 채팅방에 접근할 권한이 없습니다.");
    }
  }
}
