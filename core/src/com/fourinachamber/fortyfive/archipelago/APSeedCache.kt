package com.fourinachamber.fortyfive.archipelago

import com.badlogic.gdx.Gdx
import com.fourinachamber.fortyfive.utils.FortyFiveLogger
import onj.builder.buildOnjObject
import onj.parser.OnjParser
import onj.value.OnjObject

object APSeedCache {

    private const val logTag = "APSeedCache"
    private const val saveFilePath = "saves/APCache/seed.onj"

    fun readSeed(): String? {
        val file = Gdx.files.local(saveFilePath).file()
        if (!file.exists()) return null
        return try {
            val obj = OnjParser.parseFile(file) as? OnjObject ?: return null
            obj.getOr<String?>("apSeed", null)
                ?: obj.getOr<Long?>("apSeed", null)?.toString()
        } catch (e: Exception) {
            FortyFiveLogger.warn(logTag, "failed to read seed file: ${e.message}")
            null
        }
    }

    fun writeSeed(seed: String) {
        val file = Gdx.files.local(saveFilePath).file()
        file.parentFile?.mkdirs()
        val obj = buildOnjObject { "apSeed" with seed }
        file.writeText(obj.toString())
        FortyFiveLogger.debug(logTag, "wrote seed: $seed")
    }
}