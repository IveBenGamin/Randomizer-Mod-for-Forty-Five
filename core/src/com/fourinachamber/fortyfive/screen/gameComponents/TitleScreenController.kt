package com.fourinachamber.fortyfive.screen.gameComponents

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.utils.TimeUtils
import com.fourinachamber.fortyfive.FortyFive
import com.fourinachamber.fortyfive.archipelago.APClient
import com.fourinachamber.fortyfive.game.PermaSaveState
import com.fourinachamber.fortyfive.game.SaveState
import com.fourinachamber.fortyfive.game.UserPrefs
import com.fourinachamber.fortyfive.screen.SoundPlayer
import com.fourinachamber.fortyfive.screen.general.*
import com.fourinachamber.fortyfive.screen.general.customActor.CustomInputField
import com.fourinachamber.fortyfive.screen.general.customActor.OffSettable
import com.fourinachamber.fortyfive.utils.TemplateString
import com.fourinachamber.fortyfive.utils.Timeline
import kotlin.math.sin

class TitleScreenController : ScreenController() {

    private lateinit var screen: OnjScreen

    private var transitionAwayVelocity: Float = -1f
    private val timeline: Timeline = Timeline()

    private var isConfirmed: Boolean = false

    override fun init(onjScreen: OnjScreen, context: Any?) {
        screen = onjScreen
        timeline.startTimeline()
        TemplateString.updateGlobalParam(
            "title_screen.startButtonText",
            if (SaveState.playerCompletedFirstTutorialEncounter) "Continue" else "Start your Journey"
        )
        TemplateString.updateGlobalParam("title_screen.apConnectionError", "")
        APClient.pendingAutoConnect?.let { pending ->
            APClient.consumePendingAutoConnect()
            (screen.namedActorOrError("ap_address_field") as? CustomInputField)?.setText(pending.host)
            (screen.namedActorOrError("ap_slot_name_field") as? CustomInputField)?.setText(pending.slot)
            (screen.namedActorOrError("ap_password_field") as? CustomInputField)?.setText(pending.password ?: "")
            APClient.connectionResultCallback = { success, errorMessage ->
                if (!success) {
                    TemplateString.updateGlobalParam(
                        "title_screen.apConnectionError",
                        "Auto-connect failed: $errorMessage"
                    )
                    screen.enterState(showAPConnectionErrorState)
                    screen.enterState(showAPConnectionPopupScreenState)
                }
            }
            APClient.connect(pending.host, pending.slot, pending.password)
        }
        if (!PermaSaveState.hasSeenInDevPopup) timeline.appendAction(Timeline.timeline {
            action {
                screen.enterState(showInDevelopmentReminder)
            }
            delayUntil { isConfirmed }
            action {
                screen.leaveState(showInDevelopmentReminder)
                PermaSaveState.hasSeenInDevPopup = true
            }
        }.asAction())
    }

