# Synapse: Academic Revision & Spaced Repetition Study Engine

A focused, offline-first academic management and exam revision application designed for students. Synapse integrates the SuperMemo SM-2 spaced repetition algorithm, AES-256 local encrypted storage, interactive retention analytics, an AI academic tutor powered by Gemini, and seamless PDF/Markdown export into a high-performance AMOLED dark interface.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during Phase 1 clarification:
> - **Revision Workflows**: All three modalities supported—Flashcards with SM-2 active recall scheduling, Topic revision checklists with mastery tracking, and Exam countdowns with timed mock quizzes.
> - **Persistence & Security**: Hybrid offline-first encrypted architecture using Room + AES-256-GCM encryption with local backup export and cloud sync readiness.
> - **Visual Identity**: Deep AMOLED dark aesthetic (`#0A0E17` / `#05070B`) accented with electric cyan (`#00E5FF`) and vibrant violet (`#7C4DFF`).

---

### 1. Overview & Core Concept

Synapse transforms exam preparation from passive reading into an active, data-driven revision habit:
- **Spaced Repetition Engine (SM-2)**: Implements algorithmic review scheduling calculating Ease Factor ($EF$), interval $I(n)$, and repetition count based on active recall performance ratings (Again, Hard, Good, Easy).
- **Exam Countdown & Topic Checklists**: Track subjects, syllabi, mastery percentages, and remaining days until target exams.
- **Encrypted Academic Vault**: Stores sensitive notes, study material, and flashcards with local AES-256-GCM cipher encryption to guarantee privacy.
- **Multimodal AI Academic Tutor**: Leverages Gemini 3.8 Flash to summarize study notes, generate flashcard decks from text or textbook photos, explain challenging concepts, and provide daily challenge questions.
- **Interactive Retention Visualizations**: Canvas-based memory retention curves, 7-day study consistency bars, and topic mastery radar indicators.
- **Export & Backup Tools**: Generates formatted PDF study guides directly on-device using Android's native `PdfDocument` engine, alongside Markdown export and JSON backup archives.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Minimalist AMOLED Dashboard**:
   - Top status bar with academic countdowns, daily streak, and cards due today.
   - "Question of the Day" interactive challenge card with instant answer feedback.
   - Fast action bar: "Quick Review", "New Flashcard", "AI Tutor", "Scan Note".
2. **Spaced Repetition Review Flow**:
   - Card displays prompt/question; user taps or swipes to reveal answer.
   - Rating buttons: **Again** (<1m), **Hard** (1.2x), **Good** (Normal), **Easy** (Bonus).
   - Audio-haptic feedback, immediate interval recalculation, and session summary.
3. **Topic Mastery & Syllabus Checklist**:
   - Nested folder and tag hierarchy (e.g., *Physics > Electromagnetism*).
   - Topic mastery scores with scheduled re-test triggers.
4. **AI Academic Tutor & Photo Scanner**:
   - Chat interface for concept explanations and exam advice.
   - Text/Photo note input: extracts core concepts and one-tap converts them into SM-2 flashcard decks.
5. **Study Analytics & Progress**:
   - Retention curve vs. forgetting curve visualization.
   - Study time tracker, card maturity distribution (New, Learning, Mature), and review forecasts.
6. **Encrypted Vault & PDF Exporter**:
   - Note editor with folder tagging, encryption toggle, and one-touch "Export to PDF" / "Export Markdown".

#### Visual Identity & Theme
- **Theme Direction**: Ultra-clean AMOLED Dark with neon cyber-academic accents.
- **Color Palette**:
  - `Background`: `#05080E` (Deepest Black)
  - `Surface`: `#0D131F` (Dark Indigo Slate)
  - `Surface Variant`: `#162032` (Elevated Panel)
  - `Primary Accent`: `#00E5FF` (Electric Cyan)
  - `Secondary Accent`: `#7C4DFF` (Deep Violet)
  - `Success / Easy`: `#00E676` (Mint Green)
  - `Warning / Hard`: `#FFAB00` (Amber)
  - `Danger / Again`: `#FF5252` (Coral Red)
  - `Text Primary`: `#F0F4FC`
  - `Text Muted`: `#8A9BB5`
- **Typography & Hierarchy**:
  - Display: Clean bold monospace/geometric styling for stats and countdowns.
  - Body: High-legibility sans-serif with ample line height for long study notes.
