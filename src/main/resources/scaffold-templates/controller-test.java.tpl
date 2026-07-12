package com.scbk.sms.controller.@@MODULE_NAME@@;

@@IMPORTS@@
@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class @@DOMAIN_CLASS@@ControllerTest {

    @Mock
    private @@DOMAIN_CLASS@@Service service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new @@DOMAIN_CLASS@@Controller(service)).build();
    }

    @Test
    void data는_ApiResponse_포맷으로_응답한다() throws Exception {
        // given
        given(service.search(any())).willReturn(
            PageResponseDTO.of(List.of(), new @@DOMAIN_CLASS@@SearchRequestDTO(), 0));

        // when / then
        mockMvc.perform(get("@@SCREEN_URL@@/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalCount").value(0));
    }
@@CRUD_TESTS@@}
