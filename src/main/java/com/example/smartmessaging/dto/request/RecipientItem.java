package com.example.smartmessaging.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class RecipientItem {
    
    @NotNull(message = "고객 ID는 필수입니다.")
    private Long customerId;
    
    @NotNull(message = "액션은 필수입니다. (ADD 또는 REMOVE)")
    @Pattern(regexp = "^(ADD|REMOVE)$", message = "액션은 ADD 또는 REMOVE만 가능합니다.")
    private String action;
}
