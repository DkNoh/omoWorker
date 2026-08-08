package com.scbk.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HttpClientValidationContractTest {

  private static final Path HTTP_CLIENT =
      Path.of("src", "main", "resources", "static", "js", "common", "http-client.js");

  @Test
  void resolver가_활성_모달을_문서_폴백보다_먼저_검색함() throws IOException {
    // given
    String js = Files.readString(HTTP_CLIENT);
    String modalLookup = block(js, "const findValidationField", "const resolveValidationField");
    String resolver = block(js, "const resolveValidationField", "const invalidClearListeners");

    // when
    int modalData = modalLookup.indexOf("'[data-field]'");
    int modalId = modalLookup.indexOf("'[id]'");
    int modalName = modalLookup.indexOf("'[name]'");
    int activeModal = resolver.indexOf("findValidationField(activeModal, field)");
    int documentData = resolver.indexOf("findExactAttribute(document, '[data-field]'");
    int documentId = resolver.indexOf("document.getElementById(field)");
    int documentName = resolver.indexOf("findExactAttribute(document, '[name]'");

    // then
    assertThat(modalData).isGreaterThanOrEqualTo(0).isLessThan(modalId);
    assertThat(modalId).isLessThan(modalName);
    assertThat(activeModal).isGreaterThanOrEqualTo(0).isLessThan(documentData);
    assertThat(documentData).isLessThan(documentId);
    assertThat(documentId).isLessThan(documentName);
  }

  @Test
  void resolver가_서버_필드를_선택자에_보간하지_않고_정확히_비교함() throws IOException {
    // given
    String js = Files.readString(HTTP_CLIENT);
    String exactMatcher = block(js, "const findExactAttribute", "const findValidationField");
    String modalLookup = block(js, "const findValidationField", "const resolveValidationField");
    String resolver = block(js, "const resolveValidationField", "const invalidClearListeners");

    // when / then
    assertThat(exactMatcher)
        .contains("root.querySelectorAll(selector)", "element.getAttribute(attribute) === value")
        .doesNotContain("${value}", "+ value");
    assertThat(modalLookup)
        .contains("findExactAttribute(root, '[data-field]', 'data-field', field)")
        .contains("findExactAttribute(root, '[id]', 'id', field)")
        .contains("findExactAttribute(root, '[name]', 'name', field)")
        .doesNotContain("${field}", "+ field");
    assertThat(resolver)
        .contains("findValidationField(activeModal, field)")
        .contains("findExactAttribute(document, '[data-field]', 'data-field', field)")
        .contains("document.getElementById(field)")
        .contains("findExactAttribute(document, '[name]', 'name', field)")
        .doesNotContain("${field}", "+ field");
  }

  @Test
  void invalid_clear_listener가_필드별로_교체되고_양쪽_이벤트에서_정리됨() throws IOException {
    // given
    String js = Files.readString(HTTP_CLIENT);
    String removal = block(js, "const invalidClearListeners", "const markFieldInvalid");
    String marker = block(js, "const markFieldInvalid", "axios.interceptors.response.use");

    // when
    int removePrevious = marker.indexOf("removeInvalidClearListeners(fieldEl)");
    int markInvalid = marker.indexOf("fieldEl.classList.add('is-invalid')");
    int rememberListener = marker.indexOf("invalidClearListeners.set(fieldEl, clearInvalid)");
    int addInput = marker.indexOf("fieldEl.addEventListener('input', clearInvalid)");
    int addChange = marker.indexOf("fieldEl.addEventListener('change', clearInvalid)");

    // then
    assertThat(removal)
        .contains("const invalidClearListeners = new WeakMap()")
        .contains("invalidClearListeners.get(fieldEl)")
        .contains("fieldEl.removeEventListener('input', listener)")
        .contains("fieldEl.removeEventListener('change', listener)")
        .contains("invalidClearListeners.delete(fieldEl)");
    assertThat(marker)
        .contains("fieldEl.classList.remove('is-invalid');\n            removeInvalidClearListeners(fieldEl)");
    assertThat(removePrevious).isGreaterThanOrEqualTo(0).isLessThan(markInvalid);
    assertThat(markInvalid).isLessThan(rememberListener);
    assertThat(rememberListener).isLessThan(addInput);
    assertThat(addInput).isLessThan(addChange);
  }

  @Test
  void 필드_오류_핸들러가_resolver와_listener_bookkeeping을_실제로_사용함() throws IOException {
    // given
    String js = Files.readString(HTTP_CLIENT);
    String errorHandler = block(js, "apiErr.errors.forEach", "const get = async");

    // when / then
    assertThat(errorHandler)
        .contains("const fieldEl = resolveValidationField(e.field)")
        .contains("markFieldInvalid(fieldEl)")
        .contains("notify.alert(displayMsg, '오류')")
        .contains("return Promise.reject(error)")
        .doesNotContain("fieldEl.addEventListener", "fieldEl.classList.add('is-invalid')");
  }

  private static String block(String source, String startMarker, String endMarker) {
    int start = source.indexOf(startMarker);
    int end = source.indexOf(endMarker, start);
    assertThat(start).as("시작 마커가 존재해야 한다: %s", startMarker).isGreaterThanOrEqualTo(0);
    assertThat(end).as("종료 마커가 존재해야 한다: %s", endMarker).isGreaterThan(start);
    return source.substring(start, end);
  }
}
