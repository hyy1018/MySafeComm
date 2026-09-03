# My Safe Community App — Database Schema (v2)

Source: `Note/yay.pdf` (original outline) + Community Feed addon module (this update).

## Existing Tables (from yay.pdf)

| Table | Key Fields | Purpose |
|---|---|---|
| Users | Id, Password, Name, Role, Phone, Address, Email, AvatarUri, LastSeenActivityAt | Resident, Admin, Responder accounts (Phone/Address/Email split into their own columns; were briefly one shared "Contact" field). LastSeenActivityAt drives the Community activity badge |
| Reports | ReportID, UserID, Title, Location, Description, Status, Photo | Hazard submissions and tracking |
| Alerts | AlertID, Title, Body, Priority, Location, IssuedBy, Timestamp | Community notices, formatted as formal official notices (Location + IssuedBy + Timestamp-as-date) |
| AlertAcknowledgements | AlertID, UserID, Timestamp | Per-user "Confirm Acknowledgment" on urgent alerts (composite PK, mirrors Likes); also backs a red badge on the bottom nav's Alert tab (`AlertDao.getUnacknowledgedUrgentCount`) when one exists |
| EmergencyContacts | ServiceID, Name, PhoneNo, CategoryEmergency | Single-tap directory for SOS -- just the 5 primary numbers, the separate "Specialized Services" list (IsSpecialized) was cut |
| SafetyGuides | GuideID, CategorySafety, Steps | Procedural content for Safety Guide library (Steps: one step per line, each formatted `Title||Description`) |

## New Tables — Community Feed Addon Module

Reddit/Facebook-style feed: users upload posts, comment, and like; admin can edit posts.

| Table | Key Fields | Purpose |
|---|---|---|
| Posts | PostID (PK), UserID (FK → Users.Id), Content, ImageURL, Timestamp, IsEdited, EditedByAdminID (FK → Users.Id, nullable) | User-uploaded posts; tracks if/who (admin) last edited a post |
| Comments | CommentID (PK), PostID (FK → Posts.PostID), UserID (FK → Users.Id), Content, Timestamp, ParentCommentID (FK → Comments.CommentID, nullable, self-referencing) | Comments on a post; ParentCommentID null = top-level, set = an IG-style reply to that comment (one level deep only) |
| Likes | LikeID (PK), PostID (FK → Posts.PostID), UserID (FK → Users.Id), Timestamp | Like records; unique constraint on (PostID, UserID) to prevent duplicate likes and allow like-count queries |

### Relationships
- Users 1—N Posts (author)
- Posts 1—N Comments
- Posts 1—N Likes (unique per user per post)
- Users 1—N Comments (author)
- Users 1—N Likes
- Users (Role = Admin) 0—N Posts edited (via EditedByAdminID)

### Activity feed (Instagram-style, not a real push notification)

