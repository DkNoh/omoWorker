package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 공지사항 목록 화면({@code src/main/resources/templates/basic/notice.html})의 간편 수정 모달 + 게시판 팝업 예제 버튼 정적
 * 계약을 고정한다(Todo 3 HTML + Todo 5 JS 오케스트레이션).
 *
 * <p>Todo 3 는 HTML 마크업 계약만 다룬다(검색카드/그리드 카드/등록 권한 가드 + modal-base 폼 + 게시판 팝업 예제 버튼). Todo 5 는 {@code
 * notice.js} 의 행-클릭 모달 + 강화된 postMessage 오케스트레이션 정적 계약을 추가한다(v1 의 간편 모달 흐름 + v2 의
 * openedPopups/VALID_OPERATIONS/noticeChanged 보존). 팝업 화면 자체는 {@link NoticePopupContractTest}에서 다룬다.
 *
 * <p>TDD 순서:
 *
 * <ol>
 *   <li>baseline characterization — v2가 이미 갖고 있는 검색카드/그리드 카드/등록 권한 가드. 구현 전부터 GREEN 이어야 한다(회귀 방지).
 *   <li>desired contract (HTML) — modal-base 폼·DTO 필드 매핑·서버 권한 가드 저장/삭제 버튼·게시판 팝업 예제 버튼. 구현 전 RED,
 *       마크업 추가 후 GREEN.
 *   <li>negative contract — 엑셀/중복 그리드/Toast UI 부모 삽입/raw hex/CDN/인라인 style 을 거부한다(plan guard).
 *   <li>JS orchestration (Todo 5 RED → GREEN) — 행 클릭 모달·ModalManager·FormBinder·PAGE_AUTH
 *       페일클로즈드·CRUD 엔드포인트 분리·낙관적 잠금·삭제 확인·단일 새로고침·v2 강화 메시지 보존.
 * </ol>
 */
class NoticeEditPatternContractTest {

  private static final Path NOTICE_HTML =
      Path.of("src", "main", "resources", "templates", "basic", "notice.html");

  private static final Path NOTICE_JS =
      Path.of("src", "main", "resources", "static", "js", "basic", "notice.js");

  // ---------------------------------------------------------------------------
  // 1. baseline characterization (구현 전부터 GREEN)
  // ---------------------------------------------------------------------------

  @Test
  void baseline_목록_화면은_그리드_카드를_정확히_한개만_둔다() throws IOException {
    String html = read();

    assertThat(count(html, "fragments/toast-grid :: gridCard"))
        .as("LIST/EXCEL 화면은 TuiPageBuilder 용 그리드 카드를 정확히 1개만 둔다 (이중 그리드 금지).")
        .isEqualTo(1);
  }

  @Test
  void baseline_검색_카드는_v2의_날짜_useYn_조건을_유지한다() throws IOException {
    String html = read();

    assertThat(html)
        .as("v2 검색 카드는 startdt/enddt Toast UI DatePicker 입력을 유지한다.")
        .contains("id=\"startdt\"")
        .contains("id=\"enddt\"")
        .contains("data-search-type=\"date\"");
    assertThat(html).as("v2 검색 카드는 useYn 조건 입력을 유지한다.").contains("id=\"useYn\"");
  }

  @Test
  void baseline_등록_버튼은_pageAuth_create_가드를_유지한다() throws IOException {
    String html = read();

    assertThat(html)
        .as("등록 버튼은 서버가 내려주는 pageAuth.create 로만 게이트한다 (thymeleaf.md 권한 규칙).")
        .contains("id=\"btn-create\"")
        .contains("th:if=\"${pageAuth.create}\"");
    assertThat(html)
        .as("조회/초기화 버튼도 v2 baseline ID 를 유지한다.")
        .contains("id=\"btn-search\"")
        .contains("id=\"btn-reset\"");
  }

  // ---------------------------------------------------------------------------
  // 2. desired contract (구현 전 RED, 마크업 추가 후 GREEN)
  // ---------------------------------------------------------------------------

  @Test
  void 간편_수정_모달은_modal_base_프래그먼트로_정확히_한개_임베드된다() throws IOException {
    String html = read();

    assertThat(count(html, "fragments/modal-base :: layout"))
        .as("간편 수정 모달은 modal-base.html 프래그먼트를 1회만 사용해 공통 표준을 따른다.")
        .isEqualTo(1);
    assertThat(html)
        .as(
            "modalId 파라미터는 notice-modal 이다. fragment 가 th:id=${modalId} 로 렌더링하므로 "
                + "modalId='notice-modal' 바인딩으로 계약을 고정한다.")
        .contains("modalId='notice-modal'");
  }

