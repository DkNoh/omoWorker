package com.scbk.sms.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 외부 API 호출 시스템 한 곳의 인증 정보.
 *
 * <p>API Key 원문은 설정 파일이나 DB에 저장하지 않는다. 외부 시스템에 키를 최초 한 번 전달한 뒤 서버에는 SHA-256 해시만
 * {@code SMS_EXTERNAL_API_KEY_SHA256} 환경변수로 주입한다. 예제는 단일 client를 지원하며, 연동 시스템이 늘어나면 이 클래스를 DB 기반
 * client 저장소로 교체한다.
 */
@ConfigurationProperties(prefix = "sms.external-api.authentication")
public class ApiKeyProperties {

  /** 요청의 X-API-Client-Id와 비교할 외부 시스템 식별자. 비밀값은 아니다. */
  private String clientId = "";

  /** 요청 API Key 원문을 SHA-256으로 계산한 64자리 16진수. 키 원문을 설정하면 안 된다. */
  private String keySha256 = "";

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId == null ? "" : clientId.trim();
  }

  public String getKeySha256() {
    return keySha256;
  }

  public void setKeySha256(String keySha256) {
    this.keySha256 = keySha256 == null ? "" : keySha256.trim().toLowerCase();
  }

  /** client ID와 올바른 SHA-256 해시가 모두 주입된 경우에만 인증을 시도할 수 있다. */
  public boolean isConfigured() {
    return StringUtils.hasText(clientId) && keySha256.matches("[0-9a-f]{64}");
  }
}
