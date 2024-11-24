package com.service.runnersmap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SignUpDto {

    private String email;
    private String password;
    private String confirmPassword;
    private String nickname;
    private String gender;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CompleteSignUpDto {

    @NotBlank(message = "닉네임을 입력해주세요")
    private String nickname;  // 닉네임
    @NotBlank(message = "성별을 입력해주세요.")
    private String gender;    // 성별

  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class LoginDto {

    @Email(message = "이메일 형식에 맞게 입력해주세요")
    @NotBlank(message = "이메일을 입력하세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요. 소문자, 숫자, 특수문자 포함 8자 이상")
    private String password;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountDeleteDto {

    private String password;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountInfoDto {

    private String nickname;
    private String email;
    private String gender;
    private String profileImageUrl;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountUpdateDto {

    private String newNickname;
    private String newPassword;
    private String newConfirmPassword;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class LastPositionDto {
    private String lastPosition;
  }
}