package com.scbk.sms.service.sms;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.dto.sms.SmsHistoryUpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.sms.SmsHistoryMapper;
import com.scbk.sms.util.ExcelUtil;
import com.scbk.sms.util.MaskingUtil;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송이력 서비스.
 *
 * <p>정책(docs/base/audit-masking-policy.md, domain-rules.md):
 *
 * <ul>
 *   <li>목록/엑셀은 수신번호(receiverNo)를 마스킹해 표시한다.
 *   <li>원문 조회는 /unmask(CAN_MASK_VIEW)로만 분리하며 감사 로그(@PrivacyLog) 대상이다.
 *   <li>발신번호(senderNo)는 사내 발신 번호이므로 마스킹하지 않는다.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SmsHistoryService {

  /** 엑셀 다운로드 최대 허용 건수(매퍼 FETCH NEXT와 일치). 초과 시 요청을 거부한다. */
  static final int EXCEL_ROW_LIMIT = 50_000;

  private final SmsHistoryMapper mapper;

  @Transactional(readOnly = true)
  public PageResponseDTO<SmsHistoryVO> search(SmsHistorySearchRequestDTO request) {
    request.validate();
    int totalCount = mapper.count(request);
    List<SmsHistoryVO> list = mapper.selectList(request);
    list.forEach(this::applyListMasking);
    return PageResponseDTO.of(list, request, totalCount);
  }

  /** 엑셀 다운로드. 목록과 동일 검색조건, 수신번호 마스킹 적용. */
  @Transactional(readOnly = true)
  public void downloadExcel(HttpServletResponse response, SmsHistorySearchRequestDTO request) {
    request.validate();
    List<SmsHistoryVO> rows = mapper.selectListForExcel(request);
    if (rows.size() >= EXCEL_ROW_LIMIT) {
      // FETCH NEXT EXCEL_ROW_LIMIT 으로 잘렸다는 것은 실제 조건 건수가 상한을 넘었다는 뜻
      throw new CustomException(ErrorCode.EXCEL_ROW_LIMIT_EXCEEDED);
    }
    List<Map<String, Object>> data = rows.stream().map(this::toMaskedRow).toList();
    String[] headers = {"발송ID", "요청ID", "발송일시", "수신번호", "발신번호", "유형", "상태", "결과코드", "결과메시지"};
    String[] keys = {
      "smsHistoryId",
      "requestId",
      "sentAt",
      "receiverNo",
      "senderNo",
      "sendType",
      "sendStatus",
      "resultCd",
      "resultMsg"
    };
    ExcelUtil.downloadExcel(response, "발송이력", headers, data, keys);
  }

  /** 원문 상세. CAN_MASK_VIEW 권한이 있는 사용자만 호출 가능(컨트롤러 /unmask). 마스킹하지 않는다. */
  @Transactional(readOnly = true)
  public SmsHistoryVO getUnmaskedDetail(Integer smsHistoryId) {
    SmsHistoryVO vo = mapper.selectDetail(smsHistoryId);
    if (vo == null) {
      throw new CustomException(ErrorCode.DATA_NOT_FOUND);
    }
    return vo;
  }

  @Transactional
  public void create(SmsHistoryUpdateRequestDTO request) {
    mapper.insert(request);
  }

  @Transactional
  public void update(SmsHistoryUpdateRequestDTO request) {
    int updated = mapper.update(request);
    if (updated == 0) {
      // 다른 사용자가 먼저 수정했거나(낙관적 잠금) 대상이 없다
      throw new CustomException(ErrorCode.UPDATE_CONFLICT);
    }
  }

  @Transactional
  public void delete(Integer smsHistoryId) {
    int deleted = mapper.delete(smsHistoryId);
    if (deleted == 0) {
      // 다른 사용자가 먼저 삭제했거나 대상이 없다
      throw new CustomException(ErrorCode.DELETE_CONFLICT);
    }
  }

  /** 목록/엑셀 표시용 마스킹. 수신번호만 적용(정책). */
  private void applyListMasking(SmsHistoryVO vo) {
    vo.setReceiverNo(MaskingUtil.maskPhone(vo.getReceiverNo()));
  }

  private Map<String, Object> toMaskedRow(SmsHistoryVO vo) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("smsHistoryId", vo.getSmsHistoryId());
    row.put("requestId", vo.getRequestId());
    row.put("sentAt", vo.getSentAt());
    row.put("receiverNo", MaskingUtil.maskPhone(vo.getReceiverNo()));
    row.put("senderNo", vo.getSenderNo());
    row.put("sendType", vo.getSendType());
    row.put("sendStatus", vo.getSendStatus());
    row.put("resultCd", vo.getResultCd());
    row.put("resultMsg", vo.getResultMsg());
    return row;
  }
}
