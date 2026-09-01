# CI — build APK pe GitHub Actions

Workflow-ul e în `ci/build-apk.yml`, dar **trebuie mutat** în
`.github/workflows/build-apk.yml` ca GitHub să-l ruleze.

Nu l-am putut pune direct acolo: tokenul agentului nu are permisiunea
`workflows`, iar GitHub respinge push-ul care creează fișiere în
`.github/workflows/`. Mutarea o faci tu, o singură dată.

## Varianta 1 — din browser (30 secunde)

1. Deschizi repo-ul pe GitHub, branch-ul `arena/01a05dc3-vcapp`.
2. **Add file → Create new file**.
3. La nume scrii: `.github/workflows/build-apk.yml`
4. Copiezi tot conținutul din `ci/build-apk.yml` și îl lipești acolo.
5. **Commit changes** direct pe `arena/01a05dc3-vcapp`.

## Varianta 2 — din terminal

```bash
git checkout arena/01a05dc3-vcapp
git pull
mkdir -p .github/workflows
git mv ci/build-apk.yml .github/workflows/build-apk.yml
git commit -m "CI: build debug APK"
git push
```

## Ce face workflow-ul

- rulează la fiecare push pe `arena/01a05dc3-vcapp`, la pull request și manual
  (**Actions → Build APK → Run workflow**)
- JDK 17 + Android SDK + Gradle 8.7
- generează wrapper-ul Gradle (nu e commituit în repo)
- `./gradlew assembleDebug`
- urcă rezultatul ca artifact: **VcApp-debug-apk** → `VcApp-debug-<sha>.apk`,
  păstrat 30 de zile
- dacă buildul pică, urcă și rapoartele din `app/build/reports/`

## De unde iei APK-ul

**Actions** → ultimul run verde → jos, la **Artifacts** → `VcApp-debug-apk`.
Descarci zip-ul, îl dezarhivezi și instalezi `.apk`-ul pe telefon
(trebuie activat „Install unknown apps" pentru browser/file manager).

APK-ul e **debug, semnat cu cheia de debug** — bun pentru instalat pe telefonul
tău, nu pentru Play Store. Pentru release semnat vezi `BUILD.md`.
