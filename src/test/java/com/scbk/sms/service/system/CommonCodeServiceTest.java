package com.scbk.sms.service.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.mapper.system.CommonCodeMapper;
import com.scbk.sms.vo.common.CommonCodeVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonCodeServiceTest {

  @Mock private CommonCodeMapper commonCodeMapper;

  private CommonCodeService commonCodeService;

  @BeforeEach
  void setUp() {
    commonCodeService = new CommonCodeService(commonCodeMapper);
  }

  @Test
  void dept_타입은_활성_부서_목록을_반환한다() {
    // given
    CommonCodeVO dep = new CommonCodeVO();
    dep.setCode("D001");
    dep.setName("정보개발부");
    given(commonCodeMapper.selectDepartments(null)).willReturn(List.of(dep));

    // when
    List<CommonCodeVO> codes = commonCodeService.getCommonCodes("dept", null);

    // then
    assertThat(codes).hasSize(1);
    assertThat(codes.get(0).getCode()).isEqualTo("D001");
  }

  @Test
  void role_타입은_keyword를_전달해_조회한다() {
    // given
    given(commonCodeMapper.selectRoles("ADMIN")).willReturn(List.of());

    // when
    List<CommonCodeVO> codes = commonCodeService.getCommonCodes("role", "ADMIN");

    // then
    assertThat(codes).isEmpty();
  }

  @Test
  void 지원하지_않는_타입은_빈_목록으로_숨기지_않고_오류로_보고한다() {
    // when / then
    assertThatThrownBy(() -> commonCodeService.getCommonCodes("bank", null))
        .isInstanceOf(CustomException.class)
        .extracting(e -> ((CustomException) e).getErrorCode())
        .isEqualTo(ErrorCode.UNSUPPORTED_CODE_TYPE);
  }
}
