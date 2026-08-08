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
/**
 * [( ${model.domainClass()} )] 업무의 MyBatis Mapper 계약.
 *
 * <p>메서드명과 Mapper XML statement id는 반드시 함께 변경한다. 쓰기 메서드는 UpdateRequestDTO의 화이트리스트와 실제 PK만 사용한다.
 * 생성 후에는 개발자가 직접 수정해 소유한다.
 */
@Mapper
public interface [( ${model.domainClass()} )]Mapper {

    /** 현재 검색조건에 맞는 전체 건수를 반환한다. */
    int count([( ${model.domainClass()} )]SearchRequestDTO request);

    /** 현재 페이지에 표시할 목록을 결정적 정렬 순서로 조회한다. */
    List<[( ${model.domainClass()} )]VO> selectList([( ${model.domainClass()} )]SearchRequestDTO request);
[# th:if="${model.includePrivacy()}"]
    /** 감사 대상 원문 상세 조회용 단건 쿼리. */
    [( ${model.domainClass()} )]VO selectDetail(@Param("[( ${model.pkFieldName()} )]") [( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]);
[/][# th:if="${model.includeCreateUpdate()}"]
    /** 등록 허용 필드만 INSERT하고 영향받은 행 수를 반환한다. */
    int insert([( ${model.domainClass()} )]UpdateRequestDTO request);

    /** 실제 PK와 선택적 잠금값으로 UPDATE하고 영향받은 행 수를 반환한다. */
    int update([( ${model.domainClass()} )]UpdateRequestDTO request);

    /** 실제 PK로 한 건을 DELETE하고 영향받은 행 수를 반환한다. */
    int delete([# th:each="pk, iter : ${model.pkFields()}"]@Param("[( ${pk.fieldName()} )]") [( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
[/][# th:if="${model.includeExcel()}"]
    /** ExcelUtil의 동적 컬럼 계약에 맞춰 전체 결과를 Map 목록으로 조회한다. */
    List<Map<String, Object>> selectListForExcel([( ${model.domainClass()} )]SearchRequestDTO request);
[/]}
