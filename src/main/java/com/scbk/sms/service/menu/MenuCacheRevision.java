package com.scbk.sms.service.menu;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 메뉴·권한 변경 커밋을 세션 캐시에 전달하기 위한 애플리케이션 단위 revision. */
@Component
public class MenuCacheRevision {

  private final AtomicLong revision = new AtomicLong();

  public long current() {
    return revision.get();
  }

  /**
   * 활성 트랜잭션 동기화가 있으면 실제 커밋 뒤에 revision을 올린다. 트랜잭션 밖에서 호출된 경우에는 즉시 올린다.
   */
  public void invalidateAfterCommit() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              revision.incrementAndGet();
            }
          });
      return;
    }
    revision.incrementAndGet();
  }
}
