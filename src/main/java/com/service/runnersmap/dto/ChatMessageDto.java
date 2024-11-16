package com.service.runnersmap.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

  private Long chatRoomId;
  private Long senderId;
  private String senderNickname;
  private String message;
  private LocalDateTime sentAt; // 메시지 전송시간
  private Long postId;
}