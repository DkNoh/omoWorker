package com.scbk.sms.service.basic;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.basic.CodeSearchRequestDTO;
import com.scbk.sms.mapper.basic.CodeMapper;
import com.scbk.sms.vo.basic.CodeVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다.
 * Scaffold 생성 코드. 업무 로직은 이 파일에 직접 추가한다.
 */
@Service
@RequiredArgsConstructor
public class CodeService {

    private final CodeMapper mapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<CodeVO> search(CodeSearchRequestDTO request) {
        request.validate();
        int totalCount = mapper.count(request);
        List<CodeVO> list = mapper.selectList(request);
        return PageResponseDTO.of(list, request, totalCount);
    }
}
