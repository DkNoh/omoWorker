package com.scbk.sms.controller.[( ${model.moduleName()} )];

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
[# th:if="${model.includeCreateUpdate()}"]import static org.mockito.BDDMockito.then;
[/]import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
[# th:if="${model.includeCreateUpdate()}"]import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
[/]import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
import com.scbk.sms.service.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Service;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
[# th:if="${model.includeCreateUpdate()}"]import org.springframework.http.MediaType;
[/]import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class [( ${model.domainClass()} )]ControllerTest {

    @Mock
    private [( ${model.domainClass()} )]Service service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new [( ${model.domainClass()} )]Controller(service)).build();
    }

    @Test
    void data는_ApiResponse_포맷으로_응답한다() throws Exception {
        // given
        given(service.search(any())).willReturn(
            PageResponseDTO.of(List.of(), new [( ${model.domainClass()} )]SearchRequestDTO(), 0));

        // when / then
        mockMvc.perform(get("[( ${model.screenUrl()} )]/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalCount").value(0));
    }
[# th:if="${model.includeCreateUpdate()}"]
    @Test
    void create는_등록_성공_메시지를_반환한다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{[# th:each="column, iter : ${model.editableColumns()}"]\"[( ${column.fieldName()} )]\":\"[( ${model.sampleParamValue(column.javaType())} )]\"[# th:if="${!iter.last}"],[/][/]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("등록되었습니다."));

        then(service).should().create(any());
    }

    @Test
    void update는_수정_성공_메시지를_반환한다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{[# th:each="column, iter : ${model.editableColumns()}"]\"[( ${column.fieldName()} )]\":\"[( ${model.sampleParamValue(column.javaType())} )]\"[# th:if="${!iter.last}"],[/][/]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("수정되었습니다."));

        then(service).should().update(any());
    }

    @Test
    void delete는_삭제_성공_메시지를_반환한다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/delete")
[# th:each="pk, iter : ${model.pkFields()}"]                .param("[( ${pk.fieldName()} )]", "[( ${model.sampleParamValue(pk.javaType())} )]")[# th:if="${iter.last}"])[/]
[/]            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("삭제되었습니다."));

        then(service).should().delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/]);
    }
[/]}
