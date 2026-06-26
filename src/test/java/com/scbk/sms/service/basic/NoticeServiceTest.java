package com.scbk.sms.service.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.dto.basic.NoticeUpdateRequestDTO;
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.exception.CustomException;
import com.scbk.sms.mapper.basic.NoticeMapper;
import com.scbk.sms.vo.basic.NoticeVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

  @Test
  void 수정_결과가_0건이면_충돌로_실패한다() {
    // given : 낙관적 잠금 — 다른 사용자가 먼저 수정했거나 대상이 없는 상황
    given(mapper.update(any())).willReturn(0);

    // when / then
    assertThatThrownBy(() -> service.update(new NoticeUpdateRequestDTO()))
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

  // TODO: 업무 규칙 테스트를 추가한다 (검증 조건, 상태 전이, 마스킹 등)
}
