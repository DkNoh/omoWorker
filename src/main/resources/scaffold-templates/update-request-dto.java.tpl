package com.scbk.sms.dto.[( ${model.moduleName()} )];

[# th:if="${model.hasType('BigDecimal')}"]import java.math.BigDecimal;
[/][# th:if="${model.hasType('LocalDate')}"]import java.time.LocalDate;
[/][# th:if="${model.hasType('LocalDateTime')}"]import java.time.LocalDateTime;
[/]import lombok.Data;
[# th:if="${model.hasEditableRequiredNotBlank()}"]import jakarta.validation.constraints.NotBlank;
[/][# th:if="${model.hasEditableRequiredNotNull()}"]import jakarta.validation.constraints.NotNull;
[/]
/**
 * 등록·수정 API가 허용하는 입력 필드만 선언한 화이트리스트 DTO.
 *
 * <p>Scaffold가 실제 PK, nullable 메타데이터와 컬럼 옵션을 기준으로 최초 골격을 만든다. 생성 후 실제 수정을 허용할 필드만 남기고,
 * REG_ID/REG_DTTM 같은 감사 필드·시스템 상태·권한 필드는 클라이언트 입력으로 추가하지 않는다.
 */
@Data
public class [( ${model.domainClass()} )]UpdateRequestDTO {

[# th:if="${model.pkColumns().isEmpty()}"]    // TODO: PK 필드 (WHERE 조건). 실제 PK 컬럼명으로 교체한다
    private String id;

[/][# th:if="${!model.pkColumns().isEmpty()}"]    /** PK 필드 (WHERE 조건): [( ${#strings.listJoin(model.pkColumns(), ', ')} )] */
[# th:each="pk : ${model.pkFields()}"]    private [( ${pk.javaType()} )] [( ${pk.fieldName()} )];
[/]
[/][# th:each="column : ${model.editableColumns()}"]    /** 화면과 Mapper가 함께 사용하는 수정 허용 필드. */
[# th:if="${column.requiresNotBlank()}"]    @NotBlank
[/][# th:if="${column.requiresNotNull()}"]    @NotNull
[/]    private [( ${column.javaType()} )] [( ${column.fieldName()} )];
[/][# th:if="${!model.lockColumn().isEmpty()}"]
    /** 낙관적 잠금용. 조회 시점의 [( ${model.lockColumn()} )] (hidden으로 받는다) */
    private [( ${model.lockJavaType()} )] [( ${model.beforeLockFieldName()} )];
[/]}
