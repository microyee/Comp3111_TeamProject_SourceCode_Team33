package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import static com.hkust.marketplace.utils.MarketplaceAccounts.*;

public final class SellerMarketplace {
    public record ListingDetails(ListingType type, String title, String category, String condition,
                                 String description, String meetup, Double price, Integer lendingDays,
                                 List<String> photos) {}

    public record ListingStats(String id, String title, int views, long offers, long loanRequests,
                               long claims, long completedDeals) {}

    public record Stats(List<ListingStats> listings, long offers, long loanRequests, long claims,
                        long completedSales, long returnedLoans, long completedGiveaways, long views,
                        double averageRating, int ratingCount) {}

    private final DummyDataService data;
    private final MarketplaceAccounts accounts;

    public SellerMarketplace(DummyDataService data) {
        this.data = data;
        accounts = new MarketplaceAccounts(data);
    }

    public Listing save(User seller, String listingId, ListingDetails details) {
        synchronized (data) {
            accounts.requireAccount(seller, UserRole.SELLER);
            Listing item = listingId == null ? new Listing() : ownedListing(seller, listingId);
            if (listingId != null) require(canEdit(seller, item.getId()), "This listing can no longer be edited.");
            require(details != null && details.type() != null, "Choose a listing type.");
            String title = requiredText(details.title(), "Item name", 100);
            String description = requiredText(details.description(), "Description", 2000);
            require(details.category() != null && DummyDataService.CATEGORIES.contains(details.category()), "Choose a category.");
            require(details.condition() != null && DummyDataService.CONDITIONS.contains(details.condition()), "Choose a condition.");
            require(details.meetup() != null && DummyDataService.HKUST_LOCATIONS.contains(details.meetup()),
                    "Choose an HKUST meet-up location.");
            switch (details.type()) {
                case SELL -> require(MarketplaceActivity.validPrice(details.price()) && details.price() <= 1_000_000,
                        "Enter a price between HK$0.01 and HK$1,000,000 with up to two decimal places.");
                case LEND -> require(details.lendingDays() != null && details.lendingDays() >= 1 && details.lendingDays() <= 365,
                        "Enter a maximum loan duration between 1 and 365 whole days.");
                case GIVEAWAY -> { }
            }
            var photos = new LinkedHashSet<>(details.photos() == null ? List.<String>of() : details.photos());
            require(photos.size() <= 6 && photos.stream().noneMatch(path -> clean(path).isEmpty()), "Attach up to six photos.");

            item.setTitle(title);
            item.setDescription(description);
            item.setCategory(details.category());
            item.setCondition(details.condition());
            item.setMeetupLocation(details.meetup());
            item.setType(details.type());
            item.setPrice(details.type() == ListingType.SELL ? details.price() : null);
            item.setLendingDays(details.type() == ListingType.LEND ? details.lendingDays() : null);
            item.setPhotoPaths(List.copyOf(photos));
            item.setSellerUsername(seller.getUsername());
            item.setSellerFullName(seller.getFullName());
            item.setStatus(ListingStatus.PENDING_REVIEW);
            item.setReviewedAt(null);
            item.setRejectionReason(null);
            data.addListing(item);

            String action = listingId == null ? "submitted" : "resubmitted";
            data.addNotification(new Notification(DummyDataService.ADMIN_USERNAME, "Listing " + action + " for review",
                    "@" + seller.getUsername() + " " + action + " " + title, "Submission", "REVIEW", item.getId()));
            data.addNotification(new Notification(seller.getUsername(), "Listing pending review", title,
                    "Listing", "LISTINGS", item.getId()));
            return item;
        }
    }

    public boolean canEdit(User seller, String id) {
        synchronized (data) {
            Listing listing = ownedListing(seller, id);
            return switch (listing.getStatus()) {
                case LIVE, PENDING_REVIEW, REJECTED -> data.getActiveRequestsForListing(id).isEmpty();
                case RESERVED, SOLD, LENT, GIVEN, REMOVED -> false;
            };
        }
    }

    public void remove(User seller, String id) {
        synchronized (data) {
            Listing listing = ownedListing(seller, id);
            require(canEdit(seller, id), "This listing can no longer be deleted.");
            listing.setStatus(ListingStatus.REMOVED);
            listing.setRemovalReason("Withdrawn by seller");
            listing.setRemovedBy(seller.getUsername());
            listing.setRemovedAt(LocalDateTime.now());
        }
    }

