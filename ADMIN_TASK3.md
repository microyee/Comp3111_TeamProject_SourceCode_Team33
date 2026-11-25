# Task 3 — Admin Portal: implementation notes

This document maps every Task 3 requirement to the source code, records the edge cases
that were implemented, and gives a short demo script for the TA.

## 1. Source map

| Requirement | UI / controller | Supporting logic |
|---|---|---|
| **3.1 Admin login** | `controllers/admin/AdminLoginController.java`, `fxml/admin/AdminLoginView.fxml` | `utils/PasswordUtils.java`, `utils/SessionManager.java` |
| **3.2 Listing review & approval** | `controllers/admin/AdminReviewPane.java` | `DummyDataService.approveListing`, `rejectListing` |
| **3.3 Manage all users** | `controllers/admin/AdminUsersPane.java`, `AdminUserFormDialog.java` | `DummyDataService.deactivateUser`, `reactivateUser`, `deleteUser`, `getActiveSellers` |
| **3.4 Manage own profile** | `controllers/admin/AdminProfilePane.java` | `PasswordUtils`, `DummyDataService.checkPasswordStrength` |
| **3.5 All deals record** | `controllers/admin/AdminDealsPane.java` | `DummyDataService.getAllDeals`, `Deal` |
| **3.6 Notification board** | `controllers/admin/AdminNotificationsPane.java` | `Notification`, `DummyDataService.getUnreadCount`, `markNotificationRead`, `markAllNotificationsRead` |
| **3.7 Manage all listings** | `controllers/admin/AdminListingsPane.java`, `AdminListingFormDialog.java` | `DummyDataService.takeDownListing`, `getLiveListings`, `getDealsForListing` |
| **3.8 Seller ratings overview** | `controllers/admin/AdminRatingsPane.java` | `DummyDataService.getPublicAverageRating`, `recalculateSellerRating` |
| Shell / navigation / unread badge | `AdminDashboardController.java`, `fxml/admin/AdminDashboardView.fxml` | `SessionManager` |
| Shared presentation helpers | `AdminUi.java`, `AdminDialogs.java` | — |
| Audit log | `AdminUsersPane` → **Audit Log** button | `AuditLogEntry`, `DummyDataService.logAudit` |
| Data model additions | Abstract `User` + `Buyer`/`Seller`/`Admin`; `Request` with `Deal.requestId`; `Rating` with `dealId`; `Chat`/`Message`; `Listing` removal reason; `Notification` read state + related screen | — |
| UI motion, photos & click actions | `controllers/UiAnimations.java` (page fade/slide, spinner, hover lift, badge pulse, card stagger), `controllers/PhotoViewer.java` (zoom/prev/next viewer) | `utils/ImageSupport.java`, real photos in `resources/images/listings/*.png` (credits in `CREDITS.md`); seller/admin listing clicks open the edit form |

The admin packages are deliberately split into one pane per subtask.  The dashboard only
handles navigation and the unread badge; business rules live in `DummyDataService` so the
same rule is used by the UI and could be reused by a future API layer.

## 2. Shared rules

### Password strength (also documented in `README.md`)
- At least **8 characters**
- At least one **uppercase** letter
- At least one **lowercase** letter
- At least one **digit**

Passwords are stored as salted **PBKDF2-HMAC-SHA256** hashes
(`pbkdf2$iterations$salt$hash`).  The seeded `admin/admin123` credential is
pre-provisioned for the demo; the strength rule applies to passwords set or changed via
the UI.

### Username uniqueness
Checked case-insensitively across buyers, sellers and admins, including soft-deleted
accounts (the username is not recycled).

### Full name
Must not be empty or blank.

### Deletion policy (Task 3.3)
**Soft delete**, with an audit-log entry:
- account flagged `deleted`, inactive, hidden from all user lists, cannot log in;
- the user's listings become `REMOVED` and leave the marketplace;
- active deals are cancelled and counterparties are notified;
- deals/chats/ratings are retained for history and audit;
- the audit log records actor, action, target and timestamp.