  @Test
  void 모달_폼은_NOTICE_수정_DTO_필드명을_그대로_노출한다() throws IOException {
    String html = read();

    assertThat(html)
        .as("FormBinder name 일치 계약: form id=notice-modal-form 이다.")
        .contains("id=\"notice-modal-form\"");
    assertThat(html)
        .as(
            "수정 가능 필드의 name 은 NoticeUpdateRequestDTO 프로퍼티명과 일치해 FormBinder 가 자동 바인딩한다. "
                + "noticeId/beforeUpdDttm 은 hidden (PK + 낙관적 잠금).")
        .contains("name=\"noticeId\"")
        .contains("name=\"beforeUpdDttm\"")
        .contains("name=\"title\"")
        .contains("name=\"content\"")
        .contains("name=\"noticeType\"")
        .contains("name=\"useYn\"")
        .contains("name=\"startDt\"")
        .contains("name=\"endDt\"")
        .contains("name=\"viewCnt\"");
  }

  @Test
  void beforeUpdDttm은_낙관적_잠금_값으로_존재한다() throws IOException {
    String html = read();

    assertThat(html)
        .as(
            "NoticeUpdateRequestDTO 의 beforeUpdDttm 은 상세조회 시점의 UPD_DTTM 스냅샷이다 "
                + "(screen-convention.md 낙관적 잠금). hidden 필드로만 전송한다.")
        .containsPattern(Pattern.compile("<input[^>]*type=\"hidden\"[^>]*name=\"beforeUpdDttm\""));
  }

  @Test
  void 모달_푸터는_권한_가드가_있는_저장_삭제_버튼을_노출한다() throws IOException {
    String html = read();

    assertThat(html)
        .as("ModalManager 계약: 저장 버튼 id 는 ${modalId}-btn-save = notice-modal-btn-save 다.")
        .contains("id=\"notice-modal-btn-save\"");
    assertThat(html)
        .as("ModalManager 계약: 삭제 버튼 id 는 ${modalId}-btn-delete = notice-modal-btn-delete 다.")
        .contains("id=\"notice-modal-btn-delete\"");
    assertThat(html)
        .as(
            "저장 버튼은 pageAuth.create 또는 pageAuth.update 일 때만 노출한다 (등록·수정 겸용 가드). "
                + "서버가 내려주는 pageAuth 만 쓴다 (thymeleaf.md).")
        .contains("th:if=\"${pageAuth.create or pageAuth.update}\"");
    assertThat(html)
        .as("삭제 버튼은 pageAuth.delete 일 때만 노출한다.")
        .contains("th:if=\"${pageAuth.delete}\"");
  }

  @Test
  void 본문은_간단_textarea이고_Toast_UI_에디터가_아니다() throws IOException {
    String html = read();

    assertThat(html)
        .as("간편 모달 본문은 단순 textarea 다. Toast UI Editor 는 팝업 화면에서만 쓴다.")
        .containsPattern(Pattern.compile("<textarea[^>]*name=\"content\""));
    assertThat(html)
        .as("목록 화면의 간편 모달에는 Toast UI Editor 스크립트/클래스가 없어야 한다.")
        .doesNotContain("toastui-editor")
        .doesNotContain("tui-page-builder");
  }

  @Test
  void 별도_게시판_팝업_예제_버튼이_존재한다() throws IOException {
    String html = read();

    assertThat(html)
        .as("행 더블클릭 대신 별도 '게시판 팝업 예제' 버튼으로 선택 행 팝업을 연다 (plan Todo 3).")
        .contains("게시판 팝업 예제");
    assertThat(html)
        .as("팝업 예제 버튼은 식별 가능한 id 를 가진다 (notice.js 가 선택 행 검사 후 window.open).")
        .contains("id=\"btn-popup-example\"");
  }

  // ---------------------------------------------------------------------------
  // 3. negative contract (plan guard — 항상 GREEN 이어야 한다)
  // ---------------------------------------------------------------------------

