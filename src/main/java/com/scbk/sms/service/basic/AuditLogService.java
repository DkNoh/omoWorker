package com.scbk.sms.service.basic;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.basic.AuditLogSearchRequestDTO;
import com.scbk.sms.mapper.basic.AuditLogMapper;
import com.scbk.sms.vo.basic.AuditLogVO;
import com.scbk.sms.util.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper mapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<AuditLogVO> search(AuditLogSearchRequestDTO request) {
        request.validate();
        int totalCount = mapper.count(request);
        List<AuditLogVO> list = mapper.selectList(request);
        return PageResponseDTO.of(list, request, totalCount);
    }

    @Transactional(readOnly = true)
    public void downloadExcel(AuditLogSearchRequestDTO request, HttpServletResponse response) {
        String[] headers = {"NOTICE_ID", "TITLE", "NOTICE_TYPE", "USE_YN", "VIEW_CNT", "REG_ID", "REG_DTTM", "UPD_ID", "UPD_DTTM"};
        String[] keys = {"NOTICE_ID", "TITLE", "NOTICE_TYPE", "USE_YN", "VIEW_CNT", "REG_ID", "REG_DTTM", "UPD_ID", "UPD_DTTM"};
        List<Map<String, Object>> list = mapper.selectListForExcel(request);
        ExcelUtil.downloadExcel(response, "AuditLog_export", headers, list, keys);
    }
}
