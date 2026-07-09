package com.scbk.sms.service.system.scaffold;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

final class ResourceTemplateRenderer {

  private static final Pattern TOKEN_PATTERN = Pattern.compile("@@[A-Z0-9_]+@@");

  private ResourceTemplateRenderer() {}

  static String render(String resourcePath, Map<String, String> tokens) {
    String rendered = readResource(resourcePath);
    for (Map.Entry<String, String> entry : tokens.entrySet()) {
      rendered = rendered.replace("@@" + entry.getKey() + "@@", entry.getValue());
    }
    if (TOKEN_PATTERN.matcher(rendered).find()) {
      throw new IllegalStateException("Unresolved scaffold template token in " + resourcePath);
    }
    return rendered;
  }

  private static String readResource(String resourcePath) {
    ClassLoader loader = ResourceTemplateRenderer.class.getClassLoader();
    try (InputStream input = loader.getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Scaffold template resource not found: " + resourcePath);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read scaffold template: " + resourcePath, e);
    }
  }
}
