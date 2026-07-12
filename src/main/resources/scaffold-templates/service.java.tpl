package com.scbk.sms.service.@@MODULE_NAME@@;

@@IMPORTS@@
/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다.
 */
@Service
@RequiredArgsConstructor
public class @@DOMAIN_CLASS@@Service {

    private final @@DOMAIN_CLASS@@Mapper mapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<@@DOMAIN_CLASS@@VO> search(@@DOMAIN_CLASS@@SearchRequestDTO request) {
        request.validate();
        int totalCount = mapper.count(request);
        List<@@DOMAIN_CLASS@@VO> list = mapper.selectList(request);
@@MASK_LIST_COLUMNS@@        return PageResponseDTO.of(list, request, totalCount);
    }
@@CRUD_SECTION@@@@EXCEL_SECTION@@@@PRIVACY_SECTION@@}
