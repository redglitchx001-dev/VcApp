# Real-time voice injection on a rooted Android (Magisk + LSPosed)

This is the "proper" way to make a voice changer feed your **changed voice straight
into the microphone** of WhatsApp, Discord, Messenger or any other app — instead of
routing it through the speaker like stock phones have to do.

> ⚠️ **This requires root and is experimental.** You are modifying how Android reads
> audio. Read everything, back up your phone, and know how to disable a module from
> **safe mode / recovery** if something goes wrong and the phone won't boot normally.

---

## 1. Why the normal app can't do this

Android sandbox forbids one app from replacing the microphone of another app.
WhatsApp, Discord and Messenger always read the **real physical microphone** through
the OS. There is no public API to inject audio into their input — this is an Android
architecture limitation, not a bug in VcApp.

A **rooted** phone can get around this by *hooking* the call app's audio capture
(`AudioRecord` / `AAudio`) so that it receives processed audio instead of the real mic.

---

## 2. The three layers you need

| Layer | What it does | Notes |
|---|---|---|
| **Magisk** | Gives root + a module system | You already have this |
| **LSPosed** | Hooks the call app's audio capture inside the app process | Needed for virtual-mic modules |
| **Virtual-mic module** | Feeds your changed voice into the hooked capture | e.g. GlassMic, Echidna, PhantomMic |

---

## 3. Critical: Android 15/16 breaks classic LSPosed

On Android 15 and 16 the **classic LSPosed does not load**, which is why older
virtual-mic modules (e.g. PhantomMic) "don't work" even on a rooted phone. The
working combination reported by users on Android 16 is:

- **Zygisk-Next** — the addon Zygisk provider (turn OFF Magisk's built-in Zygisk and
  install Zygisk-Next instead).
- **JingMatrix LSPosed 1.10.2** — the maintained fork of LSPosed. Builds are served
  from its **GitHub Actions** (you must be logged in to download them).

> Community reports (2025): "Use Zygisk-Next instead of Magisk's Zygisk — that is what
> worked for me on Android 16." And a Pixel 8 + Android 15 user confirmed PhantomMic
> stopped loading once LSPosed hooks no longer attached.

---

## 4. Step-by-step setup

### 4.1 Update Magisk
Open Magisk, make sure root works (`su` in a terminal). Keep Magisk and its modules
up to date.

### 4.2 Disable built-in Zygisk, install Zygisk-Next
1. Magisk → Settings → turn **Zygisk OFF**.
2. Install the **Zygisk-Next** module from its repository.
3. Reboot.

### 4.3 Install JingMatrix LSPosed 1.10.2
1. Get the APK from **JingMatrix/LSPosed** GitHub → **Actions** → latest successful
   build (sign in to GitHub to download artifacts).
2. Install it, then enable the LSPosed module in Magisk.
3. Reboot.

### 4.4 Install a virtual-microphone module
Two open-source options that target Android 15/16:

- **GlassMic** (`lm060719/io.mo.glassmic`)
  - Virtual microphone; hooks both `AudioRecord` and native `AAudio`.
  - Floating ball, notification controls, optional TTS, per-consumer sample-rate
    conversion.
  - Recommended scope: `android` and `system`, plus your call apps.
- **Echidna** (`supermarsx/echidna`)
  - Real-time voice changer injected **inside** the call app process.
  - Zygisk + LSPosed, native DSP (EQ, compressor, pitch, formant, reverb), presets,
    per-app profiles.
  - Explicitly warns it is very device-specific and may not work on many phones.

> Both are experimental. Only flash them if you can recover the device from safe mode
> / recovery if a module breaks boot.

### 4.5 Enable the module in LSPosed
1. Open **LSPosed**.
2. Enable the virtual-mic module.
3. Add **WhatsApp / Discord / Messenger** (and any call app you use) to its scope.
4. Reboot.

### 4.6 Use VcApp normally
1. Start VcApp (Live tab) with your voice/effects as usual.
2. Open your call app and start the call.
3. The module hooks the call app's `AudioRecord` so it receives VcApp's processed
   voice instead of the real microphone.

---

## 5. Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| Module "doesn't load" | Classic LSPosed on Android 15/16 | Use Zygisk-Next + JingMatrix LSPosed 1.10.2 (see §3) |
| Hooks attach but nothing plays | Sample-rate / channel mismatch | Pick a module with per-consumer conversion (GlassMic) |
| Phone bootloops after flashing | Bad module / SELinux | Boot to safe mode or recovery and disable the module |
| Works in recorder but not in call | Call uses a different capture path | Try the native AAudio/tinyalsa path or a different module |

---

## 6. Safer fallback (no root needed)

If you don't want the risk, the loudspeaker/earpiece path in VcApp works everywhere:
use **wired earphones with a mic**, Output = **Earpiece**, and talk near the mic. You
don't hear yourself and the other side hears your changed voice clearly.

---

## Fair use

Made for fun and creativity. Do not use this to impersonate somebody, harass anyone,
or record calls where that is illegal in your country.
