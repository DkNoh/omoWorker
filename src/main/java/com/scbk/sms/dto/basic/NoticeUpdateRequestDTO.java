package com.scbk.sms.dto.basic;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 개발자 소유 수동 참조: scaffold 복사 후 커스터마이즈한 window.open CRUD 예제. 재생성하지 않고 직접 수정한다. 수정 가능한 필드만 선언하는 화이트리스트
 * DTO. TODO: 실제 수정을 허용할 필드만 남기고 제거한다. REG_ID/REG_DTTM, 시스템 필드, 권한 필드는 선언하지 않는다.
 */
@Data
public class NoticeUpdateRequestDTO {

  /** PK 필드 (WHERE 조건): NOTICE_ID */
  private Integer noticeId;

  private String title;
  private String content;
  private String noticeType;
  private String useYn;
  private LocalDateTime startDt;
  private LocalDateTime endDt;
  private Integer viewCnt;

  /** 낙관적 잠금용. 상세 조회 시점의 UPD_DTTM을 팝업 hidden 필드로 전달한다. */
  private LocalDateTime beforeUpdDttm;
}
