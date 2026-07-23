package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * notice window.open 팝업이 실제 동작 계약을 만족하는지 정적 리소스를 스캔해 검증한다. 대상: notice-popup.html, notice-popup.js,
 * notice.js, NoticeMapper.xml. JS 테스트 러너가 없으므로 리소스 파일 내용으로 계약을 고정한다.
 */
class NoticePopupContractTest {

  private static final Path RESOURCES = Path.of("src", "main", "resources");

  @Test
  void 팝업_화면은_레이아웃의_script_프래그먼트를_사용한다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));

    assertThat(html)
        .as("defaultLayout이 선언한 프래그먼트는 script(단수)다. scripts(복수)면 JS가 주입되지 않는다.")
        .contains("layout:fragment=\"script\"")
        .doesNotContain("layout:fragment=\"scripts\"");
  }

  @Test
  void 목록_조회는_CONTENT를_프로젝션하지_않고_상세만_가져온다() throws IOException {
    String xml = read(RESOURCES.resolve("mapper/basic/NoticeMapper.xml"));
    String baseQuery = block(xml, "<sql id=\"baseQuery\">", "</sql>");
    String selectDetail = block(xml, "<select id=\"selectDetail\"", "</select>");

    assertThat(baseQuery)
        .as("CLOB CONTENT는 페이지 목록/baseQuery에서 제외한다 (mybatis-oracle.md, 무제한 인출 금지).")
        .doesNotContain("CONTENT");
    assertThat(selectDetail)
        .as("selectDetail은 CONTENT를 명시적으로 조회해 상세 본문을 반환한다.")
        .contains("CONTENT");
  }

  @Test
  void 팝업은_ApiClient_언래핑_계약에_맞게_데이터를_다룬다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "http-client.js 응답 인터셉터가 ApiResponse를 언래핑해 get/post는 알맹이(data)를 반환한다. "
                + "따라서 res.data 재언래핑과 res.code 성공 검사는 항상 실패한다.")
        .doesNotContain("res.data")
        .doesNotContain("res.code");
  }

  @Test
  void 팝업은_native_date값을_ISO_로컬_datetime으로_변환해_전송한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "DTO startDt/endDt는 LocalDateTime이므로 <input type=date>의 YYYY-MM-DD를 "
                + "ISO 로컬 datetime(YYYY-MM-DDT00:00:00)으로 변환해야 Jackson 바인딩이 된다.")
        .contains("T00:00:00");
  }

  @Test
  void 팝업은_postMessage_수신처를_현재_origin으로_제한한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as("postMessage targetOrigin은 window.location.origin으로 제한한다 (와일드카드 '*' 금지).")
        .contains("postMessage(")
        .contains("window.location.origin")
        .doesNotContain("'*')");
  }

  @Test
  void 부모_창은_발신_origin을_검증한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(js)
        .as("message 수신側은 event.origin이 window.location.origin인지 검증해야 한다.")
        .contains("ev.origin")
        .contains("window.location.origin");
  }

  @Test
  void 부모는_operation별_구분된_한글_성공_toast를_표시한다() throws IOException {
    String popupJs = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(popupJs).as("팝업은 공통 HTTP 오류 알림과 중복되는 alert를 직접 표시하지 않는다.").doesNotContain("alert(");
    assertThat(parentJs)
        .as(
            "created/updated/deleted operation 각각 등록/수정/삭제 한글 메시지로 매핑한다. "
                + "삭제를 '저장되었습니다.'로 보고하는 구형 결함(MINOR-1)이 재발하면 실패한다.")
        .contains("등록되었습니다.")
        .contains("수정되었습니다.")
        .contains("삭제되었습니다.");
  }

  @Test
  void 팝업은_삭제_버튼을_가지며_생성_모드에서는_숨긴다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(html).as("상세/수정 전용 삭제 버튼(btn-delete)이 마크업에 존재해야 한다.").contains("btn-delete");
    assertThat(js)
        .as("생성 모드(isCreateMode)에서는 삭제 버튼을 숨긴다(생성 모드에서 삭제 노출 금지).")
        .contains("isCreateMode")
        .contains("btn-delete");
  }

  @Test
  void 삭제는_존재하는_noticeId로_기존_delete_엔드포인트를_Apiclient_remove로_호출한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "삭제는 기존 /basic/notice/delete 엔드포인트를 ApiClient.remove 로 호출하며 "
                + "Controller @RequestParam 과 일치하는 noticeId 파라미터를 전달한다 "
                + "(백엔드 삭제 로직을 중복 구현하지 않는다).")
        .contains("/basic/notice/delete")
        .contains("ApiClient.remove")
        .contains("noticeId");
  }

  @Test
  void 삭제_확인은_프로젝트_공통_modal_confirm을_쓰고_브라우저_confirm을_쓰지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as("삭제 확인은 CommonUtils.confirm(프로젝트 모달)을 사용한다. 브라우저 window.confirm 은 금지.")
        .contains("CommonUtils.confirm")
        .doesNotContain("window.confirm");
  }

  @Test
  void 빈_noticeId로는_삭제_API를_호출하지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    String removeFn = functionBody(js, "removeNotice");
    assertThat(removeFn).as("removeNotice 함수 본문이 존재해야 한다.").contains("noticeId");

    int guardReturn = removeFn.indexOf("return");
    int confirm = removeFn.indexOf("CommonUtils.confirm");
    int deleteCall = removeFn.indexOf("ApiClient.remove");
    assertThat(guardReturn)
        .as("noticeId 가 blank 일 때 API 호출 전 early return 이 존재해야 한다 (방어).")
        .isGreaterThan(-1);
    assertThat(confirm)
        .as("blank-id early return 이 confirm 보다 먼저 와야 한다.")
        .isGreaterThan(guardReturn);
    assertThat(deleteCall)
        .as("delete API 호출은 confirm 콜백 안에서, early return 보다 뒤에 있어야 한다.")
        .isGreaterThan(confirm);
  }

  @Test
  void create_update_delete_성공은_중립_변경_메시지로_부모에_한번알린다() throws IOException {
    String popupJs = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(popupJs)
        .as(
            "create/update/delete 성공 후 부모로 보내는 postMessage action은 저장에 국한되지 않는 "
                + "중립 이벤트 noticeChanged 이다 (save-specific noticeSaved 는 제거).")
        .contains("noticeChanged")
        .doesNotContain("noticeSaved");
    assertThat(parentJs)
        .as("부모 수신侧도 동일한 중립 이벤트 noticeChanged 만 수신한다 (송/수신 원자 변경).")
        .contains("noticeChanged")
        .doesNotContain("noticeSaved");
  }

  @Test
  void 부모_수신측은_와일드카드_origin을_허용하지_않는다() throws IOException {
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(parentJs)
        .as("부모 message 리스너는 와일드카드 '*' targetOrigin/origin 허용을 두지 않는다.")
        .doesNotContain("'*')");
  }

  @Test
  void 팝업_화면은_부모_메뉴_권한을_JS객체로_노출한다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));

    assertThat(html)
        .as(
            "팝업은 서버가 정규화한 부모 메뉴 pageAuth(create/update/delete)를 JS로 노출해 "
                + "런타임 모드에 따라 버튼을 독립 게이트한다(MAJOR-2).")
        .contains("${pageAuth.create}")
        .contains("${pageAuth.update}")
        .contains("${pageAuth.delete}");
  }

  @Test
  void 팝업은_모드와_권한에_따라_저장_삭제_버튼을_독립적으로_제어한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "생성 모드 저장은 create, 수정 모드 저장은 update, 삭제는 delete 권한으로 각각 독립 게이트한다. "
                + "pageAuth.none 사용자가 파괴/편집 버튼을 본 채로 상호작용에 실패하는 결함(MAJOR-2)을 막는다.")
        .contains("noticePageAuth")
        .contains("applyMode")
        .contains("pageAuth.create")
        .contains("pageAuth.update")
        .contains("pageAuth.delete");
  }

  @Test
  void 팝업은_성공_종류를_bounded_operation으로_부모에_전달한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as(
            "create/update/delete 성공을 중립 action(noticeChanged)과 함께 bounded operation "
                + "(created|updated|deleted)으로 부모에 전달한다(MINOR-1).")
        .contains("noticeChanged")
        .contains("created")
        .contains("updated")
        .contains("deleted")
        .doesNotContain("noticeSaved");
  }

  @Test
  void 부모는_발신_source가_열린_팝업_핸들일때만_수락한다() throws IOException {
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    assertThat(parentJs)
        .as(
            "부모는 window.open 으로 연 팝업 핸들을 추적하고 ev.source 가 그 핸들일 때만 수락한다 "
                + "(같은 origin 임의 프레임의 갱신/허위 토스트 차단, MINOR-3).")
        .contains("window.open")
        .contains("openedPopups")
        .contains("ev.source");
  }

  @Test
  void 부모는_알수없는_operation은_갱신_또는_toast_하지_않는다() throws IOException {
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));

    // Todo 5: notice.js 리스너는 refreshCurrentPage 래퍼로 갱신을 중앙화한다. 모달 저장/삭제 경로와
    // 메시지 수신 경로가 같은 래퍼를 쓰므로 파일 전체의 첫 searchData 는 리스너 밖에 있다.
    // 계약 의미(리스너 본문의 갱신은 whitelist 통과 후에만 실행)를 유지하기 위해
    // 리스너 시작점 이후의 whitelist/refresh 순서와 직접 searchData 호출 부재를 검증한다.
    int listenerStart = parentJs.indexOf("addEventListener('message'");
    int whitelist = parentJs.indexOf("VALID_OPERATIONS.has", listenerStart);
    int refreshInListener = parentJs.indexOf("refreshCurrentPage", listenerStart);
    int searchDataInListener = parentJs.indexOf("searchData", listenerStart);
    assertThat(listenerStart).as("message 리스너가 존재해야 한다.").isGreaterThan(-1);
    assertThat(whitelist)
        .as("operation 은 명시적 Set 화이트리스트(VALID_OPERATIONS.has)로 검증해야 한다.")
        .isGreaterThan(listenerStart);
    assertThat(refreshInListener)
        .as("갱신(refreshCurrentPage)은 화이트리스트 검사보다 뒤에 있어야 한다 (미지정 operation은 갱신 안 함).")
        .isGreaterThan(whitelist);
    // 래퍼 정의 자체는 파일 어디에나 있을 수 있지만, 리스너 본문이 searchData 를 직접
    // 건드리지 않고 래퍼로만 갱신한다(중앙화된 에러 처리).
    assertThat(searchDataInListener)
        .as("리스너 본문은 searchData 를 직접 부르지 않고 refreshCurrentPage 로 감싼다.")
        .isEqualTo(-1);
  }

  @Test
  void 프로토타입_키와_미지정_operation은_화이트리스트에_없다() throws IOException {
    String parentJs = read(RESOURCES.resolve("static/js/basic/notice.js"));
    String setLiteral = block(parentJs, "new Set([", "])");

    assertThat(setLiteral)
        .as("operation 화이트리스트는 new Set([...]) 선언이어야 한다 (일반 객체 직접 참조 금지).")
        .isNotEmpty();
    assertThat(setLiteral)
        .as("화이트리스트는 bounded operation created/updated/deleted 세 값만 포함한다.")
        .contains("'created'")
        .contains("'updated'")
        .contains("'deleted'");
    for (String banned :
        new String[] {"constructor", "toString", "__proto__", "valueOf", "saved"}) {
      assertThat(setLiteral)
          .as(
              "프로토타입 키/임의 미지정값 '%s' 는 Set.has() 가 false 인 화이트리스트 원소여야 한다 "
                  + "(일반 객체 속성 조회가 프로토타입 키를 truthy 로 평가해 갱신/토스트를 우회하는 결함 방어).",
              banned)
          .doesNotContain("'" + banned + "'");
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 baseline (v2 invariants): noticePageAuth name / bounded operations / mode-specific
  // emission.
  // These already hold on canonical v2 and pin the v2 contract against accidental v1 drift.
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업은_noticePageAuth_객체이름을_유지하고_PAGE_AUTH를_도입하지_않는다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(html)
        .as("v2 계약: 팝업은 window.noticePageAuth 객체로 권한을 노출한다 (v1 PAGE_AUTH가 아님).")
        .contains("window.noticePageAuth")
        .doesNotContain("window.PAGE_AUTH");
    assertThat(js)
        .as("v2 계약: JS는 noticePageAuth 만 읽는다. v1 PAGE_AUTH 리터럴을 도입하면 안 된다.")
        .contains("noticePageAuth")
        .doesNotContain("PAGE_AUTH");
  }

  @Test
  void 팝업은_save_단일_엔드포인트나_엑셀_기능을_두지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js).as("/save 단일 endpoint 사용 금지 (/create, /update 분리 원칙).").doesNotContain("/save");
    assertThat(js.toLowerCase()).as("엑셀 다운로드 기능은 팝업 범위 밖이다.").doesNotContain("excel");
    assertThat(js).as("와일드카드 postMessage '*' targetOrigin 사용 금지 (재확인).").doesNotContain("'*')");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: 팝업 전용 크롬 숨김(fail-safe popup view styling).
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업은_앱_크롬을_숨기는_css_프래그먼트를_정확히_하나_가진다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));

    assertThat(numberOfOccurrences(html, "layout:fragment=\"css\""))
        .as("popup-only css 프래그먼트는 정확히 하나여야 한다 (defaultLayout의 css 슬롯 1:1 매핑).")
        .isEqualTo(1);

    String cssFragment = fragment(html, "css");
    assertThat(cssFragment)
        .as("popup 뷰에서 사이드바와 헤더(앱 크롬)를 숨긴다. 공통 레이아웃 파일은 수정하지 않는다.")
        .contains(".sidebar")
        .contains(".header")
        .contains("display: none");
    assertThat(cssFragment)
        .as("콘텐츠 영역이 팝업 전체 폭을 쓰도록 한다 (sidebar/header 숨김 후에도 세로 스크롤 확보).")
        .contains("content-area");
  }

  @Test
  void 팝업_css_프래그먼트는_원색이나_인라인_스타일을_두지_않는다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));
    String cssFragment = fragment(html, "css");

    assertThat(cssFragment).as("css 프래그먼트가 존재해야 속성 검증이 의미를 갖는다.").isNotEmpty();
    assertThat(HEX_COLOR.matcher(cssFragment).find())
        .as("css 프래그먼트는 raw hex 색을 쓰지 않는다 (DESIGN.md: --sms-* 토큰만 허용).")
        .isFalse();
    assertThat(cssFragment).as("popup 전용 규칙은 @import 없이 토큰 기반으로 자급자족한다.").doesNotContain("@import");
    assertThat(inlineStyledHex(html)).as("본문에도 인라인 style=\"...#hex...\" 색 선언을 두지 않는다.").isFalse();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: fail-closed missing-auth (보존: noticePageAuth 이름).
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업은_missing_auth를_fail_closed_기본false로_폴백한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String fallback = block(js, "window.noticePageAuth ||", "\n");

    assertThat(fallback)
        .as(
            "defaultLayout이 noticePageAuth 를 주입하지 않은 경우 빈 객체 폴백이 아니라 "
                + "명시적으로 { create: false, update: false, delete: false } 로 폴백해야 "
                + "이후 pageAuth.create/update/delete 접근이 falsy 가 되어 쓰기가 차단된다 (fail-closed).")
        .isNotEmpty();
    assertThat(fallback)
        .as("fail-closed 폴백 객체는 create/update/delete 모두 false.")
        .contains("create: false")
        .contains("update: false")
        .contains("delete: false");
    assertThat(fallback)
        .as("허용적(true) 폴백은 fail-open 이므로 금지 — 누락 권한을 true 로 승격하지 않는다.")
        .doesNotContain("create: true")
        .doesNotContain("update: true")
        .doesNotContain("delete: true");
  }

  @Test
  void 팝업_save는_모드별_noticePageAuth_권한을_명시적으로_검사한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String saveFn = methodBody(js, "async function save()");

    assertThat(saveFn).as("save 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(saveFn)
        .as(
            "CREATE 모드 저장은 pageAuth.create, UPDATE 모드 저장은 pageAuth.update 로 명시적으로 가드한다 "
                + "(applyMode 우회 우려 방어 — 버튼이 보여도 권한 없으면 차단).")
        .contains("isCreateMode")
        .contains("pageAuth.create")
        .contains("pageAuth.update");
  }

  @Test
  void 팝업_delete는_noticePageAuth_delete_권한을_명시적으로_검사한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String removeFn = methodBody(js, "function removeNotice()");

    assertThat(removeFn).as("removeNotice 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(removeFn)
        .as("삭제 핸들러도 pageAuth.delete 로 명시적 가드한다 (applyMode 우회 방어).")
        .contains("pageAuth.delete");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: safe button binding (Thymeleaf th:if 누락 대비 null 가드).
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업은_버튼을_직접_addEventListener_없이_null_가드로_바인딩한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    boolean hasDirectChain = DIRECT_ADD_EVENT_LISTENER.matcher(js).find();
    assertThat(hasDirectChain)
        .as(
            "btn-save/btn-delete 는 Thymeleaf th:if 권한 가드로 렌더링되지 않을 수 있다. "
                + "getElementById(...).addEventListener 직접 체인은 그 경우 null 참조 오류를 낸다 "
                + "(null 가드 헬퍼 사용 강제).")
        .isFalse();
    assertThat(js).as("btn-delete 요소 참조는 존재한다 (Thymeleaf 가드 후에도 안전 바인딩).").contains("btn-delete");
  }

  @Test
  void 팝업_applyMode는_버튼이_누락되어도_직접_속성참조로_죽지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String applyMode = methodBody(js, "function applyMode()");

    assertThat(applyMode).as("applyMode 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(DIRECT_BUTTON_ATTR.matcher(applyMode).find())
        .as(
            "btn-save/btn-delete 는 Thymeleaf th:if 권한 가드로 누락될 수 있다. "
                + "getElementById(...).disabled/.style 직접 체인은 null 참조 오류를 낸다 (누락 버튼 안전).")
        .isFalse();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: detail-load failure write block (fail-closed 부분 폼 보호).
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업_loadDetail은_HTTP_실패시_detailLoadFailed_플래그를_세운다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String loadDetail = methodBody(js, "async function loadDetail(noticeId)");

    assertThat(loadDetail).as("loadDetail 함수 본문이 존재해야 한다.").isNotEmpty();
    int getIdx = loadDetail.indexOf("ApiClient.get");
    int httpCatchIdx = loadDetail.indexOf("detailLoadFailed", getIdx);
    assertThat(httpCatchIdx)
        .as(
            "HTTP 실패(ApiClient.get reject) 시 detailLoadFailed=true 를 세워 applyMode 가 "
                + "save/delete 를 fail-closed 하게 만든다 (부분 폼 저장 방지).")
        .isGreaterThan(getIdx);
    assertThat(loadDetail.indexOf("applyMode()", httpCatchIdx))
        .as("HTTP 실패 분기 후 applyMode() 를 호출해 버튼 상태를 갱신한다.")
        .isGreaterThan(httpCatchIdx);
  }

  @Test
  void 팝업_loadDetail은_바인딩_실패시_detailLoadFailed_플래그를_세운다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String loadDetail = methodBody(js, "async function loadDetail(noticeId)");

    int fillFormIdx = loadDetail.indexOf("fillForm(");
    assertThat(fillFormIdx).as("loadDetail 은 detail 응답을 fillForm 으로 바인딩한다.").isGreaterThan(-1);
    int bindingCatchIdx = loadDetail.indexOf("detailLoadFailed", fillFormIdx);
    assertThat(bindingCatchIdx)
        .as(
            "fillForm() 호출이 throw 한(바인딩 실패) 경우 detailLoadFailed=true 를 세운다 "
                + "(HTTP 200 이라도 폼이 온전하지 않으면 저장·삭제 차단).")
        .isGreaterThan(fillFormIdx);
  }

  @Test
  void 팝업_fillForm은_바인딩_오류를_삼키지_않고_던진다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String fillFormFn = methodBody(js, "function fillForm(data)");

    assertThat(fillFormFn).as("fillForm 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(fillFormFn)
        .as(
            "fillForm 이 DOM/에디터 바인딩 오류를 try/catch 로 삼키면 loadDetail 이 실패를 감지하지 못해 "
                + "빈/부분 폼으로 저장·삭제할 수 있다. fillForm 은 오류를 그대로 위로 던진다.")
        .doesNotContain("catch (err)");
    assertThat(fillFormFn)
        .as("fillForm 은 여전히 폼 필드에 값을 채운다 (noticeId/title/...).")
        .contains("noticeId")
        .contains("title");
  }

  @Test
  void 팝업_loadDetail은_HTTP_실패를_인터셉터에_위임해_로컬_경고를_중복_표시하지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String loadDetail = methodBody(js, "async function loadDetail(noticeId)");

    java.util.regex.Matcher m = CATCH_BLOCK.matcher(loadDetail);
    assertThat(m.find()).as("loadDetail 에 최소 한 개의 catch 블록이 존재해야 한다.");
    String httpCatchBody = m.group(1);
    assertThat(httpCatchBody)
        .as(
            "HTTP 실패 catch(인터셉터가 이미 중앙 에러 다이얼로그를 띄움) 는 CommonUtils.toast "
                + "중복 알림을 하지 않는다: "
                + httpCatchBody)
        .doesNotContain("CommonUtils.toast");
  }

  @Test
  void 팝업_loadDetail은_바인딩_실패시에만_로컬_경고를_한번_표시한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String loadDetail = methodBody(js, "async function loadDetail(noticeId)");

    int toastCalls = numberOfOccurrences(loadDetail, "CommonUtils.toast");
    assertThat(toastCalls)
        .as("loadDetail 의 로컬 경고는 바인딩 실패 catch 하나에서만 호출된다 " + "(HTTP 실패는 인터셉터 담당 → 중복 금지).")
        .isEqualTo(1);
    assertThat(loadDetail).as("바인딩 실패 로컬 경고는 warning 타입이다.").contains("'warning'");
  }

  @Test
  void 팝업_applyMode는_상세_로드_실패_상태에서_저장_삭제를_모두_차단한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String applyMode = methodBody(js, "function applyMode()");

    assertThat(applyMode).as("applyMode 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(applyMode)
        .as("부분 상세 로드 실패(detailLoadFailed) 상태를 applyMode 가 인지해 fail-closed 한다.")
        .contains("detailLoadFailed");
    assertThat(applyMode)
        .as("실패 상태에서는 저장 버튼을 비활성화(disabled) 한다 — 부분 폼으로 저장 금지.")
        .contains("btn-save")
        .contains("disabled");
    assertThat(applyMode)
        .as("실패 상태에서는 삭제 버튼을 숨긴다(style.display) — 부분 폼으로 삭제 금지.")
        .contains("btn-delete")
        .contains("style.display");
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: centralized HTTP error behavior (catch → 인터셉터 위임).
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업_저장은_collectFormData_호출이_try_블록_내부에_있다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String saveFn = methodBody(js, "async function save()");

    assertThat(saveFn).as("save 함수 본문이 존재해야 한다.").isNotEmpty();
    int tryIdx = saveFn.indexOf("try {");
    int collectIdx = saveFn.indexOf("collectFormData()");
    assertThat(tryIdx).as("save 함수에 try 블록이 존재해야 한다.").isGreaterThanOrEqualTo(0);
    assertThat(collectIdx)
        .as("collectFormData() 호출이 try 블록 내에 있어야 한다 (unhandled rejection 방지).")
        .isGreaterThan(tryIdx);
    int catchIdx = saveFn.indexOf("catch (err)");
    assertThat(collectIdx).as("collectFormData() 호출이 catch 블록 전에 끝나야 한다.").isLessThan(catchIdx);
  }

  @Test
  void 팝업_삭제_확인_콜백은_try_블록으로_감싸여_있다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js)
        .as("삭제 확인 콜백 내 noticeId 추출 및 API 호출이 try 블록에 들어가야 한다.")
        .contains("CommonUtils.confirm")
        .contains("try {");
    java.util.regex.Matcher m = CATCH_BLOCK.matcher(js);
    while (m.find()) {
      String body = m.group(1);
      assertThat(body)
          .as("삭제 콜백 catch 블록은 postMessage/close/notifyParentAndClose 를 호출하지 않는다: " + body)
          .doesNotContain("postMessage")
          .doesNotContain("window.close")
          .doesNotContain("notifyParentAndClose");
    }
  }

  @Test
  void 팝업_catch_블록들은_발신_닫기를_하지_않는다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    java.util.regex.Matcher m = CATCH_BLOCK.matcher(js);
    int catchCount = 0;
    while (m.find()) {
      catchCount++;
      String body = m.group(1);
      assertThat(body)
          .as("catch 블록 #" + catchCount + " 은 인터셉터에 위임해야 하며 발신/닫기를 호출하지 않는다: " + body)
          .doesNotContain("notifyParentAndClose")
          .doesNotContain("postMessage")
          .doesNotContain("window.close");
    }
    assertThat(catchCount)
        .as("적어도 save/delete/detail 의 3개 catch 블록이 인터셉터 위임으로 존재해야 한다.")
        .isGreaterThanOrEqualTo(3);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Todo 4 RED → GREEN: generated Toast UI Editor ARIA 라벨링.
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  void 팝업_본문_라벨은_접근성_연결을_위한_안정적인_id를_가진다() throws IOException {
    String html = read(RESOURCES.resolve("templates/basic/notice-popup.html"));
    String labelTag = openTag(html, "content-label");

    assertThat(labelTag)
        .as("본문 라벨은 Toast UI 에디터 초기화 후 aria-labelledby 로 참조할 안정적인 id(content-label) 를 가진다.")
        .isNotEmpty();
    assertThat(labelTag)
        .as("content-label 요소는 본문 입력(for=\"content\") 을 가리키는 label 여는 태그다.")
        .contains("for=\"content\"")
        .contains("<label");
    assertThat(html).as("해당 라벨의 텍스트는 '본문'이다 (사용자에게 보이는 접근 가능한 이름).").contains(">본문</label>");
  }

  @Test
  void 팝업은_에디터_초기화후_생성된_표면에_aria를_부여한다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));

    assertThat(js).as("본문 라벨 id 를 상수로 고정한다 (오타/드리프트 방지).").contains("'content-label'");
    assertThat(js)
        .as("Toast UI Editor 초기화 직후 생성된 contenteditable 표면에 aria-labelledby 를 부여한다.")
        .contains("aria-labelledby")
        .as("접근 가능한 이름 보강을 위해 aria-label 도 함께 부여한다.")
        .contains("aria-label");
    assertThat(js)
        .as("contenteditable 표면을 textbox 역할로 명시해 스크린리더가 입력란으로 인식한다.")
        .contains("role")
        .as("여러 줄 입력란임을 명시한다.")
        .contains("aria-multiline");
    assertThat(js)
        .as("Toast UI Editor 가 만든 그룹/컨테이너 표면을 쿼리한다 (.toastui-editor 계열).")
        .contains("toastui-editor");
  }

  @Test
  void 팝업_접근성_부여함수는_new_토스트에디터_직후에_호출된다() throws IOException {
    String js = read(RESOURCES.resolve("static/js/basic/notice-popup.js"));
    String initEditor = methodBody(js, "function initEditor()");

    assertThat(initEditor).as("initEditor 함수 본문이 존재해야 한다.").isNotEmpty();
    int editorCtor = initEditor.indexOf("new toastui.Editor(");
    int a11yCall = initEditor.indexOf("applyEditorA11y");
    assertThat(editorCtor).as("initEditor 는 Toast UI Editor 생성자를 호출한다.").isGreaterThanOrEqualTo(0);
    assertThat(a11yCall)
        .as("접근성 부여는 Toast UI Editor 생성 '직후' 실행되어야 생성된 DOM 표면을 잡을 수 있다.")
        .isGreaterThan(editorCtor);
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path);
  }

  private static String block(String content, String open, String close) {
    int start = content.indexOf(open);
    int end = content.indexOf(close, start);
    if (start < 0 || end < 0) {
      return "";
    }
    return content.substring(start, end);
  }

  private static String functionBody(String js, String fnName) {
    String marker = "function " + fnName;
    int fnStart = js.indexOf(marker);
    if (fnStart < 0) {
      return "";
    }
    int braceOpen = js.indexOf('{', fnStart);
    int depth = 0;
    int end = braceOpen;
    for (int i = braceOpen; i < js.length(); i++) {
      char c = js.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          end = i;
          break;
        }
      }
    }
    return js.substring(fnStart, end + 1);
  }

  /** 지정한 함수 선언(`signature`)부터 짝이 맞는 닫는 중괄호까지 본문을 추출한다(중첩 brace 처리). */
  private static String methodBody(String content, String signature) {
    int start = content.indexOf(signature);
    if (start < 0) {
      return "";
    }
    int braceOpen = content.indexOf('{', start);
    if (braceOpen < 0) {
      return "";
    }
    int depth = 0;
    int i = braceOpen;
    for (; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          break;
        }
      }
    }
    if (i >= content.length()) {
      return "";
    }
    return content.substring(start, i + 1);
  }

  /** `<th:block layout:fragment="name">...</th:block>` 슬롯 본문을 추출한다 (중첩 없는 평평한 구조 가정). */
  private static String fragment(String html, String name) {
    String open = "layout:fragment=\"" + name + "\"";
    int start = html.indexOf(open);
    if (start < 0) {
      return "";
    }
    int tagClose = html.indexOf('>', start);
    int end = html.indexOf("</th:block>", tagClose);
    if (tagClose < 0 || end < 0) {
      return "";
    }
    return html.substring(tagClose + 1, end);
  }

  /** 지정 id를 가진 버튼/요소의 여는 태그(다음 `>` 까지)를 추출한다. 없으면 빈 문자열. */
  private static String openTag(String html, String id) {
    String marker = "id=\"" + id + "\"";
    int idx = html.indexOf(marker);
    if (idx < 0) {
      return "";
    }
    int tagStart = html.lastIndexOf('<', idx);
    int tagEnd = html.indexOf('>', idx);
    if (tagStart < 0 || tagEnd < 0) {
      return "";
    }
    return html.substring(tagStart, tagEnd + 1);
  }

  private static int numberOfOccurrences(String haystack, String needle) {
    int count = 0;
    int from = 0;
    while (true) {
      int idx = haystack.indexOf(needle, from);
      if (idx < 0) {
        return count;
      }
      count++;
      from = idx + needle.length();
    }
  }

  private static boolean inlineStyledHex(String html) {
    return INLINE_HEX_STYLE.matcher(html).find();
  }

  private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
  private static final Pattern INLINE_HEX_STYLE =
      Pattern.compile("style=\"[^\"]*#[0-9a-fA-F]{3,8}");
  // getElementById(...).addEventListener 직접 체인 매칭 — null 가드 헬퍼 강제.
  private static final Pattern DIRECT_ADD_EVENT_LISTENER =
      Pattern.compile("getElementById\\([^)]+\\)\\.addEventListener");
  // getElementById(...).disabled|style 직접 체인 매칭 — applyMode 누락 버튼 null 가드 강제.
  private static final Pattern DIRECT_BUTTON_ATTR =
      Pattern.compile("getElementById\\([^)]+\\)\\.(?:disabled|style)");
  // catch (e) { ... } 본문 추출 — 중첩 brace 가 없는 평평한 catch 본문 가정.
  private static final Pattern CATCH_BLOCK =
      Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{([^}]*)\\}");
}
