# HKUST Marketplace — JavaFX + Maven

**Team 33** · COMP 3111 F2026 · Activity 2 submission  
**GitHub repository:** <https://github.com/microyee/Comp3111_TeamProject_SourceCode_Team33>

A **JavaFX + Maven** campus marketplace application with three role-based portals:
Buyer, Seller and Admin. The portals share an in-memory data store
(`DummyDataService`), so the application runs without a database or network connection.
See [`ADMIN_TASK3.md`](ADMIN_TASK3.md) for the Task 3 feature map and demo script.

## Project documentation

- [Unit test and coverage report](TESTING.md) — 64 tests, 100% pass, **79.65% branch coverage**
- [JaCoCo coverage report (HTML)](docs/coverage/index.html)
- [JavaDoc (HTML)](docs/javadoc/index.html)
- [Application screenshots with sample inputs/outputs](docs/screenshots/README.md)
- [Supplementary notes](docs/SUPPLEMENTARY_NOTES.md) — architecture, WoW factors, algorithms, assumptions and limitations
- [Activity 1 — revised UML and use-case specification](docs/Comp3111_TeamProject_Activity_1_Final_Revised.pdf)
- [Activity 2 — documentation and source code](docs/COMP3111_Team33_Activity_2_Documentation_and_Source_Code.pdf)
- [Final submission evidence and Sprint 2 Project Management Pack — 18-page PDF](docs/COMP3111_Team33_Final_Submission_Evidence.pdf)
- [Sprint 2 Project Management Pack — editable Word version](COMP3111_Team33_Sprint2_Project_Management_Updated.docx)

### Activity 2 requirement checklist

