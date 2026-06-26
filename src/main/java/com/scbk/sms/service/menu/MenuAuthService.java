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

    for (Map.Entry<String, Set<MenuPermission>> entry : SUFFIX_PERMISSIONS.entrySet()) {
      String suffix = entry.getKey();
      if (!path.endsWith(suffix)) {
        continue;
      }
      String baseUrl = path.substring(0, path.length() - suffix.length());
      Set<MenuPermission> basePermissions = menuSource.getPermissions(baseUrl, roleCodes);
      if (basePermissions.containsAll(entry.getValue())) {
        return;
      }
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    throw new CustomException(ErrorCode.ACCESS_DENIED);
  }

  private static Map<String, Set<MenuPermission>> createSuffixPermissions() {
    Map<String, Set<MenuPermission>> map = new LinkedHashMap<>();
    map.put("/data", Set.of(MenuPermission.READ));
    map.put("/search", Set.of(MenuPermission.READ));
    map.put("/detail", Set.of(MenuPermission.READ));
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
