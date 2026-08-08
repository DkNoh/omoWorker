package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * fragments/modal-base.html 프래그먼트의 렌더링 계약을 고정한다.
 *
 * <p>배경: Thymeleaf 3.1+는 deprecated된 unwrap 프래그먼트 표현식 {@code th:replace="${...}"}를 더 이상 관대하게 처리하지
 * 않는다. {@code footerContent=null} 호출(스캐폴드 CRUD 생성 화면과 동일)에서 같은 요소의 {@code th:if="${footerContent !=
 * null}"} 가 {@code th:replace="${footerContent}"}의 fragment 해석을 막지 못해 "template or fragment could
 * not be resolved" 오류가 났다 (2026-07-20 customer-search 화면).
 *
 * <p>수정: footerContent 분기를 nesting 한다 — {@code th:if}를 부모 {@code <th:block>}에 두고 {@code
 * th:replace}는 자식 요소로 옮겨, {@code footerContent == null}이면 th:replace 요소 자체가 렌더 트리에 존재하지 않게 했다.
 * bodyContent(항상 non-null fragment expression)는 기존 {@code th:replace="${bodyContent}"} 구문을 유지한다.
 *
 * <p>이 테스트는 두 호출 계약(footerContent=null, footerContent=~{::#modal-footer})이 모두 예외 없이 렌더링되는지 검증한다.
 *
 * <p>주의: modal-base.html은 SpEL Elvis 연산자({@code ${size ?: 'modal-md'}})를 쓰므로 production과 동일하게
 * {@link SpringTemplateEngine}(SpringStandardDialect → SpEL)로 렌더링해야 한다. 순수 {@link
 * org.thymeleaf.TemplateEngine}의 OGNL은 Elvis를 파싱하지 못한다.
 */
class ModalBaseFragmentTest {

  private SpringTemplateEngine newEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode("HTML");
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }

  @Test
  void footerContent가_null이면_기본_footer가_예외없이_렌더링된다() {
    // given : scaffold CRUD page.html.tpl 이 생성하는 호출과 동일 (footerContent=null)
    SpringTemplateEngine engine = newEngine();

    // when : 이전에는 TemplateInputException("could not be resolved") 발생
    String result = engine.process("modal-base-null-footer-test", new Context());

    // then : 모달 식별자, 본문, 기본 footer(삭제/닫기/저장)가 모두 렌더링된다
    assertThat(result).contains("null-footer-modal");
    assertThat(result).contains("본문");
    assertThat(result).contains("id=\"null-footer-modal-btn-delete\"");
    assertThat(result).contains("id=\"null-footer-modal-btn-save\"");
    assertThat(result).contains("닫기");
  }

  @Test
  void footerContent가_제공되면_커스텀_footer가_기본_footer를_대체한다() {
    // given : basic/notice.html 이 사용하는 호출과 동일 (footerContent=~{::#modal-footer})
    SpringTemplateEngine engine = newEngine();

    // when
    String result = engine.process("modal-base-custom-footer-test", new Context());

    // then : 커스텀 footer가 렌더링되고, 기본 footer의 저장/삭제 버튼은 노출되지 않는다
    assertThat(result).contains("custom-footer-modal");
    assertThat(result).contains("id=\"custom-btn\"");
    assertThat(result).doesNotContain("custom-footer-modal-btn-save");
    assertThat(result).doesNotContain("custom-footer-modal-btn-delete");
  }
}
