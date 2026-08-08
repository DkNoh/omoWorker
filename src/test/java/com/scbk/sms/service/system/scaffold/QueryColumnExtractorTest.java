package com.scbk.sms.service.system.scaffold;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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
  void 직접_컬럼은_SELECT_alias에서_원본_컬럼으로_연결한다() {
    String query =
        "SELECT A.SEND_TYPE AS TYPE_CD, A.RECEIVER_NO, COUNT(1) AS SEND_CNT FROM SMS_HISTORY A";

    Map<String, String> sources = QueryColumnExtractor.extractDirectColumnSources(query);

    assertThat(sources)
        .containsEntry("TYPE_CD", "SEND_TYPE")
        .containsEntry("RECEIVER_NO", "RECEIVER_NO")
        .doesNotContainKey("SEND_CNT");
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
  void 검색변수는_원본컬럼과_BETWEEN_시작종료_역할로_연결한다() {
    String query =
        """
            SELECT A.SENT_AT, A.SEND_TYPE, A.RECEIVER_NO
            FROM SMS.SMS_HISTORY A
            WHERE A.SEND_TYPE = $send_type
              AND A.SENT_AT BETWEEN $sent_at_from AND $sent_at_to
              AND A.RECEIVER_NO LIKE '%' || $receiver_no || '%'
            """;

    Map<String, QueryColumnExtractor.SearchParameterSource> sources =
        QueryColumnExtractor.extractSearchParameterSources(query);

    assertThat(sources.get("sendType").columnName()).isEqualTo("SEND_TYPE");
    assertThat(sources.get("sendType").rangePosition())
        .isEqualTo(QueryColumnExtractor.SearchRangePosition.NONE);
    assertThat(sources.get("sentAtFrom").columnName()).isEqualTo("SENT_AT");
    assertThat(sources.get("sentAtFrom").rangePosition())
        .isEqualTo(QueryColumnExtractor.SearchRangePosition.START);
    assertThat(sources.get("sentAtTo").columnName()).isEqualTo("SENT_AT");
    assertThat(sources.get("sentAtTo").rangePosition())
        .isEqualTo(QueryColumnExtractor.SearchRangePosition.END);
    assertThat(sources.get("receiverNo").columnName()).isEqualTo("RECEIVER_NO");
  }

  @Test
  void snake_case를_camelCase로_변환한다() {
    assertThat(QueryColumnExtractor.toCamelCase("SEND_DT")).isEqualTo("sendDt");
    assertThat(QueryColumnExtractor.toCamelCase("receiver_no")).isEqualTo("receiverNo");
    assertThat(QueryColumnExtractor.toCamelCase("STATUS")).isEqualTo("status");
  }

  @Test
  void UNION_쿼리에서_컬럼을_추출한다() {
    String query =
        "SELECT A.ID, A.NAME FROM SMS.TBL_A A UNION SELECT B.ID, B.NAME FROM SMS.TBL_B B";
    List<String> columns = QueryColumnExtractor.extractColumns(query);
    assertThat(columns).isNotEmpty();
  }

  @Test
  void WITH_CTE_쿼리에서_컬럼을_추출한다() {
    String query = "WITH cte AS (SELECT A.ID, A.NAME FROM SMS.TBL_A A) SELECT ID, NAME FROM cte";
    List<String> columns = QueryColumnExtractor.extractColumns(query);
    assertThat(columns).isNotEmpty();
  }
}
