package com.scbk.sms.service.menu;

import com.scbk.sms.exception.CustomException;
import com.scbk.sms.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 요청 URL을 메뉴 권한으로 검증한다.
 *
 * <p>판정 순서: 1. 요청 URL이 메뉴 URL과 정확히 일치하면 화면 접근으로 보고 READ를 검증한다. (/campaign/sms/register처럼 suffix와
 * 겹치는 화면 URL이 suffix 규칙보다 먼저 잡힌다) 2. 일치하는 메뉴가 없으면 URL suffix를 떼고 부모 화면 URL 기준으로 액션 권한을 검증한다. 3. 어느
 * 메뉴에도 연결되지 않는 URL은 거부한다.
 *
 * <p>"활성 메뉴 존재"와 "현재 역할에 부여된 권한"은 별개다. 활성 메뉴로 등록된 URL에 현재 역할이 아무 권한도 부여받지 못한 경우(빈 권한), suffix 부모의
 * 권한으로 우회하지 않고 곧바로 접근을 거부한다. 그렇지 않으면 부모 메뉴의 READ/CREATE 권한이 정확 메뉴 화면으로 승격되는 UI 권한 상승이 발생한다.
 */
@Service
public class MenuAuthService {

  /** URL suffix -> 필요 권한. 등록/수정 겸용 legacy /save는 CREATE와 UPDATE를 모두 요구한다. */
  private static final Map<String, Set<MenuPermission>> SUFFIX_PERMISSIONS =
      createSuffixPermissions();

  private final MenuSource menuSource;

  public MenuAuthService(MenuSource menuSource) {
    this.menuSource = menuSource;
  }

  public void checkAccess(String path, List<String> roleCodes) {
    Set<MenuPermission> screenPermissions = menuSource.getPermissions(path, roleCodes);
    if (!screenPermissions.isEmpty()) {
      if (screenPermissions.contains(MenuPermission.READ)) {
        return;
      }
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    // 활성 메뉴로 등록됐지만 현재 역할에 부여된 권한이 없으면 suffix 부모 권한으로 우회하지 않고 거부.
    if (menuSource.hasMenu(path)) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    String suffix = matchSuffix(path);
    if (suffix != null) {
      String baseUrl = path.substring(0, path.length() - suffix.length());
      Set<MenuPermission> basePermissions = menuSource.getPermissions(baseUrl, roleCodes);
      if (basePermissions.containsAll(SUFFIX_PERMISSIONS.get(suffix))) {
        return;
      }
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    throw new CustomException(ErrorCode.ACCESS_DENIED);
  }

  /**
   * pageAuth 렌더링용 기준 메뉴 URL. {@link #checkAccess} 1·2단계와 동일한 의미로 path 가 등록된 메뉴면 그대로, 아니면 등록된
   * suffix를 제거한 부모 화면 URL을 반환한다. 어느 쪽도 아니면 원래 path를 반환한다. 실제 접근 차단은 여전히 {@link
   * #checkAccess}(Interceptor)가 최종 권한으로 사용한다.
   *
   * <p>활성 메뉴로 등록됐지만 현재 역할에 부여된 권한이 없는 경우(빈 Set)에도 정확 URL 자신을 반환한다. suffix 부모로 넘어가면 {@code
   * PageAuth}가 부모의 쓰기 권한을 노출한다.
   */
  public String resolveBaseMenuPath(String path, List<String> roleCodes) {
    if (menuSource.hasMenu(path) || !menuSource.getPermissions(path, roleCodes).isEmpty()) {
      return path;
    }
    String suffix = matchSuffix(path);
    if (suffix != null) {
      String baseUrl = path.substring(0, path.length() - suffix.length());
      if (!baseUrl.isEmpty()) {
        return baseUrl;
      }
    }
    return path;
  }

  /** {@code path}가 끝나는 등록된 suffix를 반환한다. 없으면 {@code null}. */
  private String matchSuffix(String path) {
    for (String suffix : SUFFIX_PERMISSIONS.keySet()) {
      if (path.endsWith(suffix)) {
        return suffix;
      }
    }
    return null;
  }

  private static Map<String, Set<MenuPermission>> createSuffixPermissions() {
    Map<String, Set<MenuPermission>> map = new LinkedHashMap<>();
    map.put("/data", Set.of(MenuPermission.READ));
    map.put("/search", Set.of(MenuPermission.READ));
    map.put("/detail", Set.of(MenuPermission.READ));
    map.put("/popup", Set.of(MenuPermission.READ));
    map.put("/tree", Set.of(MenuPermission.READ));
    map.put("/create", Set.of(MenuPermission.CREATE));
    map.put("/register", Set.of(MenuPermission.CREATE));
    map.put("/update", Set.of(MenuPermission.UPDATE));
    map.put("/save", Set.of(MenuPermission.CREATE, MenuPermission.UPDATE));
    map.put("/delete", Set.of(MenuPermission.DELETE));
    map.put("/approve", Set.of(MenuPermission.APPROVE));
    map.put("/reject", Set.of(MenuPermission.APPROVE));
    map.put("/cancel", Set.of(MenuPermission.CANCEL));
    map.put("/excel", Set.of(MenuPermission.DOWNLOAD));
    map.put("/download", Set.of(MenuPermission.DOWNLOAD));
    map.put("/export", Set.of(MenuPermission.DOWNLOAD));
    map.put("/unmask", Set.of(MenuPermission.MASK_VIEW));
    return map;
  }
}