### Deactivation / reactivation
Deactivating a user prevents login and hides that seller's `LIVE` listings from
`getLiveListings()` without deleting them.  Reactivation restores login and visibility.

## 3. Edge cases implemented

| Area | Edge case | Behaviour |
|---|---|---|
| Login | empty username/password | submit button disabled; handler also shows "both required" |
| Login | buyer/seller credential | "not an Administrator account" message |
| Login | wrong password | "Incorrect password" |
| Login | deleted/inactive account | rejected |
| Direct navigation | no admin session | dashboard redirects to admin login |
| Review | no pending listings | empty-state card |
| Review | approve/reject twice or concurrent status change | re-reads status; warns and refreshes |
| Review | blank rejection reason | submit disabled; service also rejects blank |
| Review | persistence/save failure | error dialog; listing remains pending |
| Users | no users / no filter matches | empty-state card |
| Users | duplicate username | inline error in Add User dialog |
| Users | blank full name | inline error |
| Users | weak password | lists every failed rule |
| Users | admin account | excluded from list; cannot be deleted |
| Users | deactivate/reactivate/delete | confirmation dialog, including cancel path |
| Users | delete | warning shows linked listings/deals/ratings; soft delete + audit |
| Users | reactivate | live listings visible again |
| Listings | no listings / no filter matches | empty-state placeholder |
| Listings | add with no active seller | warning and abort |
| Listings | seller deactivated between opening and saving | validation rejects |
| Listings | missing price (Sell) / duration (Lend) | field-level error |
| Listings | invalid price/duration | field-level error |
| Listings | edit an already-removed listing | warning and refresh |
| Listings | take down non-live listing | action disabled; service re-checks |
| Listings | blank removal reason | submit disabled; service also rejects |
| Listings | take down with active offers | offers cancelled; seller + each buyer notified |
| Listings | photo limit | max 6; extra selections skipped with a warning |
| Deals | no deals / no matches | empty state |
| Deals | start date after end date | inline error, previous result kept |
| Ratings | no ratings / no matches | empty state |
| Ratings | hide/unhide | confirmation; public average recomputed; audit entry |
| Ratings | date range invalid | inline error, previous result kept |
| Notifications | no notifications | empty state |
| Notifications | open unread item | marks read and updates badge |
| Notifications | mark all read | all marked + badge updated |
| Notifications | related listing/account deleted | stored details shown; navigation not offered |
| Profile | blank full name | inline error |
| Profile | wrong current password | inline error |
| Profile | weak new password | lists failed rules |
| Profile | confirmation mismatch | inline error |
| Profile | password unchanged | password fields may stay blank |

## 4. Demo script (Admin portal)

1. Start the app and choose **Admin**.
2. Try `alice` / `password1` → wrong-account-type message.  Then `admin` / `admin123`.
3. **Listing Review**:
   - Approve **Mini Fridge** (confirm) → it becomes Live.
   - Reject any new submission with a blank reason → submit is disabled; enter a reason
     → seller receives a notification containing the reason.
4. **Manage Users**:
   - Search `diana`, deactivate → her live listings disappear from the buyer marketplace;
     reactivate → they return.
   - **+ Add User** with a duplicate username, blank name or weak password → inline errors;
     correct input creates the account.
   - Delete a user → read the warning, confirm; then open **Audit Log** to see the entry.
5. **All Listings**:
   - **+ Add Listing** on behalf of a seller → starts as Pending Review.
   - **Edit** a listing and change the price/photo set.
   - **Take Down** a live listing (e.g. the MacBook charger) with a reason → seller and
     the buyer with an active offer are notified; the offer is cancelled.
6. **All Deals**: filter by status/type and a date range; enter an invalid range to see
   the inline error.
7. **Seller Ratings**: filter by seller/score/date; hide a rating → the public average
   changes; unhide → it is restored.
