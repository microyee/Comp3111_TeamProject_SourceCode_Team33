# Supplementary Notes — Team 33, HKUST Marketplace

These notes support the final Sprint 2 submission. They summarise the system
characteristics, the WoW factors, the algorithms behind the main workflows, the
justification for the recommendation/moderation results, and the assumptions and
limitations of the implementation.

## 1. What the system is

HKUST Marketplace is a JavaFX desktop application with three role-specific portals
inside one process:

- **Buyer portal** — registration/login, browse and filter live listings, listing
  detail with photo viewer, favourites, follow sellers, chat, sale offers, borrow
  requests, giveaway claims, counteroffer handling, deal completion/returns, history,
  seller ratings, notifications and listing reports.
- **Seller portal** — registration/login, create/edit/remove listings with up to six
  photos, incoming request handling (accept/decline/counter), mark-sold, private
  chats, seller statistics, review replies/flags and notifications.
- **Admin portal** — seeded admin login, listing moderation (approve/reject), all
  listings (edit, add on behalf of a seller, take down with a mandatory reason), user
  management (add/edit, deactivate/reactivate, soft delete, audit log), all deals,
  ratings moderation, reports, notification board and own profile.

The application uses a shared in-memory `DummyDataService` singleton, so all three
portals observe the same live data during a session. Passwords are stored as salted
PBKDF2-HMAC-SHA256 hashes; no plaintext password is persisted.

## 2. Architecture and design characteristics

| Layer | Responsibility |
|---|---|
| `models` | UML-aligned domain entities: `User` with `Buyer`/`Seller`/`Admin` subclasses, `Listing`, `Request`, `Deal`, `Rating`, `Chat`, `Message`, `Notification`, `Report`, `AuditLogEntry` |
| `utils` | `DummyDataService` (in-memory store and seed data), `MarketplaceAccounts` (registration/authentication), `MarketplaceCatalog` (browse/favourites/follows/reports), `MarketplaceActivity` (request/deal state machine), `SellerMarketplace` (seller workflows), `PasswordUtils`, `SessionManager`, `ImageSupport`, `SceneManager` |
| `controllers` | JavaFX controllers and reusable presentation helpers (`PortalUi`, `PhotoViewer`, `UiAnimations`) |
| `controllers.admin` | Task 3 panes/dialogs kept small and single-purpose (review, listings, users, deals, ratings, reports, notifications, profile) |
| `resources/fxml` + `css` | View definitions and the shared HKUST visual theme |

Key design choices:

- **Business rules live outside the controllers.** `MarketplaceActivity`,
  `MarketplaceCatalog`, `MarketplaceAccounts` and `SellerMarketplace` are plain Java,
  which makes them unit-testable without JavaFX. Controllers only bind UI events to
  those services.
- **Role-based access at the screen boundary.** Every dashboard re-checks
  `SessionManager.isSessionValid()` and the expected role; deactivating/deleting an
  account also invalidates any live session via `SessionManager.logoutIf`.
- **Immutable audit trail.** All administrative changes call `data.logAudit(...)`;
  the admin can inspect the log from **Manage Users → Audit Log**.
- **Soft delete, not destructive delete.** Deleting a user marks the account deleted,
  removes their listings from the marketplace, cancels active deals and notifies
  counterparties, while keeping deals, chats and ratings for history/audit.

## 3. WoW factors

1. **Three portals, one live session.** A TA can keep the application open, switch
   roles and watch a listing move from pending → live → reserved → sold, with the
   notification badge updating in each portal.
2. **Rich JavaFX presentation.** Animated page transitions, hover-lift cards,
   loading overlays, pulsing unread badges, photo-card grids, and a zoomable photo
   viewer with wheel/keyboard controls.
3. **Real moderation workflow.** Admin rejection and take-down require a written
   reason, notify the seller and (for take-down) every buyer with an active offer,
   and cancel those offers. Ratings can be hidden/unhidden with the public average
   recalculated immediately.
4. **End-to-end request lifecycle.** Offers, borrows and giveaway claims all share a
   single request/deal model, with counteroffers, chat threads per listing/buyer/
   seller pair, loan start/return and automatic return handling.
5. **Defensive UI.** Keyboard login (Enter moves from username to password and
   submits), inline validation messages, disabled submit buttons for mandatory
   reasons, stale-data guards for dialogs, and click-target detection so clicking a
   button inside a clickable table row does not open the row editor.
6. **Testability.** Business rules are separated from views, and the project ships
   64 automated tests with **79.67% branch coverage** (see
   [`TESTING.md`](../TESTING.md)).

## 4. Algorithms and rules

### 4.1 Browse, filter and sort (`MarketplaceCatalog.browse`)

- Combines keyword (title, description, seller, category, condition, location),
  type, category, condition and price-range filters.
