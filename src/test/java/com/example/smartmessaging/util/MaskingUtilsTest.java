package com.example.smartmessaging.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilsTest {

    @Test
    void 짧은_전화번호도_원문을_노출하지_않는다() {
        assertThat(MaskingUtils.maskPhone("123")).isEqualTo("***");
        assertThat(MaskingUtils.maskPhone("abc")).isEqualTo("***");
    }

    @Test
    void 일반_전화번호는_기존_형식을_유지해_마스킹한다() {
        assertThat(MaskingUtils.maskPhone("01012345678")).isEqualTo("010-****-5678");
        assertThat(MaskingUtils.maskPhone("021234567")).isEqualTo("021-***-4567");
    }
}
