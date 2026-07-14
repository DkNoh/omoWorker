package com.scbk.sms.service.[( ${model.moduleName()} )];

import static org.assertj.core.api.Assertions.assertThat;
[# th:if="${model.includeCreateUpdate()}"]import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
[/]import static org.mockito.BDDMockito.given;
[# th:if="${model.includeCreateUpdate()}"]import static org.mockito.BDDMockito.then;
[/]
import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
[# th:if="${model.includeCreateUpdate()}"]import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]UpdateRequestDTO;
import com.scbk.sms.exception.CustomException;
[/]import com.scbk.sms.mapper.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Mapper;
import com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class [( ${model.domainClass()} )]ServiceTest {

    @Mock
    private [( ${model.domainClass()} )]Mapper mapper;

    private [( ${model.domainClass()} )]Service service;

    @BeforeEach
    void setUp() {
        service = new [( ${model.domainClass()} )]Service(mapper);
    }

    @Test
    void 목록_조회는_페이지_응답으로_감싼다() {
        // given
        [( ${model.domainClass()} )]SearchRequestDTO request = new [( ${model.domainClass()} )]SearchRequestDTO();
        request.setPage(1);
        request.setSize(10);
        given(mapper.count(request)).willReturn(1);
        given(mapper.selectList(request)).willReturn(List.of(new [( ${model.domainClass()} )]VO()));

        // when
        PageResponseDTO<[( ${model.domainClass()} )]VO> result = service.search(request);

        // then
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getContents()).hasSize(1);
    }
[# th:if="${model.includeCreateUpdate()}"]
    @Test
    void 수정_결과가_0건이면_충돌로_실패한다() {
        // given : 낙관적 잠금 — 다른 사용자가 먼저 수정했거나 대상이 없는 상황
        given(mapper.update(any())).willReturn(0);

        // when / then
        assertThatThrownBy(() -> service.update(new [( ${model.domainClass()} )]UpdateRequestDTO()))
            .isInstanceOf(CustomException.class);
    }

    @Test
    void 삭제는_Mapper에_위임한다() {
        // given
        given(mapper.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/])).willReturn(1);

        // when
        service.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/]);

        // then
        then(mapper).should().delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/]);
    }

    @Test
    void 삭제_결과가_0건이면_충돌로_실패한다() {
        // given : 다른 사용자가 먼저 삭제했거나 대상이 없는 상황
        given(mapper.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/])).willReturn(0);

        // when / then
        assertThatThrownBy(() -> service.delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/]))
            .isInstanceOf(CustomException.class);
    }
[/]
    // TODO: 업무 규칙 테스트를 추가한다 (검증 조건, 상태 전이, 마스킹 등)
}
