package com.example

import com.example.model.AndroidSdkLabels
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun sdkLabelsMapSupportedAndroidVersions() {
    assertEquals("Android 17", AndroidSdkLabels.getName(37))
    assertEquals("Baklava / 16", AndroidSdkLabels.getName(36))
    assertEquals("Q / 10", AndroidSdkLabels.getName(29))
  }

  @Test
  fun sdkLabelsUseFallbackForOlderVersions() {
    assertEquals("Lollipop+", AndroidSdkLabels.getName(24))
  }
}