    override fun onUnhandledEvent(event: Event) = when (event) {

        is QuitGameEvent -> timeline.appendAction(Timeline.timeline {
            action {
                screen.enterState(showConfirmationPopupScreenState)
                TemplateString.updateGlobalParam("title_screen.popupText", "Are you sure you want to quit?")
            }
            delayUntil { isConfirmed || showConfirmationPopupScreenState !in screen.screenState }
            action {
                if (isConfirmed) Gdx.app.exit()
            }

        }.asAction())

        is AbandonRunEvent -> timeline.appendAction(Timeline.timeline {
            action {
                screen.enterState(showConfirmationPopupScreenState)
                TemplateString.updateGlobalParam("title_screen.popupText", "Are you sure you want to abandon" +
                        " your run? You will loose all the progress you made and all bullets that haven't been saved yet.")
            }
            delayUntil { isConfirmed || showConfirmationPopupScreenState !in screen.screenState }
            action {
                if (isConfirmed) FortyFive.newRun(false)
                isConfirmed = false
                screen.leaveState(showConfirmationPopupScreenState)
            }

        }.asAction())

        is ResetGameEvent -> if (APClient.isArchipelago) {
            screen.enterState(showNoResetDuringAPPopupState)
        } else timeline.appendAction(Timeline.timeline {
            action {
                screen.enterState(showConfirmationPopupScreenState)
                TemplateString.updateGlobalParam("title_screen.popupText", "Are you sure you want to reset" +
                        " the game? The game will behave as if it were freshly installed.")
            }
            delayUntil { isConfirmed || showConfirmationPopupScreenState !in screen.screenState }
            action {
                if (isConfirmed) FortyFive.resetAll()
                TemplateString.updateGlobalParam(
                    "title_screen.startButtonText",
                    if (SaveState.playerCompletedFirstTutorialEncounter) "Continue" else "Start your journey"
                )
                isConfirmed = false
                screen.leaveState(showConfirmationPopupScreenState)
            }

        }.asAction())

        is PopupConfirmationEvent -> {
            isConfirmed = true
        }

        is StartGameEvent -> {
            FortyFive.changeToInitialScreen()
        }


        is APButtonClickEvent -> {
            if (APClient.isArchipelago && showConfirmationPopupScreenState !in screen.screenState) {
                timeline.appendAction(Timeline.timeline {
                    action {
                        screen.enterState(showConfirmationPopupScreenState)
                        TemplateString.updateGlobalParam("title_screen.popupText", "Are you sure you want to disconnect from the Archipelago server?")
                    }
                    delayUntil { isConfirmed || showConfirmationPopupScreenState !in screen.screenState }
                    action {
                        if (isConfirmed) {
                            APClient.disconnect()
                            APClient.isArchipelago = false
                            APClient.swapSaveFiles()
                            PermaSaveState.read()
                            SaveState.read()
                            UserPrefs.read()
                            TemplateString.updateGlobalParam(
                                "title_screen.startButtonText",
                                if (SaveState.playerCompletedFirstTutorialEncounter) "Continue" else "Start your Journey"
                            )
                        }
                        isConfirmed = false
                        screen.leaveState(showConfirmationPopupScreenState)
                    }
                }.asAction())
            } else {
                screen.enterState(showAPConnectionPopupScreenState)
            }
        }

        is APConnectEvent -> {
            val address = (screen.namedActorOrError("ap_address_field") as CustomInputField).text.toString()
            val slotName = (screen.namedActorOrError("ap_slot_name_field") as CustomInputField).text.toString()
            val password = (screen.namedActorOrError("ap_password_field") as CustomInputField).text.toString().takeIf { it.isNotBlank() }
            val connectButton = screen.namedActorOrError("ap_connect_button") as CustomLabel
            screen.leaveState(showAPConnectionErrorState)
            connectButton.isDisabled = true
            APClient.connectionResultCallback = { success, errorMessage ->
                if (showAPConnectionPopupScreenState in screen.screenState) {
                    if (success) {
                        screen.leaveState(showAPConnectionPopupScreenState)
                        TemplateString.updateGlobalParam(
                            "title_screen.startButtonText",
                            if (SaveState.playerCompletedFirstTutorialEncounter) "Continue" else "Start your Journey"
                        )
                        connectButton.isDisabled = false
                    } else {
                        TemplateString.updateGlobalParam(
                            "title_screen.apConnectionError",
                            "Failed to connect: $errorMessage"
                        )
                        screen.enterState(showAPConnectionErrorState)
                        connectButton.isDisabled = false
                    }
                }
            }
            APClient.connect(address, slotName, password)
        }

        else -> {}

    }

    private fun doTransitionAwayAnim() {
        transitionAwayVelocity += Gdx.graphics.deltaTime * 80f
        repeat(15) { i ->
            val actor = screen.namedActorOrError("title_screen_bullet_${i + 1}")
            actor as OffSettable
            actor.offsetY -= transitionAwayVelocity
        }
    }

    override fun update() {
        timeline.updateTimeline()
        if (transitionAwayVelocity != -1f) {
            doTransitionAwayAnim()
            return
        }
        if (OnjScreen.transitionAwayScreenState in screen.screenState) {
            transitionAwayVelocity = 0f
            repeat(1) { i ->
                screen.afterMs(i * 30) {
                    SoundPlayer.situation("title_screen_card_drop", screen)
                }
            }
            return
        }
        if (APClient.isArchipelago) screen.enterState(showAPConnectedState)
        else screen.leaveState(showAPConnectedState)
        repeat(15) { i ->
            val actor = screen.namedActorOrError("title_screen_bullet_${i + 1}")
            actor as OffSettable
            actor.offsetY = sin(TimeUtils.millis() * 0.001 + i * i * 100).toFloat() * 6f
        }
    }

    companion object {
        const val showConfirmationPopupScreenState = "show_confirmation_popup"
        const val showInDevelopmentReminder = "show_in_development_reminder"
        const val showAPConnectionPopupScreenState = "show_ap_connection_popup"
        const val showAPConnectionErrorState = "show_ap_connection_error"
        const val showAPConnectedState = "ap_connected"
        const val showNoResetDuringAPPopupState = "show_no_reset_during_ap_popup"
    }

}
