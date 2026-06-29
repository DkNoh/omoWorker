package com.scbk.sms.service.basic;

import com.scbk.sms.dto.common.PageResponseDTO;
import com.scbk.sms.dto.basic.NoticeSearchRequestDTO;
import com.scbk.sms.mapper.basic.NoticeMapper;
import com.scbk.sms.vo.basic.NoticeVO;
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
public class NoticeService {

    private final NoticeMapper mapper;

    @Transactional(readOnly = true)
    public PageResponseDTO<NoticeVO> search(NoticeSearchRequestDTO request) {
        request.validate();
        int totalCount = mapper.count(request);
        List<NoticeVO> list = mapper.selectList(request);
        return PageResponseDTO.of(list, request, totalCount);
    }
}
