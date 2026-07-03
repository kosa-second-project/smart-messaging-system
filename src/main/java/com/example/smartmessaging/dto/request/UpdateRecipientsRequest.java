package com.example.smartmessaging.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
public class UpdateRecipientsRequest {
    
    @NotEmpty(message = "업데이트할 항목이 없습니다.")
    @Valid
    private List<RecipientItem> items;
}