`ActivityScreen` (route `activity`, opened from a heart icon on Community Feed's top bar) lists
likes and comments made on the signed-in resident's own posts by *other* people -- self-actions
are excluded via `LikeDao.getLikesOnUserPosts` / `CommentDao.getCommentsOnUserPosts` (both join
back to Posts and filter `userId != postOwnerId`). No new table: unread state is just
`Users.LastSeenActivityAt` compared against each like/comment's timestamp
(`PostDao.getUnseenActivityCount`), and opening the screen stamps it to "now". A red badge
shows the unseen count on both the heart icon and the bottom nav's Community tab.

### Comment replies (Instagram-style, one level deep)

`PostDetailScreen` groups a post's comments into top-level (`parentCommentId == null`) and
replies (grouped by their parent's id), showing each top-level comment followed by its replies
indented beneath it. Tapping "Reply" under a top-level comment (not offered on a reply itself --
no reply-to-a-reply) sets which comment you're replying to; the bottom bar shows "Replying to
&lt;name&gt;" with a cancel button, and sending calls
`PostDetailViewModel.addComment(userId, content, parentCommentId)`.

Editing a comment's text is self-only: `CommentRow`'s `canEdit` is strictly
`comment.userId == currentUserId`, separate from `canDelete` (post owner or Admin). So the post
owner and Admin can still delete someone else's comment, but can never edit its wording -- only
the person who wrote it can. `PostDetailViewModel.updateComment` does the Room update plus the
matching Supabase `update()`, same dual-write shape as every other edit in the app.

## Navigation Update

Main Hub has four top-level entries: Report, Alert, Community Feed, and SOS.

```
Main Hub
├── Report
├── Alert
├── Community Feed
│     ├── Post List (view/upload posts)
│     ├── Post Detail (comments, like button)
│     └── Admin: Edit Post
└── SOS
      ├── Emergency Contacts (5 numbers, single-tap call)
      └── Safety Guides (Fire/Flood/Power Outage/Earthquake -> step detail)
```

Admin flow gains: **Manage Posts** (edit post content), alongside existing Manage Reports / Manage Alerts.

Safety Guide used to be its own fifth Main Hub entry (`SafetyGuideScreen`, route `guide`). It's
now folded into SOS (`EmergencyHubScreen`) as a second section below the contacts grid, in the
same `LazyVerticalGrid` with a "Safety Guides" header row between the two -- one continuous page
rather than tabs, so it doesn't read as two features stitched together. `guide_detail/{guideId}`
is unchanged. The bottom nav and Main Hub both dropped their separate Guide entry as a result
(bottom nav is back to 5 tabs).

## Login

`LoginScreen` is now the app's actual start destination, gating everything else; `UserSession`
(in-memory, cleared on process death) tracks who is signed in, replacing the earlier hardcoded
`DemoSession` placeholder. Four seeded test accounts (idempotent `INSERT OR IGNORE`, so they
appear on every install without needing a fresh database):

| ID | Password | Role |
|---|---|---|
| test1 | abc123456 | RESIDENT |
| test2 | abc123456 | RESIDENT |
| admin1 | abc123456 | ADMIN |
| admin2 | abc123456 | ADMIN |

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
resident bottom nav bar. All built on existing DAO methods (no new tables):

| Screen | Route | Backs onto |
|---|---|---|
| Manage Reports | `admin_reports` | `ReportDao.getAll()` / `.updateStatus()` — status chips (Pending/In Progress/Solved) per report |
| Manage Alerts | `admin_alerts` + `admin_alert_form?alertId={id}` | `AlertDao` insert/update/delete; one form screen handles both Add (`alertId=-1`) and Edit |
| Manage Posts | `admin_posts` → `community_post/{postId}` | Browsing list only; tapping a post opens the same `PostDetailScreen` a resident sees for their own post, where Admin gets edit/delete-post/delete-comment (see Community Feed addon module below) |
| Manage SOS | `admin_sos` + `admin_contact_form?serviceId={id}` + `admin_guide_form?guideId={id}` | Full CRUD on `EmergencyContactDao`/`SafetyGuideDao` (insert/update/delete) — same Add-or-Edit form idiom as Manage Alerts (`id=-1` means Add); a change to any contact/guide shows up in resident SOS (`EmergencyHubScreen`) and Search immediately, since both already read the same live `Flow` |
| Manage Users | `admin_users` → `admin_add_admin` / `admin_reset_password` / `admin_delete_user` | Add Admin, Reset Password (both pre-existing), and Delete User Account — type an ID, confirm, permanently gone |
| Messages | `admin_messages` (badge: unseen count) | `MessagesInboxScreen` — see Contact Admin messaging below |

Manage SOS's guide form builds the same `"Title||Description"`-per-line `steps` string the seed
data uses (one `StepInput` row per step, a "+ Add Step" button appends another, a trash icon
removes one) — so an admin-added guide renders through `SafetyGuideDetailScreen` identically to
the 4 seeded ones, no special-casing needed.

## Profile

`ProfileScreen` (route `profile/{userId}`) is one screen for two cases: your own profile
(editable: name, avatar via a photo picker, phone, address, email) when `userId` matches the
signed-in user, or a read-only view of someone else's profile otherwise. Reached from the Main
Hub's account icon (own profile), from tapping any post/comment author's name, and from
`MembersScreen` (route `members`, reachable via a People icon on Community Feed) — a directory
listing every account so residents can see who else is in the community. Your own row is pinned
to the top of that list with a "ME" label and does nothing when tapped (nothing to view/message
about yourself); every other row is tappable to their profile and carries a small chat icon that
opens a direct message thread with them (see Contact Admin messaging below — the same generic
messaging, not a separate system).

Own profile also has a "Change Password" button (`ChangePasswordScreen`, route `change_password`
— self-service, requires the current password, same `PasswordRules` and new-can't-equal-old rule
as Admin's reset). Saving profile changes returns to Main Hub, same pop-or-navigate idiom as the
bottom bar's Home tab, rather than leaving the resident stranded on the form.

A dedicated friends/connections system (request/accept flow, its own table) was asked about and
intentionally not built — direct messaging already covers "reach a specific person," and a
follow/friend list on top of that would be a separate feature with no clear use yet.

Manage Contacts (EmergencyContacts) and Manage Guides (SafetyGuides) don't have Admin screens
yet — they weren't in the original `yay.pdf` wireframes and are still just the seeded data.

## Messaging (Contact Admin, direct chat, and checking for replies)

`LoginScreen`'s "Forgot?" used to just show a snackbar telling you to contact your admin, with no
actual way to do it. It now opens `ContactAdminScreen` (route `contact_admin`, a public route --
reachable while signed out): you type your own User ID (since you can't authenticate), pick which
admin to send it to from a dropdown built from `UserViewModel.users` filtered to `role == ADMIN`
(so it grows automatically as more admins are added), and write a message. Not real-time chat, per
the brief -- it's a simple inbox.

New table: `Messages (MessageID PK, FromUserID FK->Users.Id, ToUserID FK->Users.Id, Body,
Timestamp)`. `FromUserId`/`ToUserId` are generic, not fixed to "resident"/"admin" -- an admin's
reply, and a direct message between two residents (started from a chat icon in Community's
Members list), are just more rows in the same table, so one set of screens serves every case:

- `MessagesInboxScreen` (route `messages_inbox/{userId}`) -- who `userId` has exchanged messages
  with (`MessageDao.getConversationPartnerIds`). Reached three ways: Admin Hub's "Messages" card
  (`admin_messages`, wraps this with `userId` = the admin's own session id, plus an unseen-count
  badge -- see below), a signed-in resident's Main Hub top bar mail icon (same pattern, their own
  session id, sitting between Search and the Profile avatar), and a signed-out resident's Login →
  "Check Messages" (`CheckMessagesScreen`: type your ID, same existence check as Contact Admin,
  then this screen with that typed id).
- `MessageThreadScreen` (route `message_thread/{myUserId}/{otherUserId}`) -- the full
  back-and-forth between those two ids plus a reply box, via `MessageDao.getThread(userA, userB)`.
  `myUserId` is passed explicitly rather than read from session, precisely so the signed-out
  "Check Messages" path (no session to read) works the same way as the signed-in ones.

Local Room's `Flow` already updates both screens live the moment a message is inserted -- the
Refresh button in both screens' top bar exists for the case the teacher's brief specifically
called out as acceptable if real-time is hard: pulling in a message that only exists in Supabase
because it was sent from a different device/install (`MessageViewModel.refreshFromCloud`).

Unread badge: `Users.LastSeenMessagesAt` (same shape as `LastSeenActivityAt`) compared against
each message's timestamp via `MessageDao.getUnseenMessageCount`. Wired onto Admin Hub's "Messages"
card, Main Hub's mail icon, and Community Feed's People icon (all three read the same count; it's
just badged wherever a resident might reach messaging from). Stamped "now" whenever
`MessagesInboxScreen` or `MembersScreen` opens, since those are the actual entry points to reading
a message or starting a per-person chat (mirrors the heart icon/`ActivityScreen` pairing above).

Editing and deleting a message are self-only, same rule as editing a comment: `MessageThreadScreen`
only ever offers the Edit/Delete icons on a bubble where `message.fromUserId == myUserId`, never
on the other person's message. There's no "delete for me" -- `Messages` is one shared row per
message, not a per-user copy, so a delete removes it from both people's thread and an edit changes
what both people see.

## Deleting an account

Both "Profile → Delete My Account" (self) and "Admin → Manage Users → Delete User Account"
(anyone else, looked up by typed ID) call the same underlying deletion: `UserDao.delete()` locally
plus `supabase.from("users").delete()` remotely. Every other table's foreign key to `users(id)` is
`ON DELETE CASCADE` (both in Room's `@ForeignKey` annotations and in `supabase_setup.md`'s SQL),
so removing a user also removes everything they own -- their reports, posts, comments, likes,
messages (sent or received), and alert acknowledgements. This is a deliberate choice, not an
oversight: deleting an account means the person and everything tied to them is gone, not just
their login disabled. The only difference between the two entry points is what happens right
after: self-delete immediately logs the session out and returns to Login (since the account you're
signed in as no longer exists); Admin deleting someone else just returns to the Manage Users list,
since the Admin's own session is untouched. Admin's delete screen also refuses to target the
Admin's own id, pointing them at Profile's self-delete instead, so a mid-session accidental
self-lockout isn't the failure mode of a mistyped ID.

## Architecture: ViewModel + StateFlow, and Supabase as a second data store

Matches the course's taught Room method (`RoomDbTest.zip`: `PersonDb`/`PersonViewModel`/
`PersonViewModelFactory`) rather than screens calling a DAO directly:

```
UI -> ViewModel (StateFlow via dao.getAll().stateIn(...)) -> Dao -> Room (local)
               \-> Supabase (cloud), same write, mirrored -> Postgres
```

Every entity has a matching `viewmodel/*ViewModel.kt` (one file per module: SOS's
`EmergencyContactViewModel`/`SafetyGuideViewModel`, `UserViewModel`/`UserDetailViewModel`,
`ReportViewModel`/`MyReportsViewModel`/`ReportDetailViewModel`, `AlertViewModel`/
`AlertDetailViewModel`/`AlertAckViewModel`, `PostViewModel`/`PostDetailViewModel`/
`PostLikeViewModel`/`ActivityViewModel`). No screen calls `AppDatabase...Dao()` and reads from it
directly anymore — it's always through one of these ViewModels, obtained via
`viewModel(factory = ...Factory(dao))`. A ViewModel that needs one specific row (a single user's
profile, one report, one alert) takes that id through its Factory instead of the shared list
ViewModel, so its `StateFlow` is built once in the ViewModel's body rather than re-created every
recomposition — see `SafetyGuideDetailViewModel`/`UserDetailViewModel`/`ReportDetailViewModel` for
the pattern. Two small, deliberately-left exceptions: the bottom nav's badge counts
(`AlertDao.getUnacknowledgedUrgentCount`, `PostDao.getUnseenActivityCount`) stay as direct DAO
`Flow`s in `AppBottomBar`/`CommunityFeedScreen`, since each spans two entities with no single
ViewModel to live in cleanly, and they're read-only.

Every entity class (`UserEntity`, `ReportEntity`, `AlertEntity`,
`AlertAcknowledgementEntity`, `EmergencyContactEntity`, `SafetyGuideEntity`, `PostEntity`,
`CommentEntity`, `LikeEntity`, and their enums) is annotated both `@Entity` (Room) and
`@Serializable` (kotlinx.serialization) — the same class doubles as the Supabase row model, so
there's no separate "cloud" data class to keep in sync. Per the requirement that data live in
**both** places, every ViewModel write method writes to Room first, then mirrors the identical
write to the matching Supabase table (`Note/supabase_setup.md` has the full table DDL). Since Room
assigns autoGenerate ids locally, the Supabase insert explicitly reuses that same id rather than
letting Postgres generate its own, so a row's id agrees between both stores. Every Supabase call is
gated behind `isSupabaseConfigured` (true once `SupabaseClient.kt`'s placeholder URL/key are
replaced with a real project's values) and wrapped in `try/catch`, so the app runs fully offline
against Room alone until the cloud project is set up, and a lost network connection afterwards
never blocks the local write.
