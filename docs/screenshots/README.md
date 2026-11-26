# Application screenshots (sample inputs and outputs)

These screenshots come from the final Sprint 2 build and are the same images used in
the Sprint 2 Project Management Pack. They show representative inputs and the resulting
UI state for each portal.

| Screenshot | Portal | Sample input / action | Output shown |
|---|---|---|---|
| [buyer-browse-marketplace.png](buyer-browse-marketplace.png) | Buyer | Open **Browse Marketplace** with the seeded live listings | Photo-card grid with type badge, title, price, seller, category, condition and meet-up location |
| [buyer-listing-details.png](buyer-listing-details.png) | Buyer | Open the **Ceramic Coffee Mug** listing | Full description, photo, seller profile, favourite, report, claim, chat and back actions |
| [buyer-my-deals.png](buyer-my-deals.png) | Buyer | Open **My Deals** with pending, countered and accepted requests | Request status, counterparty, proposed terms, Accept/Decline counteroffer, Withdraw and Chat actions |
| [buyer-messages.png](buyer-messages.png) | Buyer | Open **Messages** and type a message in the composer | Per-listing conversation thread with timestamps and a search box |
| [buyer-favourites.png](buyer-favourites.png) | Buyer | Save a listing, then open **Favourites** | Saved listing card with **View listing** and **Remove favourite** |
| [buyer-following.png](buyer-following.png) | Buyer | Follow a seller, then open **Following** | Followed seller card with **View profile** and **Unfollow** |
| [seller-my-listings.png](seller-my-listings.png) | Seller | Create/edit listings and open **My Listings** | Owned listings with status, type, price, views, photo and per-row actions |
| [seller-incoming-requests.png](seller-incoming-requests.png) | Seller | Buyer submits an offer; seller opens **Incoming Requests** | Offer details with **Accept**, **Decline** and **Counter** actions |
| [seller-stats.png](seller-stats.png) | Seller | Open **Seller Stats** | Views, offers, loan requests, claims, completed deals and average rating, with charts |
| [admin-listing-review.png](admin-listing-review.png) | Admin | Open **Listing Review & Approval** with a pending submission | Seller, category, type, condition, meet-up, price/duration, photos and **Approve** / **Reject** |
| [admin-all-listings.png](admin-all-listings.png) | Admin | Open **Manage All Listings** with the search/filter toolbar | Every listing with status badges and per-row **Edit** / **Take Down** actions |
| [admin-moderation-reports.png](admin-moderation-reports.png) | Admin | Buyer reports a listing; admin opens **Moderation Reports** | Open report, reported listing, reporter, reason and **Resolve** action |
| [sprint2-burndown.png](sprint2-burndown.png) | Project | Planned versus remaining Sprint 2 effort | Completed 120 h sprint with 0 h remaining |
| [hkust-logo.png](hkust-logo.png) | Shared | Application branding | HKUST logo used on the welcome and login screens |

The application screens were captured at 1200 × 780 from the running JavaFX
application; the logo and the Sprint 2 burndown chart keep their native sizes.
Reproduce the application screens with:

```powershell
.\mvnw.cmd clean compile javafx:run
```
