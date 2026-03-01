# KanjiSage Roadmap

Current: **v1.6.1** (Beta) | Stage: v1.x.x = Beta, v2.x.x = Store Release

---

## v1.7.0 — Glass UI Theme (Target: 2026-03-15)
- Glass morphism UI across all screens (frosted panels, transparency, blur)
- Shared glass theme system with KanjiJourney / EigoSage / EigoJourney
- Dark mode support with glass overlays
- Redesigned Settings and Profile screens with glass cards
- Camera overlay glass pill redesign (timer, debug HUD)

## v1.8.0 — AI Dialog Vision (Target: 2026-04-01)
- **Scan → AI understands → conversation**: tap detected kanji to start AI dialog
- Multi-turn conversation about scanned text (meaning, usage, context)
- Contextual grammar explanations for detected sentences
- Example sentences generated from scanned vocabulary
- History of scanned words with AI-generated study notes
- **Dependency**: Backend AI endpoint (Claude API or on-device LLM)

## v1.9.0 — Play Store Readiness (Target: 2026-04-15)
- CameraX upgrade 1.3.1 → 1.4+ (16KB page alignment fix)
- Animated splash screen (motion logo, ~1s branded intro)
- Store listing assets: icon, screenshots (6 devices), feature graphic
- Data safety form completion
- IARC content rating questionnaire
- Create subscription products in Play Console (`kanjisage_premium_monthly`, `kanjisage_premium_annual`)
- Internal testing track deployment + purchase flow verification
- Accessibility review (TalkBack, font scaling, contrast)

## v1.10.0 — Word Transfer & Cross-App (Target: 2026-05-01)
- **Word transfer to KanjiJourney**: send scanned kanji to study deck
- J Coin cross-app rewards (scan in KanjiSage, earn in ecosystem)
- Shared vocabulary database format with EigoSage
- Deep links between Sage ↔ Journey apps
- **Dependency**: KanjiJourney word import API, shared J Coin backend

## v2.0.0 — Store Release (Target: 2026-06-01)
- Google Play Store publication (US + Japan)
- Staged rollout (10% → 50% → 100%)
- Production monitoring and crash reporting (Firebase Crashlytics)
- App Store Connect submission (iOS port decision)
- Marketing: jworks-ai.com landing page, promo video
- Press kit for Japanese language learning communities

---

## Future (v2.x.x — Post-Launch)

### v2.1.0 — Professional Tools
- Medical terminology mode (医学用語): specialized kanji dictionary for healthcare interpreters
- Legal terminology mode (法律用語): contract/court document scanning
- Conference interpreter assist: real-time scan + translation overlay
- Custom dictionary import (user-defined term lists)

### v2.2.0 — Advanced OCR
- Handwritten kanji recognition (ML Kit handwriting or custom model)
- Traditional kanji (旧字体) support
- Multi-column vertical text (newspaper/magazine layout)
- Document scanning mode (photo → full-page OCR → structured output)

### v2.3.0 — Social & Community
- Share scanned passages with friends
- Community word lists (curated vocabulary packs)
- Leaderboards via J Coin ecosystem
- Teacher dashboard for classroom use (B2B licensing)

### v2.4.0 — Platform Expansion
- iOS native port (SwiftUI + Vision framework)
- iPad companion app with larger display features
- Raspberry Pi kiosk mode (museum/school installations)
- Wear OS quick-scan widget

---

## Cross-App Dependencies

| Feature | Depends On | App |
|---------|-----------|-----|
| Word transfer to study deck | KanjiJourney word import API | KanjiJourney |
| J Coin cross-app rewards | Shared J Coin backend | All apps |
| Glass UI theme | Shared theme library | All apps |
| Shared vocabulary format | Common schema definition | EigoSage, EigoJourney |
| Deep links | URL scheme registration | KanjiJourney |

---

## Milestones

| Milestone | Version | Target |
|-----------|---------|--------|
| Beta feature-complete | v1.10.0 | 2026-05-01 |
| Play Store submission | v2.0.0 | 2026-06-01 |
| 100 active users | v2.0.x | 2026-07-01 |
| Professional tools | v2.1.0 | 2026-08-01 |
| iOS launch | v2.4.0 | 2026-Q4 |

---

Last updated: 2026-03-01
