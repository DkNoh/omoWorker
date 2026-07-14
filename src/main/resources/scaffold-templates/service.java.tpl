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
[/][# th:if="${model.includePrivacy()}"]import com.scbk.sms.util.MaskingUtil;
[/]import java.util.List;
[# th:if="${model.includeExcel()}"]import java.util.Map;
[/][# th:each="pkImport : ${model.pkParamImports()}"]import [( ${pkImport} )];
[/]import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다.
 */
@Service
@RequiredArgsConstructor
public class [( ${model.domainClass()} )]Service {

    private final [( ${model.domainClass()} )]Mapper mapper;

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
    @Transactional
    public void create([( ${model.domainClass()} )]UpdateRequestDTO request) {
        // TODO: 등록 전 업무 규칙 검증(중복 체크, 필수값 보정 등)을 여기에 추가한다.
        mapper.insert(request);
    }

    @Transactional
    public void update([( ${model.domainClass()} )]UpdateRequestDTO request) {
        // TODO: 수정 전 업무 규칙 검증(상태 전이, 권한 확인 등)을 여기에 추가한다.
        int updated = mapper.update(request);
        if (updated == 0) {
            // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다
            throw new CustomException(ErrorCode.UPDATE_CONFLICT);
        }
    }

    @Transactional
    public void delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.javaType()} )] [( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]) {
        int deleted = mapper.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${pk.fieldName()} )][# th:if="${!iter.last}"], [/][/]);
        if (deleted == 0) {
            // 다른 사용자가 먼저 삭제했거나 대상이 없다
            throw new CustomException(ErrorCode.DELETE_CONFLICT);
        }
    }
[/][# th:if="${model.includeExcel()}"]
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
    @Transactional(readOnly = true)
    public [( ${model.domainClass()} )]VO getUnmaskedDetail([( ${model.pkJavaType()} )] [( ${model.pkFieldName()} )]) {
        [( ${model.domainClass()} )]VO vo = mapper.selectDetail([( ${model.pkFieldName()} )]);
        if (vo == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        return vo;
    }
[/]}
