# Technical notes: Android text-selection toolbar investigation

## Scope

The initial investigation used a diagnostic archive from a Nothing Phone (4a) running Android 16 / SDK 36. Device-specific fingerprints and unrelated system details are intentionally omitted from this public note.

## Findings

- DeepL's package is `com.deepl.mobiletranslator`.
- Its text-processing activity is `com.deepl.mobiletranslator/.MiniTranslatorActivity`.
- The framework contains `android.widget.Editor$ProcessTextIntentActionsHandler.onInitializeMenu(Menu)`.
- In the examined framework, standard `PROCESS_TEXT` menu entries are added with `MenuItem.setShowAsAction(0)`, corresponding to `SHOW_AS_ACTION_NEVER`, which normally directs them to overflow.
- The framework also contains `com.android.internal.widget.floatingtoolbar.FloatingToolbar` and `LocalFloatingToolbarPopup`.
- `LocalFloatingToolbarPopup.layoutMainPanelItems(List, int)` was identified as a candidate point for adjusting the final toolbar item layout.
- The corresponding popup implementation was not found in the examined SystemUI APK, supporting a framework/app-process hook rather than a SystemUI-only hook.

## Why DeepL may move between the main row and overflow

The observed behavior—DeepL appearing in the visible row for short selections but under the overflow menu for longer selections—may reflect differences in menu composition, another action path, or OEM-specific layout behavior. The initial findings alone do not prove which factor is responsible in every app.

## Proposed approach

The prototype attempts to hook `LocalFloatingToolbarPopup.layoutMainPanelItems` in app processes to apply priority and visibility rules. It also attempts to alter matching menu items' `showAsAction` flags.

This is a candidate implementation, not a confirmed fix. The exact parameter types and item representation can vary by Android build. Compile and device tests must confirm that the hook matches the target framework and that reordering the supplied list is safe.

## Limitations

- Apps using fully custom selection UIs may bypass the framework popup.
- `SHOW_AS_ACTION_ALWAYS` cannot guarantee visibility if space is insufficient.
- A genuine second row requires coordinated popup layout, measurement, positioning, and touchable-inset changes and is not implemented by the prototype.
