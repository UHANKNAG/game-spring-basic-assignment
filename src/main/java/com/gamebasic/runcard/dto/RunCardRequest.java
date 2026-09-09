package com.gamebasic.runcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
public class RunCardRequest {
    // TODO (Lv 5): API 명세의 카드 필드 제약을 Bean Validation 어노테이션으로 붙이세요.
    @NotBlank
    private String cardType;
    @Range(min = 0, max = 10)
    private int acquiredFloor;
}
