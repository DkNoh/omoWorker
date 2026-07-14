package com.scbk.sms.service.system.scaffold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Phase 1 Step 1: 새 Thymeleaf 기반 ResourceTemplateRenderer의 계약 테스트.
 *
 * <p>텍스트 모드(TEXT mode)에서 동작해야 한다. HTML/JS/XML/SQL 산물이 섞여 있으므로, 런타임 Thymeleaf
 * 표현식(${pageAuth.create})과 JS 템플릿 리터럴, MyBatis 플레이스홀더는 scaffold 렌더링 시 리터럴로 보존되어야 한다.
 *
 * <p>모든 프로덕션 scaffold 템플릿이 Thymeleaf로 전환 완료되었다. 따라서 Thymeleaf 평가 후에도 남은 {@code @@TOKEN@@}은 마이그레이션
 * 누락이며, 렌더러는 즉시 {@link IllegalStateException}으로 fail-fast한다.
 */
class ResourceTemplateRendererTest {

  @Test
  void 클래스패스의_tpl을_UTF8로_읽어_인라인_표현식을_치환한다() {
    // when
    String result =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/simple.tpl", Map.of("name", "공지사항"));

    // then
    assertThat(result).contains("Hello 공지사항!");
  }

  @Test
  void th_each로_컬렉션을_루프한다() {
    // when
    String result =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/loop.tpl", Map.of("items", List.of("A", "B", "C")));

    // then
    assertThat(result).contains("- A").contains("- B").contains("- C");
  }

  @Test
  void iter_상태로_구분자를_제어한다() {
    // when
    String result =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/iter-status.tpl",
            Map.of(
                "columns",
                List.of(
                    Map.of("name", "title"),
                    Map.of("name", "noticeType"),
                    Map.of("name", "useYn"))));

    // then: 첫 항목 앞에는 구분자가 없고 이후 항목 앞에만 ", "가 붙는다
    assertThat(result.trim()).isEqualTo("title, noticeType, useYn");
  }

  @Test
  void th_if와_th_unless로_조건부_출력한다() {
    // when
    String whenTrue =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/condition.tpl", Map.of("show", true));
    String whenFalse =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/condition.tpl", Map.of("show", false));

    // then
    assertThat(whenTrue).contains("YES").doesNotContain("NO");
    assertThat(whenFalse).contains("NO").doesNotContain("YES");
  }

  @Test
  void th_text는_HTML_이스케이프하고_th_utext는_raw_출력한다() {
    // when
    String result =
        ResourceTemplateRenderer.render(
            "scaffold-templates-test/escape.tpl",
            Map.of("raw", "<b>bold</b>", "html", "<b>bold</b>"));

    // then
    assertThat(result).contains("Value: <b>bold</b>");
    assertThat(result).contains("Escaped: &lt;b&gt;bold&lt;/b&gt;");
  }

  @Test
  void 생성된_화면의_런타임_Thymeleaf_표현식은_리터럴로_보존한다() {
    // when
    String result =
        ResourceTemplateRenderer.render("scaffold-templates-test/literal.tpl", Map.of());

    // then: 속성 밖 ${...}는 Thymeleaf가 자동 inline 대상으로 보지 않으므로 그대로 남는다
    assertThat(result).contains("th:if=\"${pageAuth.create}\"");
  }

  @Test
  void JS_템플릿_리터럴과_MyBatis_플레이스홀더를_보존한다() {
    // when
    String result =
        ResourceTemplateRenderer.render("scaffold-templates-test/literal.tpl", Map.of());

    // then: JS template literal ${field}와 MyBatis #{field,...}는 그대로 출력되어야 한다
    assertThat(result).contains("`[name=\"${field}\"]`");
    assertThat(result).contains("#{field,jdbcType=VARCHAR}");
  }

  @Test
  void 레거시_TOKEN이_남아있으면_예외로_실패한다() {
    // given: 모든 프로덕션 템플릿이 Thymeleaf로 전환 완료. @@TOKEN@@ 잔류는 마이그레이션 누락이므로 fail-fast해야 한다.
    // expect: 어떤 템플릿/토큰인지 바로 알 수 있도록 메시지에 resourcePath와 첫 토큰을 포함한다
    assertThatThrownBy(
            () ->
                ResourceTemplateRenderer.render(
                    "scaffold-templates-test/legacy-token.tpl", Map.of("DOMAIN_NAME", "공지사항")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("scaffold-templates-test/legacy-token.tpl")
        .hasMessageContaining("@@DOMAIN_NAME@@");
  }

  @Test
  void 누락된_템플릿은_명확한_예외로_실패한다() {
    // expect
    assertThatThrownBy(
            () ->
                ResourceTemplateRenderer.render(
                    "scaffold-templates-test/does-not-exist.tpl", Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("scaffold-templates-test/does-not-exist.tpl");
  }

  @Test
  void null_variables_맵도_허용한다() {
    // when
    String result = ResourceTemplateRenderer.render("scaffold-templates-test/literal.tpl", null);

    // then
    assertThat(result).contains("th:if=\"${pageAuth.create}\"");
  }
}
