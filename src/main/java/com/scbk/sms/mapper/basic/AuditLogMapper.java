package com.scbk.sms.mapper.basic;

import com.scbk.sms.dto.basic.AuditLogSearchRequestDTO;
import com.scbk.sms.vo.basic.AuditLogVO;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface AuditLogMapper {

    int count(AuditLogSearchRequestDTO request);

    List<AuditLogVO> selectList(AuditLogSearchRequestDTO request);

    // ExcelUtil 계약상 Map을 사용한다 (동적 컬럼 예외)
    List<Map<String, Object>> selectListForExcel(AuditLogSearchRequestDTO request);
}
