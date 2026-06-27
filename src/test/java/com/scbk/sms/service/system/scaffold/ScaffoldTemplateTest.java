package com.scbk.sms.service.system.scaffold;

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

  private static final int GOLDEN_SCAFFOLD_HASH = -131864380;

  @Test
  void 드리프트_게이트_스캐폴드_출력_변경_감지() {
    ScaffoldModel m = model(true, true, true);
    StringBuilder all = new StringBuilder();
    all.append(DtoTemplate.generate(m));
    all.append(UpdateRequestDtoTemplate.generate(m));
    all.append(VoTemplate.generate(m));
    all.append(MapperInterfaceTemplate.generate(m));
    all.append(MapperXmlTemplate.generate(m));
    all.append(ServiceTemplate.generate(m));
    all.append(ControllerTemplate.generate(m));
    all.append(ServiceTestTemplate.generate(m));
    all.append(ControllerTestTemplate.generate(m));
    all.append(HtmlTemplate.generate(m));
    all.append(JsTemplate.generate(m));
    all.append(MenuSqlTemplate.generate(m));
    assertThat(all.toString().hashCode())
        .as("스캐폴드 출력이 변경됨. 의도적이면 GOLDEN_SCAFFOLD_HASH 갱신.")
        .isEqualTo(GOLDEN_SCAFFOLD_HASH);
  }

  @Test
  void DTO는_PageRequestDTO를_상속하고_Lombok_기반으로_생성한다() {
    // when
    String code = DtoTemplate.generate(model(false, false, false));

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
    String code = VoTemplate.generate(model(false, false, false));

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
    String serviceCode = ServiceTemplate.generate(model(false, false, false));
    String controllerCode = ControllerTemplate.generate(model(false, false, false));

    // then
    assertThat(serviceCode).contains("@RequiredArgsConstructor");
    assertThat(serviceCode).doesNotContain("public SmsHistoryService(");
    assertThat(controllerCode).contains("@RequiredArgsConstructor");
    assertThat(controllerCode).doesNotContain("public SmsHistoryController(");
  }

  @Test
  void Controller는_create와_update를_분리하고_save를_만들지_않는다() {
    // when
    String code = ControllerTemplate.generate(model(true, false, false));

    // then
    assertThat(code).contains("@PostMapping(\"/create\")");
    assertThat(code).contains("@PostMapping(\"/update\")");
    assertThat(code).doesNotContain("/save");
  }

  @Test
  void 수정_요청은_화이트리스트_DTO로만_받는다() {
    // when
    String controllerCode = ControllerTemplate.generate(model(true, false, false));
    String updateDtoCode = UpdateRequestDtoTemplate.generate(model(true, false, false));
    String serviceCode = ServiceTemplate.generate(model(true, false, false));
    String xmlCode = MapperXmlTemplate.generate(model(true, false, false));

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
    String code = ControllerTemplate.generate(model(false, true, true));

    // then
    assertThat(code).contains("@PrivacyLog(action = \"발송이력조회 목록 조회\")");
    assertThat(code).contains("@PrivacyLog(action = \"발송이력조회 엑셀 다운로드\")");
  }

  @Test
  void MapperXml은_입력받은_정렬과_OFFSET_FETCH를_사용한다() {
    // when
    String xml = MapperXmlTemplate.generate(model(false, false, false));

    // then
    assertThat(xml).contains("ORDER BY A.SEND_DT DESC, A.HIST_ID DESC");
    assertThat(xml).contains("OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY");
    assertThat(xml).contains("<if test=\"startDt != null and startDt != ''\">");
  }

  @Test
  void 메뉴SQL은_v3_스키마를_사용한다() {
    // when
    String sql = MenuSqlTemplate.generate(model(false, false, false));

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
    String controllerCode = ControllerTemplate.generate(model);
    String htmlCode = HtmlTemplate.generate(model);

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
    String code = ServiceTestTemplate.generate(model(true, false, false));

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
    String code = ControllerTestTemplate.generate(model(false, false, false));

    // then
    assertThat(code).contains("MockMvcBuilders.standaloneSetup");
    assertThat(code).contains("get(\"/sms/history/data\")");
    assertThat(code).contains("jsonPath(\"$.code\").value(200)");
  }

  @Test
  void ControllerTest는_LIST_화면이면_create_update_delete_테스트를_만들지_않는다() {
    // when
    String code = ControllerTestTemplate.generate(model(false, false, false));

    // then : CUD가 없는 화면은 create/update/delete 엔드포인트도, then(service) 검증도 없다
    assertThat(code).doesNotContain("/create");
    assertThat(code).doesNotContain("/update");
    assertThat(code).doesNotContain("/delete");
    assertThat(code).doesNotContain("then(service)");
  }

  @Test
  void ControllerTest는_CRUD이면_create_update_delete_성공_메시지를_검증한다() {
    // when
    String code = ControllerTestTemplate.generate(model(true, false, false));

    // then : 성공 메시지는 ApiResponse.data가 아니라 message 필드에 담긴다
    assertThat(code).contains("post(\"/sms/history/create\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"등록되었습니다.\")");
    assertThat(code).contains("then(service).should().create(any())");

    assertThat(code).contains("post(\"/sms/history/update\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"수정되었습니다.\")");
    assertThat(code).contains("then(service).should().update(any())");

    assertThat(code).contains("post(\"/sms/history/delete\")");
    assertThat(code).contains(".param(\"receiverNo\", \"1\")");
    assertThat(code).contains("jsonPath(\"$.message\").value(\"삭제되었습니다.\")");
    assertThat(code).contains("then(service).should().delete(\"1\")");
  }

  @Test
  void HTML은_screen_convention_골격을_따른다() {
    // when
    String html = HtmlTemplate.generate(model(false, true, false));

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
    String html = HtmlTemplate.generate(model);

    // then
    assertThat(html).contains("id=\"sendType\" class=\"form-control scaffold-search-control\"");
    assertThat(html).contains("<input type=\"text\" id=\"sendType\"");
    assertThat(html).doesNotContain("<input type=\"date\" id=\"sendType\"");
  }

  @Test
  void JS는_TuiPageBuilder로_그리드를_초기화한다() {
    // when
    String js = JsTemplate.generate(model(false, false, false));

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
  void TuiPageBuilder는_PAGE_AUTH로_모달_수정삭제를_제어한다() throws Exception {
    // when
    String builder =
        Files.readString(Path.of("src/main/resources/static/js/common/tui-page-builder.js"));

    // then
    assertThat(builder).contains("_hasPagePermission('update')");
    assertThat(builder).contains("_hasPagePermission('delete')");
    assertThat(builder).contains("window.PAGE_AUTH[permission] === true");
    assertThat(builder).contains("return false;");
  }

  @Test
  void JS는_날짜_타입_컬럼에_공통_포매터를_부착한다() {
    // when : SEND_DT는 LocalDate, RECEIVER_NO는 String
    String js = JsTemplate.generate(model(false, false, false));

    // then
    assertThat(js)
        .contains(
            "name: 'sendDt', align: 'center', width: 150, editable: true, formatter: TuiCommon.fmt.date");
    assertThat(js).contains("name: 'receiverNo', align: 'center', width: 150, editable: true }");
  }

  @Test
  void MapperXml은_날짜_비교_가이드_주석을_포함한다() {
    // when
    String xml = MapperXmlTemplate.generate(model(false, false, false));

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
    String xml = MapperXmlTemplate.generate(dateModel);

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
    String xml = MapperXmlTemplate.generate(optionModel);

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
    String html = HtmlTemplate.generate(optionModel);
    String js = JsTemplate.generate(optionModel);
    String xml = MapperXmlTemplate.generate(optionModel);

    // then
    assertThat(html).contains("scaffold-search-card");
    assertThat(html).contains("scaffold-date-field scaffold-date-field-sm");
    assertThat(html).contains("<input type=\"text\" id=\"sendDtFrom\" data-search-type=\"date\"");
    assertThat(html).contains("id=\"sendDtFromPickerLayer\" class=\"scaffold-date-picker-layer\"");
    assertThat(html).contains("<span>~</span>");
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
            "A.SEND_DT <![CDATA[ <= ]]> TO_TIMESTAMP(#{sendDtTo} || '235959', 'YYYYMMDDHH24MISS')");
  }

  @Test
  void 컬럼옵션은_숨김_헤더_너비_정렬_날짜포맷_마스킹을_JS에_반영한다() {
    // given
    ScaffoldRequestDTO request = requestWithOptions();
    request.setColumnOptions(
        List.of(
            columnOption("SMS_HISTORY_ID", false, "이력ID", 120, "right", "NONE", "NONE"),
            columnOption("SEND_DT", true, "발송일시", 170, "center", "DATETIME", "NONE"),
            columnOption("RECEIVER_NO", true, "수신번호", 180, "left", "NONE", "PHONE")));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String js = JsTemplate.generate(optionModel);

    // then
    assertThat(js)
        .contains("header: '이력ID', name: 'smsHistoryId', align: 'right', width: 120, hidden: true");
    assertThat(js)
        .contains(
            "header: '발송일시', name: 'sendDt', align: 'center', width: 170, formatter: ({ value }) => TuiCommon.formatDate(value, 'YYYY-MM-DD HH:mm')");
    assertThat(js)
        .contains(
            "header: '수신번호', name: 'receiverNo', align: 'left', width: 180, formatter: ({ value }) => TuiCommon.maskValue(value, 'PHONE')");
    assertThat(js).doesNotContain("function formatDate(value, pattern)");
    assertThat(js).doesNotContain("function maskValue(value, type)");
  }

  @Test
  void 컬럼옵션의_입력마스크와_검증은_JS_컬럼에_조건부로_반영한다() {
    // given : RECEIVER_NO에만 inputMask/validate 지정
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumn("SMS_HISTORY_ID");
    ScaffoldColumnOptionDTO phone = new ScaffoldColumnOptionDTO();
    phone.setColumnName("RECEIVER_NO");
    phone.setEditable(true);
    phone.setHeaderName("수신번호");
    phone.setWidth(160);
    phone.setAlign("left");
    phone.setInputMask("phone");
    phone.setValidate("required|phone");
    request.setColumnOptions(List.of(phone));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String js = JsTemplate.generate(optionModel);

    // then : 지정한 컬럼만 inputMask/validate를 가진다
    assertThat(js)
        .contains(
            "name: 'receiverNo', align: 'left', width: 160, editable: true, inputMask: 'phone', validate: 'required|phone'");
    // 옵션 없는 컬럼에는 출력되지 않는다
    assertThat(js)
        .doesNotContain(
            "name: 'sendType', align: 'center', width: 150, editable: true, inputMask:");
  }

  @Test
  void 시간타입_PK는_delete_파라미터에_java_time_import를_추가한다() {
    // given : LocalDateTime 컬럼이 복합 PK에 포함된다
    ScaffoldRequestDTO request = requestWithOptions();
    request.setScreenMode("CRUD");
    request.setPkColumns(List.of("SMS_HISTORY_ID", "SEND_DT"));
    ScaffoldModel optionModel = optionModel(request);

    // when
    String controller = ControllerTemplate.generate(optionModel);
    String service = ServiceTemplate.generate(optionModel);
    String mapper = MapperInterfaceTemplate.generate(optionModel);
    String serviceTest = ServiceTestTemplate.generate(optionModel);

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
    String updateDto = UpdateRequestDtoTemplate.generate(optionModel);
    String mapper = MapperInterfaceTemplate.generate(optionModel);
    String controller = ControllerTemplate.generate(optionModel);
    String xml = MapperXmlTemplate.generate(optionModel);
    String serviceTest = ServiceTestTemplate.generate(optionModel);

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
    String updateDto = UpdateRequestDtoTemplate.generate(optionModel);
    String mapper = MapperInterfaceTemplate.generate(optionModel);
    String controller = ControllerTemplate.generate(optionModel);
    String xml = MapperXmlTemplate.generate(optionModel);
    String js = JsTemplate.generate(optionModel);

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
    assertThat(js).contains("pkFields: ['smsHistoryId', 'sendType']");
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
    assertThatThrownBy(() -> MapperXmlTemplate.generate(optionModel))
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
    String xml = MapperXmlTemplate.generate(optionModel);

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
    String updateDto = UpdateRequestDtoTemplate.generate(optionModel);
    String xml = MapperXmlTemplate.generate(optionModel);
    String js = JsTemplate.generate(optionModel);

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

    assertThat(js).contains("name: 'sendType', align: 'center', width: 120, editable: true");
    assertThat(js).contains("name: 'sendStatus', align: 'center', width: 120");
    assertThat(js)
        .doesNotContain("name: 'sendStatus', align: 'center', width: 120, editable: true");
    assertThat(js)
        .contains(
            "name: 'receiverNo', align: 'left', width: 160, modalVisible: false, editable: true");
    assertThat(js)
        .contains(
            "name: 'updDttm', align: 'center', width: 160, hidden: true, modalVisible: false");
    assertThat(js)
        .doesNotContain(
            "name: 'updDttm', align: 'center', width: 160, hidden: true, modalVisible: false, editable: true");
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
    String sql = MenuSqlTemplate.generate(optionModel);

    // then
    assertThat(sql).contains("'SMS_HISTORY', 'G_SMS_SEARCH', '발송이력조회', '/sms/history'");
    assertThat(sql).contains("2, 10, 'M'");
    assertThat(sql).contains("'SMS_HISTORY', 'ROLE_ADMIN'");
  }

  @Test
  void 화면모드는_엑셀_상세_CRUD_생성범위를_나눈다() {
    // given
    ScaffoldRequestDTO excelRequest = requestWithOptions();
    excelRequest.setScreenMode("EXCEL");
    ScaffoldRequestDTO detailRequest = requestWithOptions();
    detailRequest.setScreenMode("DETAIL");
    ScaffoldRequestDTO crudRequest = requestWithOptions();
    crudRequest.setScreenMode("CRUD");
    crudRequest.setPkColumn("SMS_HISTORY_ID");
    crudRequest.setLockColumn("UPD_DTTM");
    ScaffoldRequestDTO listWithModalRequest = requestWithOptions();
    listWithModalRequest.setScreenMode("LIST");
    listWithModalRequest.setIncludeModal(true);

    // when
    ScaffoldModel excelModel = optionModel(excelRequest);
    ScaffoldModel detailModel = optionModel(detailRequest);
    ScaffoldModel crudModel = optionModel(crudRequest);
    ScaffoldModel listWithModalModel = optionModel(listWithModalRequest);

    // then
    assertThat(HtmlTemplate.generate(excelModel)).contains("id=\"btn-excel\"");
    assertThat(JsTemplate.generate(excelModel)).contains("window.PAGE_AUTH.download !== true");
    assertThat(ControllerTemplate.generate(excelModel)).contains("@GetMapping(\"/excel\")");
    assertThat(ControllerTemplate.generate(excelModel)).doesNotContain("@PostMapping(\"/create\")");

    assertThat(JsTemplate.generate(detailModel)).contains("autoModal: true");
    assertThat(JsTemplate.generate(detailModel)).doesNotContain("modalActions:");
    assertThat(HtmlTemplate.generate(detailModel)).doesNotContain("id=\"btn-excel\"");
    assertThat(ControllerTemplate.generate(detailModel))
        .doesNotContain("@PostMapping(\"/create\")");

    assertThat(JsTemplate.generate(crudModel)).contains("autoModal: true");
    assertThat(JsTemplate.generate(crudModel)).contains("modalActions:");
    assertThat(JsTemplate.generate(crudModel)).contains("createUrl: '/sms/history/create'");
    assertThat(JsTemplate.generate(crudModel)).contains("updateUrl: '/sms/history/update'");
    assertThat(JsTemplate.generate(crudModel)).contains("deleteUrl: '/sms/history/delete'");
    assertThat(JsTemplate.generate(crudModel)).contains("pkFields: ['smsHistoryId']");
    assertThat(JsTemplate.generate(crudModel)).contains("beforeLockField: 'beforeUpdDttm'");
    assertThat(HtmlTemplate.generate(crudModel)).contains("id=\"btn-create\"");

    assertThat(JsTemplate.generate(listWithModalModel)).contains("autoModal: true");
    assertThat(JsTemplate.generate(listWithModalModel)).doesNotContain("modalActions:");
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
}
