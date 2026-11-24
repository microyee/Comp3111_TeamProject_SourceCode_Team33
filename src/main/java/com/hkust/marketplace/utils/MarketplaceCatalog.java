package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.hkust.marketplace.utils.MarketplaceAccounts.*;

public final class MarketplaceCatalog {
    public enum Sort {
        NEWEST("Newest"), PRICE_ASC("Price low to high"), PRICE_DESC("Price high to low");
        private final String label;
        Sort(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public record Filter(String query, ListingType type, String category, String condition,
                         Double minPrice, Double maxPrice, Sort sort) {}

    private final DummyDataService data;
    private final MarketplaceAccounts accounts;
    private final Map<String, Set<String>> favourites = new HashMap<>();
    private final Map<String, Set<String>> follows = new HashMap<>();
    private final Map<String, Report> reports = new LinkedHashMap<>();
    private int nextReportId = 1;

    public MarketplaceCatalog(DummyDataService data) {
        this.data = data;
        accounts = new MarketplaceAccounts(data);
    }

    public List<Listing> browse(User buyer, Filter filter) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Objects.requireNonNull(filter, "filter");
            validateRange(filter.minPrice(), filter.maxPrice());
            String query = clean(filter.query()).toLowerCase(Locale.ROOT);
            Stream<Listing> listings = data.getLiveListings().stream()
                    .filter(l -> filter.type() == null || l.getType() == filter.type())
                    .filter(l -> filter.category() == null || filter.category().equals(l.getCategory()))
                    .filter(l -> filter.condition() == null || filter.condition().equals(l.getCondition()))
                    .filter(l -> contains(l.getTitle(), query) || contains(l.getDescription(), query)
                            || contains(l.getSellerFullName(), query));
            if (filter.minPrice() != null || filter.maxPrice() != null) {
                listings = listings.filter(l -> l.getPrice() != null
                        && (filter.minPrice() == null || l.getPrice() >= filter.minPrice())
                        && (filter.maxPrice() == null || l.getPrice() <= filter.maxPrice()));
            }
            Comparator<Listing> newest = Comparator.comparing(Listing::getPostedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Listing::getId);
            return listings.sorted(switch (Objects.requireNonNullElse(filter.sort(), Sort.NEWEST)) {
                case NEWEST -> newest;
                case PRICE_ASC -> Comparator.comparing(Listing::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(newest);
                case PRICE_DESC -> Comparator.comparing(Listing::getPrice,
                        Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(newest);
            }).toList();
        }
    }

    public Listing openListing(User buyer, String id) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Listing listing = data.findListing(id);
            require(listing != null, "Listing no longer exists.");
            if (!isPublic(listing)) {
                require(data.getRequestsForBuyer(buyer.getUsername()).stream().anyMatch(r -> id.equals(r.getListingId()))
                        || data.getDealsForBuyer(buyer.getUsername()).stream().anyMatch(d -> id.equals(d.getListingId()))
                        || data.getChatsForBuyer(buyer.getUsername()).stream().anyMatch(c -> id.equals(c.getListingId())),
                        "This listing is unavailable.");
            }
            listing.setViewCount(listing.getViewCount() + 1);
            return listing;
        }
    }

    public boolean isPublic(Listing listing) {
        return listing.getStatus() == ListingStatus.LIVE && available(data.findUser(listing.getSellerUsername()));
    }

    public void favourite(User buyer, String id, boolean save) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Set<String> ids = entries(favourites, buyer);
            if (!save) {
                ids.remove(id);
                return;
            }
            Listing listing = data.findListing(id);
            require(listing != null && isPublic(listing), "This listing is unavailable.");
            ids.add(id);
        }
    }

    public boolean isFavourite(User buyer, String id) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            return entries(favourites, buyer).contains(id);
        }
    }

    public List<Listing> favourites(User buyer) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            return entries(favourites, buyer).stream().map(data::findListing).filter(Objects::nonNull).toList();
        }
    }

    public void follow(User buyer, String username, boolean save) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            String name = clean(username).toLowerCase(Locale.ROOT);
            Set<String> names = entries(follows, buyer);
            if (!save) {
                names.remove(name);
                return;
            }
            User seller = data.findUser(name);
            require(available(seller) && seller.getRole() == UserRole.SELLER, "Seller profile is unavailable.");
            names.add(name);
        }
    }

    public boolean isFollowing(User buyer, String seller) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            return entries(follows, buyer).contains(clean(seller).toLowerCase(Locale.ROOT));
        }
    }

    public List<User> following(User buyer) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            return entries(follows, buyer).stream().map(data::findUser).filter(Objects::nonNull).toList();
        }
    }

    void notifyFollowers(Listing listing) {
        follows.forEach((username, sellers) -> {
            if (sellers.contains(listing.getSellerUsername()) && available(data.findUser(username))) {
                data.addNotification(new Notification(username, "New listing from " + listing.getSellerFullName(),
                        listing.getTitle(), "Listing", "LISTING", listing.getId()));
            }
        });
    }

    public Report reportListing(User buyer, String id, String reason) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Listing listing = data.findListing(id);
            require(listing != null && isPublic(listing), "This listing is unavailable.");
            return fileReport(buyer, Report.Target.LISTING, id, listing.getTitle(), reason);
        }
    }

    Report fileReport(User user, Report.Target target, String id, String subject, String reason) {
        String message = requiredText(reason, "Reason", 1000);
        require(reports.values().stream().noneMatch(r -> r.isOpen() && r.target() == target
                && r.targetId().equals(id) && r.reporter().equals(user.getUsername())),
                "You already have an open report for this item.");
        String reportId = "RP" + nextReportId++;
        Report report = new Report(reportId, user.getUsername(), target, id, subject,
                message, LocalDateTime.now(), null, null);
        reports.put(reportId, report);
        data.addNotification(new Notification(DummyDataService.ADMIN_USERNAME, "New moderation report",
                "@" + user.getUsername() + " reported " + subject + ". " + message,
                "Report", "REPORTS", reportId));
        return report;
    }

    public List<Report> reports(User admin) {
        synchronized (data) {
            accounts.requireAccount(admin, UserRole.ADMIN);
            return reports.values().stream().sorted(Comparator.comparing(Report::submittedAt).reversed()).toList();
        }
    }

    public void resolve(User admin, String reportId, String resolution) {
        synchronized (data) {
            accounts.requireAccount(admin, UserRole.ADMIN);
            Report report = reports.get(reportId);
            require(report != null && report.isOpen(), "This report has already been resolved or is unavailable.");
            String note = requiredText(resolution, "Resolution", 1000);
            reports.put(reportId, report.resolve(note, LocalDateTime.now()));
            data.logAudit(admin.getUsername(), "RESOLVE_REPORT", "REPORT", reportId, note);
            data.addNotification(new Notification(report.reporter(), "Report reviewed", report.subject() + ": " + note,
                    "Report"));
        }
    }

    public static boolean contains(String value, String normalizedQuery) {
        return clean(value).toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    public static void validateRange(Double min, Double max) {
        require(validBound(min) && validBound(max) && (min == null || max == null || max >= min),
                "Enter a valid price range. The maximum must be at least the minimum.");
    }

    private static boolean validBound(Double value) {
        return value == null || Double.isFinite(value) && value >= 0;
    }

    private Set<String> entries(Map<String, Set<String>> map, User user) {
        return map.computeIfAbsent(user.getUsername(), key -> new LinkedHashSet<>());
    }
}
