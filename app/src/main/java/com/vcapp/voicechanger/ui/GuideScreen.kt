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
