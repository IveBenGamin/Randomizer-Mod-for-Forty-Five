package com.fourinachamber.fortyfive.archipelago

import com.badlogic.gdx.Gdx
import com.fourinachamber.fortyfive.game.PermaSaveState
import com.fourinachamber.fortyfive.game.SaveState
import com.fourinachamber.fortyfive.map.events.RandomCardSelection
import com.fourinachamber.fortyfive.rendering.NotificationOverlay
import kotlin.math.min

object ItemsAndLocations {

    val pendingTraps: MutableList<String> = mutableListOf()

    fun receiveItem(itemName: String, playerName: String, flags: Int) {
        Gdx.app.postRunnable {
            NotificationOverlay.show(if (playerName == APClient.myName) {
                "Gave \$magenta\$yourself\$magenta$ ${APColors.itemWrap(itemName, flags)}"
            } else {
                "Received ${APColors.itemWrap(itemName, flags)} from \$yellow$$playerName\$yellow$"
            })
            when (itemName) {
                "Hot Potato Trap" -> pendingTraps.add("Hot Potato Trap")
                "Bewitched Trap" -> pendingTraps.add("Bewitched Trap")
                "Bewitching Trap" -> pendingTraps.add("Bewitching Trap")
                "Burning Trap" -> pendingTraps.add("Burning Trap")
                "1 Cash" -> SaveState.earnMoney(1)
                "5 Cash" -> SaveState.earnMoney(5)
                "10 Cash" -> SaveState.earnMoney(10)
                "25 Cash" -> SaveState.earnMoney(25)
                "50 Cash" -> SaveState.earnMoney(50)
                "75 Cash" -> SaveState.earnMoney(75)
                "100 Cash" -> SaveState.earnMoney(100)
                "Partial Heal" -> SaveState.playerLives = min(SaveState.maxPlayerLives, SaveState.playerLives + (17..23).random())
                "Full Heal" -> SaveState.playerLives = SaveState.maxPlayerLives
                "Health Upgrade" -> {
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


    val LOCATIONS: Map<String, Long> by lazy {
        val fixed = buildMap<String, Long> {
            var currentId = 1L
            for (n in 2..4 step 2) put("$n Enemies Defeated", currentId++)
            for (n in 6..20 step 2) put("$n Enemies Defeated", currentId++)
            for (n in 22..60 step 2) put("$n Enemies Defeated", currentId++)
            for (n in 62..100 step 2) put("$n Enemies Defeated", currentId++)
        }
        val bulletLocations = buildMap<String, Long> {
            var currentId = 51L
            for (card in APCardPool.cards) put(card.title, currentId++)
        }
        fixed + bulletLocations
    }
}