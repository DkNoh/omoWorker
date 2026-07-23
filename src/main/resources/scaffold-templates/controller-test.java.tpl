package com.scbk.sms.controller.[( ${model.moduleName()} )];

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

[# th:if="${model.includeCreateUpdate()}"]import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
[/]import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]SearchRequestDTO;
[# th:if="${model.includeCreateUpdate()}"]import com.scbk.sms.dto.[( ${model.moduleName()} )].[( ${model.domainClass()} )]UpdateRequestDTO;
[/]import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import com.scbk.sms.exception.GlobalExceptionHandler;
import com.scbk.sms.service.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Service;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
[# th:if="${model.includeCreateUpdate()}"]    private ObjectMapper objectMapper;
[/]
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new [( ${model.domainClass()} )]Controller(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
[# th:if="${model.includeCreateUpdate()}"]        objectMapper = new ObjectMapper().findAndRegisterModules();
[/]    }

    @Test
    void data는_정상_요청을_DTO에_매핑하고_ApiResponse로_응답한다() throws Exception {
        // given
        given(service.search(any())).willReturn(
            PageResponseDTO.of(List.of(), new [( ${model.domainClass()} )]SearchRequestDTO(), 0));
        ArgumentCaptor<[( ${model.domainClass()} )]SearchRequestDTO> captor =
            ArgumentCaptor.forClass([( ${model.domainClass()} )]SearchRequestDTO.class);

        // when / then
        mockMvc.perform(get("[( ${model.screenUrl()} )]/data")
                .param("page", "2")
                .param("size", "20")
[# th:each="searchParam : ${model.searchParams()}"]                .param("[( ${searchParam.name()} )]", "1")
[/]            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("SUCCESS"))
            .andExpect(jsonPath("$.data.totalCount").value(0));

        then(service).should(times(1)).search(captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getSize()).isEqualTo(20);
[# th:each="searchParam : ${model.searchParams()}"]        assertThat(captor.getValue().get[( ${model.capitalize(searchParam.name())} )]()).isEqualTo("1");
[/]    }

    @Test
    void data의_숫자_파라미터가_잘못되면_400이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(get("[( ${model.screenUrl()} )]/data").param("page", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void data에_지원하지_않는_HTTP_메서드는_405이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/data"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value(405));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void 서비스_예외는_공통_ApiResponse로_변환한다() throws Exception {
        // given
        given(service.search(any())).willThrow(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        // when / then
        mockMvc.perform(get("[( ${model.screenUrl()} )]/data"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
            .andExpect(jsonPath("$.data").doesNotExist());

        then(service).should(times(1)).search(any());
    }
[# th:if="${model.includeCreateUpdate()}"]
    @Test
    void create는_정상_JSON을_DTO에_매핑하고_한_번_호출한다() throws Exception {
        // given
        ArgumentCaptor<[( ${model.domainClass()} )]UpdateRequestDTO> captor =
            ArgumentCaptor.forClass([( ${model.domainClass()} )]UpdateRequestDTO.class);

        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{[# th:each="column, iter : ${model.editableColumns()}"]\"[( ${column.fieldName()} )]\":\"[( ${model.sampleParamValue(column.javaType())} )]\"[# th:if="${!iter.last}"],[/][/]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("등록되었습니다."));

        then(service).should(times(1)).create(captor.capture());
[# th:each="column : ${model.editableColumns()}"]        assertThat(captor.getValue().get[( ${model.capitalize(column.fieldName())} )]()).isEqualTo([( ${model.sampleValue(column.javaType())} )]);
[/]    }

    @Test
    void update는_정상_JSON을_DTO에_매핑하고_한_번_호출한다() throws Exception {
        // given
        ArgumentCaptor<[( ${model.domainClass()} )]UpdateRequestDTO> captor =
            ArgumentCaptor.forClass([( ${model.domainClass()} )]UpdateRequestDTO.class);

        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{[# th:each="column, iter : ${model.editableColumns()}"]\"[( ${column.fieldName()} )]\":\"[( ${model.sampleParamValue(column.javaType())} )]\"[# th:if="${!iter.last}"],[/][/]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("수정되었습니다."));

        then(service).should(times(1)).update(captor.capture());
[# th:each="column : ${model.editableColumns()}"]        assertThat(captor.getValue().get[( ${model.capitalize(column.fieldName())} )]()).isEqualTo([( ${model.sampleValue(column.javaType())} )]);
[/]    }
[# th:if="${model.hasEditableRequiredNotBlank() or model.hasEditableRequiredNotNull()}"]
    @Test
    void 필수값이_누락되면_400이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.errors").isArray());

        then(service).shouldHaveNoInteractions();
    }
[/][# th:if="${model.hasEditableRequiredNotBlank()}"]
    @Test
    void 필수_문자열이_공백이면_400이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{[# th:each="column, iter : ${model.editableColumns()}"]\"[( ${column.fieldName()} )]\":\"[# th:if="${column.requiresNotBlank()}"]   [/][# th:unless="${column.requiresNotBlank()}"][( ${model.sampleParamValue(column.javaType())} )][/]\"[# th:if="${!iter.last}"],[/][/]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        then(service).shouldHaveNoInteractions();
    }
[/]
    @Test
    void 잘못된_JSON은_400이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void 지원하지_않는_ContentType은_415이고_서비스를_호출하지_않는다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/create")
                .contentType(MediaType.TEXT_PLAIN)
                .content("value=1"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value(415));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void 선언되지_않은_필드는_UpdateRequestDTO에_매핑되지_않는다() throws Exception {
        // given
        ArgumentCaptor<[( ${model.domainClass()} )]UpdateRequestDTO> captor =
            ArgumentCaptor.forClass([( ${model.domainClass()} )]UpdateRequestDTO.class);

        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"unexpectedSystemField\":\"tampered\"[# th:each="column : ${model.editableColumns()}"],\"[( ${column.fieldName()} )]\":\"[( ${model.sampleParamValue(column.javaType())} )]\"[/]}"))
            .andExpect(status().isOk());

        then(service).should(times(1)).update(captor.capture());
        JsonNode mapped = objectMapper.valueToTree(captor.getValue());
        assertThat(mapped.has("unexpectedSystemField")).isFalse();
    }

    @Test
    void delete는_PK를_정확히_매핑하고_한_번_호출한다() throws Exception {
        // when / then
        mockMvc.perform(post("[( ${model.screenUrl()} )]/delete")
[# th:each="pk, iter : ${model.pkFields()}"]                .param("[( ${pk.fieldName()} )]", "[( ${model.sampleParamValue(pk.javaType())} )]")[# th:if="${iter.last}"])[/]
[/]            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("삭제되었습니다."));

        then(service).should(times(1)).delete([# th:each="pk, iter : ${model.pkFields()}"][( ${model.sampleValue(pk.javaType())} )][# th:if="${!iter.last}"], [/][/]);
    }
[/]}
