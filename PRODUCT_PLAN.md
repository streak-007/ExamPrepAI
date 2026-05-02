# ExamPrepAI Product Plan

## Product Vision

ExamPrepAI is a quiz-based competitive exam preparation app designed to help students:

- practice questions consistently
- learn through high-quality explanations
- track progress over time
- build long-term study habits

The long-term goal is to grow it into a full AI-powered learning ecosystem for competitive exam aspirants.

## Core Product Promise

The app should answer a simple question well:

Can a student open the app, solve questions, understand mistakes, improve weak areas, and come back tomorrow?

## Target Users

Students preparing for competitive exams such as:

- UPSC
- SSC
- Banking
- GATE
- other exam-specific categories added over time

## Product Principles

- Fast, clean, mobile-first experience
- Thumb-friendly UI with minimal friction
- Learning-focused feedback, not just answer checking
- Modular architecture that can scale into AI, planning, and community features
- Clear free-to-premium upgrade path

## Technical Direction

Current implementation direction for the Android app:

- Platform: Android
- UI: Jetpack Compose
- Architecture: MVVM
- Data flow: View -> ViewModel -> Repository
- Local persistence for MVP 1: preferred
- Backend later: Firebase / Firestore
- Local caching roadmap: Room

## Delivery Strategy

We will start by building MVP 1 end-to-end with local/sample data first, while keeping the architecture ready for Firebase, Room, payments, AI, and admin tooling later.

Reason:

- reduces setup risk
- speeds up visible progress
- validates the core learning loop first
- keeps future integrations easier because the app structure is planned upfront

## MVP 1 - Core Quiz and Learning

### Goal

Prove the app's core value:

Can a student come in, solve questions, learn from explanations, track progress, and want to return daily?

### Features

#### 1. Onboarding

Collects:

- exam type
- selected subjects

Behavior:

- stores selections locally
- can later sync to Firebase optionally
- personalizes the starting experience and dashboard

#### 2. Quiz Engine

Core interaction:

- one MCQ at a time
- four options
- user selects and submits an answer

Must support:

- skip question
- revisit skipped questions
- mark question for review
- optional timer

#### 3. Explanation Layer

This is the key differentiator of the app.

After submission, every question should show:

- correct answer highlighted
- concept explanation in simple language
- reference source such as a book, paper, or exam source
- common mistakes students make on that question

#### 4. Progress Tracking

Simple metrics for MVP 1:

- total attempted questions
- correct count
- wrong count
- accuracy percentage

Display:

- shown on a simple dashboard

#### 5. Free vs Premium UI

For MVP 1, monetization is visual only.

Behavior:

- some question sets remain free
- some sets show a premium lock badge
- no payment flow yet

Purpose:

- prepares the product for later monetization

#### 6. Clean UI / UX

The experience should feel:

- minimal
- fast
- dark-mode friendly
- smooth in transitions
- accessible
- comfortable for one-handed use

#### 7. Firebase Readiness

Not required for the first implementation pass, but the structure should be ready for:

- Firestore for questions
- Firestore for attempts
- optional authentication
- efficient query/index planning

#### 8. Room Readiness

Architecture should stay compatible with:

- offline caching
- local question storage
- sync when backend is added later

## MVP 2 - Monetization, Mock Tests, and Analytics

### Goal

Validate retention and willingness to pay.

### Features

#### 1. Payments

- Google Play Billing
- subscription or one-time unlock
- unlock premium question bank, mock tests, and advanced analytics

#### 2. Mock Tests

- full-length timed exam simulation
- real exam pattern alignment
- detailed post-test analysis
- weak area breakdown
- rank estimation

#### 3. Advanced Analytics Dashboard

- topic-wise performance graphs
- strength vs weakness view
- time spent per subject
- accuracy trends
- exam readiness score

#### 4. Adaptive Learning

- difficulty adjusts based on performance
- easier questions if struggling
- harder questions if performing well
- weak topics tagged automatically

#### 5. Gamification

- daily streaks
- XP points
- levels
- daily challenges
- reminders and push notifications

#### 6. Leaderboards

- rank by accuracy, XP, or solved count
- exam-type filtering
- weekly and all-time competition

#### 7. Offline Mode

- download question sets
- solve without internet
- sync attempts later

#### 8. Admin Content Panel

Web dashboard for the content team:

- upload questions via CSV or JSON
- add explanations
- tag topic and difficulty
- push new papers regularly

## MVP 3 - Study Planner

### Goal

Personalize preparation around the student's exam date.

### Features

- exam-date based planning
- daily study targets
- auto-adjustment for missed days
- weak-topic prioritization
- readiness countdown on home screen

## MVP 4 - AI Doubt Solver

### Goal

Turn the app into an interactive tutor, not just a quiz bank.

### Features

- question-level doubt chat
- contextual AI explanations based on the exact attempted question
- awareness of student's answer and weak topics
- voice-based doubt input
- deeper concept explanations and follow-ups

## MVP 5 - Community and PDF Converter

### Goal

Create network effects and scale content generation.

### Features

#### 1. PDF to Quiz

- upload previous year paper PDFs
- parse into interactive quizzes
- generate explanations with AI

#### 2. Peer Discussion Threads

- each question gets a discussion section
- students debate answers
- share mnemonics and alternate explanations

#### 3. Community Q&A

- students post public doubts
- other students or moderators answer
- verified answers become reusable knowledge

#### 4. Content Contribution

- advanced users submit questions
- admin review workflow
- approved submissions earn XP

## Suggested Build Order

### Phase 1

Build a strong local-first MVP 1 foundation:

- onboarding
- sample exam and subject selection
- quiz flow
- explanations
- progress dashboard
- premium-locked sets UI
- reusable design system basics
- MVVM structure

### Phase 2

Strengthen the data layer:

- repositories and models prepared for backend swap
- Room integration
- seed/sample content handling
- analytics-ready attempt tracking

### Phase 3

Add business and retention systems:

- billing
- streaks
- notifications
- mock tests
- advanced analytics

### Phase 4

Add intelligence layers:

- planner
- adaptive learning
- AI doubt solver

### Phase 5

Expand ecosystem:

- admin panel
- community
- PDF conversion

## MVP 1 Implementation Notes

Recommended scope for the first build:

- use local mock data only
- no Firebase integration yet
- no payment integration yet
- no authentication yet
- architecture should remain ready for those additions

Recommended first screens:

- onboarding
- home/dashboard
- quiz player
- answer review/explanation state
- premium content preview/locked cards

Recommended first data entities:

- Exam
- Subject
- QuestionSet
- Question
- Option
- UserPreferences
- QuizAttempt
- ProgressStats

## Success Criteria for MVP 1

MVP 1 is successful if a student can:

- choose their exam and subjects
- start a quiz quickly
- answer and submit questions
- understand why an answer is right or wrong
- see progress clearly
- notice premium content without broken flows

## Current Project Status

Current repository status at the start of planning:

- Android app scaffold exists
- Jetpack Compose starter template is present
- no real product features implemented yet
