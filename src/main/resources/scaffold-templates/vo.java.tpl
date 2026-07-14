package com.scbk.sms.vo.[( ${model.moduleName()} )];

[# th:if="${model.typeMap().containsValue('BigDecimal')}"]import java.math.BigDecimal;
[/][# th:if="${model.typeMap().containsValue('LocalDate')}"]import java.time.LocalDate;
[/][# th:if="${model.typeMap().containsValue('LocalDateTime')}"]import java.time.LocalDateTime;
[/]import lombok.Data;

[# th:if="${model.includePrivacy()}"]// 개인정보 컬럼은 Service에서 MaskingUtil로 마스킹한 값을 담는다.
[/]/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Data
public class [( ${model.domainClass()} )]VO {

[# th:each="field : ${model.voFields()}"]    private [( ${field.javaType()} )] [( ${field.fieldName()} )];
[/]}