- **Interactive Feedback**:
  - Spring-animated card flips, smooth progress fill bars, and tactile haptic clicks on review ratings.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real SM-2 Algorithmic Scheduling vs. Fixed Intervals**
  - *Chosen Approach*: Implement the full SuperMemo SM-2 algorithm ($EF' = EF + (0.1 - (5-q) \times (0.08 + (5-q) \times 0.02))$, minimum $EF = 1.3$).
  - *Why*: Provides genuine cognitive science-backed memory retention adapted to user difficulty ratings.
- **Decision 2: Hybrid Offline-First Encrypted Architecture**
  - *Chosen Approach*: Room local database coupled with Android `javax.crypto` AES-GCM encryption for notes and cards, with local backup export and sync readiness.
  - *Why*: Allows zero-latency, full offline access in flight/library mode without network dependence, while protecting student data.
- **Decision 3: Native Android `PdfDocument` Generation**
  - *Chosen Approach*: Direct on-device vector and typography rendering to PDF via Android's graphics canvas.
  - *Why*: Eliminates heavy external PDF libraries, ensures zero network transmission of private study materials, and works 100% offline.
- **Decision 4: Multimodal AI Processing via Gemini 3.8 Flash**
  - *Chosen Approach*: Use Gemini 3.8 Flash for both fast text chat, note summarization, and direct photo notes processing (extracting diagrams and generating flashcards).
  - *Why*: Lowest latency, superior reasoning for academic subjects, and native image understanding.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│ ┌─────────────────┬─────────────────┬────────────────┬───────────────┐ │
│ │  DashboardView  │ FlashcardReview │ TopicChecklist │  AiTutorChat  │ │
│ ├─────────────────┼─────────────────┼────────────────┼───────────────┤ │
│ │ ProgressCharts  │   NotesVault    │ DailyChallenge │ ExportManager │ │
│ └─────────────────┴─────────────────┴────────────────┴───────────────┘ │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │ StateFlow / Events
┌───────────────────────────────────┴────────────────────────────────────┐
│                    MVVM ViewModels & Business Logic                    │
│ ┌───────────────────────────┐ ┌──────────────────────────────────────┐ │
│ │  FlashcardReviewViewModel │ │  AcademicAnalyticsViewModel          │ │
│ ├───────────────────────────┤ ├──────────────────────────────────────┤ │
│ │  AiTutorViewModel         │ │  NotesAndTasksViewModel              │ │
│ └───────────────────────────┘ └──────────────────────────────────────┘ │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                       Domain & Service Engines                         │
│ ┌──────────────────────┐ ┌────────────────────┐ ┌────────────────────┐ │
│ │ SpacedRepetitionSM2  │ │ CryptoSecurityVault│ │ PdfExportEngine    │ │
│ └──────────────────────┘ └────────────────────┘ └────────────────────┘ │
│ ┌──────────────────────┐ ┌────────────────────┐ ┌────────────────────┐ │
│ │ FirebaseAiService    │ │ StudyNotification  │ │ CloudBackupEngine  │ │
│ └──────────────────────┘ └────────────────────┘ └────────────────────┘ │
└───────────────────────────────────▲────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                     Data Layer (Offline-First)                         │
│ ┌────────────────────────┐ ┌─────────────────┐ ┌─────────────────────┐ │
│ │ Room Database (Local)  │ │ SharedPreferences│ │ Android File System │ │
│ │ • Decks & Flashcards   │ │ • Encrypted Keys│ │ • PDF & MD Exports  │ │
│ │ • Topics & Checklists  │ │ • Study Streaks │ │ • Photo Cache       │ │
│ │ • Tasks & Exams        │ │ • Settings      │ │                     │ │
│ └────────────────────────┘ └─────────────────┘ └─────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data Entities
1. **Flashcard**: `id`, `deckId`, `question`, `answer`, `repetitionCount`, `intervalDays`, `easeFactor`, `nextReviewDate`, `isEncrypted`, `tags`.
2. **Deck & Subject**: `id`, `title`, `subject`, `colorHex`, `examDate`.
3. **AcademicTask**: `id`, `title`, `description`, `subject`, `priority`, `dueDate`, `isCompleted`.
4. **TopicChecklist**: `id`, `subject`, `topicName`, `masteryScore` (0-100), `lastRevisedDate`, `nextTestDate`.
5. **StudySessionLog**: `id`, `timestamp`, `cardsReviewed`, `correctCount`, `durationMinutes`, `retentionRate`.

#### Key Execution Sequences
- **SM-2 Execution**: On button tap (Again=1, Hard=2, Good=3, Easy=4) $\rightarrow$ Calculate new $EF$, interval in days, and set `nextReviewDate` $\rightarrow$ Persist update to Room $\rightarrow$ Recalculate session retention rate and chart telemetry.
- **AI Flashcard Auto-Gen**: Input lecture note or textbook photo $\rightarrow$ Query Gemini 3.8 Flash with structured JSON schema $\rightarrow$ Stream flashcards directly into the user's selected deck.
- **PDF Export**: Gather notes and revision summaries for selected topic $\rightarrow$ Paint formatted pages via `PdfDocument` $\rightarrow$ Save to app documents and trigger Android Sharesheet.
