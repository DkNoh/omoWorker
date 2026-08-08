package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * SweetAlert2 공통 통합 계약을 정적 리소스 스캔으로 검증한다. 대상: vendored SweetAlert2 자산, MANIFEST.md,
 * defaultLayout.html, notify.js, admin-ui-bridge.css, common-utils.js, modal-patterns.html. JS 테스트 러너가
 * 없으므로 리소스 파일 내용으로 계약을 고정한다.
 */
class NotifySweetAlertContractTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  // ─────────────────────────────────────────────────────────────────────────
  // 1. Vendored asset 존재 및 무결성
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void vendored_asset_파일_존재_및_비어있지_않음() throws IOException {
    // given
    Path js = RESOURCES.resolve("static/lib/sweetalert2/11.26.25/sweetalert2.all.min.js");
    Path css = RESOURCES.resolve("static/lib/sweetalert2/11.26.25/sweetalert2.min.css");
    Path license = RESOURCES.resolve("static/lib/sweetalert2/11.26.25/LICENSE");

    // when / then
    assertThat(Files.exists(js)).as("sweetalert2.all.min.js 가 존재해야 한다.").isTrue();
    assertThat(Files.size(js)).as("sweetalert2.all.min.js 가 0바이트면 안 된다.").isGreaterThan(0);

    assertThat(Files.exists(css)).as("sweetalert2.min.css 가 존재해야 한다.").isTrue();
    assertThat(Files.size(css)).as("sweetalert2.min.css 가 0바이트면 안 된다.").isGreaterThan(0);

    assertThat(Files.exists(license)).as("LICENSE 가 존재해야 한다.").isTrue();
    assertThat(Files.size(license)).as("LICENSE 가 0바이트면 안 된다.").isGreaterThan(0);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 2. MANIFEST.md 기록
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void manifest에_SweetAlert2_항목이_SHA256과_함께_기록됨() throws IOException {
    // given
    String manifest = read(RESOURCES.resolve("static/lib/MANIFEST.md"));

    // when / then
    assertThat(manifest)
        .as("MANIFEST.md 에 sweetalert2.all.min.js 항목이 기록되어야 한다.")
        .contains("sweetalert2/11.26.25/sweetalert2.all.min.js");
    assertThat(manifest)
        .as("MANIFEST.md 에 sweetalert2.min.css 항목이 기록되어야 한다.")
        .contains("sweetalert2/11.26.25/sweetalert2.min.css");
    assertThat(manifest)
        .as("MANIFEST.md 에 LICENSE 항목이 기록되어야 한다.")
        .contains("sweetalert2/11.26.25/LICENSE");
    assertThat(manifest).as("MANIFEST.md 에 버전 11.26.25 가 기록되어야 한다.").contains("11.26.25");
    assertThat(manifest).as("MANIFEST.md 에 라이선스 MIT 가 기록되어야 한다.").contains("MIT");
    assertThat(manifest).as("MANIFEST.md 에 SHA-256 무결성 해시가 기록되어야 한다.").contains("SHA-256");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 3. defaultLayout 로드 순서
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void defaultLayout에서_SweetAlert2가_notify_js_보다_먼저_로드됨() throws IOException {
    // given
    String html = read(RESOURCES.resolve("templates/defaultLayout.html"));

    // when
    int swalJs = html.indexOf("sweetalert2.all.min.js");
    int notifyJs = html.indexOf("notify.js");

    // then
    assertThat(swalJs).as("defaultLayout 에 sweetalert2.all.min.js 스크립트가 존재해야 한다.").isGreaterThan(-1);
    assertThat(notifyJs).as("defaultLayout 에 notify.js 스크립트가 존재해야 한다.").isGreaterThan(-1);
    assertThat(swalJs)
        .as("SweetAlert2 JS 는 notify.js 보다 먼저 로드되어야 한다 (window.Swal 의존).")
        .isLessThan(notifyJs);
    assertThat(html).as("defaultLayout 에 sweetalert2.min.css 스타일이 존재해야 한다.").contains("sweetalert2.min.css");
    assertThat(html)
        .as("CDN 참조 금지 — src/href 에 http:// 또는 https:// 직접 참조가 없어야 한다 (th:href=\"@{...}\" 는 허용).")
        .doesNotContain("src=\"http")
        .doesNotContain("href=\"http");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 4. modal-patterns 샘플 로드 순서
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void modal_patterns_샘플에서_SweetAlert2_로드_순서_일치() throws IOException {
    // given
    String html = read(RESOURCES.resolve("static/samples/modal-patterns.html"));

    // when
    int swalJs = html.indexOf("../lib/sweetalert2/11.26.25/sweetalert2.all.min.js");
    int notifyJs = html.indexOf("notify.js");

    // then
    assertThat(swalJs).as("modal-patterns 샘플에 sweetalert2.all.min.js 상대 경로가 존재해야 한다.").isGreaterThan(-1);
    assertThat(html)
        .as("modal-patterns 샘플에 sweetalert2.min.css 상대 경로가 존재해야 한다.")
        .contains("../lib/sweetalert2/11.26.25/sweetalert2.min.css");
    assertThat(notifyJs).as("modal-patterns 샘플에 notify.js 가 존재해야 한다.").isGreaterThan(-1);
    assertThat(swalJs)
        .as("샘플에서도 SweetAlert2 JS 가 notify.js 보다 먼저 로드되어야 한다.")
        .isLessThan(notifyJs);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 5. notify.js Swal.fire 사용 및 공개 API 보존
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void notify_js가_Swal_fire를_사용하고_공개_API를_보존함() throws IOException {
    // given
    String js = read(RESOURCES.resolve("static/js/common/notify.js"));

    // when / then
    assertThat(js).as("notify.js 는 SweetAlert2 Swal.fire 로 다이얼로그를 표시한다.").contains("Swal.fire");
    assertThat(js).as("window.Swal fail-closed 가드가 존재해야 한다.").contains("window.Swal");
    assertThat(js).as("공개 API alert 시그니처가 보존되어야 한다.").contains("alert: (msg, title, callback)");
    assertThat(js).as("공개 API confirm 시그니처가 보존되어야 한다.").contains("confirm: (msg, callback, title, onCancel)");
    assertThat(js).as("공개 API toast 가 보존되어야 한다.").contains("toast,");
    assertThat(js).as("공개 API refreshIcons 가 보존되어야 한다.").contains("refreshIcons,");
    assertThat(js)
        .as("정적 메뉴와 사이드바 토글 아이콘은 최초 DOM 구성 직후 렌더링되어야 한다.")
        .contains("document.addEventListener('DOMContentLoaded', refreshIcons)");
    assertThat(js).as("공개 API showModal 이 보존되어야 한다.").contains("showModal,");
    assertThat(js).as("공개 API hideModal 이 보존되어야 한다.").contains("hideModal,");
    assertThat(js).as("공개 API getFrameworkModal 이 보존되어야 한다.").contains("getFrameworkModal");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 6. 구형 custom modal 구현 제거
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void notify_js에서_구현_custom_modal_overlay와_직접_ESC_listener가_제거됨() throws IOException {
    // given
    String js = read(RESOURCES.resolve("static/js/common/notify.js"));

    // when / then
    assertThat(js).as("구형 custom-modal-overlay 구현이 제거되어야 한다.").doesNotContain("custom-modal-overlay");
    assertThat(js).as("직접 ESC keydown listener 가 제거되어야 한다 (SweetAlert2 내장).").doesNotContain("addEventListener('keydown'");
    assertThat(js).as("버튼 cloneNode 로직이 제거되어야 한다.").doesNotContain("cloneNode");
    assertThat(js).as("native window.confirm 폴백이 금지된다 (fail-closed 원칙).").doesNotContain("window.confirm");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 7. 고정 customClass 이름
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void notify_js가_고정_customClass_이름을_사용함() throws IOException {
    // given
    String js = read(RESOURCES.resolve("static/js/common/notify.js"));

    // when / then
    assertThat(js).as("customClass container 이름이 고정되어야 한다.").contains("sms-swal-container");
    assertThat(js).as("customClass popup 이름이 고정되어야 한다.").contains("sms-swal-popup");
    assertThat(js).as("customClass title 이름이 고정되어야 한다.").contains("sms-swal-title");
    assertThat(js).as("customClass message 이름이 고정되어야 한다.").contains("sms-swal-message");
    assertThat(js).as("customClass actions 이름이 고정되어야 한다.").contains("sms-swal-actions");
    assertThat(js).as("customClass confirm 버튼 이름이 고정되어야 한다.").contains("sms-swal-confirm");
    assertThat(js).as("customClass cancel 버튼 이름이 고정되어야 한다.").contains("sms-swal-cancel");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 8. text 옵션 렌더링 (XSS 방지)
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void notify_js가_text_옵션으로_메시지를_렌더링함() throws IOException {
    // given
    String js = read(RESOURCES.resolve("static/js/common/notify.js"));

    // when / then
    assertThat(js).as("메시지는 text 옵션으로 렌더링해야 한다 (HTML 이스케이프).").contains("text: msg");
    assertThat(js).as("html: 옵션 사용 금지 — XSS 방지 (SweetAlert2 text 는 자동 이스케이프).").doesNotContain("html:");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 9. bridge CSS sms-swal 클래스 및 토큰
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void bridge_CSS에_sms_swal_클래스와_필수_토큰이_정의됨() throws IOException {
    // given
    String css = read(RESOURCES.resolve("static/css/admin-ui-bridge.css"));

    // when / then
    assertThat(css).as("bridge CSS 에 .sms-swal-container 클래스가 정의되어야 한다.").contains(".sms-swal-container");
    assertThat(css).as("bridge CSS 에 .sms-swal-popup 클래스가 정의되어야 한다.").contains(".sms-swal-popup");
    assertThat(css).as("bridge CSS 에 .sms-swal-confirm 클래스가 정의되어야 한다.").contains(".sms-swal-confirm");
    assertThat(css).as("bridge CSS 에 .sms-swal-cancel 클래스가 정의되어야 한다.").contains(".sms-swal-cancel");
    assertThat(css).as("overlay z-index 는 --sms-z-overlay 토큰을 사용해야 한다.").contains("var(--sms-z-overlay)");
    assertThat(css).as("confirm 버튼은 --sms-primary 토큰을 사용해야 한다.").contains("var(--sms-primary)");
    assertThat(css).as("popup 배경은 --sms-bg-surface 토큰을 사용해야 한다.").contains("var(--sms-bg-surface)");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 10. common-utils.js getter 보존
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void common_utils_getter가_보존됨() throws IOException {
    // given
    String js = read(RESOURCES.resolve("static/js/common/common-utils.js"));

    // when / then
    assertThat(js).as("CommonUtils.confirm getter 가 보존되어야 한다.").contains("get confirm()");
    assertThat(js)
        .as("CommonUtils.confirm 은 window.Notify.confirm 으로 위임해야 한다.")
        .contains("window.Notify ? window.Notify.confirm");
  }

  // ─────────────────────────────────────────────────────────────────────────

  private static String read(Path path) throws IOException {
    return Files.readString(path);
  }
}
