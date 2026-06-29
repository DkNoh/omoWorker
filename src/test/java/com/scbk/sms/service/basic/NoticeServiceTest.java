package com.scbk.sms.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.mapper.basic.NoticeMapper;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class NoticeServiceTest {

  @Mock private NoticeMapper mapper;

  private NoticeService service;

  @BeforeEach
  void setUp() {
    service = new NoticeService(mapper);
  }

  @Test
  void 목록_조회는_페이지_응답으로_감싼다() {
    // given
    NoticeSearchRequestDTO request = new NoticeSearchRequestDTO();
    request.setPage(1);
    request.setSize(10);
    given(mapper.count(request)).willReturn(1);
    given(mapper.selectList(request)).willReturn(List.of(new NoticeVO()));

    // when
    PageResponseDTO<NoticeVO> result = service.search(request);

    // then
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getContents()).hasSize(1);
  }

  // TODO: 업무 규칙 테스트를 추가한다 (검증 조건, 상태 전이, 마스킹 등)
}
