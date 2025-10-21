package com.chathub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "帳號不可為空")
    @Size(min = 4, max = 20, message = "帳號長度必須在 4-20 字元之間")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "帳號只能包含英文字母、數字和底線")
    private String username;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, message = "密碼長度必須至少 8 字元")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "密碼必須包含大小寫字母、數字和符號各至少一個"
    )
    private String password;

    @NotBlank(message = "確認密碼不可為空")
    private String confirmPassword;
}