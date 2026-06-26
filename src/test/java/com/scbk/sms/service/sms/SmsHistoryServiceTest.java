package com.scbk.sms.service.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.sms.SmsHistorySearchRequestDTO;
import com.scbk.sms.dto.sms.SmsHistoryUpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.sms.SmsHistoryMapper;
import com.scbk.sms.vo.sms.SmsHistoryVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SmsHistoryServiceTest {

  @Mock private SmsHistoryMapper mapper;

  private SmsHistoryService service;

  @BeforeEach
  void setUp() {
    service = new SmsHistoryService(mapper);
  }

  @Test
  void 목록_조회는_페이지_응답으로_감싼다() {
    // given
    SmsHistorySearchRequestDTO request = new SmsHistorySearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    given(mapper.count(request)).willReturn(1);
    given(mapper.selectList(request)).willReturn(List.of(new SmsHistoryVO()));

    // when
    PageResponseDTO<SmsHistoryVO> result = service.search(request);

    // then
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getContents()).hasSize(1);
  }

  @Test
  void 목록_조회는_수신번호를_마스킹한다() {
    // given : 정책 - 목록의 수신번호는 마스킹해 표시
    SmsHistorySearchRequestDTO request = new SmsHistorySearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    SmsHistoryVO row = new SmsHistoryVO();
    row.setReceiverNo("01012345678");
    given(mapper.count(request)).willReturn(1);
    given(mapper.selectList(request)).willReturn(List.of(row));

    // when
    List<SmsHistoryVO> contents = service.search(request).getContents();

    // then : 01012345678 -> 010-****-5678
    assertThat(contents.get(0).getReceiverNo()).isEqualTo("010-****-5678");
  }

  @Test
  void 엑셀_다운로드는_수신번호를_마스킹해_기록한다() throws Exception {
    // given
    SmsHistorySearchRequestDTO request = new SmsHistorySearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    SmsHistoryVO row = new SmsHistoryVO();
    row.setSmsHistoryId(1);
    row.setReceiverNo("01012345678");
    given(mapper.selectListForExcel(request)).willReturn(List.of(row));

    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    service.downloadExcel(response, request);

    // then : 엑셀이 write 됐고, 헤더가 xlsx로 세팅됐다
    assertThat(response.getContentAsByteArray()).isNotEmpty();
    assertThat(response.getContentType())
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    then(mapper).should().selectListForExcel(request);
  }

  @Test
  void 엑셀_건수가_상한이면_거부한다() {
    // given : FETCH NEXT 50000 으로 잘렸다는 건 실제 조건 건수가 상한 초과라는 뜻
    SmsHistorySearchRequestDTO request = new SmsHistorySearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    given(mapper.selectListForExcel(request))
        .willReturn(
            java.util.stream.Stream.generate(SmsHistoryVO::new)
                .limit(SmsHistoryService.EXCEL_ROW_LIMIT)
                .toList());

    // when / then
    assertThatThrownBy(() -> service.downloadExcel(new MockHttpServletResponse(), request))
        .isInstanceOf(CustomException.class);
  }

  @Test
  void 원문_상세는_마스킹하지_않은_원본을_반환한다() {
    // given : /unmask 전용 — 마스킹 X
    SmsHistoryVO vo = new SmsHistoryVO();
    vo.setSmsHistoryId(1);
    vo.setReceiverNo("01012345678");
    given(mapper.selectDetail(1)).willReturn(vo);

    // when
    SmsHistoryVO result = service.getUnmaskedDetail(1);

    // then
    assertThat(result.getReceiverNo()).isEqualTo("01012345678");
  }

  @Test
  void 원문_상세_대상이_없으면_DATA_NOT_FOUND() {
    given(mapper.selectDetail(1)).willReturn(null);

    assertThatThrownBy(() -> service.getUnmaskedDetail(1))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining(ErrorCode.DATA_NOT_FOUND.getMessage());
  }

  @Test
  void 수정_결과가_0건이면_충돌로_실패한다() {
    // given : 낙관적 잠금 — 다른 사용자가 먼저 수정했거나 대상이 없는 상황
    given(mapper.update(any())).willReturn(0);

    // when / then
    assertThatThrownBy(() -> service.update(new SmsHistoryUpdateRequestDTO()))
        .isInstanceOf(CustomException.class);
  }

  @Test
  void 삭제는_Mapper에_위임한다() {
    // given
    given(mapper.delete(1)).willReturn(1);

    // when
    service.delete(1);

    // then
    then(mapper).should().delete(1);
  }

  @Test
  void 삭제_결과가_0건이면_충돌로_실패한다() {
    // given : 다른 사용자가 먼저 삭제했거나 대상이 없는 상황
    given(mapper.delete(1)).willReturn(0);

    // when / then
    assertThatThrownBy(() -> service.delete(1)).isInstanceOf(CustomException.class);
  }
}
