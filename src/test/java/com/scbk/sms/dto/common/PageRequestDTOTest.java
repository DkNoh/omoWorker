package com.scbk.sms.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageRequestDTOTest {

  @Test
  void offset은_page와_size로_계산한다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setPage(3);
    request.setSize(20);

    // when
    int offset = request.getOffset();

    // then
    assertThat(offset).isEqualTo(40);
  }

  @Test
  void validate는_비정상_page와_size를_기본값으로_보정한다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setPage(0);
    request.setSize(-5);

    // when
    request.validate();

    // then
    assertThat(request.getPage()).isEqualTo(1);
    assertThat(request.getSize()).isEqualTo(10);
  }

  @Test
  void validate는_size_상한을_100으로_제한한다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setSize(500);

    // when
    request.validate();

    // then
    assertThat(request.getSize()).isEqualTo(100);
  }
}