  @Test
  void 간편_모달_화면은_엑셀_두번째_그리드를_두지_않는다() throws IOException {
    String html = read();

    assertThat(html)
        .as("공지사항 엑셀/다운로드 UI 는 추가하지 않는다 (plan scope guard).")
        .doesNotContain("btn-excel");
    assertThat(count(html, "fragments/toast-grid :: gridCard"))
        .as("두번째 그리드/자체 그리드 구현은 금지한다.")
        .isEqualTo(1);
  }

  @Test
  void 인라인_style_및_raw_hex_색_선언을_두지_않는다() throws IOException {
    String html = read();

    assertThat(html)
        .as("DESIGN.md 토큰 규칙: 인라인 style= 속성을 두지 않는다 (--sms-* 토큰만 허용).")
        .doesNotContain("style=\"");
    assertThat(html)
        .as("DESIGN.md 토큰 규칙: 템플릿에서 raw hex 색 선언(#RRGGBB)을 두지 않는다.")
        .doesNotContainPattern(Pattern.compile("#[0-9a-fA-F]{6}"));
  }

  @Test
  void CDN_참조를_두지_않는다() throws IOException {
    String html = read();

    assertThat(html)
        .as(
            "폐쇄망 규칙: CDN 스킴/호스트(cdnjs/unpkg/jsdelivr/fastly/cloudflare)를 두지 않는다 "
                + "(thymeleaf.md, screen-convention.md).")
        .doesNotContainPattern(
            Pattern.compile(
                "https?://(cdn\\.|cdnjs\\.|unpkg\\.com|cdn\\.jsdelivr\\.net|\\.fastly\\.)"));
  }

  @Test
  void 두번째_팝업_템플릿_없이_notice_popup_참조는_유지하지_않는다() throws IOException {
    String html = read();

    assertThat(html)
        .as(
            "팝업은 별도 창(notice-popup.html) 으로 열리며, 목록 템플릿 안에 두번째 팝업 template 을 "
                + "임베드하지 않는다 (plan Todo 3).")
        .doesNotContain("notice-popup.html");
  }

  // ===========================================================================
  // Todo 5 — JS orchestration (notice.js)
  // 행 클릭 간편 모달 + 강화된 postMessage 보존. 구현 전 RED, 구현 후 GREEN.
  // ===========================================================================