| Requirement | Evidence in this repository |
|---|---|
| Project ZIP | Package the repository root as described in [Submission ZIP](#submission-zip); upload the ZIP to Canvas |
| GitHub repository link | <https://github.com/microyee/Comp3111_TeamProject_SourceCode_Team33> |
| Screenshots of sample inputs/outputs | [`docs/screenshots/README.md`](docs/screenshots/README.md) — 12 application screens with the input/action that produced each output |
| Unit test report, 100% pass | [`TESTING.md`](TESTING.md) and [`docs/test-reports/`](docs/test-reports) — 64 tests, 0 failures, 0 errors, 0 skipped |
| Coverage report, > 65% branch | [`docs/coverage/index.html`](docs/coverage/index.html) — 79.65% branch, 90.59% line |
| JavaDoc | [`docs/javadoc/index.html`](docs/javadoc/index.html) — all 51 source files across 7 packages |
| Supplementary notes | [`docs/SUPPLEMENTARY_NOTES.md`](docs/SUPPLEMENTARY_NOTES.md) and page 17 of the final evidence PDF |

The Activity 1 revision replaces the two diagrams with implementation-aligned versions
and adds UC-BU-10 (Report Listing), UC-BU-11 (Manage Favourites & Following) and
UC-AD-9 (Manage Reports). Activity 2 documents the architecture, security rules,
feature coverage, test evidence, source excerpts and a complete 51-file source index.

The final 18-page evidence PDF combines four Scrum meeting minutes, the Gantt chart,
the Sprint 2 backlog, the burndown chart, a representative Git commit log,
outcome/verification evidence, two UI-evidence pages, unit-test and coverage evidence,
JavaDoc evidence, sample inputs/outputs, supplementary notes and a repository
checklist. The editable Word version contains the same 18 pages with native, editable
tables.

Verification snapshot: **64 automated tests, 0 failures, 0 errors** (`./mvnw clean verify`),
with **79.65% branch coverage** (1,742 / 2,187 branches) and **90.59% line coverage**.
See [`TESTING.md`](TESTING.md) and the committed
[JaCoCo report](docs/coverage/index.html).

## Prerequisites

1. **JDK 17 or newer** (full JDK, not only JRE)
   - Download: https://adoptium.net/ (Temurin 17 or 21 recommended)
   - After install, open a **new** PowerShell and check:
     ```powershell
     java -version
     javac -version
     ```
   - Both commands must work. If `javac` is missing, you only have a JRE — install the JDK.

2. **Maven is optional** — this project includes the **Maven Wrapper** (`mvnw.cmd` on Windows).

## How to Run (Windows)

Open **PowerShell** or **Command Prompt** in the project folder  
(`...\HKUST-Marketplace\HKUST-Marketplace`), then either double-click `run.bat` or run:

```bat
.\run.bat
```

`run.bat` checks for a full JDK, keeps Maven's wrapper and local repository inside the
project (`.mvn-home\` and `.m2-repository\`), starts the application, and pauses on
failure so the error stays visible.  You can also call the wrapper directly:

```powershell
.\mvnw.cmd clean compile javafx:run
```

First run downloads Maven automatically (needs internet). Then the app window should open.

### How to Run (macOS / Linux)

```bash
chmod +x mvnw run.sh    # once
./run.sh
```

`run.sh` keeps Maven's wrapper and local repository inside the project
(`.mvn-home/` and `.m2-repository/`).  This is required in restricted environments
that block writes to `~/.m2`; those cache directories are listed in `.gitignore` and
should be excluded from the submission ZIP.

### Alternative: if you already installed Maven and added it to PATH

```powershell
mvn clean compile javafx:run
```

### Run from IntelliJ IDEA

1. Open the project folder as a Maven project
2. Wait for dependencies to download
3. Open `MarketplaceApp.java` → right-click → **Run**

### Run from VS Code

1. Install **Extension Pack for Java**
2. Open the folder
3. Run `MarketplaceApp.java`

## Demo Credentials

| Role   | Username  | Password   |
|--------|-----------|------------|
| Admin  | `admin`   | `admin123` |
| Buyer  | `alice`   | `password1`|
| Buyer  | `bob`     | `password1`|
| Seller | `charlie` | `password1`|
| Seller | `diana`   | `password1`|

## Features Implemented

### Shared
- Welcome screen with role selection (Buyer / Seller / Admin)
- HKUST branding (logo + colour palette: deep blue `#003974` + gold `#A68137`)
- Modern card-based UI with consistent sidebars and top bars
- In-memory dummy data (no database / no network)
- Passwords stored as salted **PBKDF2-HMAC-SHA256** hashes (never plaintext)
- Domain model follows the Activity 1 UML: abstract `User` with `Buyer`, `Seller` and
  `Admin` subclasses; `Request` linked to `Deal`; `Rating`; `Chat` composed of
  `Message`; `Listing`; `Notification`
- Real listing photographs (Flickr/Openverse), photo-card marketplace grid, animated
  page transitions, hover-lift cards, loading indicators and pulsing unread badges
- Clickable photo thumbnails open a zoomable viewer (wheel/buttons, previous/next);
  seller and admin listing rows/cards open the edit form on click
- All login screens support keyboard login: Enter in the username field moves to the
  password field, and Enter in the password field submits the form

### Buyer Portal
- Register, log in, manage profile and change password
- Browse and filter live listings, view photos and seller profiles, save favourites and follow sellers
- Chat, submit sale offers and loan requests, claim giveaways and respond to counteroffers
- View and complete deals, return borrowed items, search completed history and rate sellers
- Receive borrowing reminders and activity notifications, and report listings to an admin

### Seller Portal
- Register, log in, manage profile and change password
- Submit sale, lending and giveaway listings with up to six photos
- Edit and resubmit eligible listings, remove listings, and mark sales as sold
- Review, accept, decline and counter buyer requests, and communicate in private chats
- View real listing and deal statistics, reply to reviews and flag feedback for moderation
- Receive notifications when listings are reviewed, including any rejection reason

### Admin Portal (Task 3 — complete)
- **Login** with the seeded admin credential; buyer/seller credentials are rejected
- **Listing Review & Approval** — approve (with confirmation) or reject with a mandatory,
  stored reason; seller is notified
- **Manage All Users** — search/filter, add user (validation), edit full name,
  deactivate/reactivate, soft delete with confirmation and audit log
- **Manage All Listings** — view every status, edit any listing, add on behalf of an
  active seller, take down a live listing with a mandatory reason (notifies the seller
  and all buyers with active offers, cancels those offers)
- **Reports** — review listing and rating reports, open the reported item, and record a resolution
- **All Deals Record** — read-only oversight with keyword/type/status/date filters
- **Seller Ratings Overview** — filter by user/score/date; hide/unhide ratings (hidden
  ratings are excluded from the public seller average)
- **Notification Board** — categorised, timestamped, unread highlighting, mark read /
  mark all read, navigation to the related screen
- **Manage Own Profile** — edit full name and change password with current-password,
  strength and confirmation checks

## Password strength rules

A password set through registration, admin **Add User**, or the admin **Profile** screen
must contain:

- at least **8 characters**,
- at least one **uppercase** letter,
- at least one **lowercase** letter, and
- at least one **digit**.

The seeded accounts (`admin/admin123`, `alice/password1`, …) are pre-provisioned for the
demo; the strength rule applies to passwords set or changed through the UI.

## Deletion policy (Task 3.3)

Permanent user deletion is a **soft delete**, not a hard delete:

1. The account is marked deleted and inactive, hidden from all user lists, and can no
   longer log in.
2. The user's listings are set to **Removed** and disappear from the marketplace.
3. Active deals involving the account are cancelled and the counterparties are notified.
4. Deals, chats and ratings are **retained** so history and audit trails stay intact.
5. An **audit-log entry** records the admin, action, target and time.  The audit log is
   available from **Manage Users → Audit Log**.

Deactivation is reversible: a deactivated seller's live listings are hidden from the
marketplace but reappear when the account is reactivated.


## Project Structure

```
HKUST-Marketplace/
├── mvnw / mvnw.cmd          ← Maven Wrapper (use these!)
├── run.sh / run.bat         ← one-click launchers (macOS/Linux, Windows)
├── pom.xml
├── README.md
├── TESTING.md              ← unit-test and coverage summary
├── ADMIN_TASK3.md          ← Task 3 source map, edge cases and demo script
├── CREDITS.md              ← real photo sources and licences
├── COMP3111_Team33_Sprint2_Project_Management_Updated.docx
├── docs/                   ← evidence, PDFs, screenshots, JavaDoc, coverage
│   ├── COMP3111_Team33_Final_Submission_Evidence.pdf
│   ├── COMP3111_Team33_Activity_2_Documentation_and_Source_Code.pdf
│   ├── Comp3111_TeamProject_Activity_1_Final_Revised.pdf
│   ├── SUPPLEMENTARY_NOTES.md
│   ├── screenshots/
│   ├── test-reports/
│   ├── coverage/
│   └── javadoc/
└── src/
    ├── main/
    │   ├── java/com/hkust/marketplace/
    │   │   ├── MarketplaceApp.java
    │   │   ├── controllers/
    │   │   ├── models/
    │   │   └── utils/
    │   └── resources/
    │       ├── fxml/
    │       ├── css/styles.css
    │       └── images/
    │           ├── hkust_logo.png
    │           └── listings/        ← real listing photographs (see CREDITS.md)
    └── test/java/com/hkust/marketplace/   ← 20 test classes + FxTest support
```

## Troubleshooting

### `mvn` is not recognized
Use the wrapper instead:
```powershell
.\mvnw.cmd clean compile javafx:run
```

### `java` / `javac` not recognized
Install JDK 17+ from https://adoptium.net/ and **restart the terminal**.  
Make sure the installer option “Add to PATH” was checked.

### Build fails with JavaFX / DISPLAY errors
You need a normal desktop session (not a headless server). On Windows this is usually fine.

### `mkdir: /Users/.../.m2/wrapper: Operation not permitted`

Your terminal is not allowed to write outside the project directory, so Maven cannot
create `~/.m2/wrapper`. Use the project launcher instead:

```bash
./run.sh
```

Or set the locations explicitly:

```bash
MAVEN_USER_HOME="$PWD/.mvn-home" \
  ./mvnw -Dmaven.repo.local="$PWD/.m2-repository" clean compile javafx:run
```

On Windows PowerShell:

```powershell
$env:MAVEN_USER_HOME="$PWD\.mvn-home"
.\mvnw.cmd -Dmaven.repo.local="$PWD\.m2-repository" clean compile javafx:run
```

### To see the real error
```powershell
.\mvnw.cmd clean compile javafx:run -e
```
Copy the lines that start with `Exception in thread` or `Caused by`.

## Future work (production hardening)

1. Replace `DummyDataService` with real repositories / REST clients
2. Keep the password-hashing approach (or upgrade PBKDF2 to BCrypt/Argon2) — never store plaintext
3. Add JWT / session auth and enforce RBAC on the API (the UI already guards the admin screens)
4. Persist data in a database, including the audit log
5. Add a photo-upload service, real-time chat and a background scheduler for loan reminders

## Admin implementation notes

See [`ADMIN_TASK3.md`](ADMIN_TASK3.md) for:

- a Task 3.1–3.8 → source-file map,
- the exact edge cases implemented (empty states, concurrent status changes, mandatory
  reasons, invalid date ranges, deleted related items, password failures),
- a step-by-step demo script for the admin portal.

## Photo credits

The listing photographs are real photos sourced from Flickr via Openverse.  See
[`CREDITS.md`](CREDITS.md) for each photo's title, creator, licence and source URL.

## Submission ZIP

Create the ZIP from the repository root and include `src/`, `docs/`, `pom.xml`, `mvnw`,
`mvnw.cmd`, `.mvn/`, `run.sh`, `run.bat`, `README.md`, `TESTING.md`, `ADMIN_TASK3.md`,
`CREDITS.md` and `COMP3111_Team33_Sprint2_Project_Management_Updated.docx`.  Exclude
generated/restricted-environment caches: `target/`, `.mvn-home/`, `.m2-repository/`,
`.DS_Store`, `*.zip` and IDE folders.  Verify the extracted ZIP with
`./mvnw clean verify` before uploading it to Canvas.

## Licence

Educational use — HKUST COMP3111 Team 33 project.

## Tests

Run `mvnw.cmd clean verify` on Windows or `./mvnw clean verify` on macOS and Linux.
JaCoCo writes the full branch-coverage report to `target/site/jacoco/index.html`;
the committed snapshot is [`docs/coverage/index.html`](docs/coverage/index.html).
The Surefire reports and the test-by-test summary are in
[`TESTING.md`](TESTING.md). JavaFX UI tests require a display. On a headless Linux
machine, use `xvfb-run -a ./mvnw clean verify` to include them; without a display
they are skipped while the domain tests still run.

The generated JavaDoc is committed at [`docs/javadoc/index.html`](docs/javadoc/index.html).
Regenerate it with:

```powershell
.\mvnw.cmd -DskipTests javadoc:javadoc
```

## Try an end-to-end transaction

1. Log in as a seller, submit a listing, then log out.
2. Log in as the admin, approve the listing in **Listing Review**, then log out.
3. Log in as a buyer, open the listing, send a request and log out.
4. Log in as the seller, open **Incoming Requests**, accept or counter it, then log out.
5. Return to the buyer portal. Accept a counteroffer if applicable, then complete the deal or return the loan from **My Deals**. Review the seller from **Deal History**.

Leave the application open when switching roles. Marketplace data is held in memory
and resets on restart. Listing photos are read from their selected local files.
