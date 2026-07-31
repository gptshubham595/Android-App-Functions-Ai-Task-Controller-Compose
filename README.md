# Android App Functions — AI Task Controller

> **"Hey Google, read my todo."**  
> A prototype exposing native app capabilities to on-device AI assistants via Android 16's App Functions API.

[![Medium](https://img.shields.io/badge/Medium-Read%20Article-black?logo=medium)](https://medium.com/stackademic/android-building-android-16s-app-functions-game-changer-for-ai-assistant-control-09bd194cbf88)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-green?logo=android)](https://developer.android.com/jetpack/compose)

---

## What it does

This prototype demonstrates how Android 16's **App Functions** enable an AI assistant to control a native app through natural language — without the user manually opening the app or navigating screens.

| User says | App Function triggered |
|-----------|------------------------|
| *"Hey Google, read my todo."* | `READ_TASKS` — returns task list to assistant |
| *"Mark my first task done."* | `COMPLETE_TASK` — updates local state and confirms |
| *"Add buy groceries."* | `CREATE_TASK` — inserts task via Room and confirms |

The assistant receives structured responses (not raw UI), enabling voice-first, hands-free interaction.

<img src="https://github.com/gptshubham595/TodoJetpackCompose/blob/main/Screen_recording_20250108_040942-ezgif.com-video-to-gif-converter.gif" width="300" height="600"/>

---

## Architecture
┌─────────────────┐     App Functions Schema      ┌──────────────────┐
│  AI Assistant   │ ◄────────────────────────────► │   Todo App       │
│  (Gemini / AOSP)│   READ_TASKS, CREATE_TASK,    │  • Room DB       │
│                 │   COMPLETE_TASK               │  • ViewModel     │
└─────────────────┘                               │  • Compose UI    │
▲                                         └──────────────────┘
│                                                    │
└──────────────── Structured response ◄──────────────┘

- **AppFunctionManager** registers capabilities at runtime
- **Intent-based contracts** define input/output schemas per action
- **Room DB** persists state; assistant reads/writes through the function layer, never touching UI directly
- **Jetpack Compose** renders local UI independently of assistant traffic

---

## Why this matters

Android 16's App Functions are the OS-level bridge between AI assistants and native apps. Instead of brittle deep-links or screen-scraping, the app **declares what it can do** and the assistant **invokes it securely**.

This pattern scales beyond todos:
- Messaging apps exposing *"send a text to Mom"*
- Health apps exposing *"log my blood pressure"*
- Enterprise apps exposing *"approve the pending expense"*

---

## Tech Stack

- **Kotlin** + **Jetpack Compose**
- **Android 16 App Functions API** (developer preview)
- **Room** for local persistence
- **Hilt** for DI
- **AppFunctionManager** for runtime capability registration

---

## Setup

```bash
git clone https://github.com/gptshubham595/Android-App-Functions-Ai-Task-Controller-Compose.git
```
- Open in Android Studio with Android 16 SDK. Deploy to an Andorid 16 emulator or device running Android 16 Beta.

## Deep Dive
Read the full technical breakdown on Medium:
- [Android 16's App Functions — Game-Changer for AI Assistant Control](https://medium.com/stackademic/android-building-android-16s-app-functions-game-changer-for-ai-assistant-control-09bd194cbf88)



