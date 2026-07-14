package com.scbk.sms.service.system.scaffold;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Scaffold 템플릿 렌더러.
 *
 * <p>{@link SpringTemplateEngine}을 직접 new로 만들어 쓴다. 웹 요청을 처리하는 Boot의 SpringTemplateEngine 빈과는 공유하지
 * 않고, layout-dialect도 등록하지 않는다. Scaffold는 local 전용 개발 도구이므로 캐시를 끄고 클래스패스 리소스를 직접 resolve한다.
 *
 * <p>SpringTemplateEngine을 고른 이유: standalone {@code TemplateEngine}이 기본으로 쓰는 OGNL은 JavaBean 규칙을 너무
 * 엄격하게 적용해 {@code record} accessor({@code param.name()})나 {@code getXxx()} 없는 메서드 호출을 거부한다.
 * SpringEL은 record accessor와 일반 메서드 호출을 모두 허용한다.
 *
 * <p>{@link TemplateMode#TEXT}를 사용한다. HTML/JS/XML/SQL 산물이 섞여 있고, 생성된 화면이 나중에 웹 컨테이너에서 다시 Thymeleaf로
 * 평가해야 하는 {@code th:if="${pageAuth.create}"} 같은 런타임 표현식, JS template literal, MyBatis placeholder를
 * scaffold 렌더링 시 리터럴로 보존해야 하기 때문이다. TEXT 모드에서는 {@code [# th:each="..."]} 같은 명시적 textual 태그만 평가하고
 * 나머지는 텍스트로 취급한다.
 *
 * <p><b>레거시 토큰 fail-fast</b>: 모든 프로덕션 scaffold 템플릿이 Thymeleaf로 전환 완료되었다. Thymeleaf 평가 후에도 남은
 * {@code @@TOKEN@@}은 마이그레이션 누락이므로, 렌더러는 템플릿 경로와 해당 토큰을 메시지에 담아 즉시 {@link IllegalStateException}으로
 * 실패시킨다. 토큰을 조용히 치환하는 fallback은 더 이상 제공하지 않는다.
 */
final class ResourceTemplateRenderer {

  /** Thymeleaf로 전환되지 않은 템플릿에 남은 {@code @@KEY@@} 토큰. 렌더 후 잔류하면 마이그레이션 누락으로 간주해 즉시 실패시킨다. */
  private static final Pattern LEGACY_TOKEN_PATTERN = Pattern.compile("@@[A-Z0-9_]+@@");

  private static final SpringTemplateEngine ENGINE = createEngine();

  private ResourceTemplateRenderer() {}

  private static SpringTemplateEngine createEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    // 빈 prefix/suffix: 호출부가 리소스의 full path를 넘긴다(예: "scaffold-templates/dto.java.tpl").
    resolver.setPrefix("");
    resolver.setSuffix("");
    resolver.setTemplateMode(TemplateMode.TEXT);
    resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
    // local 개발 중 템플릿 수정을 바로 반영하기 위해 캐시를 끈다.
    resolver.setCacheable(false);
    // 누락된 템플릿을 deterministic하게 실패시킨다.
    resolver.setCheckExistence(true);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    // SpringEL 컴파일러를 켜서 반복 표현식 평가를 빠르게 한다.
    engine.setEnableSpringELCompiler(true);
    return engine;
  }

  /**
   * Scaffold 템플릿을 Thymeleaf TEXT 모드로 렌더링한다.
   *
   * @param resourcePath 클래스패스 기준 템플릿 full path
   * @param variables 템플릿 컨텍스트 변수. {@code null}이면 빈 컨텍스트로 렌더링한다.
   * @return 렌더링 결과
   * @throws IllegalStateException 템플릿을 찾을 수 없거나, 평가에 실패하거나, 평가 후에도 {@code @@TOKEN@@}이 남은 경우
   */
  static String render(String resourcePath, Map<String, ?> variables) {
    Context context = new Context(Locale.ROOT);
    if (variables != null) {
      for (Map.Entry<String, ?> entry : variables.entrySet()) {
        context.setVariable(entry.getKey(), entry.getValue());
      }
    }

    String rendered = processThymeleaf(resourcePath, context);
    detectUnresolvedLegacyTokens(rendered, resourcePath);
    return rendered;
  }

  private static String processThymeleaf(String resourcePath, Context context) {
    try {
      return ENGINE.process(resourcePath, context);
    } catch (TemplateProcessingException e) {
      throw new IllegalStateException("Failed to render scaffold template: " + resourcePath, e);
    }
  }

  /**
   * Thymeleaf 평가 후에도 남은 {@code @@TOKEN@@}을 마이그레이션 누락으로 간주해 즉시 실패시킨다. 모든 프로덕션 scaffold 템플릿이
   * Thymeleaf로 전환되었으므로, 토큰이 남았다는 것은 템플릿을 고치지 않았음을 뜻한다. 어떤 템플릿/토큰인지 바로 알 수 있도록 메시지에 {@code
   * resourcePath}와 발견한 첫 토큰을 포함한다.
   */
  private static void detectUnresolvedLegacyTokens(String rendered, String resourcePath) {
    Matcher matcher = LEGACY_TOKEN_PATTERN.matcher(rendered);
    if (matcher.find()) {
      throw new IllegalStateException(
          "Unresolved legacy @@TOKEN@@ in scaffold template "
              + resourcePath
              + ": "
              + matcher.group()
              + ". Migrate the template to Thymeleaf expressions.");
    }
  }
}
