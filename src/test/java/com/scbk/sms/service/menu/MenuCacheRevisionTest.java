package com.scbk.sms.service.menu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class MenuCacheRevisionTest {

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void 트랜잭션_밖에서는_revision을_즉시_증가시킨다() {
    MenuCacheRevision revision = new MenuCacheRevision();

    revision.invalidateAfterCommit();

    assertThat(revision.current()).isEqualTo(1L);
  }

  @Test
  void 활성_트랜잭션에서는_커밋_후에만_revision을_증가시킨다() {
    MenuCacheRevision revision = new MenuCacheRevision();
    TransactionSynchronizationManager.initSynchronization();

    revision.invalidateAfterCommit();
    assertThat(revision.current()).isZero();

    TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

    assertThat(revision.current()).isEqualTo(1L);
  }

  @Test
  void 롤백되어_afterCommit이_호출되지_않으면_revision을_유지한다() {
    MenuCacheRevision revision = new MenuCacheRevision();
    TransactionSynchronizationManager.initSynchronization();

    revision.invalidateAfterCommit();

    assertThat(revision.current()).isZero();
  }
}
