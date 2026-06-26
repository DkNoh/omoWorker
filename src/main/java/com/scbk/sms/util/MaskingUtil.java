package com.scbk.sms.util;

import java.util.regex.Pattern;

/**
 * 개인정보(이름, 전화번호, 주민번호, 카드번호)를 화면 표시/로그 기록 전에 마스킹하는 유틸리티.
 *
 * <p>MaskingUtil.maskPhone("01012345678") -> 010-****-5678 MaskingUtil.maskName("홍길동") -> 홍*동
 * MaskingUtil.maskRrn("9001011234567") -> 900101-1****** MaskingUtil.maskCard("1234567812345678")
 * -> 1234-****-****-5678
 */
public final class MaskingUtil {

  private static final Pattern RRN_PATTERN = Pattern.compile("\\d{13}");
  private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{10,11}");

  private MaskingUtil() {}

  /** 이름 마스킹. 2글자는 끝 글자, 3글자 이상은 첫/끝 글자 제외 전부 마스킹한다. */
  public static String maskName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return name;
    }

    String targetName = name.trim();
    int length = targetName.length();

    if (length < 2) {
      return targetName;
    }

    if (length == 2) {
      return targetName.charAt(0) + "*";
    }

    StringBuilder masked = new StringBuilder();
    masked.append(targetName.charAt(0));
    for (int i = 0; i < length - 2; i++) {
      masked.append("*");
    }
    masked.append(targetName.charAt(length - 1));

    return masked.toString();
  }

  /** 전화번호 마스킹. 하이픈을 자동 삽입하고 가운데 자리를 마스킹한다. 01012345678 -> 010-****-5678 */
  public static String maskPhone(String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      return phone;
    }

    String cleanPhone = phone.replaceAll("[^0-9]", "");
    int length = cleanPhone.length();

    if (length < 9) {
      return cleanPhone;
    }

    if (length == 9) {
      return cleanPhone.replaceFirst("(\\d{2})(\\d{3})(\\d{4})", "$1-***-$3");
    } else if (length == 10) {
      if (cleanPhone.startsWith("02")) {
        return cleanPhone.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "$1-****-$3");
      }
      return cleanPhone.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-***-$3");
    } else if (length == 11) {
      return cleanPhone.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-****-$3");
    }

    return cleanPhone;
  }

  /** 주민등록번호 마스킹. 뒤 7자리 중 성별 자리만 남긴다. 9001011234567 -> 900101-1****** */
  public static String maskRrn(String rrn) {
    if (rrn == null || rrn.trim().isEmpty()) {
      return rrn;
    }
    String cleanRrn = rrn.replaceAll("[^0-9]", "");
    if (cleanRrn.length() != 13) {
      return cleanRrn;
    }
    return cleanRrn.replaceFirst("(\\d{6})(\\d{1})(\\d{6})", "$1-$2******");
  }

  /** 카드번호 마스킹. 가운데 8자리를 마스킹한다. 1234567812345678 -> 1234-****-****-5678 */
  public static String maskCard(String card) {
    if (card == null || card.trim().isEmpty()) {
      return card;
    }
    String cleanCard = card.replaceAll("[^0-9]", "");
    if (cleanCard.length() != 16) {
      return cleanCard;
    }
    return cleanCard.replaceFirst("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-****-****-$4");
  }

  /**
   * 자유 텍스트(JSON 직렬화된 파라미터 등) 안의 개인정보 후보를 마스킹한다. 감사 로그에 개인정보 원문을 남기지 않기 위한 용도다.
   *
   * <p>- 연속 13자리 숫자는 주민번호 후보로 보고 maskRrn을 적용한다. - 연속 10~11자리 숫자는 전화번호 후보로 보고 maskPhone을 적용한다.
   */
  public static String maskPrivacyInText(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String masked = RRN_PATTERN.matcher(text).replaceAll(match -> maskRrn(match.group()));
    return PHONE_PATTERN.matcher(masked).replaceAll(match -> maskPhone(match.group()));
  }
}
