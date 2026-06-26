package com.scbk.sms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingUtilTest {

  @Test
  void 이름은_첫_끝_글자만_남기고_마스킹한다() {
    assertThat(MaskingUtil.maskName("홍길동")).isEqualTo("홍*동");
    assertThat(MaskingUtil.maskName("남궁민수")).isEqualTo("남**수");
    assertThat(MaskingUtil.maskName("홍길")).isEqualTo("홍*");
  }

  @Test
  void 전화번호는_가운데_자리를_마스킹한다() {
    assertThat(MaskingUtil.maskPhone("01012345678")).isEqualTo("010-****-5678");
    assertThat(MaskingUtil.maskPhone("010-1234-5678")).isEqualTo("010-****-5678");
    assertThat(MaskingUtil.maskPhone("0212345678")).isEqualTo("02-****-5678");
  }

  @Test
  void 주민번호는_성별_자리만_남기고_마스킹한다() {
    assertThat(MaskingUtil.maskRrn("9001011234567")).isEqualTo("900101-1******");
    assertThat(MaskingUtil.maskRrn("900101-1234567")).isEqualTo("900101-1******");
  }

  @Test
  void 카드번호는_가운데_8자리를_마스킹한다() {
    assertThat(MaskingUtil.maskCard("1234567812345678")).isEqualTo("1234-****-****-5678");
  }

  @Test
  void 텍스트_안의_주민번호와_전화번호_후보를_마스킹한다() {
    // given : 감사 로그용 JSON 직렬화 파라미터
    String json = "{\"ssn\":\"9001011234567\",\"phone\":\"01012345678\",\"name\":\"홍길동\"}";

    // when
    String masked = MaskingUtil.maskPrivacyInText(json);

    // then
    assertThat(masked).contains("900101-1******");
    assertThat(masked).contains("010-****-5678");
    assertThat(masked).doesNotContain("9001011234567");
    assertThat(masked).doesNotContain("01012345678");
  }
}
