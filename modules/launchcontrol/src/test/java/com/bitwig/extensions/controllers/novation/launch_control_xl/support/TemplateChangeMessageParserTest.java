package com.bitwig.extensions.controllers.novation.launch_control_xl.support;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateChangeMessageParserTest {

   @Test
   void parsesUserSlot() {
      final OptionalInt slot = TemplateChangeMessageParser.parseUserTemplateSlot("f000202902117700f7");
      assertTrue(slot.isPresent());
      assertEquals(1, slot.getAsInt());
   }

   @Test
   void parsesFactoryTemplateId() {
      final OptionalInt template = TemplateChangeMessageParser.parseTemplateId("f00020290211770cf7");
      assertTrue(template.isPresent());
      assertEquals(12, template.getAsInt());
   }

   @Test
   void rejectsInvalidMessage() {
      final OptionalInt slot = TemplateChangeMessageParser.parseUserTemplateSlot("f01234");
      assertTrue(slot.isEmpty());
   }
}
