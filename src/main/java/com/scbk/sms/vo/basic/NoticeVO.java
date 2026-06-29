package com.scbk.sms.vo.basic;

import java.time.LocalDateTime;
import lombok.Data;

/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Data
public class NoticeVO {

    private Integer noticeId;
    private String title;
    private String noticeType;
    private String useYn;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private Integer viewCnt;
    private LocalDateTime regDttm;
}
