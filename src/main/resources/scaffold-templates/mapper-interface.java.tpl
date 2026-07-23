package com.scbk.sms.mapper.[( ${model.moduleName()} )];

import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
[# th:if="${model.includeCreateUpdate()}"]import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]UpdateRequestDTO;
[/]import com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO;
import java.util.List;
[# th:if="${model.includeExcel()}"]import java.util.Map;
[/][# th:each="pkImport : ${model.pkParamImports()}"]import [( ${pkImport} )];
[/]import org.apache.ibatis.annotations.Mapper;
[# th:if="${model.includeCreateUpdate() or model.includePrivacy()}"]import org.apache.ibatis.annotations.Param;
[/]
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface [( ${model.domainClass()} )]Mapper {

    int count([( ${model.domainClass()} )]SearchRequestDTO request);

    List<[( ${model.domainClass()} )]VO> selectList([( ${model.domainClass()} )]SearchRequestDTO request);
[# th:if="${model.includePrivacy()}"]
    [( ${model.domainClass()} )]VO selectDetail(@Param("[( ${model.pkFieldName()} )]") [( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]);
[/][# th:if="${model.includeCreateUpdate()}"]
    int insert([( ${model.domainClass()} )]UpdateRequestDTO request);

    int update([( ${model.domainClass()} )]UpdateRequestDTO request);

    int delete([# th:each="pk, iter : ${model.pkFields()}"]@Param("[( ${pk.fieldName()} )]") [( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
[/][# th:if="${model.includeExcel()}"]
    // ExcelUtil 계약상 Map을 사용한다 (동적 컬럼 예외)
    List<Map<String, Object>> selectListForExcel([( ${model.domainClass()} )]SearchRequestDTO request);
[/]}