- Whitespace is normalised and matching is case-insensitive.
- Price ranges are validated with `validateRange` (finite, non-negative, min ≤ max).
- Results are sorted by newest, price ascending/descending, or rating; unpriced
  listings are pushed to the end for price sorts.
- Only `LIVE` listings whose seller is still active are visible; a seller
  deactivation hides listings without deleting them.

### 4.2 Request/deal state machine (`MarketplaceActivity`)

- `submit` validates the listing is public, that the buyer has no other active
  request for it, and that the terms match the listing type (positive 2-dp price for
  sale; a future start/end within the lending duration for a loan; no terms for a
  giveaway).
- A seller can `respond` with ACCEPT, DECLINE or COUNTER (counter only for
  sale/lending). Acceptance creates the deal, reserves the listing and notifies both
  parties.
- `withdraw` cancels an active buyer request and releases a giveaway claim.
- `finish` completes a sale or returns a loan, marks the listing sold/available and
  records completion time; `advanceLoans` auto-returns overdue loans.
- Chat is restricted to the buyer/seller pair associated with the listing or deal.
- `rate` is allowed once per finished deal; the seller's public average excludes
  hidden ratings.

### 4.3 Admin recommendation/moderation results

- **Approval:** re-reads the listing before deciding; only a `PENDING_REVIEW`
  listing can be approved. If the seller is inactive, the listing becomes `LIVE`
  but stays hidden until reactivation (the seller notification explains this).
- **Rejection:** requires a non-blank reason, stores the trimmed reason, notifies the
  seller and exposes it on the seller's listing so it can be corrected and
  resubmitted.
- **Take-down:** requires a reason, only works on `LIVE` listings, removes the
  listing, notifies the seller and all buyers with active offers, cancels those
  offers and returns the cancelled count for the confirmation message.
- **Ratings:** hiding a rating removes it from the public average/count but keeps it
  for audit; unhiding restores it. The public average is recomputed on every change.
- **Reports:** a report is filed with a mandatory reason and a snapshot of the
  target; the admin can resolve it with a mandatory resolution note, which notifies
  the reporter.

## 5. Assumptions

- The application is a single-user desktop demo. `SessionManager` holds one current
  user; switching roles means logging out and logging in again (the in-memory data
  survives while the application stays open).
- "Persistence" is session-scoped by design; restarting the application restores the
  seeded demo data.
- Listing photo paths may point to classpath images (seeded demo photos) or local
  files chosen by the user; if a file disappears the UI shows a neutral placeholder.
- Seeded accounts (`admin/admin123`, `alice/password1`, `bob/password1`,
  `charlie/password1`, `diana/password1`) are pre-provisioned for the demo; the
  password-strength rule applies to passwords set through the UI.
- The admin portal is not publicly registerable; only the seeded admin account can
  sign in there.

## 6. Known limitations

- Data is in memory only (no database/network); this is intentional for the course
  demo and keeps the application fully runnable offline.
- Password hashing uses PBKDF2 with a per-user random salt; production systems should
  use a memory-hard algorithm (Argon2/bcrypt) and rate limiting.
- Photo metadata/EXIF is not stripped, and photo storage is a local path rather than
  an upload service.
- Chat is a retained demo thread with no real-time transport; messages appear when a
  screen is refreshed/reopened.
- The JavaFX UI tests require a display. On headless Linux they are skipped; use
  `xvfb-run -a ./mvnw clean verify` for the full suite.

## 7. Demo access and walkthrough

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Buyer | `alice` | `password1` |
| Buyer | `bob` | `password1` |
| Seller | `charlie` | `password1` |
| Seller | `diana` | `password1` |

Suggested end-to-end demo:

1. Log in as a seller, create a listing (sale, lending or giveaway) and submit it.
2. Log in as the admin, approve the listing in **Listing Review**.
3. Log in as a buyer, open the listing, save it, chat with the seller and submit a
   request/counteroffer.
4. Log in as the seller, accept/counter the request from **Incoming Requests**.
5. Return to the buyer and complete the deal or return the loan; rate the seller from
   **Deal History**.
6. As the admin, inspect **All Deals**, **Seller Ratings**, **Moderation Reports**,
   the notification board and the audit log.

## 8. Verification evidence

- Unit test and coverage report: [`TESTING.md`](../TESTING.md)
- JaCoCo HTML report: [`docs/coverage/index.html`](coverage/index.html)
- JavaDoc: [`docs/javadoc/index.html`](javadoc/index.html)
- Screenshots: [`docs/screenshots/README.md`](screenshots/README.md)
- Sprint 2 management pack: [`COMP3111_Team33_Sprint2_Project_Management_Updated.docx`](../COMP3111_Team33_Sprint2_Project_Management_Updated.docx)
