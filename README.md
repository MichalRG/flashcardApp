# vocabBoost

A small Android app that helps you learn English words with flashcards, a few minutes a day.

**Version 1.10** spaces opposite test directions and learning retries so the same word cannot give away its next answer. It retains ungraded Context practice, independent translation schedules and recurring long-term maintenance.

Each card shows an English word or its Polish translation; tap it to see the English word, one or two example sentences and the Polish translation, then answer **"Knew it"** or **"Didn't know"**. **Start test** prioritizes due reviews and unfinished initial learning, with up to 8 distinct new words. New words need one successful recall in each direction before that direction enters its repetition schedule. Missed initial-learning cards can return later in the same session, with other words in between.

After initial learning, a direction you remember moves up the ladder of 1, 3, 7, 14 and 30 days, then checks after 60, 90, 180 and 365 days. It then stays in maintenance, with another review every 365 days instead of permanent retirement. A missed scheduled review returns tomorrow. Tests default to Mixed, which includes both directions. After 10 minutes, finish the current card and choose **Stop or Continue**; from minute 5 the screen tells you it's fine to stop.

- **Practice** lets you flip through every word at any time without changing when it comes back. Choose **Flashcards** for EN-first / PL-first cards, or **Context** to complete English sentences. The app remembers both choices.
- **Difficult words** offers extra, ungraded practice for words missed three times in a row in the selected direction.
- Home shows your time in the app today, a daily goal (10 minutes by default; choose 5 for a gentler start) and your streak of days with the goal met
- **Progress** shows initial learning, repetition stages and due counts separately for each direction
- Reminders at about 08:00 and 17:00, only when cards are waiting in the selected test mode and today's goal is not met; no always-running background service
- Dark mode, fully offline: no account, no network, all data stays on the phone and is not copied to a cloud backup or a new phone

## Daily workload and goal

- At most **8 new words per day** by default, shared across all tests and directions.
  Settings allow 0, 3, 5 or 8; an already chosen lower limit is preserved when updating.
  Larger limits saved by older versions are capped at 8. Answering a returning word does
  not use up the new-word allowance. The first graded attempt introduces a word;
  its second direction and any retries never spend another slot.
- In Mixed, 8 new words can mean **16 initial direction cards, plus retries**.
- **Due reviews and unfinished initial learning** have priority over new words.
  New cards can fill spacing gaps when no review can safely be shown next.
  There is no 5-, 10- or 20-card limit on reviews, but directions without enough
  spacing remain due for another session.
- You can end a test at any time. At ten minutes, the app offers Stop or Continue
  after the current answer instead of automatically ending. Continue applies for
  the rest of that session, without repeated time-limit prompts.
- Unanswered scheduled cards remain due. Unfinished initial learning is available
  in another session, even the same day and with the new-word limit set to zero.
- The daily goal is time-based, not a limit on how many words you may review. Home
  confirms when it is met, and further study is optional. No more reminders are sent
  that day, even if some reviews remain. Reminders can resume the next day.
- Updating preserves an already chosen time goal and all learning progress.

## Long-term maintenance

- The successful-review sequence is **1, 3, 7, 14, 30, 60, 90, 180, 365 days,
  then Maintenance**. Completing the 90-day check schedules the 180-day check;
  completing that schedules the 365-day check.
- After the 365-day check, successful maintenance reviews keep scheduling another
  **365 days** indefinitely. Words never disappear permanently from automatic reviews.
- Dates use calendar-day arithmetic from the actual answer day, not from an overdue
  date. A 365-day interval may cross a leap day; it is not a fixed anniversary.
- A missed review at any long-term stage resets only that direction to the 1-day
  stage due tomorrow. It follows the existing mature-review policy, not an
  initial-learning retry.
- Previously retired directions are now maintenance **due in the next test**.
  The old `RET` storage code is retained for compatibility; an old record with no due
  date stays immediately eligible until graded. A successful answer then stores its
  next 365-day date. No bulk rewrite or progress reset is needed.
- EN → PL and PL → EN continue independently. Maintenance reviews do not consume
  the daily allowance of new words or change the other direction's date.
- Practice and Progress display the new stages. Maintenance uses the existing test
  queue and two daily reminders; it adds no background service or extra alarms.
- The 180- and 365-day intervals are practical defaults, not scientifically optimal
  universal intervals.

## Test direction

- Choose **Mixed**, **PL → EN**, or **EN → PL** on Home before starting a test.
  The app remembers your choice; Mixed is the initial default.
- Mixed considers **all eligible directions**, not a weighted sample. A word due in
  both directions has two independent cards; each needs its own successful recall.
- Any two prompts for the same word have **at least 3 other cards between them**,
  preferably 4 where review priority and available material allow it. This applies
  to due reviews, new directions and inserted initial-learning retries. The queue
  is checked again after every answer so a retry cannot put an opposite prompt
  immediately beside it.