8. **Notifications**: open a Submission/Report notification, mark it read, use
   **Open related screen**, and **Mark all as read**.
9. **My Profile**: change the display name; change the password (test wrong current
   password, weak new password and mismatched confirmation first).

## 5. Known limitations / next steps

- Data is in-memory; restarting the app resets it.
- `Chat` and `Message` domain classes and one seeded thread exist, but the Buyer/Seller
  chat screen/use case is outside the Admin task and remains unimplemented.
- Buyer-side **Report Listing** is a Task 1 item; the admin board is seeded with one
  report notification so moderation navigation can be demonstrated.
- For production, move the rules from `DummyDataService` to a service/API layer, persist
  the audit log, and upgrade PBKDF2 to BCrypt/Argon2 as required.

## 6. Alignment with the Activity 1 proposal

The final Activity 1 PDF (`Comp3111_TeamProject_Activity_1_Final.pdf`) defines eight
Task 3 use cases.  All eight are implemented:

| Activity 1 use case | Implementation |
|---|---|
| UC-AD-1 Login | `AdminLoginController` |
| UC-AD-2 Review Listings | `AdminReviewPane` |
| UC-AD-3 Manage Users | `AdminUsersPane`, `AdminUserFormDialog` |
| UC-AD-4 View All Deals | `AdminDealsPane` |
| UC-AD-5 View Notifications | `AdminNotificationsPane` |
| UC-AD-6 Manage All Listings | `AdminListingsPane`, `AdminListingFormDialog` |
| UC-AD-7 Moderate Ratings | `AdminRatingsPane` |
| UC-AD-8 Manage Profile | `AdminProfilePane` |

The shared validation rules (password strength, case-insensitive unique username,
non-blank full name) and the Task 3.1–3.8 → UC-AD mapping in the PDF are followed.

### Known deviations from the proposal wording

- The rating entity is now `Rating` as in the proposal; it retains the extra seller
  reply used by the seller feedback view.
- The proposal filters ratings by separate seller and buyer fields; the code uses one
  search box that matches either seller or buyer (and also listing/comment).
- The proposal describes persistence/read-state save-failure alternative flows.  The
  demo is in-memory, so these failures cannot occur and are not simulated.
- Admin validation reports one inline error at a time rather than marking individual
  fields red.
- The profile screen has no dedicated Cancel button; navigating to another admin screen
  discards unsaved edits.
- Sidebar/page labels use the project-task wording (e.g. "All Deals", "Seller Ratings"),
  while the use-case behaviour matches the proposal's "View All Deals", "Moderate
  Ratings".

### Domain-diagram mapping (whole system)

The Activity 1 domain diagram includes `Buyer`, `Seller` and `Admin` as `User`
subclasses, plus `Request`, `Deal`, `Chat`, `Message`, `Rating` and `Notification`.
The class structure now mirrors those entities:

| Proposal domain concept | Current code |
|---|---|
| `User` / `Buyer` / `Seller` / `Admin` | Abstract `User` + `Buyer`, `Seller`, `Admin` subclasses |
| `Listing` | `Listing` |
| `Notification` | `Notification` |
| `Rating` | `Rating` (linked to `Deal` via `dealId`) |
| `Request` + `Deal` | `Request`, with `Deal.requestId`; one Request creates at most one Deal |
| `Chat`, `Message` | `Chat` and composed `Message` list; one seeded thread |

The Buyer and Seller specifications in the same PDF (`UC-BU-1..9`, `UC-SE-1..9`) are
not fully implemented by this repository; the Buyer/Seller screens are still the
supplied skeleton.  Request, Rating and Chat data are seeded/modelled, but the
Buyer/Seller workflows that create them are outside the Admin task.  Therefore the
Admin Task 3 design is fulfilled and the domain UML is represented, while the whole
three-role application is not yet a complete implementation of every Activity 1 use
case.
