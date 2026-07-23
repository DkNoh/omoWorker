package com.scbk.sms.service.system.scaffold;

import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.Artifact.*;
import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.render;
import static com.scbk.sms.service.system.scaffold.ScaffoldArtifactRenderer.renderSelected;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scbk.sms.dto.system.ScaffoldColumnOptionDTO;
import com.scbk.sms.dto.system.ScaffoldMenuOptionDTO;
import com.scbk.sms.dto.system.ScaffoldRequestDTO;
import com.scbk.sms.dto.system.ScaffoldSearchParamOptionDTO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ScaffoldTemplateTest {

  private ScaffoldModel model(boolean createUpdate, boolean excel, boolean privacy) {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setIncludeCreateUpdate(createUpdate);
    request.setIncludeExcel(excel);
    request.setIncludePrivacy(privacy);
    if (createUpdate) {
      request.setPkColumn("RECEIVER_NO");
    }
    return new ScaffoldModel(
        request,
        List.of("SEND_DT", "RECEIVER_NO"),
        List.of("startDt"),
        Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));
  }

  @Test
  void DTO는_PageRequestDTO를_상속하고_Lombok_기반으로_생성한다() {
    // when
    String code = render(SEARCH_DTO, model(false, false, false));

    // then
    assertThat(code).contains("extends PageRequestDTO");
    assertThat(code).contains("private String startDt;");
    assertThat(code).contains("@Data");
    assertThat(code).contains("@EqualsAndHashCode(callSuper = true)");
    assertThat(code).doesNotContain("public String getStartDt()");
  }

  @Test
  void VO는_추론된_타입과_Lombok으로_생성한다() {
    // when
    String code = render(VO, model(false, false, false));

    // then
    assertThat(code).contains("@Data");
    assertThat(code).contains("private LocalDate sendDt;");
    assertThat(code).contains("private String receiverNo;");
    assertThat(code).contains("import java.time.LocalDate;");
    assertThat(code).doesNotContain("public LocalDate getSendDt()");
  }

  @Test
  void Service와_Controller는_RequiredArgsConstructor를_사용한다() {
    // when
    String serviceCode = render(SERVICE, model(false, false, false));
    String controllerCode = render(CONTROLLER, model(false, false, false));

    // then
    assertThat(serviceCode).contains("@RequiredArgsConstructor");
    assertThat(serviceCode).doesNotContain("public SmsHistoryService(");
    assertThat(controllerCode).contains("@RequiredArgsConstructor");
    assertThat(controllerCode).doesNotContain("public SmsHistoryController(");
  }

  @Test
  void Controller는_create와_update를_분리하고_save를_만들지_않는다() {
    // when
    String code = render(CONTROLLER, model(true, false, false));

    // then
    assertThat(code).contains("@PostMapping(\"/create\")");
    assertThat(code).contains("@PostMapping(\"/update\")");
    assertThat(code).doesNotContain("/save");
  }

  @Test
  void 수정_요청은_화이트리스트_DTO로만_받는다() {
    // when
    String controllerCode = render(CONTROLLER, model(true, false, false));
    String updateDtoCode = render(UPDATE_DTO, model(true, false, false));
    String serviceCode = render(SERVICE, model(true, false, false));
    String xmlCode = render(MAPPER_XML, model(true, false, false));

    // then : Controller는 VO가 아니라 UpdateRequestDTO를 수신한다
    assertThat(controllerCode).contains("@RequestBody SmsHistoryUpdateRequestDTO request");
    assertThat(controllerCode).doesNotContain("@RequestBody SmsHistoryVO");

    // UpdateRequestDTO는 화이트리스트 안내를 가진다. 잠금 필드는 lockColumn 선택 시에만 생성한다
    assertThat(updateDtoCode).contains("수정을 허용할 필드만 남기고");
    assertThat(updateDtoCode).doesNotContain("beforeUpdateDttm");

    // Service는 update 0건을 충돌로 실패 처리한다
    assertThat(serviceCode).contains("ErrorCode.UPDATE_CONFLICT");
    // Service는 delete 0건도 충돌로 실패 처리한다
    assertThat(serviceCode).contains("ErrorCode.DELETE_CONFLICT");

    // XML은 추론한 기준 테이블로 실행 가능한 CRUD SQL을 만든다
    assertThat(xmlCode).contains("INSERT INTO SMS_HISTORY");
    assertThat(xmlCode).contains("UPDATE SMS_HISTORY");
    assertThat(xmlCode).contains("DELETE FROM SMS_HISTORY");
    assertThat(xmlCode).contains("RECEIVER_NO = #{receiverNo,jdbcType=VARCHAR}");
    assertThat(xmlCode).doesNotContain("TODO: 테이블명");
  }

  @Test
  void 개인정보_포함이면_PrivacyLog를_부착한다() {
    // when
    String code = render(CONTROLLER, model(false, true, true));

    // then
    assertThat(code).contains("@PrivacyLog(action = \"발송이력조회 목록 조회\", recordParameters = false)");
    assertThat(code).contains("@PrivacyLog(action = \"발송이력조회 엑셀 다운로드\")");
  }

  @Test
  void 개인정보만_포함해도_unmask용_RequestParam을_import한다() {
    // when
    ScaffoldModel privacyModel = model(false, false, true);
    String code = render(CONTROLLER, privacyModel);
    String mapper = render(MAPPER_INTERFACE, privacyModel);

    // then
    assertThat(code).contains("import org.springframework.web.bind.annotation.RequestParam;");
    assertThat(code).contains("getUnmaskedDetail(@RequestParam String");
    assertThat(code).doesNotContain("@PostMapping");
    assertThat(mapper).contains("import org.apache.ibatis.annotations.Param;");
    assertThat(mapper).contains("selectDetail(@Param(\"id\") String id)");
  }

  @Test
  void MapperXml은_입력받은_정렬과_OFFSET_FETCH를_사용한다() {
    // when
    String xml = render(MAPPER_XML, model(false, false, false));

    // then
    assertThat(xml).contains("ORDER BY A.SEND_DT DESC, A.HIST_ID DESC");
    assertThat(xml).contains("OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY");
    assertThat(xml).contains("<if test=\"startDt != null and startDt != ''\">");
  }

  @Test
  void 메뉴SQL은_v3_스키마를_사용한다() {
    // when
    String sql = render(MENU_SQL, model(false, false, false));

    // then
    assertThat(sql).contains("SMS.TB_MENU (");
    assertThat(sql).contains("MENU_ID, PARENT_MENU_ID");
    assertThat(sql).contains("CAN_READ, CAN_CREATE, CAN_UPDATE, CAN_DELETE");
    assertThat(sql).contains("CAN_APPROVE, CAN_CANCEL, CAN_DOWNLOAD, CAN_MASK_VIEW");
    assertThat(sql).contains("ROLE_CD");
    // v2 스키마 잔재가 없어야 한다
    assertThat(sql).doesNotContain("AUTH_CD");
    assertThat(sql).doesNotContain("CAN_WRITE");
    assertThat(sql).doesNotContain("UP_MENU_CD");
  }

  @Test
  void domainId에_슬래시가_있으면_v2의_3단계_URL을_그대로_재현한다() {
    // given : v2 baseline의 /campaign/sms/register 같은 3단계 경로
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("campaign");
    request.setDomainId("sms/register");
    request.setDomainClass("CampaignSmsRegister");
    request.setDomainName("SMS등록");
    request.setRawQuery("SELECT A.REG_ID FROM SMS.CAMPAIGN_SMS A WHERE 1=1");
    request.setOrderBy("A.REG_ID DESC");
    ScaffoldModel model =
        new ScaffoldModel(request, List.of("REG_ID"), List.of(), Map.of("REG_ID", "String"));

    // when
    String controllerCode = render(CONTROLLER, model);
    String htmlCode = render(PAGE_HTML, model);

    // then : screenUrl/menuId/뷰 이름/JS 경로 모두 3단계 경로를 유지한다
    assertThat(model.screenUrl()).isEqualTo("/campaign/sms/register");
    assertThat(model.menuId()).isEqualTo("CAMPAIGN_SMS_REGISTER");
    assertThat(controllerCode).contains("@RequestMapping(\"/campaign/sms/register\")");
    assertThat(controllerCode).contains("return \"campaign/sms/register\";");
    assertThat(htmlCode).contains("/js/campaign/sms/register.js");
  }

  @Test
  void ServiceTest는_Mockito_mock과_given_when_then으로_생성한다() {
    // when
    String code = render(SERVICE_TEST, model(true, false, false));

    // then
    assertThat(code).contains("@ExtendWith(MockitoExtension.class)");
    assertThat(code).contains("@Mock");
    assertThat(code).contains("// given");
    assertThat(code).contains("// when");
    assertThat(code).contains("// then");
    assertThat(code).contains("then(mapper).should().delete(\"1\")");
    assertThat(code).contains("// TODO: 업무 규칙 테스트를 추가한다");
  }

  @Test
  void ControllerTest는_MockMvc로_ApiResponse_포맷을_검증한다() {
    // when
    String code = render(CONTROLLER_TEST, model(false, false, false));

    // then
    assertThat(code).contains("MockMvcBuilders.standaloneSetup");
    assertThat(code).contains(".setControllerAdvice(new GlobalExceptionHandler())");
    assertThat(code).contains("get(\"/sms/history/data\")");
    assertThat(code).contains("jsonPath(\"$.code\").value(200)");
    assertThat(code).contains("ArgumentCaptor<SmsHistorySearchRequestDTO>");
    assertThat(code).contains("then(service).should(times(1)).search(captor.capture())");
    assertThat(code).contains("captor.getValue().getPage()).isEqualTo(2)");
    assertThat(code).contains("captor.getValue().getStartDt()).isEqualTo(\"1\")");
    assertThat(code).contains(".param(\"page\", \"invalid\")");
    assertThat(code).contains("then(service).shouldHaveNoInteractions()");
    assertThat(code).contains("status().isMethodNotAllowed()");
    assertThat(code).contains("new CustomException(ErrorCode.INTERNAL_SERVER_ERROR)");
  }

  @Test
  void ControllerTest는_LIST_화면이면_create_update_delete_테스트를_만들지_않는다() {
    // when
    String code = render(CONTROLLER_TEST, model(false, false, false));

    // then : CUD가 없는 화면은 create/update/delete 엔드포인트만 생성하지 않는다
    assertThat(code).doesNotContain("/create");
    assertThat(code).doesNotContain("/update");
    assertThat(code).doesNotContain("/delete");
    assertThat(code).contains("then(service).should(times(1)).search(captor.capture())");
  }

  @Test
  void ControllerTest는_CRUD이면_create_update_delete_성공_메시지를_검증한다() {
    // when
    String code = render(CONTROLLER_TEST, model(true, false, false));

    // then : 성공 메시지는 ApiResponse.data가 아니라 message 필드에 담긴다
    assertThat(code).contains("post(\"/sms/history/create\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"등록되었습니다.\")");
    assertThat(code).contains("then(service).should(times(1)).create(captor.capture())");

    assertThat(code).contains("post(\"/sms/history/update\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"수정되었습니다.\")");
    assertThat(code).contains("then(service).should(times(1)).update(captor.capture())");

    assertThat(code).contains("post(\"/sms/history/delete\")");
    assertThat(code).contains(".param(\"receiverNo\", \"1\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"삭제되었습니다.\")");
    assertThat(code).contains("then(service).should(times(1)).delete(\"1\")");
    assertThat(code).contains("잘못된_JSON은_400이고_서비스를_호출하지_않는다");
    assertThat(code).contains("지원하지_않는_ContentType은_415이고_서비스를_호출하지_않는다");
    assertThat(code).contains("선언되지_않은_필드는_UpdateRequestDTO에_매핑되지_않는다");
    assertThat(code).contains("mapped.has(\"unexpectedSystemField\")").contains("isFalse()");
  }

  @Test
  void ControllerTest는_required_필드가_있으면_유효한_JSON으로_성공을_검증한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    ScaffoldColumnOptionDTO sendType =
        columnOption("SEND_TYPE", true, true, true, "발송유형", 120, "center", "NONE", "NONE");
    sendType.setValidate("required");
    request.setColumnOptions(List.of(sendType));

    // when
    String code = render(CONTROLLER_TEST, optionModel(request));

    // then
    assertThat(code).contains("\\\"sendType\\\":\\\"1\\\"");
    assertThat(code).contains("필수값이_누락되면_400이고_서비스를_호출하지_않는다");
    assertThat(code).contains("필수_문자열이_공백이면_400이고_서비스를_호출하지_않는다");
    assertThat(code).contains(".content(\"{}\")");
    assertThat(code).contains("jsonPath(\"$.errors\").isArray()");
  }

  @Test
  void ControllerTest의_byte_배열_샘플은_Base64_JSON과_동일한_배열값을_사용한다() {
    // given
    ScaffoldModel model = model(true, false, false);

    // when / then
    assertThat(model.sampleParamValue("byte[]")).isEqualTo("AQ==");
    assertThat(model.sampleValue("byte[]")).isEqualTo("new byte[] {1}");
  }

  @Test
  void HTML은_screen_convention_골격을_따른다() {
    // when
    String html = render(PAGE_HTML, model(false, true, false));

    // then
    assertThat(html).contains("layout:decorate=\"~{defaultLayout}\"");
    assertThat(html).contains("id=\"btn-search\"");
    assertThat(html).contains("data-lucide=\"search\"");
    assertThat(html).contains("th:replace=\"~{fragments/toast-grid :: gridCard}\"");
    assertThat(html).contains("id=\"btn-excel\"");
    assertThat(html).contains("th:if=\"${pageAuth.download}\"");
    assertThat(html).contains("data-lucide=\"download\"");
    assertThat(html).doesNotContain("search-section");
    assertThat(html).doesNotContain("scaffold-grid");
    assertThat(html).doesNotContain("style=\"");
  }

  @Test
  void ToastGrid_fragment는_공통_mount_id와_토스트그리드_클래스를_가진다() throws Exception {
    // when
    String fragment =
        Files.readString(Path.of("src/main/resources/templates/fragments/toast-grid.html"));

    // then
    assertThat(fragment).contains("th:fragment=\"gridCard\"");
    assertThat(fragment).contains("id=\"total-count\"");
    assertThat(fragment).contains("id=\"pageSizeSelect\"");
    assertThat(fragment).contains("class=\"form-select form-select-sm toast-grid-page-size\"");
    assertThat(fragment).contains("id=\"grid\" class=\"toast-grid\"");
    assertThat(fragment).contains("id=\"pagination\"");
  }

  @Test
  void HTML은_sendType처럼_dt_글자가_붙은_일반_필드를_날짜로_오판하지_않는다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_TYPE FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_TYPE = $send_type");
    request.setOrderBy("A.SEND_TYPE");
    ScaffoldModel model =
        new ScaffoldModel(
            request, List.of("SEND_TYPE"), List.of("sendType"), Map.of("SEND_TYPE", "String"));

    // when
    String html = render(PAGE_HTML, model);

    // then
    assertThat(html).contains("id=\"sendType\" class=\"form-control scaffold-search-control\"");
    assertThat(html).contains("<input type=\"text\" id=\"sendType\"");
    assertThat(html).doesNotContain("<input type=\"date\" id=\"sendType\"");
  }

  @Test
  void JS는_TuiPageBuilder로_그리드를_초기화한다() {
    // when
    String js = render(PAGE_JS, model(false, false, false));

    // then
    assertThat(js).contains("new TuiPageBuilder({");
    assertThat(js).contains("apiUrl: '/sms/history/data'");
    assertThat(js).contains("searchInputs: ['startDt']");
    assertThat(js).doesNotContain("initSearchDatePickers");
    assertThat(js).doesNotContain("function readSearchValue");
    assertThat(js).contains("name: 'sendDt'");
  }

  @Test
  void TuiPageBuilder는_검색_DATE_picker를_공통으로_처리한다() throws Exception {
    // when
    String builder =
        Files.readString(Path.of("src/main/resources/static/js/common/tui-page-builder.js"));

    // then
    assertThat(builder).contains("_initSearchDatePickers()");
    assertThat(builder).contains("data-search-type");
    assertThat(builder).contains("document.getElementById(`${id}PickerLayer`)");
    assertThat(builder).contains("_syncSearchDatePickers()");
    assertThat(builder).contains("getSearchParams(options = {})");
  }

  @Test
  void TuiPageBuilder는_자동_상세모달과_더블클릭을_포함하지_않는다() throws Exception {
    // when
    String builder =
        Files.readString(Path.of("src/main/resources/static/js/common/tui-page-builder.js"));

    // then
    assertThat(builder).doesNotContain("grid.on('dblclick'");
    assertThat(builder).doesNotContain("autoModal");
    assertThat(builder).doesNotContain("detailApiUrl");
    assertThat(builder).doesNotContain("tui-auto-modal");
  }

  @Test
  void scaffold_Java는_산출물_문자열을_append로_조립하지_않는다() throws Exception {
    String modelSource =
        Files.readString(
            Path.of("src/main/java/com/scbk/sms/service/system/scaffold/ScaffoldModel.java"));
    String mapperSource =
        Files.readString(
            Path.of(
                "src/main/java/com/scbk/sms/service/system/scaffold/MapperXmlViewFactory.java"));

    assertThat(modelSource).doesNotContain("StringBuilder", ".append(");
    assertThat(mapperSource).doesNotContain("StringBuilder", ".append(");
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/scbk/sms/service/system/scaffold/ControllerTemplate.java")))
        .isFalse();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/java/com/scbk/sms/service/system/scaffold/ListPageRenderer.java")))
        .isFalse();
    assertThat(
            Files.readString(Path.of("src/main/resources/scaffold-templates/mapper-xml.xml.tpl")))
        .contains("th:each=\"value, iter : ${xml.insertValues()}\"");
  }

  @Test
  void scaffold_입력과_공통_JS는_자동_상세모달_계약을_노출하지_않는다() throws Exception {
    String requestDto =
        Files.readString(Path.of("src/main/java/com/scbk/sms/dto/system/ScaffoldRequestDTO.java"));
    String scaffoldUi =
        Files.readString(Path.of("src/main/resources/static/js/system/scaffold.js"));

    assertThat(requestDto).doesNotContain("includeModal");
    assertThat(scaffoldUi).doesNotContain("includeModal", "autoModal");
  }

  @Test
  void scaffold_UI와_리소스는_오직_LIST_EXCEL_CRUD만_노출하고_CRUD_PANEL을_참조하지_않는다() throws Exception {
    // given : scaffold UI 정적 자원
    String html = Files.readString(Path.of("src/main/resources/templates/system/scaffold.html"));
    String js = Files.readString(Path.of("src/main/resources/static/js/system/scaffold.js"));

    assertThat(screenModeOptionValues(html)).containsExactlyInAnyOrder("LIST", "EXCEL", "CRUD");
    assertThat(html).doesNotContain("CRUD_PANEL");
    assertThat(js).doesNotContain("CRUD_PANEL");
    assertThat(Files.exists(Path.of("src/main/resources/scaffold-templates/crud-panel"))).isFalse();
    assertThat(Files.exists(Path.of("src/main/resources/scaffold-cases/basic_notice.json")))
        .isFalse();
  }

  /** <code>#screenMode</code> select 의 option value 집합을 추출한다. */
  private static Set<String> screenModeOptionValues(String html) {
    Matcher select =
        Pattern.compile("<select\\s+id=\"screenMode\"[\\s\\S]*?</select>").matcher(html);
    if (!select.find()) {
      return Set.of();
    }
    Set<String> values = new TreeSet<>();
    Matcher option = Pattern.compile("value=\"([^\"]+)\"").matcher(select.group());
    while (option.find()) {
      values.add(option.group(1));
    }
    return values;
  }

  @Test
  void JS는_날짜_타입_컬럼에_공통_포매터를_부착한다() {
    // when : SEND_DT는 LocalDate, RECEIVER_NO는 String
    String js = render(PAGE_JS, model(false, false, false));

    // then
    assertThat(js)
        .contains("name: 'sendDt', align: 'center', width: 150, formatter: TuiCommon.fmt.date");
    assertThat(js).contains("name: 'receiverNo', align: 'center', width: 150 }");
  }

  @Test
  void MapperXml은_날짜_비교_가이드_주석을_포함한다() {
    // when
    String xml = render(MAPPER_XML, model(false, false, false));

    // then
    assertThat(xml).contains("YYYYMMDD");
    assertThat(xml).contains("TO_DATE");
  }

  @Test
  void MapperXml은_단일_날짜_조건을_자정_동등비교가_아닌_하루_범위로_변환한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery("SELECT A.SEND_DT FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT = $send_dt");
    request.setOrderBy("A.SEND_DT DESC");
    ScaffoldModel dateModel =
        new ScaffoldModel(
            request, List.of("SEND_DT"), List.of("sendDt"), Map.of("SEND_DT", "LocalDateTime"));

    // when
    String xml = render(MAPPER_XML, dateModel);

    // then
    assertThat(xml)
        .contains(
            "A.SEND_DT <![CDATA[ >= ]]> TO_TIMESTAMP(#{sendDt} || '000000', 'YYYYMMDDHH24MISS')");
    assertThat(xml)
        .contains(
            "A.SEND_DT <![CDATA[ < ]]> TO_TIMESTAMP(#{sendDt} || '000000', 'YYYYMMDDHH24MISS') + INTERVAL '1' DAY");
    assertThat(xml)
        .doesNotContain("A.SEND_DT = TO_TIMESTAMP(#{sendDt} || '000000', 'YYYYMMDDHH24MISS')");
  }

  @Test
  void MapperXml은_비날짜_부등호도_CDATA로_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID
            FROM SMS_HISTORY A
            WHERE 1=1
            AND A.RETRY_CNT > $retry_cnt
            AND A.RESULT_CD <> $result_cd
            """);
    request.setOrderBy("A.SMS_HISTORY_ID DESC");
    ScaffoldModel optionModel =
        new ScaffoldModel(
            request,
            List.of("SMS_HISTORY_ID"),
            List.of("retryCnt", "resultCd"),
            Map.of("SMS_HISTORY_ID", "Long", "RETRY_CNT", "Integer", "RESULT_CD", "String"));

    // when
    String xml = render(MAPPER_XML, optionModel);

    // then
    assertThat(xml).contains("A.RETRY_CNT <![CDATA[ > ]]> #{retryCnt}");
    assertThat(xml).contains("A.RESULT_CD <![CDATA[ <> ]]> #{resultCd}");
  }

  @Test
  void 조회조건_옵션은_날짜범위와_콤보_라디오_기본값을_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setSearchParamOptions(
        List.of(
            searchOption("sendDtFrom", "DATE", "CURRENT_MONTH_TO_TODAY", null),
            searchOption("sendDtTo", "DATE", "CURRENT_MONTH_TO_TODAY", null),
            searchOption("sendType", "SELECT", "NONE", "SMS:SMS,LMS:LMS"),
            searchOption("sendStatus", "RADIO", "NONE", "S:성공,F:실패")));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String html = render(PAGE_HTML, optionModel);
    String js = render(PAGE_JS, optionModel);
    String xml = render(MAPPER_XML, optionModel);

    // then
    assertThat(html).contains("scaffold-search-card");
    assertThat(html).contains("scaffold-date-field scaffold-date-field-md");
    assertThat(html).contains("<input type=\"text\" id=\"sendDtFrom\" data-search-type=\"date\"");
    assertThat(html).contains("id=\"sendDtFromPickerLayer\" class=\"scaffold-date-picker-layer\"");
    assertThat(html).contains("<input type=\"text\" id=\"sendDtTo\" data-search-type=\"date\"");
    assertThat(html).contains("id=\"sendDtToPickerLayer\" class=\"scaffold-date-picker-layer\"");
    assertThat(html).doesNotContain("<input type=\"date\"");
    assertThat(html).contains("<select id=\"sendType\"");
    assertThat(html).contains("class=\"form-select scaffold-search-control\"");
    assertThat(html).contains("<option value=\"SMS\">SMS</option>");
    assertThat(html).contains("class=\"d-flex align-items-center gap-2 scaffold-radio-group\"");
    assertThat(html).contains("type=\"radio\" name=\"sendStatus\" value=\"S\">성공");

    assertThat(js).contains("searchInputs: ['sendDtFrom', 'sendDtTo', 'sendType', 'sendStatus']");
    assertThat(js)
        .contains(
            "searchDefaults: {sendDtFrom: 'CURRENT_MONTH_TO_TODAY', sendDtTo: 'CURRENT_MONTH_TO_TODAY'}");
    assertThat(js).doesNotContain("searchDatePickers");
    assertThat(js).doesNotContain("document.getElementById(`${id}PickerLayer`)");
    assertThat(js).doesNotContain("function readSearchValue");

    assertThat(xml)
        .contains(
            "A.SEND_DT <![CDATA[ >= ]]> TO_TIMESTAMP(#{sendDtFrom} || '000000', 'YYYYMMDDHH24MISS')");
    assertThat(xml)
        .contains(
            "A.SEND_DT <![CDATA[ < ]]> TO_TIMESTAMP(#{sendDtTo} || '000000', 'YYYYMMDDHH24MISS') + INTERVAL '1' DAY");
  }

  @Test
  void MapperXml은_baseQuery와_searchConditions를_분리한다() {
    // given : WHERE 1=1과 검색 조건이 있는 rawQuery. baseQuery는 SELECT/FROM, searchConditions는 WHERE 이하.
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("basic");
    request.setDomainId("notice");
    request.setDomainClass("Notice");
    request.setDomainName("공지사항");
    request.setRawQuery(
        """
            SELECT A.NOTICE_ID, A.TITLE
            FROM SMS.NOTICE A
            WHERE 1=1
              AND A.TITLE LIKE '%' || $search_keyword || '%'
            """);
    request.setOrderBy("A.NOTICE_ID DESC");
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("NOTICE_ID", "TITLE"),
            List.of("searchKeyword"),
            Map.of("NOTICE_ID", "Long", "TITLE", "String"));

    // when
    String xml = render(MAPPER_XML, model);

    // then 1 : baseQuery 블록은 SELECT/FROM만. WHERE와 <if>가 없어야 한다.
    int baseQueryStart = xml.indexOf("<sql id=\"baseQuery\">");
    int baseQueryEnd = xml.indexOf("</sql>", baseQueryStart);
    String baseQuery = xml.substring(baseQueryStart, baseQueryEnd);
    assertThat(baseQuery).contains("SELECT").contains("FROM SMS.NOTICE");
    assertThat(baseQuery).doesNotContain("WHERE");
    assertThat(baseQuery).doesNotContain("<if");

    // then 2 : searchConditions 블록은 <where>+<if>. 조건은 AND로 시작한다.
    int scStart = xml.indexOf("<sql id=\"searchConditions\">");
    int scEnd = xml.indexOf("</sql>", scStart);
    String searchConditions = xml.substring(scStart, scEnd);
    assertThat(searchConditions).contains("<where>");
    assertThat(searchConditions).contains("<if test=\"searchKeyword != null");
    assertThat(searchConditions).contains("AND A.TITLE LIKE '%' || #{searchKeyword} || '%'");

    // then 3 : count/selectList는 두 include를 모두 사용
    assertThat(xml).contains("<include refid=\"baseQuery\"/>");
    assertThat(xml).contains("<include refid=\"searchConditions\"/>");
  }

  @Test
  void MapperXml은_LEFT_JOIN_서브쿼리_내_variable을_baseQuery에서_제자리_파라미터화한다() {
    // given : LEFT JOIN 서브쿼리의 WHERE 안에 $variable이 있다 (depth>=1).
    // splitRawQuery는 depth=0의 메인 WHERE에서만 분리하므로 서브쿼리의 $variable은
    // baseQuery 파트에 그대로 남는다. 이것을 searchConditions로 빼지 않고 baseQuery 안에서
    // #{var}로 파라미터화 + <if> 가드로 제자리 감싸야 한다 (집계 의미론 보존).
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("customer-search");
    request.setDomainClass("CustomerSearch");
    request.setDomainName("고객별조회");
    request.setRawQuery(
        """
            SELECT T.ID, S.CNT
            FROM TBL T
            LEFT JOIN (
                SELECT H.ID, COUNT(*) AS CNT
                FROM HIST H
                WHERE 1=1
                AND H.COL >= TO_TIMESTAMP($start_var || '000000', 'YYYYMMDDHH24MISS')
                AND H.SEND_TYPE = $send_type
                AND H.ID >= $min_id
                GROUP BY H.ID
            ) S ON S.ID = T.ID
            WHERE 1=1
            AND (T.NAME = $outer_var OR T.NAME LIKE '%' || $outer_var || '%')
            """);
    request.setOrderBy("T.ID DESC");
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("ID", "CNT", "NAME"),
            List.of("startVar", "sendType", "minId", "outerVar"),
            Map.of(
                "ID", "Long",
                "CNT", "Integer",
                "NAME", "String",
                "COL", "LocalDateTime",
                "SEND_TYPE", "String"));

    // when
    String xml = render(MAPPER_XML, model);

    // then 1 : baseQuery 블록의 서브쿼리 $variable은 #{var}로 파라미터화되어 <if>로 감싸진다
    int baseQueryStart = xml.indexOf("<sql id=\"baseQuery\">");
    int baseQueryEnd = xml.indexOf("</sql>", baseQueryStart);
    String baseQuery = xml.substring(baseQueryStart, baseQueryEnd);
    assertThat(baseQuery)
        .contains(
            "<if test=\"startVar != null and startVar != ''\">AND H.COL &gt;= TO_TIMESTAMP(#{startVar} || '000000', 'YYYYMMDDHH24MISS')</if>");
    assertThat(baseQuery)
        .contains(
            "<if test=\"sendType != null and sendType != ''\">AND H.SEND_TYPE = #{sendType}</if>");
    assertThat(baseQuery)
        .contains(
            "<if test=\"minId != null and minId != ''\">AND H.ID <![CDATA[ >= ]]> #{minId}</if>");
    assertThat(baseQuery).doesNotContain("&lt;![CDATA[");

    // then 2 : baseQuery에는 raw $variable이 남으면 안 된다
    assertThat(baseQuery).doesNotContain("$start_var");
    assertThat(baseQuery).doesNotContain("$send_type");

    // then 3 : 메인 WHERE의 $outer_var는 기존대로 searchConditions로 분리된다 (변경 없음)
    int scStart = xml.indexOf("<sql id=\"searchConditions\">");
    int scEnd = xml.indexOf("</sql>", scStart);
    String searchConditions = xml.substring(scStart, scEnd);
    assertThat(searchConditions).contains("<if test=\"outerVar != null and outerVar != ''\">");
    assertThat(searchConditions)
        .contains("AND (A.NAME = #{outerVar} OR A.NAME LIKE '%' || #{outerVar} || '%')");
    assertThat(searchConditions)
        .doesNotContain("outerVar != null and outerVar != '' and outerVar != null");
    assertThat(searchConditions).doesNotContain("T.NAME");
    assertThat(baseQuery).doesNotContain("$outer_var");
    assertThat(baseQuery).doesNotContain("#{outerVar}");
  }

  @Test
  void MapperXml은_서브쿼리_조건절_밖의_variable을_명시적으로_거부한다() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("invalid-search");
    request.setDomainClass("InvalidSearch");
    request.setDomainName("잘못된조회");
    request.setRawQuery(
        """
            SELECT T.ID, $dynamic_value AS DYNAMIC_VALUE
            FROM TBL T
            WHERE 1=1
            """);
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("ID", "DYNAMIC_VALUE"),
            List.of("dynamicValue"),
            Map.of("ID", "Long", "DYNAMIC_VALUE", "String"));

    assertThatThrownBy(() -> render(MAPPER_XML, model))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("서브쿼리 검색조건");
  }

  @Test
  void MapperXml은_BETWEEN_TIMESTAMP_조건을_상하한_TO_TIMESTAMP로_변환한다() {
    // given : notice 등록일시 REG_DTTM을 startDate/endDate 범위로 조회
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("basic");
    request.setDomainId("notice");
    request.setDomainClass("Notice");
    request.setDomainName("공지사항");
    request.setRawQuery(
        """
            SELECT A.NOTICE_ID, A.REG_DTTM
            FROM SMS.NOTICE A
            WHERE 1=1
            AND A.REG_DTTM BETWEEN $start_date AND $end_date
            """);
    request.setOrderBy("A.REG_DTTM DESC, A.NOTICE_ID DESC");
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("NOTICE_ID", "REG_DTTM"),
            List.of("startDate", "endDate"),
            Map.of("NOTICE_ID", "Long", "REG_DTTM", "LocalDateTime"));

    // when
    String xml = render(MAPPER_XML, model);

    // then : 하한은 당일 자정, 상한은 다음 날 자정 미만인 반개방 범위
    assertThat(xml)
        .contains(
            "A.REG_DTTM <![CDATA[ >= ]]> TO_TIMESTAMP(#{startDate} || '000000', 'YYYYMMDDHH24MISS') AND A.REG_DTTM <![CDATA[ < ]]> TO_TIMESTAMP(#{endDate} || '000000', 'YYYYMMDDHH24MISS') + INTERVAL '1' DAY");
    assertThat(xml)
        .contains(
            "<if test=\"startDate != null and startDate != '' and endDate != null and endDate != ''\">");
    assertThat(xml).doesNotContain("$start_date");
    assertThat(xml).doesNotContain("$end_date");
  }

  @Test
  void MapperXml은_BETWEEN_LOCALDATE_조건을_TO_DATE_상하한으로_변환한다() {
    // given : notice 노출기간 START_DT(LocalDate)를 startDate/endDate로 조회
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("basic");
    request.setDomainId("notice");
    request.setDomainClass("Notice");
    request.setDomainName("공지사항");
    request.setRawQuery(
        """
            SELECT A.NOTICE_ID, A.START_DT
            FROM SMS.NOTICE A
            WHERE 1=1
            AND A.START_DT BETWEEN $start_date AND $end_date
            """);
    request.setOrderBy("A.START_DT DESC");
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("NOTICE_ID", "START_DT"),
            List.of("startDate", "endDate"),
            Map.of("NOTICE_ID", "Long", "START_DT", "LocalDate"));

    // when
    String xml = render(MAPPER_XML, model);

    // then : LocalDate도 종료일 다음 날 미만인 반개방 범위로 변환
    assertThat(xml)
        .contains(
            "A.START_DT <![CDATA[ >= ]]> TO_DATE(#{startDate}, 'YYYYMMDD') AND A.START_DT <![CDATA[ < ]]> TO_DATE(#{endDate}, 'YYYYMMDD') + INTERVAL '1' DAY");
    assertThat(xml).doesNotContain("$start_date");
    assertThat(xml).doesNotContain("$end_date");
  }

  @Test
  void 사용자_시나리오_notice_START_END_이름으로_FROM_TO_피커가_생성된다() {
    // given : 사용자가 scaffold UI에 입력한 그대로의 rawQuery. 변수는 snake_case.
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("basic");
    request.setDomainId("notice");
    request.setDomainClass("Notice");
    request.setDomainName("공지사항");
    request.setRawQuery(
        """
            SELECT A.NOTICE_ID, A.TITLE, A.NOTICE_TYPE, A.USE_YN, A.START_DT, A.END_DT, A.VIEW_CNT, A.REG_DTTM
            FROM SMS.NOTICE A
            WHERE 1=1
              AND A.TITLE LIKE '%' || $search_keyword || '%'
              AND A.NOTICE_TYPE = $notice_type
              AND A.USE_YN = $use_yn
              AND A.REG_DTTM BETWEEN $start_date AND $end_date
            """);
    request.setOrderBy("A.REG_DTTM DESC, A.NOTICE_ID DESC");

    // when : 실제 ScaffoldService와 동일 경로로 searchVars/columns 추출
    List<String> searchVars = QueryColumnExtractor.extractSearchVars(request.getRawQuery());
    List<String> columns = QueryColumnExtractor.extractColumns(request.getRawQuery());
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            columns,
            searchVars,
            Map.ofEntries(
                Map.entry("NOTICE_ID", "Long"),
                Map.entry("TITLE", "String"),
                Map.entry("NOTICE_TYPE", "String"),
                Map.entry("USE_YN", "String"),
                Map.entry("START_DT", "LocalDate"),
                Map.entry("END_DT", "LocalDate"),
                Map.entry("VIEW_CNT", "Integer"),
                Map.entry("REG_DTTM", "LocalDateTime")));

    // then 1 : 변수 추출이 startDate/endDate 짝으로 되어야 FROM-TO 매칭이 된다
    assertThat(searchVars)
        .containsExactly("searchKeyword", "noticeType", "useYn", "startDate", "endDate");

    // then 2 : HtmlTemplate는 start*/end* 규칙으로 FROM-TO picker 2개를 생성
    String html = render(PAGE_HTML, model);
    assertThat(html).contains("id=\"startDate\" data-search-type=\"date\"");
    assertThat(html).contains("id=\"endDate\" data-search-type=\"date\"");
    assertThat(html).contains("id=\"startDatePickerLayer\"");
    assertThat(html).contains("id=\"endDatePickerLayer\"");
    assertThat(html).doesNotContain("id=\"startDt\"");
    assertThat(html).containsOnlyOnce("class=\"col-auto scaffold-date-range-separator\"");
    assertThat(html.indexOf("id=\"startDate\""))
        .isLessThan(html.indexOf("scaffold-date-range-separator"));
    assertThat(html.indexOf("scaffold-date-range-separator"))
        .isLessThan(html.indexOf("id=\"endDate\""));

    // then 3 : DTO도 startDate/endDate 두 필드
    String dto = render(SEARCH_DTO, model);
    assertThat(dto).contains("private String startDate;");
    assertThat(dto).contains("private String endDate;");
    assertThat(dto).doesNotContain("private String startDt;");

    // then 4 : MapperXml는 BETWEEN을 TO_TIMESTAMP 범위로 변환
    String xml = render(MAPPER_XML, model);
    assertThat(xml)
        .contains(
            "A.REG_DTTM <![CDATA[ >= ]]> TO_TIMESTAMP(#{startDate} || '000000', 'YYYYMMDDHH24MISS') AND A.REG_DTTM <![CDATA[ < ]]> TO_TIMESTAMP(#{endDate} || '000000', 'YYYYMMDDHH24MISS') + INTERVAL '1' DAY");
  }

  @Test
  void 컬럼옵션은_표시형식과_서버_마스킹을_생성물에_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setColumnOptions(
        List.of(
            columnOption("SMS_HISTORY_ID", false, "이력ID", 120, "right", "NONE", "NONE"),
            columnOption("SEND_DT", true, "발송일시", 170, "center", "DATETIME", "NONE"),
            columnOption("RECEIVER_NO", true, "수신번호", 180, "left", "NONE", "PHONE")));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String js = render(PAGE_JS, optionModel);
    String service = render(SERVICE, optionModel);

    // then
    assertThat(js)
        .contains("header: '이력ID', name: 'smsHistoryId', align: 'right', width: 120, hidden: true");
    assertThat(js)
        .contains(
            "header: '발송일시', name: 'sendDt', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')");
    assertThat(js).contains("header: '수신번호', name: 'receiverNo', align: 'left', width: 180 }");
    assertThat(js).doesNotContain("TuiCommon.maskValue");
    assertThat(service).contains("vo.setReceiverNo(MaskingUtil.maskPhone(vo.getReceiverNo()));");
    assertThat(optionModel.maskingMethodName("EMAIL")).isEqualTo("maskEmail");
    assertThat(optionModel.maskingMethodName("BIRTH_DATE")).isEqualTo("maskBirthDate");
    assertThat(js).doesNotContain("function formatDate(value, pattern)");
    assertThat(js).doesNotContain("function maskValue(value, type)");
  }

  @Test
  void 시간타입_PK는_delete_파라미터에_java_time_import를_추가한다() {
    // given : LocalDateTime 컬럼이 복합 PK에 포함된다
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumns(List.of("SMS_HISTORY_ID", "SEND_DT"));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String controller = render(CONTROLLER, optionModel);
    String service = render(SERVICE, optionModel);
    String mapper = render(MAPPER_INTERFACE, optionModel);
    String serviceTest = render(SERVICE_TEST, optionModel);

    // then : delete 파라미터가 쓰는 LocalDateTime의 import가 있어야 컴파일된다
    assertThat(controller).contains("import java.time.LocalDateTime;");
    assertThat(controller)
        .contains("delete(@RequestParam Long smsHistoryId, @RequestParam LocalDateTime sendDt)");
    assertThat(service).contains("import java.time.LocalDateTime;");
    assertThat(mapper).contains("import java.time.LocalDateTime;");
    // ServiceTest는 now()가 아니라 안정적 리터럴을 써야 Mockito 검증이 일관된다
    assertThat(serviceTest)
        .contains("service.delete(1L, java.time.LocalDateTime.of(2020, 1, 1, 0, 0))");
    assertThat(serviceTest).doesNotContain("LocalDateTime.now()");
  }

  @Test
  void PK와_락컬럼_옵션은_CRUD_생성물에_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    ScaffoldModel optionModel = optionModel(request);

    // when
    String updateDto = render(UPDATE_DTO, optionModel);
    String mapper = render(MAPPER_INTERFACE, optionModel);
    String controller = render(CONTROLLER, optionModel);
    String xml = render(MAPPER_XML, optionModel);
    String serviceTest = render(SERVICE_TEST, optionModel);

    // then
    assertThat(updateDto).contains("private Long smsHistoryId;");
    assertThat(updateDto).contains("private LocalDateTime beforeUpdDttm;");
    assertThat(updateDto).doesNotContain("private String beforeUpdateDttm;");
    assertThat(mapper).contains("int delete(@Param(\"smsHistoryId\") Long smsHistoryId);");
    assertThat(controller).contains("delete(@RequestParam Long smsHistoryId)");
    assertThat(xml).contains("WHERE SMS_HISTORY_ID = #{smsHistoryId,jdbcType=NUMERIC}");
    assertThat(xml).contains("AND (UPD_DTTM = #{beforeUpdDttm,jdbcType=TIMESTAMP}");
    assertThat(serviceTest).contains("service.delete(1L)");
  }

  @Test
  void 복합_PK는_DTO_Mapper_Controller_JS_WHERE에_모두_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumns(List.of("SMS_HISTORY_ID", "SEND_TYPE"));
    request.setLockColumn("UPD_DTTM");
    ScaffoldModel optionModel = optionModel(request);

    // when
    String updateDto = render(UPDATE_DTO, optionModel);
    String mapper = render(MAPPER_INTERFACE, optionModel);
    String controller = render(CONTROLLER, optionModel);
    String xml = render(MAPPER_XML, optionModel);

    // then
    assertThat(updateDto).contains("private Long smsHistoryId;");
    assertThat(updateDto).contains("private String sendType;");
    assertThat(mapper)
        .contains(
            "int delete(@Param(\"smsHistoryId\") Long smsHistoryId, @Param(\"sendType\") String sendType);");
    assertThat(controller)
        .contains("delete(@RequestParam Long smsHistoryId, @RequestParam String sendType)");
    assertThat(xml).contains("WHERE SMS_HISTORY_ID = #{smsHistoryId,jdbcType=NUMERIC}");
    assertThat(xml).contains("AND SEND_TYPE = #{sendType,jdbcType=VARCHAR}");
  }

  @Test
  void 낙관적_잠금_컬럼은_PK로_선택할_수_없다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("SMS_HISTORY_ID");
    ScaffoldModel optionModel = optionModel(request);

    // when / then
    assertThatThrownBy(() -> render(MAPPER_XML, optionModel))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must not be a PK column");
  }

  @Test
  void DB_PLATFORM_POSTGRES는_Postgres_페이징과_시간_표현식을_생성한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_DT, A.SEND_TYPE, A.SEND_STATUS, A.RECEIVER_NO, A.UPD_DTTM
            FROM SMS_HISTORY A
            WHERE 1=1
            AND A.SEND_DT = $send_dt
            """);
    ScaffoldModel optionModel =
        new ScaffoldModel(
            request,
            List.of(
                "SMS_HISTORY_ID", "SEND_DT", "SEND_TYPE", "SEND_STATUS", "RECEIVER_NO", "UPD_DTTM"),
            List.of("sendDt"),
            Map.of(
                "SMS_HISTORY_ID", "Long",
                "SEND_DT", "LocalDateTime",
                "SEND_TYPE", "String",
                "SEND_STATUS", "String",
                "RECEIVER_NO", "String",
                "UPD_DTTM", "LocalDateTime"),
            ScaffoldDialect.POSTGRES);

    // when
    String xml = render(MAPPER_XML, optionModel);

    // then
    assertThat(xml).contains("OFFSET #{offset} LIMIT #{size}");
    assertThat(xml).contains("UPD_DTTM = CURRENT_TIMESTAMP");
    assertThat(xml).contains("+ INTERVAL '1 day'");
  }

  @Test
  void editable_컬럼만_UpdateDTO와_Mapper_SET과_모달_input으로_생성한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    request.setColumnOptions(
        List.of(
            columnOption("SEND_TYPE", true, true, true, "발송유형", 120, "center", "NONE", "NONE"),
            columnOption("SEND_STATUS", true, true, false, "발송상태", 120, "center", "NONE", "NONE"),
            columnOption("RECEIVER_NO", true, false, true, "수신번호", 160, "left", "NONE", "PHONE"),
            columnOption(
                "UPD_DTTM", false, false, true, "수정일시", 160, "center", "DATETIME", "NONE")));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String updateDto = render(UPDATE_DTO, optionModel);
    String xml = render(MAPPER_XML, optionModel);

    // then
    assertThat(updateDto).contains("private String sendType;");
    assertThat(updateDto).contains("private String receiverNo;");
    assertThat(updateDto).doesNotContain("private String sendStatus;");
    assertThat(updateDto).doesNotContain("private LocalDateTime updDttm;");

    assertThat(xml).contains("SEND_TYPE = #{sendType,jdbcType=VARCHAR}");
    assertThat(xml).contains("RECEIVER_NO = #{receiverNo,jdbcType=VARCHAR}");
    assertThat(xml).contains("INSERT INTO SMS_HISTORY");
    assertThat(xml).contains("SEND_TYPE");
    assertThat(xml).contains("#{sendType,jdbcType=VARCHAR}");
    assertThat(xml).doesNotContain("               SEND_STATUS = #{sendStatus,jdbcType=VARCHAR}");
    assertThat(xml).doesNotContain("UPD_DTTM = #{updDttm}");
  }

  @Test
  void 메뉴옵션은_SQL에_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    ScaffoldMenuOptionDTO menuOption = new ScaffoldMenuOptionDTO();
    menuOption.setMenuId("SMS_HISTORY");
    menuOption.setParentMenuId("G_SMS_SEARCH");
    menuOption.setRoleCode("ROLE_ADMIN");
    menuOption.setSortOrd(10);
    request.setMenuOption(menuOption);
    ScaffoldModel optionModel = optionModel(request);

    // when
    String sql = render(MENU_SQL, optionModel);

    // then
    assertThat(sql).contains("'SMS_HISTORY', 'G_SMS_SEARCH', '발송이력조회', '/sms/history'");
    assertThat(sql).contains("2, 10, 'M'");
    assertThat(sql).contains("'SMS_HISTORY', 'ROLE_ADMIN'");
  }

  @Test
  void 화면모드는_엑셀_CRUD_생성범위를_나눈다() {
    // given
    ScaffoldRequestDTO excelRequest = requestWithOptions();
    excelRequest.setScreenMode("EXCEL");
    ScaffoldRequestDTO crudRequest = requestWithOptions();
    crudRequest.setScreenMode("CRUD");
    crudRequest.setPkColumn("SMS_HISTORY_ID");
    crudRequest.setLockColumn("UPD_DTTM");
    ScaffoldRequestDTO listRequest = requestWithOptions();
    listRequest.setScreenMode("LIST");

    // when
    ScaffoldModel excelModel = optionModel(excelRequest);
    ScaffoldModel crudModel = optionModel(crudRequest);
    ScaffoldModel listModel = optionModel(listRequest);

    // then
    Map<String, String> excelFiles = renderSelected(excelModel, PAGE_HTML, PAGE_JS);
    assertThat(excelFiles.get("history.html")).contains("id=\"btn-excel\"");
    assertThat(excelFiles.get("history.js")).contains("PAGE_AUTH.download");
    assertThat(render(CONTROLLER, excelModel)).contains("@GetMapping(\"/excel\")");
    assertThat(render(CONTROLLER, excelModel)).doesNotContain("@PostMapping(\"/create\")");

    Map<String, String> crudFiles = renderSelected(crudModel, PAGE_HTML, PAGE_JS);
    assertThat(crudFiles.get("history.html")).contains("id=\"btn-create\"");
    assertThat(crudFiles.get("history.html")).contains("fragments/modal-base :: layout");
    assertThat(crudFiles.get("history.js")).contains("API.create");
    assertThat(crudFiles.get("history.js")).contains("API.update");
    assertThat(crudFiles.get("history.js")).contains("API.delete");
    assertThat(crudFiles.get("history.js")).contains("ModalManager.init");

    Map<String, String> listFiles = renderSelected(listModel, PAGE_HTML, PAGE_JS);
    assertThat(listFiles.get("history.html")).doesNotContain("id=\"btn-create\"");
    assertThat(listFiles.get("history.html")).doesNotContain("fragments/modal-base");
  }

  @Test
  void 제거된_DETAIL_화면모드는_명시적으로_거부한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("DETAIL");
    ScaffoldModel model = optionModel(request);

    // when / then
    assertThatThrownBy(() -> renderSelected(model, PAGE_HTML, PAGE_JS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 screenMode입니다: DETAIL")
        .hasMessageContaining("(지원: LIST, EXCEL, CRUD)");
  }

  @Test
  void 제거된_CRUD_PANEL_화면모드는_명시적으로_거부한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD_PANEL");
    ScaffoldModel model = optionModel(request);

    // when / then
    assertThatThrownBy(() -> renderSelected(model, PAGE_HTML, PAGE_JS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 screenMode입니다: CRUD_PANEL")
        .hasMessageContaining("지원: LIST, EXCEL, CRUD");
  }

  @Test
  void HttpClient는_axios_인터셉터와_CSRF를_처리하고_defaultLayout에서_notify_뒤_common_utils_앞에_로드된다()
      throws Exception {
    String httpClient =
        Files.readString(Path.of("src/main/resources/static/js/common/http-client.js"));
    String layout = Files.readString(Path.of("src/main/resources/templates/defaultLayout.html"));

    assertThat(httpClient).contains("axios.interceptors.request");
    assertThat(httpClient).contains("axios.interceptors.response");
    assertThat(httpClient).contains("_csrf");
    assertThat(httpClient).contains("window.HttpClient");
    assertThat(httpClient).contains("window.ApiClient");

    assertThat(layout.indexOf("/js/common/notify.js"))
        .isLessThan(layout.indexOf("/js/common/http-client.js"));
    assertThat(layout.indexOf("/js/common/http-client.js"))
        .isLessThan(layout.indexOf("/js/common/common-utils.js"));
  }

  @Test
  void betweenDateParamsFromRawQueryRenderOneRangeSeparator() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("basic");
    request.setDomainId("notice");
    request.setDomainClass("Notice");
    request.setDomainName("공지사항");
    request.setRawQuery(
        """
            SELECT NOTICE_ID, TITLE, CONTENT, USE_YN, START_DT
            FROM NOTICE
            WHERE START_DT BETWEEN $startDT AND $endDT
            """);
    request.setOrderBy("NOTICE_ID");
    List<String> searchVars = QueryColumnExtractor.extractSearchVars(request.getRawQuery());
    ScaffoldModel model =
        new ScaffoldModel(
            request,
            List.of("NOTICE_ID", "TITLE", "CONTENT", "USE_YN", "START_DT"),
            searchVars,
            Map.of("NOTICE_ID", "Long", "START_DT", "LocalDate"));

    String html = render(PAGE_HTML, model);

    assertThat(searchVars).containsExactly("startdt", "enddt");
    assertThat(model.searchParams())
        .extracting(ScaffoldModel.SearchParam::isBetweenRangeEnd)
        .containsExactly(false, true);
    assertThat(html).containsOnlyOnce("class=\"col-auto scaffold-date-range-separator\"");
    assertThat(html.indexOf("id=\"startdt\""))
        .isLessThan(html.indexOf("scaffold-date-range-separator"));
    assertThat(html.indexOf("scaffold-date-range-separator"))
        .isLessThan(html.indexOf("id=\"enddt\""));
  }

  private ScaffoldRequestDTO requestWithOptions() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        """
            SELECT A.SMS_HISTORY_ID, A.SEND_DT, A.SEND_TYPE, A.SEND_STATUS, A.RECEIVER_NO, A.UPD_DTTM
            FROM SMS_HISTORY A
            WHERE 1=1
            AND A.SEND_DT >= $send_dt_from
            AND A.SEND_DT <= $send_dt_to
            AND A.SEND_TYPE = $send_type
            AND A.SEND_STATUS = $send_status
            """);
    request.setOrderBy("A.SEND_DT DESC, A.SMS_HISTORY_ID DESC");
    return request;
  }

  private ScaffoldModel optionModel(ScaffoldRequestDTO request) {
    return new ScaffoldModel(
        request,
        List.of("SMS_HISTORY_ID", "SEND_DT", "SEND_TYPE", "SEND_STATUS", "RECEIVER_NO", "UPD_DTTM"),
        List.of("sendDtFrom", "sendDtTo", "sendType", "sendStatus"),
        Map.of(
            "SMS_HISTORY_ID", "Long",
            "SEND_DT", "LocalDateTime",
            "SEND_TYPE", "String",
            "SEND_STATUS", "String",
            "RECEIVER_NO", "String",
            "UPD_DTTM", "LocalDateTime"));
  }

  private ScaffoldSearchParamOptionDTO searchOption(
      String name, String inputType, String defaultValue, String optionsText) {
    ScaffoldSearchParamOptionDTO option = new ScaffoldSearchParamOptionDTO();
    option.setName(name);
    option.setInputType(inputType);
    option.setDefaultValue(defaultValue);
    option.setOptionsText(optionsText);
    return option;
  }

  private ScaffoldColumnOptionDTO columnOption(
      String columnName,
      boolean visible,
      String headerName,
      int width,
      String align,
      String dateFormat,
      String maskType) {
    return columnOption(
        columnName, visible, true, false, headerName, width, align, dateFormat, maskType);
  }

  private ScaffoldColumnOptionDTO columnOption(
      String columnName,
      boolean visible,
      boolean modalVisible,
      boolean editable,
      String headerName,
      int width,
      String align,
      String dateFormat,
      String maskType) {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName(columnName);
    option.setVisible(visible);
    option.setModalVisible(modalVisible);
    option.setEditable(editable);
    option.setHeaderName(headerName);
    option.setWidth(width);
    option.setAlign(align);
    option.setDateFormat(dateFormat);
    option.setMaskType(maskType);
    return option;
  }

  @Test
  void ColumnConfig는_optionsText를_구조화된_옵션으로_변환한다() {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName("STATUS");
    option.setOptionsText("SUCCESS:성공,FAIL:실패,WAIT:대기");
    ScaffoldRequestDTO request = basicRequest();
    request.setColumnOptions(List.of(option));

    ScaffoldModel model =
        new ScaffoldModel(request, List.of("STATUS"), List.of(), Map.of("STATUS", "String"));

    ScaffoldModel.ColumnConfig config = model.columnConfigs().get(0);
    assertThat(config.hasOptions()).isTrue();
    assertThat(config.options())
        .containsExactly(
            new ScaffoldModel.SelectOption("SUCCESS", "성공"),
            new ScaffoldModel.SelectOption("FAIL", "실패"),
            new ScaffoldModel.SelectOption("WAIT", "대기"));
  }

  @Test
  void ColumnConfig는_optionsText_작은따옴표를_이스케이프한다() {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName("STATUS");
    option.setOptionsText("USER_INPUT:User's input");
    ScaffoldRequestDTO request = basicRequest();
    request.setColumnOptions(List.of(option));

    ScaffoldModel model =
        new ScaffoldModel(request, List.of("STATUS"), List.of(), Map.of("STATUS", "String"));

    String js = render(PAGE_JS, model);

    assertThat(js).contains("USER_INPUT: 'User\\'s input'");
  }

  @Test
  void JsTemplate는_optionsText_컬럼에_badgeByValue_formatter를_생성한다() {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName("STATUS");
    option.setOptionsText("SUCCESS:성공,FAIL:실패,WAIT:대기");
    ScaffoldRequestDTO request = basicRequest();
    request.setColumnOptions(List.of(option));
    ScaffoldModel model =
        new ScaffoldModel(request, List.of("STATUS"), List.of(), Map.of("STATUS", "String"));

    String js = render(PAGE_JS, model);

    assertThat(js)
        .contains(
            "formatter: TuiCommon.badgeByValue({ labels: { SUCCESS: '성공', FAIL: '실패', WAIT: '대기' } })");
  }

  @Test
  void JsTemplate는_키워드_dateFormat은_여전히_대문자_정규화한다() {
    ScaffoldColumnOptionDTO option = new ScaffoldColumnOptionDTO();
    option.setColumnName("SENT_AT");
    option.setDateFormat("datetime");
    ScaffoldRequestDTO request = basicRequest();
    request.setColumnOptions(List.of(option));
    ScaffoldModel model =
        new ScaffoldModel(
            request, List.of("SENT_AT"), List.of(), Map.of("SENT_AT", "LocalDateTime"));

    String js = render(PAGE_JS, model);

    assertThat(js).contains("TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')");
  }

  private ScaffoldRequestDTO basicRequest() {
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery("SELECT A.STATUS FROM DUAL A WHERE 1=1");
    request.setOrderBy("A.STATUS");
    return request;
  }

  @Test
  void LIST는_검색과_그리드만_생성하고_패널이나_모달을_만들지_않는다() {
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("LIST");
    ScaffoldModel model = optionModel(request);

    Map<String, String> files = renderSelected(model, PAGE_HTML, PAGE_JS);
    String html = files.get("history.html");
    String js = files.get("history.js");

    assertThat(model.screenMode()).isEqualTo("LIST");
    assertThat(html).contains("layout:decorate=\"~{defaultLayout}\"");
    assertThat(html).contains("th:replace=\"~{fragments/toast-grid :: gridCard}\"");
    assertThat(html).contains("id=\"btn-search\"");
    assertThat(html).contains("id=\"btn-reset\"");
    assertThat(html).doesNotContain("id=\"btn-create\"");
    assertThat(html).doesNotContain("id=\"detail-panel\"");
    assertThat(html).doesNotContain("id=\"btn-excel\"");
    assertThat(html).doesNotContain("tui-auto-modal");

    assertThat(js).contains("new TuiPageBuilder({");
    assertThat(js).contains("apiUrl: '/sms/history/data'");
    assertThat(js).contains("searchInputs: ['sendDtFrom', 'sendDtTo', 'sendType', 'sendStatus']");
    assertThat(js).doesNotContain("ApiClient.post");
    assertThat(js).doesNotContain("ApiClient.remove");
    assertThat(js).doesNotContain("autoModal");
    assertThat(js).doesNotContain("modalActions");
    assertThat(js).doesNotContain("btn-excel");
  }

  @Test
  void EXCEL은_엑셀_버튼과_다운로드_JS를_생성한다() {
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("EXCEL");
    ScaffoldModel model = optionModel(request);

    Map<String, String> files = renderSelected(model, PAGE_HTML, PAGE_JS);
    String html = files.get("history.html");
    String js = files.get("history.js");

    assertThat(model.screenMode()).isEqualTo("EXCEL");
    assertThat(html).contains("layout:decorate=\"~{defaultLayout}\"");
    assertThat(html).contains("th:replace=\"~{fragments/toast-grid :: gridCard}\"");
    assertThat(html).contains("id=\"btn-search\"");
    assertThat(html).contains("id=\"btn-excel\"");
    assertThat(html).contains("th:if=\"${pageAuth.download}\"");
    assertThat(html).doesNotContain("id=\"btn-create\"");
    assertThat(html).doesNotContain("id=\"detail-panel\"");
    assertThat(html).doesNotContain("tui-auto-modal");

    assertThat(js).contains("new TuiPageBuilder({");
    assertThat(js).contains("apiUrl: '/sms/history/data'");
    assertThat(js).contains("API.excel");
    assertThat(js).contains("'/sms/history/excel'");
    assertThat(js).contains("window.location.href");
    assertThat(js).contains("PAGE_AUTH.download");
    assertThat(js).doesNotContain("ApiClient.post");
    assertThat(js).doesNotContain("autoModal");
    assertThat(js).doesNotContain("modalActions");
  }

  @Test
  void EXCEL_JS는_PAGE_AUTH_누락시_다운로드를_차단하는_fail_closed_가드를_사용한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("EXCEL");
    ScaffoldModel model = optionModel(request);

    // when
    Map<String, String> files = renderSelected(model, PAGE_HTML, PAGE_JS);
    String js = files.get("history.js");

    // then : fail-open 패턴(window.PAGE_AUTH && ...)이 없어야 한다
    assertThat(js).doesNotContain("window.PAGE_AUTH && window.PAGE_AUTH.download !== true");
    // then : fail-closed 패턴(!window.PAGE_AUTH || ...)이 있어야 한다
    assertThat(js).contains("!window.PAGE_AUTH || window.PAGE_AUTH.download !== true");
  }

  @Test
  void CRUD는_모달_fragment와_편집_폼과_저장삭제_버튼을_생성한다() {
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    ScaffoldModel model = optionModel(request);

    Map<String, String> files = renderSelected(model, PAGE_HTML, PAGE_JS);
    String html = files.get("history.html");
    String js = files.get("history.js");

    assertThat(model.screenMode()).isEqualTo("CRUD");
    assertThat(html).contains("layout:decorate=\"~{defaultLayout}\"");
    assertThat(html).contains("th:replace=\"~{fragments/toast-grid :: gridCard}\"");
    assertThat(html).contains("fragments/modal-base :: layout");
    assertThat(html).contains("modalId='history-modal'");
    assertThat(html).contains("bodyContent=~{::#modal-body}");
    assertThat(html).contains("id=\"modal-body\"");
    assertThat(html).contains("id=\"detail-form\"");
    assertThat(html).contains("name=\"smsHistoryId\"");
    assertThat(html).contains("name=\"beforeUpdDttm\"");
    assertThat(html).contains("id=\"btn-create\"");
    assertThat(html).contains("th:if=\"${pageAuth.create}\"");
    assertThat(html).contains("id=\"f-sendType\"").doesNotContain("id=\"f-smsHistoryId\"");
    assertThat(html).doesNotContain("id=\"detail-panel\"");
    assertThat(html).doesNotContain("tui-auto-modal");

    assertThat(js).contains("new TuiPageBuilder({");
    assertThat(js).contains("apiUrl: '/sms/history/data'");
    assertThat(js).contains("MODAL_ID = 'history-modal'");
    assertThat(js).contains("ModalManager.init(MODAL_ID");
    assertThat(js).contains("ModalManager.open(MODAL_ID)");
    assertThat(js).contains("ModalManager.close(MODAL_ID)");
    assertThat(js).contains("ApiClient.post(API.create, payload)");
    assertThat(js).contains("ApiClient.post(API.update, payload)");
    assertThat(js).contains("ApiClient.remove(API.delete, pkParams())");
    assertThat(js).contains("FormBinder.bind('#detail-form', row)");
    assertThat(js).contains("FormBinder.toObject('#detail-form')");
    assertThat(js).contains("FieldFormat.validateForm(form)");
    assertThat(js).contains("FieldFormat.applyFieldFormats");
    assertThat(js).contains("state.mode === 'create' && auth.create === true");
    assertThat(js).contains("state.mode === 'update' && auth.update === true");
    assertThat(js).contains("DEFAULT_FORM =");
    assertThat(js).contains("PK_FIELDS =");
    assertThat(js).contains("LOCK =");
    assertThat(js).doesNotContain("autoModal");
    assertThat(js).doesNotContain("modalActions");
    assertThat(js).doesNotContain("JustValidate");
  }

  // === Task 7: 명시적 screenMode 가 legacy feature flag 보다 우선한다 ===

  @Test
  void 명시적_LIST_화면모드는_모순된_includeCreateUpdate_참을_무시하고_순수_LIST로_정규화한다() {
    // given : screenMode=LIST 와 legacy includeCreateUpdate=true 가 충돌
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("LIST");
    request.setIncludeCreateUpdate(true); // legacy CRUD 단서 - 무시되어야 한다
    request.setPkColumn("SMS_HISTORY_ID");
    ScaffoldModel model = optionModel(request);

    // when / then : 명시적 screenMode 가 권위를 가진다
    assertThat(model.screenMode()).isEqualTo("LIST");
    assertThat(model.includeCreateUpdate()).isFalse();
    assertThat(model.includeExcel()).isFalse();

    // 산출물 : CRUD 엔드포인트/UpdateRequestDTO 가 생성되지 않는다
    String controller = render(CONTROLLER, model);
    assertThat(controller).doesNotContain("@PostMapping(\"/create\")");
    assertThat(controller).doesNotContain("@PostMapping(\"/update\")");
    assertThat(controller).doesNotContain("UpdateRequestDTO");
    assertThat(ScaffoldArtifactRenderer.renderAll(model))
        .doesNotContainKey("SmsHistoryUpdateRequestDTO.java");

    // 메뉴 권한 : LIST 는 CREATE/UPDATE/DELETE/DOWNLOAD 모두 N
    String menu = render(MENU_SQL, model);
    assertThat(menu).contains("'Y', 'N', 'N', 'N',"); // CAN_READ, CREATE, UPDATE, DELETE
    assertThat(menu).contains("'N', 'N', 'N', 'N',"); // APPROVE, CANCEL, DOWNLOAD, MASK
  }

  @Test
  void 명시적_CRUD_화면모드는_모순된_includeExcel_참을_무시하고_엑셀_산출물을_만들지_않는다() {
    // given : screenMode=CRUD 와 legacy includeExcel=true 가 충돌
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setIncludeExcel(true); // legacy 엑셀 단서 - 무시되어야 한다
    request.setPkColumn("SMS_HISTORY_ID");
    request.setLockColumn("UPD_DTTM");
    ScaffoldModel model = optionModel(request);

    // when / then : 명시적 CRUD 가 권위 - legacy excel 은 무시된다
    assertThat(model.screenMode()).isEqualTo("CRUD");
    assertThat(model.includeCreateUpdate()).isTrue();
    assertThat(model.includeExcel()).isFalse();

    // 산출물 : CRUD 엔드포인트는 유지되되 엑셀 엔드포인트/의존성은 빠진다
    String controller = render(CONTROLLER, model);
    assertThat(controller).contains("@PostMapping(\"/create\")");
    assertThat(controller).doesNotContain("@GetMapping(\"/excel\")");
    assertThat(controller).doesNotContain("HttpServletResponse");
    assertThat(render(SERVICE, model)).doesNotContain("ExcelUtil");
    assertThat(render(MAPPER_INTERFACE, model)).doesNotContain("excel");

    // 메뉴 권한 : CRUD → CREATE/UPDATE/DELETE=Y, DOWNLOAD=N
    String menu = render(MENU_SQL, model);
    assertThat(menu).contains("'Y', 'Y', 'Y', 'Y',"); // CAN_READ, CREATE, UPDATE, DELETE
    assertThat(menu).contains("'N', 'N', 'N', 'N',"); // APPROVE, CANCEL, DOWNLOAD, MASK
  }

  @Test
  void 빈_screenMode는_legacy_boolean_플래그를_그대로_반영한다() {
    // given : screenMode 미지정 + legacy includeCreateUpdate=true (하위호환)
    ScaffoldRequestDTO crudRequest = requestWithOptions();
    crudRequest.setScreenMode(null);
    crudRequest.setIncludeCreateUpdate(true);
    crudRequest.setIncludeExcel(false);
    crudRequest.setPkColumn("SMS_HISTORY_ID");
    crudRequest.setLockColumn("UPD_DTTM");
    ScaffoldModel crudModel = optionModel(crudRequest);

    // when / then : legacy fallback 이 그대로 동작한다 (CRUD)
    assertThat(crudModel.screenMode()).isEqualTo("CRUD");
    assertThat(crudModel.includeCreateUpdate()).isTrue();
    assertThat(crudModel.includeExcel()).isFalse();

    // given : screenMode 공백 + legacy includeExcel=true (하위호환)
    ScaffoldRequestDTO excelRequest = requestWithOptions();
    excelRequest.setScreenMode("   ");
    excelRequest.setIncludeCreateUpdate(false);
    excelRequest.setIncludeExcel(true);
    ScaffoldModel excelModel = optionModel(excelRequest);

    // when / then : legacy fallback 이 그대로 동작한다 (EXCEL)
    assertThat(excelModel.screenMode()).isEqualTo("EXCEL");
    assertThat(excelModel.includeExcel()).isTrue();
    assertThat(excelModel.includeCreateUpdate()).isFalse();
  }

  // === showRowNumber: 그리드 No 표시 옵션 ===

  @Test
  void DTO_showRowNumber_기본값은_true이다() {
    // given / when
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();

    // then
    assertThat(request.isShowRowNumber()).isTrue();
  }

  @Test
  void DTO_showRowNumber_명시적_false는_false를_유지한다() {
    // given / when
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setShowRowNumber(false);

    // then
    assertThat(request.isShowRowNumber()).isFalse();
  }

  @Test
  void scaffold_UI_그리드_No_체크박스가_존재하고_체크되어있다() throws Exception {
    // given / when
    String html = Files.readString(Path.of("src/main/resources/templates/system/scaffold.html"));

    // then
    assertThat(html).contains("id=\"showRowNumber\"");
    assertThat(html).contains("checked");
    assertThat(html).contains("그리드 No 표시");
  }

  @Test
  void scaffold_JS_buildRequest에_showRowNumber를_직렬화한다() throws Exception {
    // given / when
    String js = Files.readString(Path.of("src/main/resources/static/js/system/scaffold.js"));

    // then
    assertThat(js).contains("showRowNumber: document.querySelector('#showRowNumber').checked");
  }

  @Test
  void LIST_JS는_기본값_그리드_No를_생성한다() {
    // given
    ScaffoldModel m = model(false, false, false);

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: ['rowNum']");
  }

  @Test
  void LIST_JS_showRowNumber_false는_빈_배열을_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setShowRowNumber(false);
    ScaffoldModel m =
        new ScaffoldModel(
            request,
            List.of("SEND_DT", "RECEIVER_NO"),
            List.of("startDt"),
            Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: []");
    assertThat(js).doesNotContain("rowHeaders: ['rowNum']");
  }

  @Test
  void EXCEL_JS는_기본값_그리드_No를_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setIncludeExcel(true);
    ScaffoldModel m =
        new ScaffoldModel(
            request,
            List.of("SEND_DT", "RECEIVER_NO"),
            List.of("startDt"),
            Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: ['rowNum']");
  }

  @Test
  void EXCEL_JS_showRowNumber_false는_빈_배열을_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setIncludeExcel(true);
    request.setShowRowNumber(false);
    ScaffoldModel m =
        new ScaffoldModel(
            request,
            List.of("SEND_DT", "RECEIVER_NO"),
            List.of("startDt"),
            Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: []");
    assertThat(js).doesNotContain("rowHeaders: ['rowNum']");
  }

  @Test
  void CRUD_JS는_기본값_그리드_No를_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setIncludeCreateUpdate(true);
    request.setPkColumn("RECEIVER_NO");
    ScaffoldModel m =
        new ScaffoldModel(
            request,
            List.of("SEND_DT", "RECEIVER_NO"),
            List.of("startDt"),
            Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: ['rowNum']");
  }

  @Test
  void CRUD_JS_showRowNumber_false는_빈_배열을_생성한다() {
    // given
    ScaffoldRequestDTO request = new ScaffoldRequestDTO();
    request.setModuleName("sms");
    request.setDomainId("history");
    request.setDomainClass("SmsHistory");
    request.setDomainName("발송이력조회");
    request.setRawQuery(
        "SELECT A.SEND_DT, A.RECEIVER_NO FROM SMS_HISTORY A WHERE 1=1\nAND A.SEND_DT >= $start_dt");
    request.setOrderBy("A.SEND_DT DESC, A.HIST_ID DESC");
    request.setIncludeCreateUpdate(true);
    request.setPkColumn("RECEIVER_NO");
    request.setShowRowNumber(false);
    ScaffoldModel m =
        new ScaffoldModel(
            request,
            List.of("SEND_DT", "RECEIVER_NO"),
            List.of("startDt"),
            Map.of("SEND_DT", "LocalDate", "RECEIVER_NO", "String"));

    // when
    String js = render(PAGE_JS, m);

    // then
    assertThat(js).contains("rowHeaders: []");
    assertThat(js).doesNotContain("rowHeaders: ['rowNum']");
  }

  @Test
  void old_case_JSON_without_showRowNumber_field_deserializes_to_true() throws Exception {
    // given : Jackson ObjectMapper로 JSON 파싱 — showRowNumber 필드가 없는 경우
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        new com.fasterxml.jackson.databind.ObjectMapper();
    String jsonWithoutField =
        """
        {
          "moduleName": "sms",
          "domainId": "history",
          "domainClass": "SmsHistory",
          "domainName": "발송이력조회",
          "rawQuery": "SELECT A.ID FROM T A",
          "orderBy": "A.ID"
        }
        """;

    // when
    ScaffoldRequestDTO request = mapper.readValue(jsonWithoutField, ScaffoldRequestDTO.class);

    // then : 기본값 true
    assertThat(request.isShowRowNumber()).isTrue();
  }
}
