# My Safe Community App — Database Schema (v2)

Source: `Note/yay.pdf` (original outline) + Community Feed addon module (this update).

## Existing Tables (from yay.pdf)

| Table | Key Fields | Purpose |
|---|---|---|
| Users | Id, Password, Name, Role, Contact, AvatarUri | Resident, Admin, Responder accounts; Contact doubles as the profile's Address field, AvatarUri is a picked photo's content Uri |
| Reports | ReportID, UserID, Title, Location, Description, Status, Photo | Hazard submissions and tracking |
| Alerts | AlertID, Title, Body, Priority, Timestamp | Community notices and warnings |
| AlertAcknowledgements | AlertID, UserID, Timestamp | Per-user "Confirm Acknowledgment" on urgent alerts (composite PK, mirrors Likes) |
| EmergencyContacts | ServiceID, Name, PhoneNo, CategoryEmergency, IsSpecialized | Single-tap directory for Emergency Hub (IsSpecialized splits the primary grid from the "Specialized Services" list) |
| SafetyGuides | GuideID, CategorySafety, Steps | Procedural content for Safety Guide library (Steps: one step per line, each formatted `Title||Description`) |

## New Tables — Community Feed Addon Module

Reddit/Facebook-style feed: users upload posts, comment, and like; admin can edit posts.

| Table | Key Fields | Purpose |
|---|---|---|
| Posts | PostID (PK), UserID (FK → Users.Id), Content, ImageURL, Timestamp, IsEdited, EditedByAdminID (FK → Users.Id, nullable) | User-uploaded posts; tracks if/who (admin) last edited a post |
| Comments | CommentID (PK), PostID (FK → Posts.PostID), UserID (FK → Users.Id), Content, Timestamp | Comments on a post |
| Likes | LikeID (PK), PostID (FK → Posts.PostID), UserID (FK → Users.Id), Timestamp | Like records; unique constraint on (PostID, UserID) to prevent duplicate likes and allow like-count queries |

### Relationships
- Users 1—N Posts (author)
- Posts 1—N Comments
- Posts 1—N Likes (unique per user per post)
- Users 1—N Comments (author)
- Users 1—N Likes
- Users (Role = Admin) 0—N Posts edited (via EditedByAdminID)

## Navigation Update

Community Feed becomes its own top-level entry in the **Main Hub**, at the same level as Report, Alerts, Emergency Hub (SOS), and Safety Guide — not merged into any existing page.

```
Main Hub
├── Report
├── Alert
├── SOS (Emergency Hub)
├── Guide
└── Community Feed   ← NEW
      ├── Post List (view/upload posts)
      ├── Post Detail (comments, like button)
      └── Admin: Edit Post
```

Admin flow gains: **Manage Posts** (edit post content), alongside existing Manage Reports / Manage Alerts.

## Login

`LoginScreen` is now the app's actual start destination, gating everything else; `UserSession`
(in-memory, cleared on process death) tracks who is signed in, replacing the earlier hardcoded
`DemoSession` placeholder. Two seeded test accounts (idempotent `INSERT OR IGNORE`, so they
appear on every install without needing a fresh database):

| ID | Password | Role |
|---|---|---|
| resident1 | demo1234 | RESIDENT |
| admin1 | admin1234 | ADMIN |

"Sign Up" (also reachable via "Request access") lets anyone self-register a RESIDENT account
with just an ID + password (no name/contact collected, no way to self-register as Admin).
"Forgot?" still just shows a snackbar — no reset flow has been designed yet.

## Admin

Logging in with an ADMIN-role account routes to `AdminHubScreen` instead of the resident
`MainHubScreen` — a separate flow, matching the Login nav split in `yay.pdf`, without the
resident bottom nav bar. Three screens, all built on existing DAO methods (no new tables):

| Screen | Route | Backs onto |
|---|---|---|
| Manage Reports | `admin_reports` | `ReportDao.getAll()` / `.updateStatus()` — status chips (Pending/In Progress/Solved) per report |
| Manage Alerts | `admin_alerts` + `admin_alert_form?alertId={id}` | `AlertDao` insert/update/delete; one form screen handles both Add (`alertId=-1`) and Edit |
| Manage Posts | `admin_posts` | `PostDao.editByAdmin()` — edit any resident's post content |

## Profile

`ProfileScreen` (route `profile/{userId}`) is one screen for two cases: your own profile
(editable: name, avatar via a photo picker, address) when `userId` matches the signed-in
user, or a read-only view of someone else's profile otherwise. Reached from the Main Hub's
account icon (own profile) and from tapping any post/comment author's name (their profile).
Friends/connections between residents were considered and intentionally skipped — this is
just profile editing plus "who is this person," not a social graph.

Manage Contacts (EmergencyContacts) and Manage Guides (SafetyGuides) don't have Admin screens
yet — they weren't in the original `yay.pdf` wireframes and are still just the seeded data.
