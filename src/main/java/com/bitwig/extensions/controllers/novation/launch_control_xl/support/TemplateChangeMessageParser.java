package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import java.util.OptionalInt;

/**
 * Parses Launch Control XL template change sysex messages (factory + user templates).
 */
public final class TemplateChangeMessageParser {

   private static final String TEMPLATE_CHANGE_PREFIX = "f0002029021177";

   private TemplateChangeMessageParser() {
   }

   public static OptionalInt parseTemplateId(final String sysex) {
      if (sysex == null || !sysex.endsWith("f7") || sysex.length() < TEMPLATE_CHANGE_PREFIX.length() + 3) {
         return OptionalInt.empty();
      }
      if (!sysex.startsWith(TEMPLATE_CHANGE_PREFIX)) {
         return OptionalInt.empty();
      }
      final String templateHex = sysex.substring(TEMPLATE_CHANGE_PREFIX.length(), TEMPLATE_CHANGE_PREFIX.length() + 2);
      final int templateId;
      try {
         templateId = Integer.parseInt(templateHex, 16);
      } catch (NumberFormatException ex) {
         return OptionalInt.empty();
      }
      if (templateId < 0 || templateId > 0x0F) {
         return OptionalInt.empty();
      }
      return OptionalInt.of(templateId);
   }

   public static OptionalInt parseUserTemplateSlot(final String sysex) {
      final OptionalInt templateId = parseTemplateId(sysex);
      if (templateId.isEmpty()) {
         return OptionalInt.empty();
      }
      final int value = templateId.getAsInt();
      if (value < 0 || value >= 8) {
         return OptionalInt.empty();
      }
      return OptionalInt.of(value + 1);
   }
}
