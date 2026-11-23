# HKUST Marketplace — JavaFX + Maven

A **JavaFX + Maven** campus marketplace application for the COMP3111 group project.

The project uses an in-memory data store (`DummyDataService`) so it runs without a
database or network connection.  The Buyer and Seller portals remain demo-level, while
**all Task 3 (Admin Portal) functions are implemented end-to-end** — see
[`ADMIN_TASK3.md`](ADMIN_TASK3.md) for the feature-by-feature mapping and demo script.

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
(`...\HKUST-Marketplace\HKUST-Marketplace`):

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

### Buyer Portal
- Registration, Login, Browse Marketplace, Make Offer / Borrow / Claim
- My Deals, Deal History, Favourites (placeholder), Notifications, Profile

### Seller Portal
- Registration, Login, Create Listing, My Listings
- Seller Stats (charts), Reviews & Feedback, Notifications, Profile

### Admin Portal (Task 3 — complete)
- **Login** with the seeded admin credential; buyer/seller credentials are rejected
- **Listing Review & Approval** — approve (with confirmation) or reject with a mandatory,
  stored reason; seller is notified
- **Manage All Users** — search/filter, add user (validation), edit full name,
  deactivate/reactivate, soft delete with confirmation and audit log
- **Manage All Listings** — view every status, edit any listing, add on behalf of an
  active seller, take down a live listing with a mandatory reason (notifies the seller
  and all buyers with active offers, cancels those offers)
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
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/hkust/marketplace/
    │   ├── MarketplaceApp.java
    │   ├── controllers/
    │   ├── models/
    │   └── utils/
    └── resources/
        ├── fxml/
        ├── css/styles.css
        └── images/hkust_logo.png
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

## For Students — Next Steps (Backend)

1. Replace `DummyDataService` with real repositories / REST clients
2. Keep the password-hashing approach (or upgrade PBKDF2 to BCrypt/Argon2) — never store plaintext
3. Add JWT / session auth and enforce RBAC on the API (the UI already guards the admin screens)
4. Persist data in a database, including the audit log
5. Photo upload, chat, auto-return for borrowed items, etc.

## Admin implementation notes

See [`ADMIN_TASK3.md`](ADMIN_TASK3.md) for:

- a Task 3.1–3.8 → source-file map,
- the exact edge cases implemented (empty states, concurrent status changes, mandatory
  reasons, invalid date ranges, deleted related items, password failures),
- a step-by-step demo script for the admin portal.

## Submission ZIP

Include `src/`, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `run.sh`, `README.md` and
`ADMIN_TASK3.md`.  Exclude generated/restricted-environment caches: `target/`,
`.mvn-home/`, `.m2-repository/`, `.DS_Store` and IDE folders.

## Licence

Educational use — HKUST COMP project skeleton.
