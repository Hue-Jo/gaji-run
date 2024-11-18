package com.service.runnersmap.service;

import com.service.runnersmap.dto.ChatMessageDto;
import com.service.runnersmap.entity.ChatMessage;
import com.service.runnersmap.entity.ChatRoom;
import com.service.runnersmap.entity.Post;
import com.service.runnersmap.entity.User;
import com.service.runnersmap.exception.RunnersMapException;
import com.service.runnersmap.repository.ChatMessageRepository;
import com.service.runnersmap.repository.ChatRoomRepository;
import com.service.runnersmap.repository.PostRepository;
import com.service.runnersmap.repository.UserPostRepository;
import com.service.runnersmap.repository.UserRepository;
import com.service.runnersmap.type.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final UserPostRepository userPostRepository;
  private final SimpMessagingTemplate template;
  private final PostRepository postRepository;


  // 사용자가 모집글에 참여했는지 확인하는 메서드
  private boolean isUserParticipatingInPost(Long userId, Long postId) {
    return userPostRepository.findByUserIdAndPostPostId(userId, postId).isPresent();
  }

  /**
   * 채팅방 생성
   */
  public ChatRoom createChatRoom(Long userId, Long postId) {

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 모집글"));

    userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    // 이미 해당 모집글에 대한 채팅방이 존재하는지 확인
    if (chatRoomRepository.findByPost_PostId(postId).isPresent()) {
      throw new RuntimeException("이미 해당 모집글에 대한 채팅방이 존재합니다.");
    }

    ChatRoom chatRoom = ChatRoom.builder().post(post).build();
    post.setChatRoom(chatRoom);

  return chatRoomRepository.save(chatRoom);
  }

  /**
   * 사용자가 채팅방에 들어왔을 때 입장 알림 메시지 전송
   */
  @Transactional
  public void handleUserEnter(ChatMessageDto chatMessageDto) {

    User sender = userRepository.findById(chatMessageDto.getSenderId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방"));

    // 참여 여부 확인
    Long postId = chatRoom.getPost().getPostId();
    if (!isUserParticipatingInPost(chatMessageDto.getSenderId(), postId)) {
      throw new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER);
    }

    // 입장 알림메시지 (모든 사용자에게 전송)
    String enterMessage = sender.getNickname() + "님이 채팅방에 입장하셨습니다.";
    chatMessageDto = ChatMessageDto.builder()
        .senderNickname(sender.getNickname())
        .message(enterMessage)
        .build();
    template.convertAndSend("/sub/chat/room/" + chatRoom.getId(), chatMessageDto);

    // 이전 메시지들 불러오기 (입장한 사용자에게만 전송되도록 수정)
    List<ChatMessageDto> previousMessages = getMessages(chatRoom.getId());
    template.convertAndSendToUser(sender.getId().toString(),"/user/queue/chat/room/" + chatRoom.getId(), previousMessages);

  }
//
//
  /**
   * 사용자가 퇴장시 퇴장 알림 메시지 전송 메서드
   */
  public void handleUserExit(ChatMessageDto chatMessageDto) {

    User sender = userRepository.findById(chatMessageDto.getSenderId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방"));

    String exitMessage = sender.getNickname() + "님이 채팅방을 나갔습니다.";
    chatMessageDto.builder()
        .message(exitMessage)
        .build();

    template.convertAndSend("/sub/chat/room/" + chatRoom.getId(), chatMessageDto);
  }


  /**
   * 클라이언트가 보낸 메시지를 저장하고 브로드캐스트하는 메서드
   */
  public void saveAndBroadcastMessage(ChatMessageDto chatMessageDto) {

    ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDto.getChatRoomId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 채팅방입니다."));

    User sender = userRepository.findById(chatMessageDto.getSenderId())
        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    // 참여 여부 확인
    Long postId = chatRoom.getPost().getPostId();
    if (!isUserParticipatingInPost(chatMessageDto.getSenderId(), postId)) {
      throw new RunnersMapException(ErrorCode.NOT_POST_INCLUDE_USER);
    }

    // 데이터베이스에 저장될 메시지
    ChatMessage message = ChatMessage.builder()
        .chatRoom(chatRoom)
        .sender(sender)
        .message(chatMessageDto.getMessage())
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);

    // 클라이언트에게 전달할 메시지
    ChatMessageDto responseDto = ChatMessageDto.builder()
        .chatRoomId(chatRoom.getId())
        .senderId(sender.getId())
        .senderNickname(sender.getNickname())
        .message(message.getMessage())
        .sentAt(message.getSentAt())
        .build();
    template.convertAndSend("/sub/chat/room/" + chatMessageDto.getChatRoomId(), responseDto);
  }


  /**
   * 메시지를 조회하는 메서드
   */
  public List<ChatMessageDto> getMessages(Long chatRoomId) {

    List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId);

    return messages.stream()
        .map(message -> ChatMessageDto.builder()
            .chatRoomId(message.getChatRoom().getId())
            .senderId(message.getSender().getId())
            .senderNickname(message.getSender().getNickname())
            .message(message.getMessage())
            .sentAt(message.getSentAt())
            .build())
        .collect(Collectors.toList());
  }
}
