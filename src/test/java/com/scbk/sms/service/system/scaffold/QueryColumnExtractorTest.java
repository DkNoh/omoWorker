package com.scbk.sms.service.system.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class QueryColumnExtractorTest {

  private static final String QUERY =
      """
        SELECT A.SEND_DT, A.RECEIVER_NO, COUNT(1) AS SEND_CNT
        FROM SMS_HISTORY A
        WHERE 1=1
        AND A.SEND_DT >= $start_dt
        AND A.RECEIVER_NO = $receiver_no
        GROUP BY A.SEND_DT, A.RECEIVER_NO
        """;

  @Test
  void SELECT_컬럼을_alias_우선으로_추출한다() {
    // when
    List<String> columns = QueryColumnExtractor.extractColumns(QUERY);

    // then
    assertThat(columns).containsExactly("SEND_DT", "RECEIVER_NO", "SEND_CNT");
  }

  @Test
  void 함수와_서브쿼리_컬럼도_서버_파서로_alias를_추출한다() {
    // given
    String query =
        """
            SELECT A.SEND_DT,
                   NVL(A.RESULT_MSG, 'OK,FAIL') AS RESULT_MSG,
                   (SELECT COUNT(1) FROM SMS.SMS_HISTORY X WHERE X.REQUEST_ID = A.REQUEST_ID) AS RETRY_CNT
            FROM SMS.SMS_HISTORY A
            WHERE A.SEND_DT >= $start_dt
            """;

    // when
    List<String> columns = QueryColumnExtractor.extractColumns(query);

    // then
    assertThat(columns).containsExactly("SEND_DT", "RESULT_MSG", "RETRY_CNT");
  }

  @Test
  void CRUD_기준_테이블을_FROM에서_추출한다() {
    assertThat(QueryColumnExtractor.extractPrimaryTable("SELECT A.ID FROM SMS.SMS_HISTORY A"))
        .isEqualTo("SMS.SMS_HISTORY");
    assertThat(QueryColumnExtractor.extractPrimaryTable("SELECT A.ID FROM SMS_HISTORY A"))
        .isEqualTo("SMS_HISTORY");
  }

  @Test
  void 검색변수를_camelCase로_추출한다() {
    // when
    List<String> vars = QueryColumnExtractor.extractSearchVars(QUERY);

    // then
    assertThat(vars).containsExactly("startDt", "receiverNo");
  }

  @Test
  void 변수_라인은_동적_if_조건으로_변환한다() {
    // when
    String sql = QueryColumnExtractor.convertToDynamicSql("AND A.SEND_DT >= $start_dt", "");

    // then
    assertThat(sql).contains("<if test=\"startDt != null and startDt != ''\">");
    assertThat(sql).contains("AND A.SEND_DT >= #{startDt}");
    assertThat(sql).contains("</if>");
  }

  @Test
  void 변수가_없는_라인은_그대로_둔다() {
    // when
    String sql = QueryColumnExtractor.convertToDynamicSql("FROM SMS_HISTORY A", "");

    // then
    assertThat(sql.trim()).isEqualTo("FROM SMS_HISTORY A");
  }

  @Test
  void snake_case를_camelCase로_변환한다() {
    assertThat(QueryColumnExtractor.toCamelCase("SEND_DT")).isEqualTo("sendDt");
    assertThat(QueryColumnExtractor.toCamelCase("receiver_no")).isEqualTo("receiverNo");
    assertThat(QueryColumnExtractor.toCamelCase("STATUS")).isEqualTo("status");
  }
}
