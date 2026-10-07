# 004 — Minimal navigation state holder instead of a navigation library

* Status: **Accepted**
* Date: 2026-10-07
* Phase: 4

## Context

The dashboard links into an application list, the list into a detail screen, and detail into a
comparison screen (spec §5). The project declares no navigation artifact, and spec §19 requires
inspecting existing capability before adding a dependency.

## Decision

Implement navigation as a small typed back stack held in Compose state:

* `Screen` — sealed set of destinations with their arguments (`Dashboard`, `AppList(filter, query)`,
  `AppDetail(packageName)`, `Compare(first, second)`).
* `AppNavigator` — holds `List<Screen>`, exposes `navigateTo`, `pop`, `popToRoot`, `canGoBack`.
* Root composable renders `navigator.current` and wires `BackHandler` to `pop()`.

## Why

* Four destinations with primitive arguments do not justify a navigation artifact, its runtime and
  its argument type-safety workarounds.
* The stack is ordinary state: it is simple to read, to test and to serialise later if needed.
* No deep links, no nested graphs, no bottom-bar destinations exist in this product.

## Consequences

* Back stack position is not saved across process death (it survives configuration changes because
  it lives in the activity's composition scope of a retained ViewModel graph). Restoring the stack
  from saved state is a documented limitation, not a silent bug.
* Adding a fifth screen with complex argument passing would be the trigger to revisit this decision
  and adopt `androidx.navigation:navigation-compose`.