- If the remaining material cannot provide that gap, the direction is postponed
  rather than shown with a fresh answer cue. The end screen explains how many
  directions were postponed. They are never automatically graded or rescheduled.
- Home and reminders count all waiting directions, including postponed ones, so
  their counts can exceed the number of cards that safely fit in one test.
- More-frequent words are spread across the queue to avoid consuming all their
  spacing material too early. Within those constraints, original review order is
  retained; first-attempt new directions are lower priority.
- Fixed modes offer only the selected direction. Starting a new word also saves
  its other direction as unfinished, so it remains available when switching modes.
- Each word has **two independent repetition schedules**. Answering PL → EN never
  advances, resets or reschedules EN → PL, and vice versa. Switching modes can therefore
  offer the same word in its other direction, without another new-word slot.
- The direction stays fixed while flipping a card or rotating the phone.
- Tests are self-graded: for a Polish-first card, recall the English word before
  revealing it; for an English-first card, recall its Polish meaning.

## Initial learning and retries

- A new direction remains **Learning** until its first **"Knew it"** answer.
  Seeing the answer or choosing **"Didn't know"** does not complete initial learning.
- A failed initial-learning card aims to return after **4 intervening cards**,
  with a minimum of 3. The global spacing check may move or postpone its opposite
  direction as well, so neither prompt gives away the other's answer.
- If no gap of at least 3 other cards is available, the app does not force an
  immediate retry. Learning stays unfinished for a later session.
- The first successful recall schedules that direction's **1-day** review for
  tomorrow, even after several misses or days of unfinished learning. It does
  not jump straight to the 3-day stage.
- Initial-learning state is saved with each graded answer. Ending a test or closing
  the app does not silently mark unfinished directions as learned.
- Retries currently apply to **initial learning only**. A failed mature review
  resets that direction to the 1-day stage due tomorrow, as before.
- The 3–4-card retry gap is a practical heuristic, not a claim of a scientifically
  optimal spacing interval.
- Spacing is measured within a test. Starting another session resets its card-gap
  history; the app does not enforce a cross-session timer.

For example, if you recall a new word from Polish but miss its English-first card,
PL → EN is due tomorrow while EN → PL remains unfinished. A later successful
English-first recall starts its own 1-day interval, without changing PL → EN.

## Practice direction

- The **Flashcards / Context** selector switches exercise type without moving to a
  different word. Flashcards is the default for both new and existing installations.
- **EN first** shows the English word; **PL first** shows its Polish translation.
- Tap the card to reveal the English word, example and Polish translation.
- Changing direction turns the current card back to its front without changing your
  position. Your choice survives navigation, app restarts and updates.
- Practice and Difficult words share this preference. Tests have their own remembered
  direction choice on Home, independent of this toggle.
- The displayed repetition stage, including unfinished Learning, belongs to the
  selected direction.
- Switching languages never changes learning progress or the repetition schedule.

## Context practice

- Open **Practice**, then choose **Context**. All 144 bundled words have an exercise
  in the same order as ordinary flashcards.
- Complete the sentence mentally or aloud, then tap to reveal the missing expression,
  completed sentence and Polish meaning. No typing or graded answer buttons are required.
- Answers use the form the sentence actually needs: for example, **overtaking**
  rather than **overtake**, or **gave me a lift** rather than **give someone a lift**.
  The base vocabulary expression is also shown when its form differs.
- Exercises use existing examples or curated context sentences, with explicit grammatical
  forms. If an example repeats the answer, a separate sentence prevents it leaking into the prompt.
  Each exercise has one gap. Other grammatically and contextually correct expressions
  may also fit; these are ungraded exercises, not exact-match tests.
- The language toggle and repetition stage are hidden in Context because it is
  English sentence practice, not a third scheduled direction. Returning to Flashcards
  restores the saved language choice and turns the card back to its front.
- The chosen exercise type survives navigation and app restarts. Context is available
  for all words, not only mature ones; it does not introduce words or consume the
  daily new-word allowance.
- Context changes no schedules, initial-learning status, difficulty streaks or test
  prompts. Difficult words remains ordinary flip-only revision.
- Time on the screen still counts toward the daily time goal. No background work,
  network access, third-party generation or extra notifications are added.

## Difficult words

- Open **Difficult words** from Home for a separate, smaller revision deck.
- A word enters after **3 consecutive "Didn't know" answers** in tests in the
  same direction. Initial attempts and initial-learning retries count; answering
  other words or the opposite direction does not interrupt its streak.
- A correct answer before the third miss resets that direction's miss streak.
- Once listed, the word stays until **2 consecutive "Knew it" answers** in regular
  tests in that direction. A miss between them resets the recovery streak but keeps the word listed.
  After leaving, the word needs 3 new consecutive misses to enter again.
- Revision here is **flip-only**, with the same EN-first / PL-first preference as
  Practice. It changes neither repetition dates nor difficulty streaks and does not
  consume the new-word allowance. Its on-screen time counts toward the daily goal.
