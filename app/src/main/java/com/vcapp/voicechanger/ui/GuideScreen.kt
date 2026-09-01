package com.vcapp.voicechanger.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Step(number: Int, title: String, body: String) {
    Text("$number. $title", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
}

@Composable
fun GuideScreen(modifier: Modifier = Modifier) {
    ScrollColumn(modifier) {

        SectionCard("Use VcApp during a call") {
            Step(
                1, "Start VcApp",
                "Open the Live tab and press Start voice changer. A notification keeps " +
                    "the engine alive while you switch apps."
            )
            Step(
                2, "Set the output to Speaker",
                "VcApp plays your processed voice through the loudspeaker."
            )
            Step(
                3, "Place your call",
                "Open WhatsApp, Discord, Messenger, Telegram or any other app and start " +
                    "the call, then turn on the call's loudspeaker."
            )
            Step(
                4, "Talk and play clips",
                "Use the floating bubble to switch voices or trigger MP3s without leaving " +
                    "the call app."
            )
        }

        SectionCard("Why it works this way") {
            Text(
                "Android does not let an app replace the microphone of another app: " +
                    "WhatsApp, Discord and Messenger always read the real microphone, and " +
                    "only a rooted phone or a desktop virtual audio cable can change that. " +
                    "VcApp therefore uses the loudspeaker path — your changed voice and your " +
                    "MP3s are played out loud and picked up by the call — plus a recorder so " +
                    "you can send processed audio as a voice message.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("\"I keep hearing myself\" or \"they can't hear me\"") {
            Text(
                "Your changed voice comes out of the phone's speaker, so you hear it and \"\n" +
                    "the call's microphone hears it too. That's how a stock phone works — it's not a bug.\n" +
                    "Live tab → Quick controls has a switch called \"Hear my own voice\". Turn it OFF if\n" +
                    "you don't want to listen to yourself.\n\n" +
                    "Important: with the speaker path, muting your own monitoring also mutes what\n" +
                    "the other person receives — the same speaker feeds both. To stop hearing yourself\n" +
                    "WHILE the other person still hears you, use earphones with a microphone: keep\n" +
                    "\"Hear my own voice\" ON, choose the Earpiece output, and talk near the mic.\n\n" +
                    "If they can't hear you at all, check: VcApp is Running (Live tab), Output is\n" +
                    "Speaker, \"Hear my own voice\" is ON, Volume is not all the way down, and your\n" +
                    "phone's media/call volume is up.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Optimal call setup (be heard clearly)") {
            Step(
                1, "Start VcApp, Output = Speaker",
                "Live tab → Start voice changer. Keep Output on Speaker and \"Hear my own voice\" ON — " +
                    "the speaker is what the call's microphone picks up."
            )
            Step(
                2, "Turn OFF the call app's loudspeaker",
                "In WhatsApp/Discord/Messenger start the call but do NOT enable its loudspeaker. " +
                    "You hear the other person through the earpiece (or headphones), so there is no " +
                    "feedback loop and they hear your changed voice clearly."
            )
            Step(
                3, "Choose a voice and set Volume + Bass",
                "Use the Voice sound panel on the Live tab, or the Effects tab, to set Volume (start " +
                    "around +4..+8 dB) and Bass to taste. Changes apply instantly during the call."
            )
            Step(
                4, "Use the floating bubble",
                "The overlay bubble lets you switch presets or fire MP3s without leaving the call."
            )
        }

        SectionCard("Root: real-time voice injection (Magisk + LSPosed)") {
            Text(
                "A rooted phone can go further and feed your changed voice straight into the " +
                    "call's microphone instead of through the speaker. It needs three layers: " +
                    "Magisk (root), LSPosed (hooking), and a virtual-microphone module. " +
                    "Full step-by-step: the file ROOT_VOICE_INJECTION.md in the project.\n\n" +
                    "On Android 15/16 the classic LSPosed does not load, so you MUST use:\n" +
                    "• Zygisk-Next (addon) instead of Magisk's built-in Zygisk\n" +
                    "• JingMatrix LSPosed 1.10.2 (builds from GitHub Actions)\n\n" +
                    "Modules known to target Android 15/16:\n" +
                    "• GlassMic — virtual microphone, AudioRecord + AAudio hooks, floating ball, " +
                    "designed for Android 15/16\n" +
                    "• Echidna — real-time voice changer injected into the call app itself\n\n" +
                    "Both are experimental and device-specific. Only flash them if you can recover " +
                    "the phone from safe mode / recovery if it bootloops.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Step(
                1, "Magisk root + unlock bootloader",
                "Magisk is already running on your phone. Keep it updated."
            )
            Step(
                2, "Install Zygisk-Next",
                "In Magisk, turn OFF the built-in Zygisk and install the Zygisk-Next addon instead — " +
                    "this is the key fix that makes LSPosed load on Android 16."
            )
            Step(
                3, "Install JingMatrix LSPosed 1.10.2",
                "Download the build from its GitHub Actions. Enable the module in Magisk and reboot."
            )
            Step(
                4, "Install a virtual-mic module",
                "Install GlassMic (or Echidna). In LSPosed, enable the module and add WhatsApp / " +
                    "Discord / Messenger to its scope. Reboot."
            )
            Step(
                5, "Use VcApp normally",
                "Keep VcApp running on Live tab. The module hooks the call app's AudioRecord so it " +
                    "receives your changed voice instead of the real microphone."
            )
        }

        SectionCard("Best results") {
            Text(
                "• Keep the phone flat, speaker facing up.\n" +
                    "• Lower the call volume a bit if you hear feedback howling.\n" +
                    "• Wired earphones with a mic give the cleanest result: the mic stays " +
                    "close to your mouth while the speaker plays the changed voice.\n" +
                    "• Use the Low cut and Noise gate controls to kill rumble and hiss.\n" +
                    "• Bluetooth output adds latency, speaker is the fastest.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Send a changed voice message") {
            Text(
                "Live tab → Record → talk with your effects on → Stop → Share, then pick " +
                    "WhatsApp, Discord, Messenger or any other app. You can also share an " +
                    "MP3 from another app straight into VcApp to add it to the soundboard.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Please be fair") {
            Text(
                "Use VcApp for fun and creativity. Do not use it to impersonate somebody, " +
                    "to harass people or to record calls where that is illegal in your country.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
