# Deferred / Not Current — Adding Cards to Reviewed Decks

This file preserves earlier interview decisions for possible future design work.
It is **not part of the current MVP requirements** and must not be implemented
or used to override `server-database-requirements.md` unless the user explicitly
reopens this feature. The current rule permits new cards only before the first
deck review.

## Earlier ideas, now superseded

- An initial simpler proposal made a previously reviewed deck due immediately
  when a card was added. This was replaced because it retained the deck's old,
  potentially long interval after the near-term review.
- The later proposal asked, on adding a new card or moving one into an already
  reviewed deck, for both the next review time and the interval to use after a
  successful review. Both choices affected the whole deck, not only the new
  card. Prior completed-review summaries and history were preserved.
- If another device had not changed the deck or card, manual synchronization
  would apply the new card and its chosen deck schedule without a second prompt.
- If two devices independently added different cards and chose different deck
  schedules, both cards would survive; the conflict screen would offer the
  Android schedule, Desktop schedule, or custom next time and interval.
- If a new card was added independently of a completed deck review, the
  conflict screen would explain that the card was not part of that review and
  offer reviewing the whole deck now (with a subsequent interval choice) or
  keeping the completed review schedule. The prior review record stayed.
- A moved-in card would have used the same destination-deck scheduling choice.

## Unresolved even in the deferred design

- Whether the interval chosen "after a successful review" would be the exact
  first delay or an input to the existing automatic interval calculation.

These notes are historical context only; they are not an implementation plan.
