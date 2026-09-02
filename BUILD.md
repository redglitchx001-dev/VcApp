# Build VcApp

Ai 3 căi să obții APK-ul de debug. Cea mai rapidă e să-l descarci gata construit
din GitHub Actions; pentru testare pe termen lung poți construi local, pe PC sau
direct pe telefon (Termux).

## 1. Descarcă APK-ul gata construit (cel mai rapid)

Repo-ul are workflow-ul [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)
care rulează la fiecare push pe `main`, la fiecare pull request și manual.

1. Deschide: **https://github.com/redglitchx001-dev/VcApp/actions**
2. Intră pe ultimul run verde **`Build APK`**.
3. Jos, la **Artifacts**, descarcă **`VcApp-debug-apk`**.
4. Dezarhivează zip-ul → obții `VcApp-debug-<sha>.apk` (debug, semnat).
5. Instalează pe telefon: `adb install VcApp-debug-<sha>.apk` sau copiază-l pe
   telefon și deschide-l (cu „Install unknown apps" activat pentru browser/file manager).

## 2. Build pe telefon — Termux + proot-distro Ubuntu (fără root/Magisk)

Merge pe orice telefon cu **~4 GB RAM liberi** (primul build e lent, ~15–40 min,
și descarcă ~1.5 GB). Nu ai nevoie de root: `proot-distro` rulează în userspace.

### Pasul 1 — o singură dată, în Termux

```bash
pkg update && pkg upgrade -y
pkg install -y proot-distro
proot-distro install ubuntu        # ~1 GB, o singură dată
```

### Pasul 2 — intră în Ubuntu și rulează scriptul de build

```bash
proot-distro login ubuntu

apt-get update && apt-get install -y git
git clone https://github.com/redglitchx001-dev/VcApp.git
cd VcApp
bash scripts/termux-build.sh        # instalează JDK 17 + Android SDK 34 + face build-ul
```

Scriptul face tot: instalează **JDK 17**, descarcă **command-line tools**,
acceptă licențele, instalează `platform-tools`, `platforms;android-34` și
`build-tools;34.0.0`, scrie `local.properties` și rulează `./gradlew assembleDebug`.

APK-ul apare în:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Pasul 3 — scoate APK-ul din Ubuntu și instalează-l

Ieși din Ubuntu și copiază APK-ul în Downloads-ul telefonului (din Termux):

```bash
exit          # ieși din proot-distro, ești din nou în Termux
termux-setup-storage    # o singură dată, dă permisiune la storage
cp /data/data/com.termux/files/usr/var/lib/proot-distro/installed-rootfs/ubuntu/root/VcApp/app/build/outputs/apk/debug/app-debug.apk \
   ~/storage/downloads/
```

Apoi deschizi **Downloads** din aplicația Files, apeși pe `app-debug.apk` și îl
instalezi (permite „Install unknown apps" pentru Files).

> Dacă ai clonat repo-ul în altă cale sau ca alt user, ajustează traseul
> `/root/VcApp/...` de mai sus.

### Depanare (Termux)

| Problemă | Rezolvare |
|---|---|
| `OutOfMemoryError` / build omorât | În `gradle.properties` pune `org.gradle.jvmargs=-Xmx2048m` (sau `1024m` pe telefoane cu 4 GB) și `org.gradle.workers.max=2`. |
| Erori de licențe SDK | `yes \| $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses` |
| `sdkmanager` not found | Verifică să existe `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` (zip-ul se extrage uneori cu alt nume de folder — scriptul îl mută automat). |
| Download command-line tools pică (versiune veche) | Actualizează `CMDLINE_TOOLS_VERSION` din `scripts/termux-build.sh` cu ultimul build de pe https://developer.android.com/studio |
| Build-ul se oprește când blochezi telefonul | Ține ecranul aprins / nu închide Termux în timpul build-ului. |

## 3. Build local pe PC (Android Studio)

- **Android Studio Koala sau mai nou** (vine cu JDK 17 inclus)
- **Android SDK Platform 34** + **Build-Tools 34** (Android Studio le descarcă singur)

```bash
git clone https://github.com/redglitchx001-dev/VcApp.git
# open folderul VcApp în Android Studio, apoi:
./gradlew assembleDebug     # APK debug
./gradlew installDebug      # instalează direct pe telefonul conectat
```

Wrapper-ul Gradle (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`) este commituit în
repo, deci `./gradlew` merge direct — nu mai e nevoie de `gradle wrapper`.

Dacă Gradle nu găsește SDK-ul, creează `local.properties` în rădăcină:

```properties
sdk.dir=/home/utilizator/Android/Sdk        # Linux
# sdk.dir=C:\\Users\\Nume\\AppData\\Local\\Android\\Sdk   # Windows
```

## APK de release (semnat)

```bash
keytool -genkey -v -keystore vcapp.jks -keyalg RSA -keysize 2048 -validity 10000 -alias vcapp
```

Apoi în Android Studio: **Build → Generate Signed Bundle / APK → APK → release**.

## După instalare, pe telefon

1. Dă permisiune la **Microfon** și **Notificări** la prima pornire.
2. Pentru bula flotantă: **Settings → Apps → VcApp → Display over other apps → Allow**
   (butonul din tab-ul *Live* te duce direct acolo).
3. Scoate VcApp din optimizarea bateriei, ca serviciul să nu fie oprit în timpul apelurilor.
