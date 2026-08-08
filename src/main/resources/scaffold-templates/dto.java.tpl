package com.scbk.sms.dto.[( ${model.moduleName()} )];

import com.scbk.sms.dto.common.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 목록 조회용 검색조건과 공통 page/size를 전달하는 요청 DTO.
 *
 * <p>필드는 QuerySpec의 {@code $검색변수}에서 생성된다. 업무별 형식·범위 검증이 필요하면 이 클래스에 Bean Validation을 추가한다.
 * 생성 후에는 개발자가 직접 수정해 소유한다.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class [( ${model.domainClass()} )]SearchRequestDTO extends PageRequestDTO {

[# th:each="searchParam : ${model.searchParams()}"]    /** QuerySpec 검색조건에서 생성된 요청값. */
    private String [( ${searchParam.name()} )];
[/]}
