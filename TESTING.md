# Unit Test and Coverage Report — Team 33

**Project:** HKUST Marketplace (JavaFX + Maven)  
**Report date:** 26 November 2026  
**Build command:** `.\mvnw.cmd clean verify`  
**Environment used for the evidence below:** Windows, Eclipse Temurin JDK 21.0.9,
Apache Maven 3.9.12, JavaFX 21.0.2, JUnit 5.11.4, JaCoCo 0.8.14

## 1. Result summary

| Check | Result |
|---|---|
| Test classes | 20 |
| Tests run | **64** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Pass rate | **100%** |
| Branch coverage | **79.65%** (1,742 / 2,187) — requirement **> 65%** ✅ |
| Line coverage | **90.59%** (4,315 / 4,763) |
| Instruction coverage | **90.25%** (23,782 / 26,352) |
| Method coverage | **84.80%** (859 / 1,013) |

The JaCoCo HTML report is committed at [`docs/coverage/index.html`](docs/coverage/index.html).
The raw Surefire reports are in [`docs/test-reports`](docs/test-reports).

## 2. Coverage by package

| Package | Branch coverage | Branches covered |
|---|---|---|
| `com.hkust.marketplace.controllers.admin` | 85.98% | 730 / 849 |
| `com.hkust.marketplace.controllers` | 69.44% | 100 / 144 |
| `com.hkust.marketplace.controllers.seller` | 56.76% | 84 / 148 |
| `com.hkust.marketplace.controllers.buyer` | 50.56% | 91 / 180 |
| `com.hkust.marketplace.models` | 72.41% | 42 / 58 |
| `com.hkust.marketplace.utils` | 86.66% | 695 / 802 |
| `com.hkust.marketplace` (application entry) | 0.00% | 0 / 6 |
| **Whole project** | **79.65%** | **1,742 / 2,187** |

The whole-project figure is above the 65% requirement even though the two largest
JavaFX dashboards (`BuyerDashboardController` and `SellerDashboardController`) are
mostly presentation code that is exercised through UI integration tests rather than
isolated unit tests.

## 3. Tests by class

| Test class | Tests | What it verifies |
|---|---:|---|
| `MarketplaceNavigationTest` | 5 | End-to-end JavaFX flows across the buyer, seller and admin portals: registration, login, browse/filter, listing detail, favourites/following, chats, offers/counteroffers, deal completion, rating, review reply/flag, admin approval/rejection, take-down, user management, profile/password changes and notifications |
| `MarketplaceActivityTest` | 10 | Request/deal state machine: offers, borrows, claims, counteroffers, withdrawal, availability checks, loan auto-return, chat permissions and rating rules |
| `MarketplaceAccountsTest` | 5 | Registration/login/authentication, role checks, password strength, profile updates and duplicate handling |
| `MarketplaceCatalogTest` | 4 | Browse filtering/sorting, favourites, following, view counts, reports and moderation resolution |
| `SellerMarketplaceTest` | 5 | Seller listing creation/editing/removal, offer responses, statistics and review moderation |
| `AdminStoreTest` | 4 | Admin approval/rejection, mandatory reasons, take-down cancellation of offers, soft deletion, audit log and session invalidation |
| `AdminDashboardTest` | 2 | Admin shell navigation for every screen, notification-target routing, unread badge, logout and role-based access control |
| `AdminLoginTest` | 1 | Admin login success/failure branches (unknown user, wrong password, non-admin, inactive/deleted admin, empty fields, Enter-key login) |
| `AdminDealsPaneTest` | 1 | All Deals Record filters: keyword, type, status, date ranges and invalid date handling |
| `AdminDialogsTest` | 3 | Information/warning/error alerts, confirmations, destructive confirmations and mandatory-reason prompts |
| `AdminListingFormDialogTest` | 4 | Add/edit listing form validation for every field and all listing types (sale, lending, giveaway) |
| `AdminListingsPaneTest` | 2 | Manage All Listings filters, add/edit/take-down flows, photo cells and stale-listing edge cases |
| `AdminNotificationsPaneTest` | 2 | Notification board rendering, read/unread behaviour, related-target availability and navigation |
| `AdminProfilePaneTest` | 1 | Admin profile validation and password-change branches |
| `AdminRatingsPaneTest` | 2 | Seller ratings filters, hide/unhide moderation and public-average recalculation |
| `AdminReviewPaneTest` | 2 | Listing approval/rejection, mandatory rejection reason, inactive-seller approval and stale cards |
| `AdminUiTest` | 2 | Presentation helpers: status/role/category badges, dates, initials and messages |
| `AdminUserFormDialogTest` | 2 | Add/edit user validation: full name, username format/uniqueness, role and password strength |
| `AdminUsersPaneTest` | 3 | User search/filter, edit, deactivate/reactivate, soft delete, add user and audit log |
| `PhotoViewerTest` | 4 | Photo viewer: empty/missing photos, navigation, zoom limits, scroll/keyboard controls and click-target detection |

## 4. Reproducing the results

```powershell
# Windows
.\mvnw.cmd clean verify

# macOS / Linux
./mvnw clean verify

# Headless Linux (JavaFX UI tests need a display)
xvfb-run -a ./mvnw clean verify
```

`verify` runs the tests, writes the Surefire reports to `target/surefire-reports`
and the JaCoCo HTML/CSV/XML reports to `target/site/jacoco`.
Open `target/site/jacoco/index.html` to browse the coverage report.

Re-verification from the cleaned submission folder (26 November 2026, macOS,
Eclipse Temurin JDK 25, Maven Wrapper): `./mvnw test` finished with
**64 tests, 0 failures, 0 errors, 0 skipped** and `BUILD SUCCESS`; the JavaFX UI
tests ran with a display and none were skipped.

The screenshot evidence for the application screens is in
[`docs/screenshots`](docs/screenshots/README.md), and the generated JavaDoc is at
[`docs/javadoc/index.html`](docs/javadoc/index.html).

## 5. Notes and limitations

- The in-memory data store is intentionally session-scoped; tests that touch the
  JavaFX UI use the production singleton and clean up the accounts/listings they
  create. Domain tests use isolated `new DummyDataService(...)` instances.
- JavaFX tests require a graphical session. On Linux without `DISPLAY` they are
  skipped (JUnit aborted) with an instruction to run under `xvfb-run`; the pure-domain
  tests still run in that environment.
- The committed coverage snapshot was produced from a full `clean verify` run on the
  configuration listed above (JDK 21). The source is unchanged in the submitted build;
  re-running with a newer JDK compiler can slightly change the bytecode branch total
  (for example, 2,219 branches at 78.2% on JDK 25) while remaining well above the
  65% requirement.
