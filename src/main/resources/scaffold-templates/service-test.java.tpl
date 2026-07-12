package com.scbk.sms.service.@@MODULE_NAME@@;

@@IMPORTS@@
@ExtendWith(MockitoExtension.class)
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
class @@DOMAIN_CLASS@@ServiceTest {

    @Mock
    private @@DOMAIN_CLASS@@Mapper mapper;

    private @@DOMAIN_CLASS@@Service service;

    @BeforeEach
    void setUp() {
        service = new @@DOMAIN_CLASS@@Service(mapper);
    }

    @Test
    void 목록_조회는_페이지_응답으로_감싼다() {
        // given
        @@DOMAIN_CLASS@@SearchRequestDTO request = new @@DOMAIN_CLASS@@SearchRequestDTO();
        request.setPage(1);
        request.setSize(10);
        given(mapper.count(request)).willReturn(1);
        given(mapper.selectList(request)).willReturn(List.of(new @@DOMAIN_CLASS@@VO()));

        // when
        PageResponseDTO<@@DOMAIN_CLASS@@VO> result = service.search(request);

        // then
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getContents()).hasSize(1);
    }
@@CRUD_TESTS@@
    // TODO: 업무 규칙 테스트를 추가한다 (검증 조건, 상태 전이, 마스킹 등)
}
