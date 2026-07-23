package com.scbk.sms.vo.basic;

import java.time.LocalDateTime;
import lombok.Data;

/** 개발자 소유 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다. */
@Data
public class NoticeVO {

  private Integer noticeId;
  private String title;
  private String content;
  private String noticeType;
  private String useYn;
  private LocalDateTime startDt;
  private LocalDateTime endDt;
  private Integer viewCnt;
  private String regId;
  private LocalDateTime regDttm;
  private String updId;
  private LocalDateTime updDttm;
}