    public boolean canMarkSold(User seller, String id) {
        synchronized (data) {
            Listing listing = ownedListing(seller, id);
            return listing.getType() == ListingType.SELL
                    && (listing.getStatus() == ListingStatus.RESERVED && acceptedSale(id) != null
                    || listing.getStatus() == ListingStatus.LIVE && data.getActiveRequestsForListing(id).isEmpty());
        }
    }

    public void markSold(User seller, String id) {
        synchronized (data) {
            Listing listing = ownedListing(seller, id);
            require(canMarkSold(seller, id), "This sale can no longer be marked sold. Resolve open offers first.");
            Deal deal = acceptedSale(id);
            if (deal == null) listing.setStatus(ListingStatus.SOLD);
            else data.activity().finish(seller, deal.getId());
        }
    }

    private Deal acceptedSale(String id) {
        return data.getDealsForListing(id).stream()
                .filter(d -> d.getDealType() == ListingType.SELL && "Accepted".equals(d.getStatus()))
                .findFirst().orElse(null);
    }

    private Listing ownedListing(User seller, String id) {
        accounts.requireAccount(seller, UserRole.SELLER);
        Listing listing = data.findListing(id);
        require(listing != null && seller.getUsername().equals(listing.getSellerUsername()), "Listing unavailable.");
        return listing;
    }

    public Stats stats(User seller) {
        synchronized (data) {
            accounts.requireAccount(seller, UserRole.SELLER);
            String username = seller.getUsername();
            List<Request> requests = data.getRequestsForSeller(username);
            List<Deal> deals = data.getDealsForSeller(username).stream().filter(Deal::isFinished).toList();
            List<Listing> items = data.getListingsBySeller(username);
            List<ListingStats> listings = items.stream().map(l -> {
                List<Request> itemRequests = requests.stream().filter(r -> l.getId().equals(r.getListingId())).toList();
                long completed = deals.stream().filter(d -> l.getId().equals(d.getListingId())).count();
                return new ListingStats(l.getId(), l.getTitle(), l.getViewCount(), count(itemRequests, ListingType.SELL),
                        count(itemRequests, ListingType.LEND), count(itemRequests, ListingType.GIVEAWAY), completed);
            }).toList();
            return new Stats(listings, count(requests, ListingType.SELL), count(requests, ListingType.LEND),
                    count(requests, ListingType.GIVEAWAY), countDeals(deals, ListingType.SELL),
                    countDeals(deals, ListingType.LEND), countDeals(deals, ListingType.GIVEAWAY),
                    items.stream().mapToLong(Listing::getViewCount).sum(),
                    data.getPublicAverageRating(username), data.getPublicRatingCount(username));
        }
    }

    private static long count(List<Request> requests, ListingType type) {
        return requests.stream().filter(r -> r.getType() == type).count();
    }

    private static long countDeals(List<Deal> deals, ListingType type) {
        return deals.stream().filter(d -> d.getDealType() == type).count();
    }

    public void reply(User seller, String id, String text) {
        synchronized (data) {
            Rating rating = manageableRating(seller, id);
            String reply = requiredText(text, "Reply", 1500);
            if (Objects.equals(rating.getSellerReply(), reply)) return;
            rating.setSellerReply(reply);
            rating.setSellerReplyAt(LocalDateTime.now());
            data.updateRating(rating);
            data.addNotification(new Notification(rating.getBuyerUsername(), "Seller replied to your review",
                    rating.getListingTitle() + " · " + reply, "Rating", "RATINGS", rating.getId()));
        }
    }

    public Report flag(User seller, String id, String reason) {
        synchronized (data) {
            Rating rating = manageableRating(seller, id);
            return data.catalog().fileReport(seller, Report.Target.RATING, id, rating.getListingTitle(), reason);
        }
    }

    private Rating manageableRating(User seller, String id) {
        accounts.requireAccount(seller, UserRole.SELLER);
        Rating rating = data.findRating(id);
        require(rating != null && seller.getUsername().equals(rating.getSellerUsername()) && !rating.isHidden(),
                "This review is unavailable for replies or reports.");
        return rating;
    }
}
