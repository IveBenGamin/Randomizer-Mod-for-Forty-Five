package com.fourinachamber.fortyfive.archipelago

import com.badlogic.gdx.graphics.Color
import com.fourinachamber.fortyfive.utils.AdvancedTextParser
import io.github.archipelagomw.flags.NetworkItem as NetworkItemFlags

object APColors {

    val colors: Map<String, Color> = mapOf(
        "cyan"      to Color.valueOf("00EEEE"),
        "plum"      to Color.valueOf("AF99EF"),
        "slateblue" to Color.valueOf("6D8BE8"),
        "salmon"    to Color.valueOf("FA8072"),
        "green"     to Color.valueOf("00FF7F"),
        "blue"      to Color.valueOf("6495ED"),
        "magenta"   to Color.valueOf("EE00EE"),
        "yellow"    to Color.valueOf("FAFAD2"),
        "orange"    to Color.valueOf("FF7700"),
        "red"       to Color.valueOf("EE0000"),
        "white"     to Color.valueOf("FFFFFF"),
    )

    val textEffects: List<AdvancedTextParser.AdvancedTextEffect> = colors.map { (name, color) ->
        AdvancedTextParser.AdvancedTextEffect.AdvancedColorTextEffect("\$${name}\$", color)
    }

    fun classify(flags: Int): String = when {
        flags and NetworkItemFlags.ADVANCEMENT != 0 -> "Progression"
        flags and NetworkItemFlags.USEFUL != 0      -> "Useful"
        flags and NetworkItemFlags.TRAP != 0        -> "Trap"
        else                                        -> "Filler"
    }

    private fun wrap(text: String, colorName: String): String = "$${colorName}$${text}$${colorName}$"

    fun itemWrap(text: String, classification: String): String = wrap(text, when (classification) {
        "Progression" -> "plum"
        "Useful"      -> "slateblue"
        "Trap"        -> "salmon"
        else          -> "cyan"
    })

    fun itemWrap(text: String, flags: Int): String = itemWrap(text, classify(flags))

    // Convert $colorName$...$colorName$ indicators to LibGDX BitmapFont markup ([#rrggbbaa]...[]).
    // Used by NotificationOverlay which draws with BitmapFont directly rather than AdvancedTextParser.
    fun toMarkup(text: String): String {
        var result = text
        for ((name, color) in colors) {
            val indicator = "\$${name}\$"
            val open = "[#$color]"
            result = buildString {
                var remaining = result
                var inColor = false
                while (remaining.isNotEmpty()) {
                    val idx = remaining.indexOf(indicator)
                    if (idx == -1) {
                        append(remaining)
                        break
                    }
                    append(remaining.substring(0, idx))
                    append(if (inColor) "[]" else open)
                    inColor = !inColor
                    remaining = remaining.substring(idx + indicator.length)
                }
            }
        }
        return result
    }
}