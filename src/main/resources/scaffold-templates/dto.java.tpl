package com.scbk.sms.dto.[( ${model.moduleName()} )];

import com.scbk.sms.dto.common.PageRequestDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Data
@EqualsAndHashCode(callSuper = true)
public class [( ${model.domainClass()} )]SearchRequestDTO extends PageRequestDTO {

[# th:each="searchParam : ${model.searchParams()}"]    private String [( ${searchParam.name()} )];
[/]}
