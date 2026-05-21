package com.fourinachamber.fortyfive.archipelago

import com.badlogic.gdx.Gdx
import com.fourinachamber.fortyfive.game.card.Card
import com.fourinachamber.fortyfive.game.card.CardPrototype
import com.fourinachamber.fortyfive.map.events.RandomCardSelection
import onj.parser.OnjParser
import onj.value.OnjArray
import onj.value.OnjObject
import kotlin.random.Random

object APCardPool {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private val TUTORIAL_BULLETS = setOf("Big Bullet", "Worker Bullet", "Incendiary Bullet", "Silver Bullet")

    private val FLAVOUR_TEXTS: List<String> by lazy {
        val onj = OnjParser.parseFile(Gdx.files.internal("config/archipelago_flavortext.onj").file()) as OnjObject
        onj.get<OnjArray>("apFlavourTexts").value.map { it.value as String }
    }

    // -------------------------------------------------------------------------
    // Public card pools
    // -------------------------------------------------------------------------

    val cards: List<CardPrototype> by lazy {
        buildPool(RandomCardSelection.allCardPrototypes)
    }

    val realCards: List<CardPrototype> by lazy {
        buildRealPool(RandomCardSelection.allCardPrototypes)
    }

    val allCards: List<CardPrototype> by lazy {
        cards + realCards
    }

    val cardsByName: Map<String, CardPrototype> by lazy {
        cards.associateBy { it.name }
    }

    // -------------------------------------------------------------------------
    // Public functions
    // -------------------------------------------------------------------------

    fun archipelagoTitle(apCardName: String): String? = cardsByName[apCardName]?.title

    fun trueCardName(apCardName: String): String =
        apCardName.trimEnd { it == 'R' }.trimEnd { it.isDigit() }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private fun buildPool(protos: List<CardPrototype>): List<CardPrototype> {
        var fallbackSeed = 0L
        return protos
            .filter { "not used" !in it.tags && "unobtainable" !in it.tags }
            .sortedBy { it.name.lowercase() }
            .flatMap { proto ->
                val rawRarity = proto.tags
                    .firstOrNull { it.startsWith("rarity") }
                    ?.removePrefix("rarity")
                    ?.toIntOrNull() ?: 1
                val rarity = rawRarity - (if (proto.title in TUTORIAL_BULLETS) 1 else 0)
                if (rarity <= 0) return@flatMap emptyList()
                val baseTags = proto.tags
                    .filter { !it.startsWith("rarity") } + listOf("rarity1", "archipelago")
                (1..rarity).map { n ->
                    val variantTitle = "${proto.title} $n"
                    val originalCreator = proto.creator
                    CardPrototype(
                        name = "${proto.name}$n",
                        title = variantTitle,
                        type = proto.type,
                        tags = baseTags,
                        forceLoadCards = proto.forceLoadCards,
                    ).also { variant ->
                        variant.creator = { screen, isSaved, areHoverDetailsEnabled ->
                            proto.drawableHandle = "${Card.cardTexturePrefix}apItemBullet"
                            val card = originalCreator!!(screen, isSaved, areHoverDetailsEnabled)
                            proto.drawableHandle = null
                            val locationId = ItemsAndLocations.LOCATIONS[variantTitle]
                            val info = locationId?.let { APClient.scoutedLocations[it] }
                            if (info != null) {
                                val itemName = info.itemName
                                val playerName = info.playerName
                                val classification = APColors.classify(info.flags)
                                card.shortDescription = if (APClient.obscuredChoices) {
                                    if (playerName == APClient.myName) {
                                        "It could be any one of your bullets"
                                    } else {
                                        "It's... something... for \$yellow$$playerName\$yellow$."
                                    }
                                } else {
                                    if (playerName == APClient.myName) {
                                        "This is your ${APColors.itemWrap(itemName, classification)}. If you don\'t know what that is, I don\'t know what to tell ya."
                                    } else {
                                        "This ${APColors.itemWrap(itemName, classification)} belongs to \$yellow$$playerName\$yellow$\n\n\nCategory: $classification"
                                    }
                                }
                            }
                            card.flavourText = FLAVOUR_TEXTS[Random(locationId ?: fallbackSeed++).nextInt(FLAVOUR_TEXTS.size)]
                            card
                        }
                    }
                }
            }
    }

    private fun buildRealPool(protos: List<CardPrototype>): List<CardPrototype> =
        protos
            .filter { "not used" !in it.tags && "unobtainable" !in it.tags }
            .sortedBy { it.name.lowercase() }
            .flatMap { proto ->
                val rawRarity = proto.tags
                    .firstOrNull { it.startsWith("rarity") }
                    ?.removePrefix("rarity")
                    ?.toIntOrNull() ?: 1
                val rarity = rawRarity - (if (proto.title in TUTORIAL_BULLETS) 1 else 0)
                if (rarity <= 0) return@flatMap emptyList()
                val baseTags = proto.tags
                    .filter { !it.startsWith("rarity") } + listOf("rarity1", "real")
                (1..rarity).map { n ->
                    CardPrototype(
                        name = "${proto.name}${n}R",
                        title = "${proto.title} $n R",
                        type = proto.type,
                        tags = baseTags,
                        forceLoadCards = proto.forceLoadCards,
                    ).also { variant ->
                        val baseDrawable = "${Card.cardTexturePrefix}${proto.name}"
                        variant.creator = { screen, isSaved, areHoverDetailsEnabled ->
                            proto.drawableHandle = baseDrawable
                            val card = proto.creator!!(screen, isSaved, areHoverDetailsEnabled)
                            proto.drawableHandle = null
                            card
                        }
                    }
                }
            }
}