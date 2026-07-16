package com.example.model

object AndroidSdkLabels {
    fun getName(apiLevel: Int): String = when (apiLevel) {
        37 -> "Android 17"
        36 -> "Baklava / 16"
        35 -> "V / 15"
        34 -> "U / 14"
        33 -> "T / 13"
        32 -> "S_V2 / 12L"
        31 -> "S / 12"
        30 -> "R / 11"
        29 -> "Q / 10"
        28 -> "Pie / 9"
        26, 27 -> "Oreo / 8"
        else -> "Lollipop+"
    }
}
