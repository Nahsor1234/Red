# JeE JEE 2027 — UI Design System

**Product:** Offline-first JEE 2027 preparation command center for students who need a calm, focused study workflow. Native Android app using Kotlin and Jetpack Compose + Material 3. **Direction:** Midnight focus studio with Swiss-style hierarchy: precise typography, deliberate negative space, cool-blue focus cues, and success green only for completed work.

## Product rules

- Dashboard values must come from stored study/task data. Never seed fake tasks, study totals, progress or streaks.
- Keep the experience useful offline. Do not add cloud dependency for UI polish.
- Make the next relevant action easy to find: resume a real task, start a focus block, review a weak chapter, or inspect actual study progress.
- Avoid shame-based streaks, urgency theater, excessive decoration and childlike gamification.

## Visual tokens

| Role | Value |
|---|---|
| App background | `#0A0F17` |
| Card surface | `#111B29` |
| Raised/featured surface | `#172438` |
| Card border | `#29384C` |
| Divider/track | `#263448` |
| Primary focus blue | `#8FAEFF` |
| Light blue accent | `#BFD1FF` |
| Soft blue surface | `#202F4B` |
| Success green | `#63D6A2` |
| Warning | `#F4C56D` |
| Destructive | `#FF7285` |
| Main text | `#F3F6FC` |
| Secondary text | `#A8B5C7` |
| Muted text | `#8493A7` |

Use blue for selected navigation, focus actions, active progress and information emphasis. Use green for success/completion only. Keep secondary text readable against the darkest card surface. Avoid colored text on a similarly bright fill; primary buttons use dark foreground text.

## Typography and layout

- Use Android system sans-serif (`FontFamily.SansSerif`) to preserve offline behavior and native rendering. Use tabular numerals for time, durations, counts and progress values.
- Use a compact hierarchy: display for rare hero moments; headline for screen titles; title for section/card labels; body for instructions; labels for metadata and eyebrow text. Avoid all-caps for long text; reserve tracked uppercase for short overlines such as “FOCUS STUDIO”.
- Keep mobile content single-column and scrollable, respect system insets, and maintain at least 48dp touch targets with 8dp between adjacent targets.
- Base spacing: 4 / 8 / 12 / 18 / 26 / 34dp. Shapes: 12dp controls, 18dp cards, 24dp featured surfaces, pill chips.

## Surfaces and effects

- Use a near-black navy base with two extremely subtle ambient blue/cyan radial glows. Do not use tiled science doodles, noisy textures, or competing gradients.
- Standard cards use a restrained dark tonal gradient, one-pixel low-contrast border, and modest elevation. Featured focus cards may use a slightly brighter blue-navy gradient and a soft accent border.
- Keep decorative blur/shadow inexpensive. Reserve the strongest surface emphasis for the next action and active focus timer.
- Inputs and dialogs use the same surfaces, shapes, borders, and focus blue. Show clear pressed, selected, disabled and error states.

## Motion and interaction

- Use short, purposeful Compose transitions: 160ms fast feedback, ~220ms state changes, ~300ms layout changes. Use spring motion only for small selection/press feedback.
- Animate progress changes, task completion, filter selection, timer state and screen transitions; preserve state and keep final values readable without motion.
- Respect Android system animation/reduced-motion preferences. Avoid infinite pulses, parallax, auto-advancing carousels, and animation that delays a study action.
- Provide haptic feedback only for meaningful actions; retain semantics, content descriptions and screen-reader state descriptions. Use Material controls and accessible hit areas.

## Screen patterns

- **Dashboard:** Greeting and exam context → one recommended/selected study focus with a clear timer CTA → today’s persisted study/goal summary → daily consistency and today’s tasks → weak-chapter attention when real evidence exists.
- **Syllabus:** Subject selection → subject progress from actual chapter data → compact status filters and search → chapter rows with transparent completion/confidence status.
- **Tasks:** Remaining/completed summary → filters → grouped, scannable tasks → obvious add action. Completion is reversible and acknowledged.
- **Focus timer:** Session context, accessible timer ring, clear start/pause/reset actions, compact duration presets, actual recent sessions. Do not animate the ring continuously when paused.
- **Analytics/revision/AI/settings:** Reuse the same background, card language, typography, top bar and navigation. Data visualizations must distinguish measured values from estimates and handle empty states honestly.

## Avoid

- Generic marketing patterns such as testimonials/carousels in the study workflow.
- Decorative glass blur, neon glow, oversized gradients, random color per card, emoji icons, confetti, fake streaks, fake metrics or text below accessible contrast.
- Relying on color alone for states; use labels/icons/semantics as well.
