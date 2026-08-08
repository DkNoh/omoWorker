package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.scbk.sms.service.system.scaffold.ScaffoldMetadataReader;
import com.scbk.sms.service.system.scaffold.ScaffoldTableMetadata;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScaffoldMetadataContractTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseMetaData metaData;
  @Mock private ResultSet tableRows;
  @Mock private ResultSet pkRows;
  @Mock private ResultSet columnRows;

  private ScaffoldMetadataReader reader;

  @BeforeEach
  void setUp() throws Exception {
    reader = new ScaffoldMetadataReader(dataSource);
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metaData);
    given(metaData.getTables(null, "SMS", "SMS_HISTORY", null)).willReturn(tableRows);
    given(tableRows.next()).willReturn(true);
    given(metaData.getPrimaryKeys(null, "SMS", "SMS_HISTORY")).willReturn(pkRows);
    given(metaData.getColumns(null, "SMS", "SMS_HISTORY", null)).willReturn(columnRows);
  }

  @Test
  void JDBC_REMARKS를_컬럼_comment로_읽고_빈_comment는_제외한다() throws Exception {
    given(pkRows.next()).willReturn(true, false);
    given(pkRows.getShort("KEY_SEQ")).willReturn((short) 1);
    given(pkRows.getString("COLUMN_NAME")).willReturn("sms_history_id");

    given(columnRows.next()).willReturn(true, true, false);
    given(columnRows.getString("COLUMN_NAME")).willReturn("sms_history_id", "send_type");
    given(columnRows.getInt("NULLABLE"))
        .willReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
    given(columnRows.getString("REMARKS")).willReturn(" 발송 이력 PK ", " ");

    ScaffoldTableMetadata metadata = reader.read("SMS.SMS_HISTORY");

    assertThat(metadata.pkColumns()).containsExactly("SMS_HISTORY_ID");
    assertThat(metadata.nullableByColumn())
        .containsEntry("SMS_HISTORY_ID", false)
        .containsEntry("SEND_TYPE", true);
    assertThat(metadata.commentsByColumn())
        .containsExactlyEntriesOf(Map.of("SMS_HISTORY_ID", "발송 이력 PK"));
  }
}
