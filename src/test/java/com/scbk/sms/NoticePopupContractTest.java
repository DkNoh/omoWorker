package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * notice window.open 팝업이 실제 동작 계약을 만족하는지 정적 리소스를 스캔해 검증한다. 대상: notice-popup.html, notice-popup.js,
 * notice.js, NoticeMapper.xml. JS 테스트 러너가 없으므로 리소스 파일 내용으로 계약을 고정한다.
 */
class NoticePopupContractTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  @Test
  void 팝업_화면은_레이아웃의_script_프래그먼트를_사용한다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));

    assertThat(html)
        .as("defaultLayout이 선언한 프래그먼트는 script(단수)다. scripts(복수)면 JS가 주입되지 않는다.")
        .contains("layout:fragment=\"script\"")
        .doesNotContain("layout:fragment=\"scripts\"");
  }

  @Test
  void 목록_조회는_CONTENT를_프로젝션하지_않고_상세만_가져온다() throws IOException {
    String xml = read(RESOURCES.resolve("mapper/basic/NoticeMapper.xml"));
    String baseQuery = block(xml, "<sql id=\"baseQuery\">", "</sql>");
    String selectDetail = block(xml, "<select id=\"selectDetail\"", "</select>");

    assertThat(baseQuery)
        .as("CLOB CONTENT는 페이지 목록/baseQuery에서 제외한다 (mybatis-oracle.md, 무제한 인출 금지).")
        .doesNotContain("CONTENT");
    assertThat(selectDetail)
        .as("selectDetail은 CONTENT를 명시적으로 조회해 상세 본문을 반환한다.")
        .contains("CONTENT");
  }

  @Test
  void 팝업은_ApiClient_언래핑_계약에_맞게_데이터를_다룬다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "http-client.js 응답 인터셉터가 ApiResponse를 언래핑해 get/post는 알맹이(data)를 반환한다. "
                + "따라서 res.data 재언래핑과 res.code 성공 검사는 항상 실패한다.")
        .doesNotContain("res.data")
        .doesNotContain("res.code");
  }

  @Test
  void 팝업은_native_date값을_ISO_로컬_datetime으로_변환해_전송한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "DTO startDt/endDt는 LocalDateTime이므로 <input type=date>의 YYYY-MM-DD를 "
                + "ISO 로컬 datetime(YYYY-MM-DDT00:00:00)으로 변환해야 Jackson 바인딩이 된다.")
        .contains("T00:00:00");
  }

  @Test
  void 팝업은_postMessage_수신처를_현재_origin으로_제한한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as("postMessage targetOrigin은 window.location.origin으로 제한한다 (와일드카드 '*' 금지).")
        .contains("postMessage(")
        .contains("window.location.origin")
        .doesNotContain("'*')");
  }

  @Test
  void 부모_창은_발신_origin을_검증한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(js)
        .as("message 수신側은 event.origin이 window.location.origin인지 검증해야 한다.")
        .contains("ev.origin")
        .contains("window.location.origin");
  }

  @Test
  void 저장_성공_알림은_부모_창의_공통_toast로_한번만_표시한다() throws IOException {
    String popupJs = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(popupJs).as("팝업은 공통 HTTP 오류 알림과 중복되는 alert를 직접 표시하지 않는다.").doesNotContain("alert(");
    assertThat(parentJs)
        .as("저장 완료는 부모 창에서 프로젝트 공통 toast로 알린다.")
        .contains("CommonUtils.toast('저장되었습니다.', 'success')");
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path);
  }

  private static String block(String content, String open, String close) {
    int start = content.indexOf(open);
    int end = content.indexOf(close, start);
    if (start < 0 || end < 0) {
      return "";
    }
    return content.substring(start, end);
  }
}
