# Frontend Guidelines

This document is the long-term frontend reference for OpsPilot.
Use it for new features, refactors, and UI polish work.

## 1. Scope and Principles
- Keep existing route behavior, auth guards, role checks, and API contracts stable unless a task explicitly changes them.
- Prefer small, reusable primitives over one-off page-specific styling.
- Preserve dark-theme visual language with strong semantic color feedback.
- Design for operators using the app daily: clarity, speed, and low cognitive load.

## 2. Stack and Architecture
- Framework: React + TypeScript + Vite + React Router.
- Styling: TailwindCSS utilities + shared CSS tokens/classes in `src/index.css`.
- UI building blocks live in `src/components/ui`.
- Shared utility helpers live in `src/lib`.

## 3. Visual System

### Color
- Use layered dark surfaces (`bg`, `surface`, `surface-elevated`) instead of flat backgrounds.
- Use semantic color intent consistently:
  - Success: green
  - Warning: amber/yellow
  - Error: red/rose
  - Info: sky/blue
- Prefer colored backgrounds with high-contrast text for status chips/callouts instead of low-contrast colored text only.

### Typography
- Keep the current two-font setup:
  - UI/body: DM Sans
  - Monospace/data: JetBrains Mono
- Maintain clear hierarchy with deliberate spacing and line-height.

### Spacing and Surfaces
- Use a consistent 4px rhythm through Tailwind spacing tokens.
- Cards/panels should keep subtle border + depth (`shadow-soft`).

## 4. Reusable Components
Prefer these primitives before introducing new custom wrappers:
- `Button`
- `Badge`
- `Card`
- `Panel`
- `PageHeader`
- `EmptyState`
- `LoadingState`
- `ErrorState`
- `Sheet`
- `TypingText`

If a new pattern repeats in 2+ places, promote it to `src/components/ui`.

## 5. Forms and Inputs
- Use shared form classes from `index.css`:
  - `.app-label`
  - `.app-input`
  - `.app-textarea`
  - `.app-select`
- Labels should be plain readable text (not pill backgrounds).
- Inputs must preserve readable foreground/placeholder/autofill contrast on dark theme.
- Keep focus-visible states obvious and accessible.

## 6. Motion Guidelines
- Motion should communicate state or hierarchy; avoid decorative-only animations.
- Respect `prefers-reduced-motion` (already supported globally in `index.css`).
- Use shared motion classes for entry and card transitions.
- Text typing animation rules:
  - Use `TypingText` for staged reveal where it improves comprehension (landing hero, chat answers).
  - Avoid restarting typing on rerenders.
  - For sequential text blocks, chain completion events intentionally.

## 7. Route-Level UX Expectations
Preserve these route intents:
- `/login`, `/register`: clear auth forms, strong readability, and responsive layout.
- `/dashboard`: quick operational snapshot with clear KPIs and actions.
- `/tenant-users`: searchable/manageable user list and invite flow.
- `/tenant-settings`: safe settings editing with clear save feedback.
- `/documents`: upload-focused workflow, status visibility, details panel.
- `/chat`: conversation-first layout, confidence visibility, sources, and low-confidence guidance.
- `/tickets`, `/analytics`: keep intentionally blank/placeholder until full implementation tasks are requested.

## 8. Accessibility Baseline
- Maintain WCAG AA contrast where possible.
- Keep keyboard navigation complete for forms, actions, and overlays.
- Do not remove focus rings; style them to match the theme.
- Ensure responsive behavior at mobile/tablet/desktop breakpoints.

## 9. Implementation Checklist (Per Frontend Task)
1. Confirm route behavior/API contracts remain intact.
2. Reuse existing UI primitives first.
3. Apply semantic colors for status and feedback.
4. Validate dark-theme readability (especially form fields and labels).
5. Check motion behavior and reduced-motion fallback.
6. Run `cd frontend && npm run build`.
7. Update `tasks.md` with completed work and verification evidence.
8. If a user correction exposed a process gap, add a note to `tasks/lessons.md`.

## 10. Non-Goals
- Do not introduce a new design system framework unless explicitly requested.
- Do not redesign unrequested backend/service behavior from frontend tasks.
- Do not replace implemented flows with placeholders.
