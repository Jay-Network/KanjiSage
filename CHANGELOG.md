# KanjiSage Changelog

All notable changes to KanjiSage are documented here.
Format follows [vMAJOR.MINOR.PATCH](VERSIONING.md): MAJOR=stage, MINOR=feature, PATCH=deploy.

---

## v1.6.1 (2026-03-01)
- Renamed KanjiLens → KanjiSage (package, strings, branding, J Coin business ID)
- Updated all source files (95 .kt), build configs, XML resources, ProGuard rules
- Updated KanjiQuest → KanjiJourney cross-promo references
- Updated 32 documentation files
- Applied JWorks versioning standard (vMAJOR.MINOR.PATCH)

## v1.6.0 (2026-02-20)
- Major platform upgrade: store-ready release build
- J Coin unified API integration
- Cumulative milestone rewards, share trigger, store UI, balance badge
- Adaptive OCR overlay — hide furigana when lines are too dense

## v1.5.2 (2026-02-13)
- Splash animation slowed 2x for smoother branded intro (~3.6s)
- Landscape rotation support (removed portrait lock)

## v1.5.1 (2026-02-13)
- Handle system: user display names (3-20 chars, alphanumeric+underscore)
- Handle prompt after 3rd scan, dismissible, editable from Profile
- Profile screen handle display with avatar initial

## v1.5.0 (2026-02-13)
- Anonymous-first auth: local UUID sessions, no auth wall on launch
- Splash → onboarding → camera flow (no sign-in required)
- AuthScreen repurposed as "Link Account" with back button
- Guest display in Profile and Settings
- Sign out → fresh anonymous session (stays on camera)
- Privacy-first: no real names stored, email-only from Google
- Onboarding tutorial: 4-page HorizontalPager
- Help & About screen with usage tips and links

## v1.4.0 (2026-02-15)
- Adaptive OCR overlay — hide furigana when lines are too dense
- Density-based rendering thresholds

## v1.3.0 (2026-02-13)
- Native offline dictionary (Kuromoji on-device)
- Button layout redesign + pricing update
- Stabilized rewards flow
- Profile developer-tier controls update

## v1.2.0 (2026-02-11)
- J Coin rewards stabilization
- Profile developer tools (admin premium toggle)

## v1.1.0 (2026-02-09)
- Replaced Stripe with Google Play Billing (`billing-ktx:6.1.0`)
- Free-tier: 5 scans/day, 60-second timer with countdown pill
- Scan-expired overlay with "Start New Scan" + "Upgrade" buttons
- PaywallScreen with localized Play Store prices
- Release APK signing (23MB vs 40MB debug)
- OCR pipeline optimization + pause/play scan feature
- Supabase feedback system (Edge Functions, shared with KanjiJourney)
- Flash icon (vector drawables, yellow tint when on)
- Profile screen with ecosystem links and subscription badge
- CameraScreen 2x3 draggable button grid
- Paywall dismiss fix (always show "Maybe later")
- Settings: "Manage Subscription on Google Play"

## v1.0.1 (2026-02-06)
- Vertical text mode (縦書き) with per-character stacked furigana
- Partial mode detection area filtering with per-segment clipping
- Draggable boundary + floating buttons for partial/full mode
- Kuromoji morphological analyzer for context-aware furigana
- UI customization: font size range, opacity, bold/color toggles
- FILL_CENTER coordinate mapping fixes
- Overlay stability improvements + crash prevention
- Fixed: Z Flip 7 crash (Compose BOM 2024.01.00→2024.02.02)
- Fixed: Division by zero in OverlayCanvas
- Fixed: Empty bounding boxes drawn
- Fixed: drawText crash on negative positions
- Fixed: Toggle button animation race condition
- 32 unit tests (all passing)

## v1.0.0 (2026-02-05)
- Initial beta release
- CameraX integration with tap-to-focus, flash, rotation
- ML Kit Japanese OCR with offline text recognition
- Text overlay rendering with labels and debug HUD
- Phase 2 furigana integration with JMDict database
- Per-kanji-segment rendering with positional alignment
- Settings UI and system insets
- Hilt dependency injection
- Room + SQLite local dictionary
- 22 unit tests

## v0.1.0 (2026-02-05)
- Initial Android project setup
- Project documentation and strategic planning
- Git repository initialized
