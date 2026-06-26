package com.scbk.sms.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseDTOTest {

  @Test
  void of는_총건수와_요청으로_페이지_정보를_계산한다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setPage(2);
    request.setSize(10);

    // when
    PageResponseDTO<String> response = PageResponseDTO.of(List.of("a", "b"), request, 25L);

    // then
    assertThat(response.getPage()).isEqualTo(2);
    assertThat(response.getSize()).isEqualTo(10);
    assertThat(response.getTotalCount()).isEqualTo(25L);
    assertThat(response.getTotalPages()).isEqualTo(3);
    assertThat(response.isHasNext()).isTrue();
    assertThat(response.isHasPrev()).isTrue();
  }

  @Test
  void of는_결과가_없어도_totalPages를_최소_1로_둔다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setPage(1);
    request.setSize(10);

    // when
    PageResponseDTO<String> response = PageResponseDTO.of(List.of(), request, 0L);

    // then
    assertThat(response.getTotalPages()).isEqualTo(1);
    assertThat(response.isHasNext()).isFalse();
    assertThat(response.isHasPrev()).isFalse();
  }

  @Test
  void of는_마지막_페이지에서_hasNext가_false다() {
    // given
    PageRequestDTO request = new PageRequestDTO();
    request.setPage(3);
    request.setSize(10);

    // when
    PageResponseDTO<String> response = PageResponseDTO.of(List.of("a"), request, 25L);

    // then
    assertThat(response.isHasNext()).isFalse();
    assertThat(response.isHasPrev()).isTrue();
  }
}