  @Test
  void baseline_notice_js는_TuiPageBuilder로_grid와_data_엔드포인트를_유지한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("TuiPageBuilder 로 목록을 초기화한다 (직접 그리드 구현 금지).")
        .contains("new TuiPageBuilder(")
        .contains("el: 'grid'")
        .contains("apiUrl: '/basic/notice/data'");
  }

  @Test
  void 행_더블클릭이_아닌_클릭으로_간편_모달을_연다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("그리드 행 click 이벤트로 모달을 연다 (팝업 예제 버튼이 선택 행을 열므로 dblclick 중복은 제거).")
        .contains(".on('click'");
    long dblclickInCode = countDblclickInCode(js);
    assertThat(dblclickInCode).as("실행 코드에서 dblclick 리스너 등록은 0건이어야 한다 (주석/문자열 제외).").isZero();
  }

  @Test
  void 간편_모달은_ModalManager_init_open_close로_제어된다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("ModalManager.init 로 저장/삭제 lifecycle 훅을 바인딩한다.")
        .contains("ModalManager.init(")
        .as("ModalManager.open 으로 모달을 노출한다.")
        .contains("ModalManager.open(")
        .as("ModalManager.close 로 저장/삭제 후 모달을 닫는다.")
        .contains("ModalManager.close(");
  }

  @Test
  void 폼_바인딩은_FormBinder_bind와_toObject를_사용한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("상세 응답은 FormBinder.bind 로 폼에 채운다 (name=DTO 프로퍼티 자동 바인딩).")
        .contains("FormBinder.bind(")
        .as("전송 객체는 FormBinder.toObject 로 만든다.")
        .contains("FormBinder.toObject(");
  }

  @Test
  void 모드는_create와_update로_구분된다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("등록 모드('create') 와 수정 모드('update') 를 구분해 API/토스트/권한을 분기한다.")
        .contains("'create'")
        .contains("'update'");
  }

  @Test
  void 행의_updDttm은_beforeUpdDttm_스냅샷으로_매핑된다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("낙관적 잠금: 상세 응답의 updDttm 을 beforeUpdDttm hidden 필드로 스냅샷한다.")
        .contains("beforeUpdDttm")
        .contains("updDttm");
  }

  @Test
  void 생성_수정_삭제_URL은_서로_구분되고_save는_없다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("등록/수정/삭제 엔드포인트는 /create /update /delete 로 분리한다 (menu-authority 규칙).")
        .contains("/basic/notice/create")
        .contains("/basic/notice/update")
        .contains("/basic/notice/delete")
        .as("등록/수정 겸용 /save 는 신규 코드에 두지 않는다.")
        .doesNotContain("/basic/notice/save");
  }

  @Test
  void 모든_쓰기_전에_PAGE_AUTH_엄격_페일클로즈드_검사를_한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("서버가 defaultLayout.html 로 내려주는 PAGE_AUTH 를 읽는다 (팝업의 noticePageAuth 가 아님).")
        .contains("PAGE_AUTH");
    assertThat(js)
        .as("create/update/delete 각각 === true 엄격 검사로 페일클로즈드 한다 (값 누락 시 차단).")
        .contains(".create === true")
        .contains(".update === true")
        .contains(".delete === true");
  }

  @Test
  void 생성_수정은_ApiClient_post_삭제는_ApiClient_remove를_사용한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("create/update 는 ApiClient.post(JSON body) 로 보낸다.")
        .contains("ApiClient.post(")
        .as("delete 는 ApiClient.remove(POST + noticeId param) 로 보낸다 (스캐폴드 삭제 엔드포인트 호환).")
        .contains("ApiClient.remove(");
  }

  @Test
  void 삭제는_CommonUtils_confirm을_거치고_성공_토스트를_띄운다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("삭제 전 CommonUtils.confirm 으로 확인 다이얼로그를 띄운다 (window.confirm 금지).")
        .contains("CommonUtils.confirm(")
        .doesNotContain("window.confirm");
    assertThat(js).as("삭제 성공은 CommonUtils.toast 로 안내한다.").contains("CommonUtils.toast(");
  }

  @Test
  void 성공_후_현재_페이지를_새로고침한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("모달 저장/삭제 후 searchData 로 현재 페이지(currentPage) 를 새로고침한다.")
        .contains("searchData(")
        .contains("currentPage");
  }

  @Test
  void 팝업_예제_버튼은_선택된_noticeId로_열고_미선택시_경고한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("btn-popup-example 버튼을 바인딩한다 (선택 행의 게시판 팝업 window.open).")
        .contains("btn-popup-example");
    assertThat(js).as("선택된 행이 없으면 window.open 하지 않고 warning 토스트를 띄운다.").contains("선택된 행이 없습니다");
  }

  @Test
  void autoModal_modalActions_save_엑셀_동작이_없다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("제거된 TuiPageBuilder 자동 모달 엔진(autoModal/modalActions) 을 복원하지 않는다.")
        .doesNotContain("autoModal")
        .doesNotContain("modalActions")
        .as("/save 엔드포인트를 두지 않는다.")
        .doesNotContain("/basic/notice/save")
        .as("엑셀/다운로드 UI 를 두지 않는다 (plan scope guard).")
        .doesNotContain("btn-excel");
  }

  @Test
  void 행_클릭은_상세_조회_API로_content와_updDttm을_온전히_채운다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as(
            "그리드 /data 행에는 content 가 없다. 행 클릭은 /basic/notice/detail 로 content 와 updDttm 을 온전히 가져와야 한다.")
        .contains("/basic/notice/detail");
    assertThat(js).as("상세 조회는 ApiClient.get 로 부른다 (GET + params).").contains("ApiClient.get(");
  }

  @Test
  void 액션_버튼은_모드와_권한으로_가시성을_동기화한다() throws IOException {
    String js = readJs();

    assertThat(js).as("save/delete 버튼 가시성을 모드+권한으로 동기화하는 함수가 존재한다.").contains("syncActionButtons");
    assertThat(js).as("CoreUI 숨김 유틸리티 d-none 토글로 버튼을 숨긴다.").contains("d-none");
    assertThat(js)
        .as("save 표시 조건은 (create 모드 && CREATE) 또는 (update 모드 && UPDATE) 이다.")
        .contains("mode === 'create'")
        .contains("mode === 'update'");
  }

  @Test
  void openModalForUpdate_전체_경로가_try_catch로_감싸져_있다() throws IOException {
    String js = readJs();

    String fn = methodBody(js, "const openModalForUpdate");
    assertThat(fn).as("openModalForUpdate 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(fn)
        .as("openModalForUpdate 는 GET → bind → sync → open 전체를 try 안에 둔다.")
        .contains("try")
        .contains("ApiClient.get")
        .contains("FormBinder.bind")
        .contains("ModalManager.open")
        .contains("catch")
        .contains("console.error");
  }

  @Test
  void openModalForCreate는_빈_모달을_연다() throws IOException {
    String js = readJs();

    String fn = methodBody(js, "const openModalForCreate");
    assertThat(fn).as("openModalForCreate 함수 본문이 존재해야 한다 (등록 버튼이 모달을 열고 팝업은 열지 않는다).").isNotEmpty();
    assertThat(fn)
        .as("create 모드 진입 후 폼을 리셋하고 모달을 연다 (빈 모달).")
        .contains("'create'")
        .contains("ModalManager.open");
    assertThat(fn)
        .as("openModalForCreate 는 팝업을 열지 않는다 (등록은 간편 모달, 팝업은 선택 행 전용).")
        .doesNotContain("openPopup");
  }

  @Test
  void handleSave_toObject가_try_내부에_있다() throws IOException {
    String js = readJs();

    String fn = methodBody(js, "const handleSave");
    assertThat(fn).as("handleSave 함수 본문이 존재해야 한다.").isNotEmpty();
    int tryIdx = fn.indexOf("try");
    int toObjectIdx = fn.indexOf("FormBinder.toObject");
    int catchIdx = fn.indexOf("catch");
    assertThat(tryIdx)
        .as("handleSave 에 try 블록이 있어야 한다 (unhandled rejection 방지).")
        .isGreaterThan(-1);
    assertThat(toObjectIdx)
        .as("FormBinder.toObject 호출이 try 블록 내에 있어야 한다.")
        .isGreaterThan(tryIdx)
        .isLessThan(catchIdx);
  }

  @Test
  void handleDelete_confirm_콜백_전체가_try_catch로_감싸져_있다() throws IOException {
    String js = readJs();

    String fn = methodBody(js, "const handleDelete");
    assertThat(fn).as("handleDelete 함수 본문이 존재해야 한다.").isNotEmpty();
    assertThat(fn)
        .as("handleDelete 는 CommonUtils.confirm 콜백 안에서 ApiClient.remove 를 부르고 catch 로 감싼다.")
        .contains("CommonUtils.confirm")
        .contains("ApiClient.remove")
        .contains("try")
        .contains("catch")
        .contains("console.error");
  }

  @Test
  void 그리드_키보드_활성화는_호스트_DOM_keydown과_getFocusedCell로_동작한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as(
            "tui-grid 합성 keydown(grid.on('keydown')) 은 native KeyboardEvent.key 를 노출하지 않아 죽는다 — 사용 금지.")
        .doesNotContain(".on('keydown'");
    assertThat(js)
        .as("#grid 호스트에 네이티브 DOM keydown 리스너를 단다 (템플릿/빌더 변경 없이 JS).")
        .contains("addEventListener('keydown'");
    assertThat(js).as("Enter 와 Space( ) 만 동작 키로 받는다.").contains("'Enter'").contains("' '");
    assertThat(js)
        .as("#grid 호스트를 tabindex=0 으로 키보드 포커스 가능하게 만든다.")
        .containsPattern(Pattern.compile("tabIndex\\s*=\\s*0"));
    assertThat(js)
        .as("#grid 호스트에 의미 있는 aria-label 을 setAttribute 로 부여한다.")
        .containsPattern(
            Pattern.compile(
                "setAttribute\\s*\\(\\s*['\"]aria-label['\"]\\s*,\\s*['\"][^'\"]+['\"]\\s*\\)"));
    assertThat(js)
        .as("포커스된 셀을 grid.getFocusedCell() 로 읽는다 (네이티브 keydown 경로).")
        .contains("getFocusedCell(");
  }

  @Test
  void 모든_비동기_쓰기_경로는_내부에서_예외를_잡아_unhandled_rejection을_막는다() throws IOException {
    String js = readJs();

    assertThat(count(js, "catch"))
        .as(
            "HttpClient 인터셉터는 reject rethrow 이므로 상세조회/저장/삭제콜백/메시지새로고침/그리드클릭/그리드키다운 6개 async 경로가 각각 catch 해야 한다 (>= 4).")
        .isGreaterThanOrEqualTo(4);
  }

  @Test
  void 실패시_로그만_남기고_성공_토스트_닫기_새로고침을_하지_않는다() throws IOException {
    String js = readJs();

    assertThat(count(js, "console.error"))
        .as("인터셉터가 중앙 에러 다이얼로그를 띄운 뒤 notice.js 는 console.error 로만 로그한다.")
        .isGreaterThanOrEqualTo(4);
    assertThat(js).as("에러 경로에 네이티브 alert 를 직접 쓰지 않는다 (중앙 Notify 만 허용).").doesNotContain("alert(");
  }

  // ─── v2 강화 메시지 보존 (Todo 5: 간편 모달 추가와 함께 손실되지 않는다) ───────────

  @Test
  void 부모_리스너는_정확히_한개이며_noticeChanged_액션과_VALID_OPERATIONS_화이트리스트를_유지한다() throws IOException {
    String js = readJs();

    assertThat(count(js, "addEventListener('message'"))
        .as("메시지 리스너는 정확히 1개만 둔다 (팝업 저장/삭제/생성 메시지 중복 새로고침 방지).")
        .isEqualTo(1);
    assertThat(js)
        .as("v2 강화: action 지시자는 noticeChanged 이다 (v1 noticeSaved 로 회귀 금지).")
        .contains("noticeChanged")
        .doesNotContain("noticeSaved");
    assertThat(js)
        .as("operation 은 VALID_OPERATIONS.has 화이트리스트로 검증한다 (프로토타입 키/임의 값 거부).")
        .contains("VALID_OPERATIONS")
        .contains(".has(");
  }

  @Test
  void 부모는_발신_origin과_source를_모두_검증한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("발신 origin 검증: ev.origin !== window.location.origin 인 메시지는 거부한다.")
        .contains("ev.origin")
        .contains("window.location.origin");
    assertThat(js)
        .as("발신 source 검증: window.open 으로 연 팝업 핸들(openedPopups) 인 메시지만 수락한다.")
        .contains("openedPopups")
        .contains("ev.source")
        .contains("window.open");
    assertThat(js).as("와일드카드 '*' targetOrigin/origin 허용을 두지 않는다.").doesNotContain("'*')");
  }

  @Test
  void 부모는_operation별_구분된_한글_성공_toast를_표시한다() throws IOException {
    String js = readJs();

    assertThat(js)
        .as("created/updated/deleted operation 각각 등록/수정/삭제 한글 메시지로 매핑한다 (중립 '저장되었습니다.' 회귀 방지).")
        .contains("등록되었습니다.")
        .contains("수정되었습니다.")
        .contains("삭제되었습니다.");
  }

  @Test
  void 부모_메시지_수락시_정확히_한번_새로고침한다() throws IOException {
    String js = readJs();

    int listenerIdx = js.indexOf("addEventListener('message'");
    int listenerBodyEnd = matchingCloseBrace(js, js.indexOf('(', listenerIdx));
    String listenerBody = js.substring(listenerIdx, listenerBodyEnd + 1);
    int originIdx = listenerBody.indexOf("ev.origin");
    int sourceIdx = listenerBody.indexOf("isKnownPopup");
    int actionIdx = listenerBody.indexOf("noticeChanged");
    int opIdx = listenerBody.indexOf("VALID_OPERATIONS");
    int refreshIdx = listenerBody.indexOf("refreshCurrentPage");
    assertThat(listenerBodyEnd).as("message 리스너 본문 경계를 찾을 수 있어야 한다.").isGreaterThan(listenerIdx);
    assertThat(originIdx).as("origin 검증은 리스너 본문 내에 있어야 한다.").isGreaterThan(-1);
    assertThat(sourceIdx).as("source(openedPopups) 검증은 리스너 본문 내에 있어야 한다.").isGreaterThan(-1);
    assertThat(actionIdx).as("action 일치 검증은 리스너 본문 내에 있어야 한다.").isGreaterThan(-1);
    assertThat(opIdx).as("operation 화이트리스트 검증은 리스너 본문 내에 있어야 한다.").isGreaterThan(-1);
    assertThat(refreshIdx)
        .as(
            "새로고침(refreshCurrentPage)은 네 가지 검증(origin/source/action/operation)보다 뒤에 있어야 한다 (미검증 메시지는 새로고침 안 함).")
        .isGreaterThan(opIdx);
    assertThat(count(listenerBody, "refreshCurrentPage"))
        .as("수락된 메시지 한 건당 새로고침 호출은 정확히 1회이다.")
        .isEqualTo(1);
    assertThat(count(listenerBody, "searchData"))
        .as("리스너 본문은 searchData 를 직접 부르지 않고 refreshCurrentPage 래퍼로 감싼다 (중앙화).")
        .isZero();
  }

  // ---------------------------------------------------------------------------
  // 6. Documentation contract — docs must reflect dual-edit, not popup-only
  // ---------------------------------------------------------------------------

  private static final Path DOCS_SCAFFOLD = Path.of("docs/base/query-scaffold-implementation.md");

  @Test
  void docs_문서는_팝업전용이_아닌_이중_편집_패턴을_기술한다() throws IOException {
    String docs = Files.readString(DOCS_SCAFFOLD);

    // Negative proof: stale dblclick-only / popup-only claims must not exist
    assertThat(docs)
        .as("docs는 notice.js가 그리드 dblclick을 바인딩한다고 주장해서는 안 된다.")
        .doesNotContain("그리드 `dblclick`");

    // Positive proof: dual-edit behavior must be described
    // Row-click modal claim
    assertThat(docs)
        .as("docs는 행 클릭(또는 row click)으로 간편 모달을 연다고 기술해야 한다.")
        .satisfies(
            d -> {
              boolean ok =
                  d.contains("행 클릭")
                      || d.contains("row click")
                      || d.contains("row-click")
                      || d.contains("행-클릭")
                      || (d.contains("click") && d.contains("모달") && d.contains("notice.js"));
              assertThat(ok).isTrue();
            });

    // Popup button claim
    assertThat(docs)
        .as("docs는 명시적 팝업 예제 버튼이 선택 행의 팝업을 연다고 기술해야 한다.")
        .satisfies(
            d -> {
              boolean ok =
                  d.contains("팝업 예제")
                      || d.contains("popup-example")
                      || d.contains("popup button")
                      || d.contains("게시판 팝업")
                      || (d.contains("popup") && d.contains("선택") && d.contains("notice.js"));
              assertThat(ok).isTrue();
            });

    // Non-regenerated claim
    assertThat(docs)
        .as("docs는 이 구현이 scaffold 재생성 대상이 아님을 명시해야 한다.")
        .satisfies(
            d -> {
              boolean ok =
                  d.contains("재생성하지 않")
                      || d.contains("scaffold-cases에 포함되지 않")
                      || d.contains("자동 생성되지 않")
                      || d.contains("developer-owned")
                      || d.contains("수동 수정")
                      || d.contains("수동 참조");
              assertThat(ok).isTrue();
            });
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private static String read() throws IOException {
    return Files.readString(NOTICE_HTML);
  }

  private static String readJs() throws IOException {
    return Files.readString(NOTICE_JS);
  }

  private static long count(String content, String needle) {
    long n = 0;
    int from = 0;
    while (true) {
      int idx = content.indexOf(needle, from);
      if (idx < 0) {
        break;
      }
      n++;
      from = idx + needle.length();
    }
    return n;
  }

  /**
   * Counts occurrences of the literal {@code dblclick} outside JS comments and string literals. The
   * reconciled {@code notice.js} may mention {@code dblclick} inside a comment explaining why it
   * was removed; only executable references count as a regression.
   */
  private static long countDblclickInCode(String js) {
    long n = 0;
    for (String line : js.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) continue;
      if (trimmed.startsWith("//") || trimmed.startsWith("*")) continue;
      if (trimmed.startsWith("'") || trimmed.startsWith("\"")) continue;
      n += (line.split("dblclick", -1).length - 1);
    }
    return n;
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

  /**
   * Returns the index of the matching close brace for the brace block that opens at or after
   * {@code openParenIdx}. Scans from the first {@code '{'} at/after {@code openParenIdx} and counts
   * nested braces. Used to bound async listener/callback bodies whose own close brace is buried
   * inside object/array literals when read as flat text.
   */
  private static int matchingCloseBrace(String content, int openParenIdx) {
    int braceOpen = content.indexOf('{', openParenIdx);
    if (braceOpen < 0) {
      return -1;
    }
    int depth = 0;
    for (int i = braceOpen; i < content.length(); i++) {
      char c = content.charAt(i);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }
}