- All words remain available in ordinary Practice. Difficult words also remain in
  their normal scheduled tests; this deck is optional extra practice.
- The list is direction-specific. The language toggle stays available even when
  the selected direction has no difficult words.
- Streaks survive app restarts and vocabulary corrections because they use stable
  word IDs. Removed vocabulary is hidden; re-adding the same ID restores its state.
- Tracking starts with version 1.5. Earlier versions did not record consecutive
  misses, so old miss history cannot be reconstructed. Updating keeps existing
  learning progress, due dates, statistics and settings; no reset is needed.
- The new tracking adds no background work or notifications.

## Updating from the shared schedule

- Version 1.7 copies each existing schedule, due date, retirement status and difficulty
  streak into **both directions**, then lets them evolve independently.
- Existing records are treated as already introduced; updating does not restart
  initial learning for previously studied words.
- Settings, daily study time, streaks and stable vocabulary IDs are preserved.
- Migration is read-only until the next graded answer, which atomically saves the
  directional schedule and difficulty state in version-two preferences. The original
  version-one keys remain unchanged; they are not the active progress after migration.
- Corrupt version-two data is reported as an error rather than silently replaced
  using an older shared record.
- No extra background work, alarms or network access are added.

## Adding words

The bundled deck contains 144 cards from the supplied vocabulary lists, with corrected spelling,
Polish translations and short English examples. Version 1.6 appends 54 cards, mainly about
driving, to the original 90 without changing their IDs or order. The original demonstration
cards are no longer in the deck; any saved progress for their IDs is retained.

Some notes were split into separate cards to keep their meanings clear:

- `smother a fire` means to put it out by cutting off its air; `stoke a fire` means to feed it.
- `rash` is a skin eruption; `road rash` means abrasions from a fall onto a road.
- `let go of` means to release something; `be let go` means to lose a job.
- `would`, `might` and `could` do not represent fixed percentages of certainty.
- `dose up` means to take medicine, not simply increase a dose.
- The confirmed ambiguous entries are `phone battery` and `wear and tear`.
- The second list's `puncture` and `wear and tear` reuse the existing cards, without duplicates.
- `clutch (gaming)` and `clutch (car)` have separate cards; the gaming card keeps its original ID.
- The confirmed phrase is `affect the plan`, not `effect the plan`. `drop`, `change` and
  `scrap the plan` are separate cards with their own meanings.
- `get a lift` means to receive a ride; `give someone a lift` means to offer one.
- `stinger` (żądło) and `stinging remark` (kąśliwa uwaga) have separate cards.
- Corrected phrases include `built-up area`, `the lanes are merging`, `lower the car`
  and `truck-mounted crane`. British `tyre` and American `tire` are shown together.
- `divert traffic` means to send it along another route, not reverse its direction.
- `hit-and-run` means leaving the scene of a collision; it need not involve a pedestrian.
- `parking ticket` can mean a parking fine or a ticket proving payment, depending on context.
- Informal, British and vulgar expressions are labelled in their Polish definitions.

All added words are immediately available in Practice. Normal tests still introduce new
words in deck order, within the existing daily allowance. Adding vocabulary changes neither
saved schedules nor difficulty streaks, and does not reset statistics or settings.

Words live in JSON files in [`app/src/main/assets/words/`](app/src/main/assets/words), not in code.
The app reads every file in that folder in file-name order (`01-general.json`, then
`02-driving.json`) and joins them into one deck. To add words, append them to the last
file or add a new file such as `03-kitchen.json`; no Kotlin change is needed.
[`prompt_creating_words.md`](prompt_creating_words.md) is a ready-made prompt that turns
notes into such a file with an AI assistant.

Each file is an object with one `words` array:

```json
{
  "words": [
    {
      "id": "give-someone-a-lift",
      "english": "give someone a lift",
      "example": "He gave me a lift to the station after work.",
      "polish": "podwieźć kogoś (brytyjskie)",
      "contextAnswer": "gave me a lift"
    }
  ]
}
```

- `id`, `english`, `example` (one or two sentences) and `polish` are required, non-blank strings.
- Context exercises use the optional `contextAnswer` (the exact grammatical form in the sentence;
  defaults to `english`) and `contextSentence` (defaults to `example`). Set them when the expression
  is inflected, has alternative display forms or appears twice. Tests require every bundled exercise
  to contain its answer exactly once as a whole expression, with no pre-existing gap.
- File names are lower-case slugs ending in `.json`. The two-digit prefix keeps the deck order obvious.
- Unknown or misspelled fields are rejected rather than ignored, and so is an `id` used twice
  anywhere in the folder. The error names the file, the word number and the field.
- The `id` is the word's permanent key: a lower-case slug such as `take-for-granted`, unique across all files. Never change it once the word is installed, or its progress is lost.
- The English text, example and translation can be corrected at any time; progress stays.
- New words are introduced in deck order (files by name, then entries within a file), so add new words at the end of the last file or in a new file with a higher number. Reordering or moving words between files never touches progress.
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
