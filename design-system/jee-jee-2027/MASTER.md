# JeE JEE 2027 — Product UI System

**Platform:** Native Android, Kotlin, Jetpack Compose + Material 3. **Product:** Offline-first JEE preparation command center. **Direction:** Light-first editorial study workspace, borrowing the skill-verified Minimalism & Swiss style. Use crisp paper surfaces, dark ink hierarchy, blue for navigation/learning state, and warm orange for the primary focus action. Do not confuse a design-system change with a screen redesign: every flow needs its own information hierarchy and adaptive interaction model.

## Product invariants

- Preserve SharedPreferences/JSON data, repository calls, callback contracts, route/back-stack behavior, input validation, and existing real stored metrics.
- Keep study and note-taking useful offline. Cloud-dependent catalog questions and optional AI/sync must clearly say when network/provider availability is required.
- Never add seeded tasks, sessions, goals, test results, analytics, mistakes, or chat history.
- Make learning actions obvious, but don't imply unsupported behavior: the dashboard timer CTA selects the timer tab and currently does not transfer focus context; Notes are not a spaced-repetition scheduler; practice has no scored result unless stored assessment records provide it.

## Visual tokens

| Role | Value |
|---|---|
| App background | `#F6F7FB` |
| Main surface | `#FFFFFF` |
| Soft secondary surface | `#EEF2F8` |
| Borders / dividers | `#E0E6F0` / `#E4E9F1` |
| Main ink | `#192436` |
| Secondary ink | `#536176` |
| Muted ink | `#66758A` |
| Navigation / learning blue | `#2563EB` |
| Deep blue for text on light surfaces | `#1D4ED8` |
| Soft selected blue | `#E7EEFF` |
| Primary focus CTA orange | `#EA580C` (dark text `#172033`) |
| Warm focus surface | `#FFF4EA` |
| Success | `#16805D` |
| Warning | `#B45309` |
| Error | `#B42318` |

System sans-serif is used for native rendering and offline availability; use clear weight/size hierarchy and tabular numerals for time/counts. Primary type: 16sp body, 13–14sp supporting text, 20–28sp screen/section heads. Don't render essential information as tiny metadata. Spacing: 4 / 8 / 12 / 16 / 24 / 32dp. Controls 14dp corners, standard cards 20dp, featured panels 28dp. Navigation outer shape is a true circle/capsule, not a rounded rectangle.

## Layout and component rules

- Rebuild screen information hierarchy: each screen has one dominant purpose, an explicit heading, useful grouping, and fewer competing cards. White-space, alignment and type—not extra borders—establish sections.
- Use white raised surfaces over `#F6F7FB`, very subtle elevation, quiet one-pixel outlines and occasional warm feature surfaces. Avoid dark glass, neon, heavy shadows and tile wallpaper.
- Main tab navigation is a floating capsule: 16–20dp horizontal inset, safe-area-aware bottom inset, fully rounded ends, five labelled destinations, a quiet selected pill inside, and a full-slot target at least 48dp high. Keep tab labels; ensure content clears the capsule.
- Preserve Material touch feedback, clear focus states, accessible semantics and independent nested actions. Use at least 48dp targets and 8dp separation where possible.
- Use adaptive `LazyColumn`/`LazyVerticalGrid` for collections, safe-area/IME-aware forms and pinned actions only when they remain reachable at large text scales.
- Empty, loading, failed, disabled, no-data and true-zero states are distinct. Label data provenance when local fallback/cloud/network matters.

## Motion

- Compose state motion uses approximately 140ms fast, 210ms standard and 280ms layout timings with standard easing/spring only for small selection feedback.
- Tab changes use a restrained low-distance fade/shared-axis; drill-in routes push/pop directionally. Keep screen state and back behavior stable.
- Animate only actual state changes: focus selection, question movement, progress, completion, save/sync, sheet transitions. Preserve final text/values and avoid unstable reordering or focus theft.
- Timer ring updates smoothly while remaining text changes on its existing tick. AI streaming follows only when reader is already at the bottom; don't animate/reparse an entire transcript per chunk.
- Respect Android system animation scale; no infinite pulses, decorative parallax, overshoot spectacle, or motion that delays study actions.

## Screen-by-screen structure

- **Home:** Compact greeting/context → one clear next-study-action panel → real daily goal/study summary → consistency and a short Today task list → weak chapter only when real signal exists.
- **Tasks:** Completed/remaining summary → truthful filters/count scopes → Today/Upcoming/Completed groups → independent check and details targets → reachable add-task form.
- **Focus:** Session context → responsive progress ring/time → large start/pause/resume action → duration presets/custom → real recent sessions.
- **Syllabus:** Subject → succinct progress → persistent search and filters → dominant chapter list with actual completion/weak/due/time metadata.
- **Chapter:** Chapter/mastery summary → real question availability and practice CTA → topic checklist; do not pretend generic checkpoint copy is lesson content.
- **Practice:** Question progress → readable math prompt/options → result/explanation → stable safe-area action dock and accessible jump/bookmark surfaces.
- **Notes:** Local notes library with search and one create action; editor preserves existing autosave/tags. Do not call it scheduled revision.
- **Mistakes:** Real unresolved count/import status → scannable mistake records → reveal saved solution → type/resolve actions; no fabricated answers.
- **Assessment:** Setup → one question at a time → truthful completion summary/history. Preserve question pools, attempt calculations, negative marks and existing records.
- **Analytics:** Existing metrics, review, readable 14-day activity, subjects, chapter evidence, mistake patterns; distinguish no baseline from measured zero.
- **Daily log:** Month/consistency summary → accessible calendar → selected-day details; only today is editable/completable; separate local save and cloud backup.
- **AI:** Clear readiness, study-data actions and coach entry; readable grouped transcript, keyboard-safe composer and reader-aware streaming; history is local and settings retain secret safety.
- **Settings/account/notifications:** Group destinations by purpose; distinguish optional account/cloud from offline use; make permission, channel, sync, backup and schedule states precise and actions independently accessible.

## Avoid

Token-only recolors; squarish full-width bottom nav; generic productivity clutter; data and labels below legibility; repeated bordered cards with equal emphasis; hidden or misleading action outcomes; fake data; inaccessible nested click targets; focus CTAs implying context transfer; high-motion loops; new cloud requirements, permissions, data schemas or features without user approval.
