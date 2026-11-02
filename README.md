# vocabBoost

A small Android app that helps you learn English words with flashcards, a few minutes a day.

Each card shows an English word or phrase; tap it to see one or two example sentences and the Polish translation, then answer **"Knew it"** or **"Didn't know"**. **Start test** brings the words that are due today, most overdue first, followed by up to 5 new words. A word you know moves up the ladder of 1, 3, 7, 14 and 30 days, then checks after 60 and 90 days, after which it is retired. A word you didn't know starts again tomorrow. From the 7-day step on, a card may show the Polish side first. A test stops after 10 minutes, and from minute 5 the screen tells you it's fine to stop.

- **Practice** lets you flip through every word at any time without changing when it comes back. Choose **EN first** or **PL first** above the card; the app remembers your choice.
- Home shows your time in the app today, a daily goal (10 minutes by default; choose 5 for a gentler start) and your streak of days with the goal met
- **Progress** shows how many words are at each step and how many are due today and tomorrow
- Reminders at about 08:00 and 17:00, only when words are waiting and today's goal is not met; nothing runs in the background
- Dark mode, fully offline: no account, no network, all data stays on the phone and is not copied to a cloud backup or a new phone

## Daily workload and goal

- At most **5 new words per day**, shared across all tests. Settings allow 0, 3 or 5;
  larger limits saved by older versions are capped at 5. Answering a returning word does
  not use up the new-word allowance.
- **All due reviews** are offered, most overdue first, before new words. There is no
  5-, 10- or 20-word limit on reviews. You can stop early or start another test if the
  ten-minute session limit is reached; unanswered words remain due.
- Remembered words move to longer intervals; missed words return tomorrow.
- The daily goal is time-based, not a limit on how many words you may review. Home
  confirms when it is met, and further study is optional. No more reminders are sent
  that day, even if some reviews remain. Reminders can resume the next day.
- Updating preserves an already chosen time goal and all learning progress.

## Practice direction

- **EN first** shows the English word; **PL first** shows its Polish translation.
- Tap the card to reveal the English word, example and Polish translation.
- Changing direction turns the current card back to its front without changing your
  position. Your choice survives navigation, app restarts and updates.
- The toggle affects Practice only. Tests keep their automatic direction: English
  first for new words and early reviews, with mixed directions from the 7-day step.
- Switching languages never changes learning progress or the repetition schedule.

## Adding words

The bundled deck contains 90 cards from the supplied vocabulary list, with corrected spelling,
Polish translations and short English examples. The original demonstration cards are no longer
in the deck; any saved progress for their IDs is retained.

Some notes were split into separate cards to keep their meanings clear:

- `smother a fire` means to put it out by cutting off its air; `stoke a fire` means to feed it.
- `rash` is a skin eruption; `road rash` means abrasions from a fall onto a road.
- `let go of` means to release something; `be let go` means to lose a job.
- `would`, `might` and `could` do not represent fixed percentages of certainty.
- `dose up` means to take medicine, not simply increase a dose.
- The confirmed ambiguous entries are `phone battery` and `wear and tear`.
- Informal, British and vulgar expressions are labelled in their Polish definitions.

All words live in one list, `app/src/main/java/com/krzyzowski/vocabboost/words/VocabWords.kt`. Each entry has an `id`, the English word or phrase, the example sentence(s) and the Polish translation.

- The `id` is the word's permanent key: a lower-case slug such as `take-for-granted`, unique in the list. Never change it once the word is installed, or its progress is lost.
- The English text, example and translation can be corrected at any time; progress stays.
- New words are introduced in list order, so append new words at the end. Reordering never touches progress.
- Removing an entry hides the word; its progress comes back if the same `id` is added again.
- After editing, run `./gradlew --offline :app:testDebugUnitTest`, bump `versionCode` in `app/build.gradle.kts`, then build and install.

## Reminders

Reminders are inexact and may arrive a few minutes late. On an idle phone Android may hold them until the screen turns on. If they do not arrive at all, set the app's battery use to **Unrestricted** in the system settings.

## Future ideas

- Offline pronunciation with the phone's text-to-speech
- Undo of the last answer
- In-app vocabulary import from a file
- Feeding focusGuard's "word of unlock" from the words being learned

Built with Kotlin and Jetpack Compose, Android 8.0+.
