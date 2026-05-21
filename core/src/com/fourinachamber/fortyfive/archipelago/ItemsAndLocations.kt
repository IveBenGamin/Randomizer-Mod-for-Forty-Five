package com.fourinachamber.fortyfive.archipelago

import com.badlogic.gdx.Gdx
import com.fourinachamber.fortyfive.game.PermaSaveState
import com.fourinachamber.fortyfive.game.SaveState
import com.fourinachamber.fortyfive.map.events.RandomCardSelection
import com.fourinachamber.fortyfive.rendering.NotificationOverlay
import com.fourinachamber.fortyfive.utils.FortyFiveLogger
import io.github.archipelagomw.flags.NetworkItem as NetworkItemFlags
import kotlin.math.min

object ItemsAndLocations {

    private const val logTag = "ItemsAndLocations"

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    val pendingTraps: MutableList<String> = mutableListOf()

    val LOCATIONS: Map<String, Long> by lazy {
        val fixed = buildMap<String, Long> {
            var currentId = 1L
            for (n in 2..4 step 2)   put("$n Enemies Defeated", currentId++)
            for (n in 6..20 step 2)  put("$n Enemies Defeated", currentId++)
            for (n in 22..60 step 2) put("$n Enemies Defeated", currentId++)
            for (n in 62..100 step 2) put("$n Enemies Defeated", currentId++)
        }
        val bulletLocations = buildMap<String, Long> {
            var currentId = 51L
            for (card in APCardPool.cards) put(card.title, currentId++)
        }
        fixed + bulletLocations
    }

    // -------------------------------------------------------------------------
    // Behavior
    // -------------------------------------------------------------------------

    fun receiveItem(itemName: String, playerName: String, flags: Int) {
        val resolvedFlags = resolveFlags(itemName, flags)
        FortyFiveLogger.debug(logTag, "receiveItem: $itemName flags=$flags resolvedFlags=$resolvedFlags classification=${APColors.classify(resolvedFlags)}")
        Gdx.app.postRunnable {
            if (resolvedFlags and NetworkItemFlags.TRAP == 0) {
                NotificationOverlay.show(if (playerName == APClient.myName) {
                    "Gave \$magenta\$yourself\$magenta$ ${APColors.itemWrap(itemName, resolvedFlags)}"
                } else {
                    "Received ${APColors.itemWrap(itemName, resolvedFlags)} from \$yellow$$playerName\$yellow$"
                })
            }
            when (itemName) {
                "Hot Potato Trap"  -> pendingTraps.add("Hot Potato Trap")
                "Bewitched Trap"   -> pendingTraps.add("Bewitched Trap")
                "Bewitching Trap"  -> pendingTraps.add("Bewitching Trap")
                "Burning Trap"     -> pendingTraps.add("Burning Trap")
                "1 Cash"           -> SaveState.earnMoney(1)
                "5 Cash"           -> SaveState.earnMoney(5)
                "10 Cash"          -> SaveState.earnMoney(10)
                "25 Cash"          -> SaveState.earnMoney(25)
                "50 Cash"          -> SaveState.earnMoney(50)
                "75 Cash"          -> SaveState.earnMoney(75)
                "100 Cash"         -> SaveState.earnMoney(100)
                "Partial Heal"     -> SaveState.playerLives = min(SaveState.maxPlayerLives, SaveState.playerLives + (17..23).random())
                "Full Heal"        -> SaveState.playerLives = SaveState.maxPlayerLives
                "Health Upgrade"   -> {
                    val amount = (5..8).random()
                    SaveState.maxPlayerLives += amount
                    SaveState.playerLives += amount
                }
                "Progressive Town Unlock" -> {
                    PermaSaveState.townsUnlockedCount++
                    PermaSaveState.write()
                }
                else -> {
                    val cardName = RandomCardSelection.allCardPrototypes.find { it.title == itemName }?.name
                    if (cardName != null) SaveState.queueCard(cardName)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    // Archipelago strips flags from precollected (start inventory) items, sending flags=0 for everything.
    // This map restores the correct flags by name so notifications still show the right color.
    private val itemFlagsByName: Map<String, Int> = buildMap {
        val t = NetworkItemFlags.TRAP
        val u = NetworkItemFlags.USEFUL
        val a = NetworkItemFlags.ADVANCEMENT
        put("Hot Potato Trap",  t); put("Bewitched Trap", t); put("Bewitching Trap", t); put("Burning Trap", t)
        put("25 Cash", u); put("50 Cash", u); put("75 Cash", u); put("100 Cash", u)
        put("Partial Heal", u); put("Full Heal", u)
        put("Health Upgrade", a); put("Progressive Town Unlock", a)
    }

    private fun resolveFlags(itemName: String, flags: Int): Int {
        if (flags != 0) return flags
        itemFlagsByName[itemName]?.let { return it }
        if (APCardPool.cards.any { it.title == itemName }) return NetworkItemFlags.USEFUL
        return 0
    }
}