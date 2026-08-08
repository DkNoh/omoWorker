package com.scbk.sms.service.[( ${model.moduleName()} )];

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
[# th:if="${model.includeCreateUpdate()}"]import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]UpdateRequestDTO;
[/][# th:if="${model.includeCreateUpdate() or model.includePrivacy()}"]import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
[/]import com.scbk.sms.mapper.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Mapper;
import com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO;
[# th:if="${model.includeExcel()}"]import com.scbk.sms.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
[/][# th:if="${!model.maskedColumns().isEmpty()}"]import com.scbk.sms.util.MaskingUtil;
[/]import java.util.List;
[# th:if="${model.includeExcel()}"]import java.util.Map;
[/][# th:each="pkImport : ${model.pkParamImports()}"]import [( ${pkImport} )];
[/]import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [( ${model.domainClass()} )] 업무 규칙과 트랜잭션 경계를 담당하는 Service.
 *
 * <p>Controller는 HTTP 변환만, Mapper는 SQL 실행만 담당한다. 중복 검사, 상태 전이, 외부 연계 같은 업무 규칙은 이 클래스에 추가한다.
 * Scaffold 최초 생성 후에는 개발자가 직접 수정해 소유한다.
 */
@Service
@RequiredArgsConstructor
public class [( ${model.domainClass()} )]Service {

    private final [( ${model.domainClass()} )]Mapper mapper;

    /** 검색조건 검증 후 건수와 현재 페이지 목록을 함께 조회한다. */
    @Transactional(readOnly = true)
    public PageResponseDTO<[( ${model.domainClass()} )]VO> search([( ${model.domainClass()} )]SearchRequestDTO request) {
        request.validate();
        int totalCount = mapper.count(request);
        List<[( ${model.domainClass()} )]VO> list = mapper.selectList(request);
[# th:if="${!model.maskedColumns().isEmpty()}"]        list.forEach(vo -> {
[# th:each="column : ${model.maskedColumns()}"]            vo.set[( ${model.capitalize(column.fieldName())} )](MaskingUtil.[( ${model.maskingMethodName(column.maskType())} )](vo.get[( ${model.capitalize(column.fieldName())} )]()));
[/]        });
[/]        return PageResponseDTO.of(list, request, totalCount);
    }
[# th:if="${model.includeCreateUpdate()}"]
    /** 등록 전 업무 규칙을 검증하고 허용된 필드만 저장한다. */
    @Transactional
    public void create([( ${model.domainClass()} )]UpdateRequestDTO request) {
        // TODO: 등록 전 업무 규칙 검증(중복 체크, 필수값 보정 등)을 여기에 추가한다.
        mapper.insert(request);
    }

    /** 수정 충돌을 0건 결과로 감지해 성공으로 오인하지 않도록 처리한다. */
    @Transactional
    public void update([( ${model.domainClass()} )]UpdateRequestDTO request) {
        // TODO: 수정 전 업무 규칙 검증(상태 전이, 권한 확인 등)을 여기에 추가한다.
        int updated = mapper.update(request);
        if (updated == 0) {
            // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다
            throw new CustomException(ErrorCode.UPDATE_CONFLICT);
        }
    }

    /** 실제 PK로 삭제하고 대상 부재 또는 선행 삭제를 충돌로 보고한다. */
    @Transactional
    public void delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]) {
        int deleted = mapper.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
        if (deleted == 0) {
            // 다른 사용자가 먼저 삭제했거나 대상이 없다
            throw new CustomException(ErrorCode.DELETE_CONFLICT);
        }
    }
[/][# th:if="${model.includeExcel()}"]
    /** 현재 검색조건의 전체 결과에 목록과 동일한 마스킹 규칙을 적용해 엑셀로 응답한다. */
    @Transactional(readOnly = true)
    public void downloadExcel([( ${model.domainClass()} )]SearchRequestDTO request, HttpServletResponse response) {
        String[] headers = {[# th:each="column, iter : ${model.getColumns()}"]"[( ${column} )]"[# th:if="${!iter.last}"], [/][/]};
        String[] keys = {[# th:each="column, iter : ${model.getColumns()}"]"[( ${column} )]"[# th:if="${!iter.last}"], [/][/]};
        List<Map<String, Object>> list = mapper.selectListForExcel(request);
[# th:each="column : ${model.maskedColumns()}"]        for (Map<String, Object> row : list) {
            Object value = row.get("[( ${column.columnName()} )]");
            if (value != null) {
                row.put("[( ${column.columnName()} )]", MaskingUtil.[( ${model.maskingMethodName(column.maskType())} )](value.toString()));
            }
        }
[/]        ExcelUtil.downloadExcel(response, "[( ${model.domainClass()} )]_export", headers, list, keys);
    }
[/][# th:if="${model.includePrivacy()}"]
    /** 감사로그가 적용된 Controller에서 호출하는 마스킹 전 원문 단건 조회다. */
    @Transactional(readOnly = true)
    public [( ${model.domainClass()} )]VO getUnmaskedDetail([( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]) {
        [( ${model.domainClass()} )]VO vo = mapper.selectDetail([( ${model.pkFieldName()} )]);
        if (vo == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        return vo;
    }
[/]}
