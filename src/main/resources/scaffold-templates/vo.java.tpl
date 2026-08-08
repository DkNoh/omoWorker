package com.scbk.sms.vo.[( ${model.moduleName()} )];

[# th:if="${model.typeMap().containsValue('BigDecimal')}"]import java.math.BigDecimal;
[/][# th:if="${model.typeMap().containsValue('LocalDate')}"]import java.time.LocalDate;
[/][# th:if="${model.typeMap().containsValue('LocalDateTime')}"]import java.time.LocalDateTime;
[/]import lombok.Data;

/**
 * SELECT 결과를 MyBatis map-underscore-to-camel-case 규칙으로 수신하는 조회 모델.
 *
 * <p>필드 타입은 JDBC ResultSetMetaData로 추론한다.[# th:if="${model.includePrivacy()}"] 개인정보 필드는 Service에서
 * MaskingUtil을 적용한 뒤 화면으로 전달한다.[/] 생성 후에는 개발자가 직접 수정해 소유한다.
 */
@Data
public class [( ${model.domainClass()} )]VO {

[# th:each="field : ${model.voFields()}"]    /** SELECT 결과 컬럼에서 생성된 조회 필드. */
    private [( ${field.javaType()} )] [( ${field.fieldName()} )];
[/]}
