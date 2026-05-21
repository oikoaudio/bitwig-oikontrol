package com.bitwig.extensions.controllers.novation.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleLedTest {

   @Test
   void stringBuilderFlushClearsDirtyState() {
      final SimpleLed led = new SimpleLed(0x90, 0x0A);
      final StringBuilder first = new StringBuilder();
      final StringBuilder second = new StringBuilder();

      led.flush(first);
      led.flush(second);

      assertEquals(" 0a 00", first.toString());
      assertEquals("", second.toString());
   }

   @Test
   void stringBuilderFlushSendsAgainAfterColorChange() {
      final SimpleLed led = new SimpleLed(0x90, 0x0A);
      final StringBuilder initial = new StringBuilder();
      final StringBuilder changed = new StringBuilder();

      led.flush(initial);
      led.setColor(5);
      led.flush(changed);

      assertEquals(" 0a 00", initial.toString());
      assertEquals(" 0a 05", changed.toString());
   }
}
