package com.scbk.sms.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 붙은 메서드는 실행 시 AOP가 자동으로 SMS.TB_PRIVACY_AUDIT_LOG에 감사 이력을 기록한다.
 *
 * <p>적용 대상 기준은 docs/base/audit-masking-policy.md를 따른다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrivacyLog {

  /** 수행 업무명. 메뉴명 + 행위 형태로 작성한다. (예: "발송이력 엑셀 다운로드") */
  String action() default "민감 정보 조회";
}
