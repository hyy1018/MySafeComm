# My Safe Community App — Database Schema (v2)

Source: `Note/yay.pdf` (original outline) + Community Feed addon module (this update).

## Existing Tables (from yay.pdf)

| Table | Key Fields | Purpose |
|---|---|---|
| Users | Id, Password, Name, Role, Phone, Address, Email, AvatarUri | Resident, Admin, Responder accounts (Phone/Address/Email split into their own columns; were briefly one shared "Contact" field) |
| Reports | ReportID, UserID, Title, Location, Description, Status, Photo | Hazard submissions and tracking |
| Alerts | AlertID, Title, Body, Priority, Location, IssuedBy, Timestamp | Community notices, formatted as formal official notices (Location + IssuedBy + Timestamp-as-date) |
| AlertAcknowledgements | AlertID, UserID, Timestamp | Per-user "Confirm Acknowledgment" on urgent alerts (composite PK, mirrors Likes); also backs a red badge on the bottom nav's Alert tab (`AlertDao.getUnacknowledgedUrgentCount`) when one exists |
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
| test1 | abc123456 | RESIDENT |
| admin1 | abc123456 | ADMIN |

"Sign Up" lets anyone self-register a RESIDENT account with just an ID + password (no way to
self-register as Admin). Passwords everywhere they're set (Sign Up, Admin's Add Admin, Admin's
Reset Password) must satisfy `PasswordRules`: 6+ characters, at least one letter and one digit.
Sign Up hands off to `CompleteProfileScreen` (route `complete_profile`) before Main Hub is
reachable — a mandatory step collecting name, email (validated with `Patterns.EMAIL_ADDRESS`),
and optional phone/address/avatar. "Forgot?" still just shows a snackbar — Admin's Manage Users
(Reset User Password) is the actual way a lost password gets recovered, since no reset flow was
designed.

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
(editable: name, avatar via a photo picker, phone, address, email) when `userId` matches the
signed-in user, or a read-only view of someone else's profile otherwise. Reached from the Main
Hub's account icon (own profile), from tapping any post/comment author's name, and from
`MembersScreen` (route `members`, reachable via a People icon on Community Feed) — a directory
listing every account so residents can see who else is in the community.

Friends/connections, private messaging, and group chat were asked about and intentionally not
built: a friends system needs a request/accept table and UI; 1:1 messaging needs a
conversations+messages schema and a chat UI; group chat needs that plus a membership table on
top. All three are meaningfully larger than anything else in this app so far (comparable to
building a small chat app) and were deferred rather than attempted partially.

Manage Contacts (EmergencyContacts) and Manage Guides (SafetyGuides) don't have Admin screens
yet — they weren't in the original `yay.pdf` wireframes and are still just the seeded data.
