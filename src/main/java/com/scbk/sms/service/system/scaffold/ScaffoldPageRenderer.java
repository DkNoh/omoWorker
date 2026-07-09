package com.scbk.sms.service.system.scaffold;

import java.util.Map;

interface ScaffoldPageRenderer {
  Map<String, String> render(ScaffoldModel model);
}
