package com.scbk.sms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 메타데이터. /docs(Swagger UI), /v3/api-docs(OpenAPI JSON).
 *
 * <p>세션 인증(쿠키) 기반이라 bearer 스키마는 두지 않는다. 화면 컨트롤러는 Thymeleaf 반환(@Controller)이므로 기본적으로 springdoc가 REST
 * 엔드포인트(@ResponseBody/@RestController + ApiResponse 반환)를 중심으로 수집한다.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI omoWorkerOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("omoWorker API")
                .description(
                    "SMS/LMS/알림톡 발송 및 이력 관리 플랫폼 API. JSON 엔드포인트는 ApiResponse<T> 래퍼를" + " 사용한다.")
                .version("v1")
                .license(new License().name("Proprietary")));
  }
}
