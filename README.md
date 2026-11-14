# Study Quest

Single-file learning app (`index.html`) packaged as an installable PWA, built and deployed by GitHub Actions.

## Publish (installable web app)
1. Create a GitHub repo and push these files to `main`.
2. Repo **Settings > Pages > Source: GitHub Actions**.
3. The *Deploy to GitHub Pages* workflow runs on every push. Your app is at `https://<user>.github.io/<repo>/`.
4. Open that link on your phone: **Chrome > Install app** / **Safari > Share > Add to Home Screen**. On desktop Chrome/Edge, click the install icon in the address bar.

## Optional: Android APK
Actions tab > *Build Android APK* > **Run workflow**. Download `study-quest-apk` from the run's artifacts (debug build; allow "install unknown apps" to sideload). Push a tag like `v1.0.0` to attach it to a Release.

## Notes
- Keep `index.html` as the app file name. Edit it, push, and the site updates (the service worker is re-stamped with the commit SHA on each deploy).
- Progress is stored in the browser's localStorage per install; Supabase login/sync works as before.

## Android home / lock screen widget
Built into the APK (see `android-widget/` and `scripts/add-widget.sh`). Install the APK, long-press your home screen > **Widgets** > **Study Quest streak**. It shows your streak, best streak, the last 7 days (🔥 played, 🥶 missed, 🧊 freeze used) and your freezes with progress to the next one. Tap it to open the app. It updates whenever you finish a level or leave the app, and refreshes itself about every 30 minutes. The widget shows the course you last had open.
Lock screen: the widget declares lock-screen support, but whether your phone offers lock-screen widgets depends on the Android version and brand.
