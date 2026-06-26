package com.scbk.sms.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * 목록 데이터를 대용량 엑셀 스트림(SXSSF)으로 변환해 즉시 다운로드시키는 공통 유틸리티.
 *
 * <p>String[] headers = {"사번", "이름"}; String[] keys = {"empId", "empNm"};
 * ExcelUtil.downloadExcel(response, "사용자_export", headers, dataList, keys);
 *
 * <p>엑셀 다운로드는 감사 로그 대상이므로 호출 메서드의 @PrivacyLog 적용 여부를 확인한다. 개인정보 컬럼은 마스킹된 값으로 조회한 데이터만 전달한다.
 */
public final class ExcelUtil {

  private ExcelUtil() {}

  /**
   * @param response HttpServletResponse
   * @param fileName 다운로드 파일명 (확장자 제외)
   * @param headers 엑셀 헤더 컬럼명 배열
   * @param dataList 데이터 목록
   * @param keys 데이터 Map에서 헤더 순서대로 추출할 키 배열
   */
  public static void downloadExcel(
      HttpServletResponse response,
      String fileName,
      String[] headers,
      List<Map<String, Object>> dataList,
      String[] keys) {
    // SXSSFWorkbook: 100행 단위로 메모리에 유지하고 나머지는 임시 파일로 플러시해 OOM을 방지한다.
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      Sheet sheet = workbook.createSheet("Data");

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
        sheet.setColumnWidth(i, 4000);
      }

      int rowIdx = 1;
      for (Map<String, Object> data : dataList) {
        Row row = sheet.createRow(rowIdx++);
        for (int i = 0; i < keys.length; i++) {
          Cell cell = row.createCell(i);
          Object value = data.get(keys[i]);
          cell.setCellValue(value != null ? value.toString() : "");
        }
      }

      String encodedFileName =
          URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader(
          "Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\"");

      workbook.write(response.getOutputStream());
      response.getOutputStream().flush();

    } catch (IOException e) {
      throw new IllegalStateException("엑셀 다운로드에 실패했습니다.", e);
    }
  }
}
