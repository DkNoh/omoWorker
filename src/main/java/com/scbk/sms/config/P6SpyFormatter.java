package com.scbk.sms.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * p6spy 로그 포맷터. local/dev SQL 로깅용. prod(JNDI datasource)에서는 P6SpyDriver가 로드되지 않으므로 이 클래스도 동작하지 않는다.
 */
public class P6SpyFormatter implements MessageFormattingStrategy {

  @Override
  public String formatMessage(
      int connectionId,
      String now,
      long elapsed,
      String category,
      String prepared,
      String sql,
      String url) {
    if (sql == null || sql.isBlank()) {
      return "";
    }
    String oneLine = sql.trim().replaceAll("\\s+", " ");
    return String.format("[p6spy] %dms | %s", elapsed, oneLine);
  }
}
