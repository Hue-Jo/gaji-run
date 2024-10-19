package com.service.runnersmap.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // 모집글 관련 에러코드
  NOT_FOUND_POST_DATA("모집글 데이터를 찾을 수 없습니다."),

  ALREADY_DEPARTURE_POST_DATA("이미 시작 처리된 내역입니다."),

  ALREADY_START_POST_DATA("이미 시작처리된 내역입니다."),

  ALREADY_COMPLETE_POST_DATA("이미 완료처리된 내역입니다."),
  
  OWNER_ONLY_ACCESS_POST_DATA("그룹장만 변경할 수 있습니다."),

  ALREADY_EXISTS_POST_DATA("아직 진행 중인 모집내역이 있어 신규 등록 불가합니다."),

  NOT_DEPARTURE_POST_DATA("출발이후에 종료 처리 가능합니다."),

  NOT_FOUND_USER("이용자를 찾을 수 없습니다."),

  NOT_VALID_USER("이미 탈퇴 혹은 강퇴 처리된 사용자입니다."),

  NOT_POST_INCLUDE_USER("모집글에 참여한 사용자가 아니므로 처리 불가합니다."),

  ALREADY_PARTICIPATE_USER("이미 참여 중인 이용자입니다."),

  NOT_FOUND_PARTICIPATE_USER("해당 이용자는 이미 모집글에 참여하지 않는 이용자 입니다."),

  NOT_FINISHED_RUNNING("러닝이 완료된 후에만 인증샷을 업로드할 수 있습니다."),

  // 유저 관련 에러코드
  INVALID_REFRESH_TOKEN("유효하지 않은 토큰입니다."),

  ALREADY_EXISTS_USER("이미 회원가입 된 이메일입니다."),

  NOT_VALID_PASSWORD("비밀번호를 다시 확인해주세요"),

  ALREADY_EXISTS_NICKNAME("이미 사용중인 닉네임입니다."),


  // 댓글 관련 에러코드
  NOT_FOUND_COMMENT("댓글이 존재하지 않습니다."),

  WRITER_ONLY_ACCESS_COMMENT_DATA("댓글 작성자만 수정/삭제가 가능합니다."),

  // 파일 관련 에러코드
  NOT_FOUND_FILE_DATA("파일을 찾을 수 없습니다.")
  ;

  private final String description;

}
