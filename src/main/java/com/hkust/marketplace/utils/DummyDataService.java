package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-memory data store for the frontend demo.
 *
 * <p>All data is lost when the application closes.  The service deliberately keeps the
 * "business rules" (approval, take-down, soft delete, notification, rating visibility)
 * here rather than in the JavaFX controllers so the same rules are applied no matter
 * which screen triggers them.</p>
 *
 * <p>TODO: Replace this service with real repositories / API clients.</p>
 */
public class DummyDataService {



    /** Username of the seeded administrator account. */
    public static final String ADMIN_USERNAME = "admin";

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Listing> listings = new LinkedHashMap<>();
    private final List<Request> requests = new ArrayList<>();
    private final List<Deal> deals = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<Rating> ratings = new ArrayList<>();
    private final List<Chat> chats = new ArrayList<>();
    private final List<AuditLogEntry> auditLog = new ArrayList<>();

    private final AtomicInteger listingIdCounter = new AtomicInteger(100);
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);
    private final AtomicInteger dealIdCounter = new AtomicInteger(500);
    private final AtomicInteger notifIdCounter = new AtomicInteger(1);
    private final AtomicInteger ratingIdCounter = new AtomicInteger(1);
    private final AtomicInteger chatIdCounter = new AtomicInteger(1);
    private final AtomicInteger messageIdCounter = new AtomicInteger(1);

    // Predefined HKUST locations
    public static final List<String> HKUST_LOCATIONS = List.of(
            "Academic Building (LG1)",
            "Library",
            "Student Centre",
            "Hall 1 Lobby",
            "Hall 2 Lobby",
            "Hall 3 Lobby",
            "Hall 4 Lobby",
            "Hall 5 Lobby",
            "Hall 6 Lobby",
            "Hall 7 Lobby",
            "Hall 8 Lobby",
            "Hall 9 Lobby",
            "UG Hall 1",
            "UG Hall 2",
            "North Gate",
            "South Gate",
            "Shaw Auditorium",
            "Coffee Shop (LG5)",
            "Sports Hall"
    );

    public static final List<String> CATEGORIES = List.of(
            "Textbooks", "Electronics", "Furniture", "Clothing",
            "Stationery", "Sports Equipment", "Kitchenware", "Others"
    );

    public static final List<String> CONDITIONS = List.of(
            "New", "Like New", "Used", "For Parts"
    );

    private static final class Singleton {
        private static final DummyDataService INSTANCE = new DummyDataService(true, LocalDateTime::now);
    }

    private final MarketplaceCatalog catalog = new MarketplaceCatalog(this);
    private final MarketplaceActivity activity;

    DummyDataService(boolean seed, Supplier<LocalDateTime> time) {
        activity = new MarketplaceActivity(this, time);
        if (seed) seedData();
    }

    public static DummyDataService getInstance() {
        return Singleton.INSTANCE;
    }

    public MarketplaceCatalog catalog() { return catalog; }
    public MarketplaceActivity activity() { return activity; }

    // =====================================================================
    // Seed data
    // =====================================================================

    private void seedData() {
        // Admin (password is hashed by the User constructor)
        User admin = new Admin("admin", "System Admin", "admin123");
        admin.setFullName("HKUST Marketplace Admin");
        users.put("admin", admin);

        // Sample buyers
        User buyer1 = new Buyer("alice", "Alice Chan", "password1");
        buyer1.setResidenceHall("Hall 3");
        users.put("alice", buyer1);

        User buyer2 = new Buyer("bob", "Bob Wong", "password1");
        buyer2.setResidenceHall("Off-campus");
        users.put("bob", buyer2);

        // Sample sellers
        User seller1 = new Seller("charlie", "Charlie Lee", "password1");
        seller1.setBio("Final year CSE student selling textbooks and gadgets.");
        seller1.setPreferredMeetup("Library");
        users.put("charlie", seller1);

        User seller2 = new Seller("diana", "Diana Ng", "password1");
        seller2.setBio("Selling furniture and kitchen items before graduation.");
        seller2.setPreferredMeetup("Hall 5 Lobby");
        users.put("diana", seller2);

        // Sample listings: one listing for every status used by the admin screens.
        Listing l1 = createListing("Calculus Textbook (Stewart)",
                "Barely used, 8th edition. Perfect for MATH 1013.",
                "Textbooks", ListingType.SELL, "Like New", 180.0, null,
                "Library", "charlie", "Charlie Lee");
        l1.setStatus(ListingStatus.RESERVED);
        l1.setPostedAt(LocalDateTime.now().minusDays(6));
        listings.put(l1.getId(), l1);

        Listing l2 = createListing("MacBook Pro Charger 61W",
                "Original Apple charger, works perfectly.",
                "Electronics", ListingType.SELL, "Used", 250.0, null,
                "Academic Building (LG1)", "charlie", "Charlie Lee");
        l2.setStatus(ListingStatus.LIVE);
        l2.setPostedAt(LocalDateTime.now().minusDays(4));
        listings.put(l2.getId(), l2);

        Listing l3 = createListing("IKEA Desk Lamp",
                "Good condition, LED bulb included.",
                "Furniture", ListingType.GIVEAWAY, "Used", null, null,
                "Hall 5 Lobby", "diana", "Diana Ng");
        l3.setStatus(ListingStatus.RESERVED);
        l3.setPostedAt(LocalDateTime.now().minusDays(3));
        listings.put(l3.getId(), l3);

        Listing l4 = createListing("Graphing Calculator TI-84",
                "Can lend for one semester. Includes USB cable.",
                "Electronics", ListingType.LEND, "Like New", null, 90,
                "Student Centre", "diana", "Diana Ng");
        l4.setStatus(ListingStatus.LIVE);
        l4.setPostedAt(LocalDateTime.now().minusDays(2));
        listings.put(l4.getId(), l4);

        Listing l5 = createListing("Mini Fridge",
                "Works well, cleaning required. Currently pending admin review.",
                "Furniture", ListingType.SELL, "Used", 400.0, null,
                "Hall 2 Lobby", "diana", "Diana Ng");
        l5.setStatus(ListingStatus.PENDING_REVIEW);
        l5.setPostedAt(LocalDateTime.now().minusHours(5));
        listings.put(l5.getId(), l5);

        Listing l6 = createListing("Broken Desk Chair",
                "Leg is cracked and the gas lift no longer holds height.",
                "Furniture", ListingType.SELL, "For Parts", 50.0, null,
                "Hall 2 Lobby", "diana", "Diana Ng");
        l6.setStatus(ListingStatus.REJECTED);
        l6.setRejectionReason("Safety risk: the broken leg is not clearly shown and the listing does not mention the gas-lift failure.");
        l6.setPostedAt(LocalDateTime.now().minusDays(9));
        l6.setReviewedAt(LocalDateTime.now().minusDays(8));
        listings.put(l6.getId(), l6);

        Listing l7 = createListing("Solid Wood Study Desk",
                "Sturdy desk, assembled. Buyer must arrange transport.",
                "Furniture", ListingType.SELL, "Used", 300.0, null,
                "Hall 1 Lobby", "charlie", "Charlie Lee");
        l7.setStatus(ListingStatus.SOLD);
        l7.setPostedAt(LocalDateTime.now().minusDays(28));
        l7.setReviewedAt(LocalDateTime.now().minusDays(27));
        listings.put(l7.getId(), l7);

        Listing giveaway = createListing("Ceramic Coffee Mug", "Clean mug, available for collection at the Library.",
                "Kitchenware", ListingType.GIVEAWAY, "Like New", null, null,
                "Library", "charlie", "Charlie Lee");
        giveaway.setStatus(ListingStatus.LIVE);
        addListing(giveaway);

        // Sample requests and the deals they create.  Every seeded deal has a
        // corresponding Request, matching the UML Request 1 — 0..1 Deal relationship.
        LocalDateTime now = LocalDateTime.now();
        Request r1 = createRequest(l1, "alice", "Accepted", 160.0, null, null,
                now.minusDays(3), now.minusDays(3));
        Deal d1 = createDeal(r1, "Accepted", null);

        Request r2 = createRequest(l4, "alice", "Pending", null,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(31),
                now.minusDays(1), null);
        Deal d2 = createDeal(r2, "Pending", null);

        Request r3 = createRequest(l2, "bob", "Pending", 220.0, null, null,
                now.minusHours(2), null);
        Deal d3 = createDeal(r3, "Pending", null);

        Request r4 = createRequest(l3, "bob", "Pending", null, null, null,
                now.minusHours(5), null);
        Deal d4 = createDeal(r4, "Pending", null);

        Request r5 = createRequest(l7, "bob", "Accepted", 300.0, null, null,
                now.minusDays(25), now.minusDays(25));
        Deal d5 = createDeal(r5, "Completed", now.minusDays(20));

        Request r6 = createRequest(l2, "alice", "Cancelled", 200.0, null, null,
                now.minusDays(5), now.minusDays(4));
        Deal d6 = createDeal(r6, "Cancelled", null);
        d6.setCancelledAt(now.minusDays(4));

        Request r7 = createRequest(l4, "bob", "Accepted", null,
                LocalDate.now().minusDays(40), LocalDate.now().minusDays(10),
                now.minusDays(42), now.minusDays(40));
        Deal d7 = createDeal(r7, "Completed", now.minusDays(10));

        Request r8 = createRequest(l4, "alice", "Accepted", null,
                LocalDate.now().minusDays(70), LocalDate.now().minusDays(30),
                now.minusDays(72), now.minusDays(70));
        Deal d8 = createDeal(r8, "Returned", now.minusDays(30));

        // Sample ratings.  One hidden rating demonstrates the moderation workflow.
        addSeedRating(null, "charlie", "bob", "Calculus Textbook (Stewart)", 5,
                "Very smooth transaction, book in excellent condition!", null,
                now.minusDays(18), false);
        addSeedRating(null, "charlie", "alice", "Calculus Textbook (Stewart)", 5,
                "Textbook exactly as described. Charlie replied quickly.", null,
                now.minusDays(12), false);
        addSeedRating(d5.getId(), "charlie", "bob", "Solid Wood Study Desk", 4,
                "Desk is sturdy. Transport was my responsibility, as advertised.", null,
                now.minusDays(6), false);
        addSeedRating(null, "diana", "bob", "IKEA Desk Lamp", 4,
                "Lamp worked well. Meet-up was easy to arrange.", null,
                now.minusDays(9), false);
        addSeedRating(d8.getId(), "diana", "alice", "Graphing Calculator TI-84", 5,
                "Calculator in great condition and the loan period was flexible.", null,
                now.minusDays(4), false);
        addSeedRating(null, "diana", "alice", "IKEA Desk Lamp", 3,
                "Item was okay, but the meet-up was delayed by 30 minutes.",
                "Sorry about the delay – I messaged you in the chat thread.",
                now.minusDays(2), true);

        recalculateSellerRating("charlie");
        recalculateSellerRating("diana");

        // A seeded private chat thread (the Chat use case is outside the Admin task,
        // but the domain relationship is modelled and retained).
        Chat chat = new Chat();
        chat.setId("C" + chatIdCounter.getAndIncrement());
        chat.setListingId(l1.getId());
        chat.setListingTitle(l1.getTitle());
        chat.setBuyerUsername("alice");
        chat.setSellerUsername("charlie");
        chat.setCreatedAt(now.minusDays(4));
        addMessage(chat, "alice", "Hi Charlie, is the textbook still available?",
                now.minusDays(4));
        addMessage(chat, "charlie", "Yes, it is. Are you free to meet at the Library?",
                now.minusDays(4).plusMinutes(5));
        chats.add(chat);

        // Sample notifications (IDs are assigned by addNotification, which also makes
        // the mark-as-read workflow reliable).
        Notification n1 = new Notification("alice", "Offer Accepted",
                "Charlie accepted your offer of HK$160 for Calculus Textbook.",
                "Offer", "DEALS", d1.getId());
        addNotification(n1);
        n1.setRead(true);

        addNotification(new Notification("charlie", "New Offer Received",
                "Alice offered HK$160 for your Calculus Textbook.",
                "Offer", "DEALS", d1.getId()));
        addNotification(new Notification("diana", "Listing Pending",
                "Your Mini Fridge listing is awaiting admin review.",
                "System", "LISTINGS", l5.getId()));
        addNotification(new Notification("bob", "Offer Cancelled",
                "Your previous offer for MacBook Pro Charger 61W was cancelled.",
                "Offer", "DEALS", d6.getId()));

        addNotification(new Notification(ADMIN_USERNAME, "New Listing Submitted",
                "Diana Ng submitted 'Mini Fridge' for review.",
                "Submission", "REVIEW", l5.getId()));
        catalog.fileReport(buyer2, Report.Target.LISTING, l2.getId(), l2.getTitle(),
                "Misleading condition description.");
        addNotification(new Notification(ADMIN_USERNAME, "User Account Updated",
                "Seller charlie updated their preferred meet-up location to Library.",
                "Account", "USERS", "charlie"));
    }

    private Listing createListing(String title, String desc, String cat, ListingType type,
                                  String condition, Double price, Integer days,
                                  String meetup, String sellerUser, String sellerName) {
        Listing l = new Listing();
        l.setId("L" + listingIdCounter.getAndIncrement());
        l.setTitle(title);
        l.setDescription(desc);
        l.setCategory(cat);
        l.setType(type);
        l.setCondition(condition);
        l.setPrice(price);
        l.setLendingDays(days);
        l.setMeetupLocation(meetup);
        l.setSellerUsername(sellerUser);
        l.setSellerFullName(sellerName);
        l.setPostedAt(LocalDateTime.now().minusDays(new Random().nextInt(10)));
        return l;
    }

    private Request createRequest(Listing listing, String buyer, String status, Double price,
                                  LocalDate borrowStart, LocalDate borrowEnd,
                                  LocalDateTime createdAt, LocalDateTime respondedAt) {
        Request request = new Request();
        request.setId("RQ" + requestIdCounter.getAndIncrement());
        request.setListingId(listing.getId());
        request.setListingTitle(listing.getTitle());
        request.setBuyerUsername(buyer);
        request.setSellerUsername(listing.getSellerUsername());
        request.setType(listing.getType());
        request.setStatus(status);
        request.setOfferPrice(price);
        request.setBorrowStart(borrowStart);
        request.setBorrowEnd(borrowEnd);
        request.setCreatedAt(createdAt);
        request.setRespondedAt(respondedAt);
        requests.add(request);
        return request;
    }

    Deal createDeal(Request request, String status, LocalDateTime completedAt) {
        Deal deal = new Deal();
        deal.setId("D" + dealIdCounter.getAndIncrement());
        deal.setRequestId(request.getId());
        deal.setListingId(request.getListingId());
        deal.setListingTitle(request.getListingTitle());
        deal.setBuyerUsername(request.getBuyerUsername());
        deal.setSellerUsername(request.getSellerUsername());
        deal.setDealType(request.getType());
        deal.setStatus(status);
        deal.setOfferPrice(request.getOfferPrice());
        deal.setBorrowStartAt(request.getBorrowStartAt());
        deal.setBorrowEndAt(request.getBorrowEndAt());
        deal.setCategory(findListing(request.getListingId()).getCategory());
        deal.setCreatedAt(request.getCreatedAt());
        deal.setCompletedAt(completedAt);
        deals.add(deal);
        return deal;
    }

    private void addSeedRating(String dealId, String seller, String buyer, String listingTitle,
                               int rating, String comment, String sellerReply,
                               LocalDateTime createdAt, boolean hidden) {
        Rating r = new Rating();
        r.setId("R" + ratingIdCounter.getAndIncrement());
        r.setDealId(dealId);
        r.setSellerUsername(seller);
        r.setBuyerUsername(buyer);
        r.setListingTitle(listingTitle);
        r.setRating(rating);
        r.setComment(comment);
        r.setSellerReply(sellerReply);
        r.setCreatedAt(createdAt);
        r.setHidden(hidden);
        ratings.add(r);
    }

    void addMessage(Chat chat, String sender, String content, LocalDateTime sentAt) {
        Message message = new Message(sender, content);
        message.setId("M" + messageIdCounter.getAndIncrement());
        message.setSentAt(sentAt);
        chat.addMessage(message);
    }

    // =====================================================================
    // Users
    // =====================================================================

    /** Active and deactivated accounts, excluding soft-deleted accounts. */
    public Collection<User> getAllUsers() {
        return users.values().stream()
                .filter(u -> !u.isDeleted())
                .collect(Collectors.toList());
    }

    /** Includes soft-deleted accounts; intended for audit/debugging only. */
    public List<User> getAllUsersIncludingDeleted() {
        return new ArrayList<>(users.values());
    }

    public boolean usernameExists(String username) {
        if (username == null) return false;
        return users.containsKey(username.trim().toLowerCase(Locale.ROOT));
    }

    public User findUser(String username) {
        if (username == null) return null;
        return users.get(username.trim().toLowerCase(Locale.ROOT));
    }

    public synchronized void registerUser(User user) {
        users.put(user.getUsername().toLowerCase(Locale.ROOT), user);
    }

    public synchronized void updateUser(User user) {
        users.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        // Listings keep a denormalised seller name for display; keep it in sync when an
        // administrator edits the seller's full name.
        for (Listing listing : listings.values()) {
            if (listing.getSellerUsername() != null
                    && listing.getSellerUsername().equalsIgnoreCase(user.getUsername())) {
                listing.setSellerFullName(user.getFullName());
            }
        }
    }

    public List<User> getUsersByRole(UserRole role) {
        return users.values().stream()
                .filter(u -> u.getRole() == role && !u.isDeleted())
                .collect(Collectors.toList());
    }

    /** Sellers who may be chosen when an admin creates a listing on their behalf. */
    public List<User> getActiveSellers() {
        return users.values().stream()
                .filter(u -> u.getRole() == UserRole.SELLER && u.isActive() && !u.isDeleted())
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Deactivates an account.  The user cannot log in and their live listings disappear
     * from the marketplace (they are retained and reappear on reactivation).
     */
    public synchronized boolean deactivateUser(String username, String actor) {
        User user = findUser(username);
        if (user == null || user.isDeleted() || !user.isActive() || user.getRole() == UserRole.ADMIN) return false;

        user.setActive(false);
        activity.cancelUnavailable(user);
        SessionManager.logoutIf(user.getUsername());

        addNotification(new Notification(user.getUsername(), "Account Deactivated",
                "Your account has been deactivated by an administrator. "
                        + "You cannot log in until it is reactivated.",
                "Account"));
        addNotification(new Notification(actor, "User Deactivated",
                "@" + user.getUsername() + " (" + user.getFullName() + ") was deactivated.",
                "Account", "USERS", user.getUsername()));
        logAudit(actor, "DEACTIVATE_USER", "USER", user.getUsername(), "Deactivated account");
        return true;
    }

    /** Reactivates a previously deactivated account. */
    public synchronized boolean reactivateUser(String username, String actor) {
        User user = findUser(username);
        if (user == null || user.isDeleted() || user.isActive()) return false;

        user.setActive(true);
        addNotification(new Notification(user.getUsername(), "Account Reactivated",
                "Your account has been reactivated by an administrator. You can log in again.",
                "Account"));
        addNotification(new Notification(actor, "User Reactivated",
                "@" + user.getUsername() + " (" + user.getFullName() + ") was reactivated.",
                "Account", "USERS", user.getUsername()));
        logAudit(actor, "REACTIVATE_USER", "USER", user.getUsername(), "Reactivated account");
        return true;
    }

    /**
     * Soft-deletes an account.
     *
     * <p>The account is marked deleted and inactive, hidden from every user list, and
     * can no longer log in.  The user's listings are set to REMOVED and disappear from
     * the marketplace, while deals, chats and ratings are retained so history and the
     * audit trail stay intact.  Active deals involving the account are cancelled and
     * the counterparties are notified.</p>
     */
    public synchronized boolean deleteUser(String username, String actor) {
        User user = findUser(username);
        if (user == null || user.isDeleted() || user.getRole() == UserRole.ADMIN) return false;

        LocalDateTime now = LocalDateTime.now();
        user.setDeleted(true);
        user.setActive(false);
        user.setDeletedAt(now);
        user.setDeletedBy(actor);
        SessionManager.logoutIf(user.getUsername());

        for (Listing listing : listings.values()) {
            if (listing.getSellerUsername() != null
                    && listing.getSellerUsername().equalsIgnoreCase(user.getUsername())
                    && listing.getStatus() != ListingStatus.REMOVED) {
                listing.setStatus(ListingStatus.REMOVED);
                listing.setRemovalReason("Seller account deleted");
                listing.setRemovedAt(now);
                listing.setRemovedBy(actor);
            }
        }

        activity.cancelUnavailable(user);

        addNotification(new Notification(actor, "User Deleted",
                "@" + user.getUsername() + " (" + user.getFullName() + ") was soft-deleted. "
                        + "Listings were removed; deals and ratings were retained for audit.",
                "Account", "USERS", user.getUsername()));
        logAudit(actor, "DELETE_USER", "USER", user.getUsername(),
                "Soft delete; role=" + user.getRole());
        return true;
    }

    // =====================================================================
    // Listings
    // =====================================================================

    public Collection<Listing> getAllListings() {
        return listings.values();
    }

    /**
     * Listings visible in the buyer marketplace: LIVE listings whose seller is still
     * active.  Deactivating a seller therefore hides their listings without deleting
     * them; reactivation makes them visible again.
     */
    public List<Listing> getLiveListings() {
        return listings.values().stream()
                .filter(l -> l.getStatus() == ListingStatus.LIVE)
                .filter(l -> {
                    User seller = findUser(l.getSellerUsername());
                    return seller != null && seller.isActive() && !seller.isDeleted();
                })
                .collect(Collectors.toList());
    }

    public List<Listing> getPendingListings() {
        return listings.values().stream()
                .filter(l -> l.getStatus() == ListingStatus.PENDING_REVIEW)
                .sorted(Comparator.comparing(Listing::getPostedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<Listing> getListingsBySeller(String username) {
        return listings.values().stream()
                .filter(l -> l.getSellerUsername() != null
                        && l.getSellerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public Listing findListing(String id) {
        return id == null ? null : listings.get(id);
    }

    public synchronized void addListing(Listing listing) {
        if (listing.getId() == null) {
            listing.setId("L" + listingIdCounter.getAndIncrement());
        }
        listings.put(listing.getId(), listing);
    }

    public synchronized void updateListing(Listing listing) {
        listings.put(listing.getId(), listing);
    }

    /** Hard removal, used by seller-side demo screens.  Admin take-down uses REMOVED. */
    public synchronized void removeListing(String id) {
        listings.remove(id);
    }

    /** Approves a pending listing and publishes it to the marketplace. */
    public synchronized boolean approveListing(String listingId, String actor) {
        Listing listing = findListing(listingId);
        if (listing == null || listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            return false;
        }
        listing.setStatus(ListingStatus.LIVE);
        listing.setReviewedAt(LocalDateTime.now());
        listing.setRejectionReason(null);
        addNotification(new Notification(listing.getSellerUsername(), "Listing Approved",
                "Your listing '" + listing.getTitle() + "' is now live in the marketplace.",
                "System", "LISTINGS", listing.getId()));
        if (catalog.isPublic(listing)) catalog.notifyFollowers(listing);
        logAudit(actor, "APPROVE_LISTING", "LISTING", listing.getId(), listing.getTitle());
        return true;
    }

    /** Rejects a pending listing.  The reason is stored and sent to the seller. */
    public synchronized boolean rejectListing(String listingId, String reason, String actor) {
        if (reason == null || reason.isBlank()) return false;
        Listing listing = findListing(listingId);
        if (listing == null || listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            return false;
        }
        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason.trim());
        listing.setReviewedAt(LocalDateTime.now());
        addNotification(new Notification(listing.getSellerUsername(), "Listing Rejected",
                "Your listing '" + listing.getTitle() + "' was rejected. Reason: " + reason.trim(),
                "System", "LISTINGS", listing.getId()));
        logAudit(actor, "REJECT_LISTING", "LISTING", listing.getId(),
                reason.trim());
        return true;
    }

    /**
     * Takes a live listing down.
     *
     * @return the number of active offers cancelled, or -1 when the listing is missing
     *         or no longer live.
     */
    public synchronized int takeDownListing(String listingId, String reason, String actor) {
        if (reason == null || reason.isBlank()) return -1;
        Listing listing = findListing(listingId);
        if (listing == null || listing.getStatus() != ListingStatus.LIVE) return -1;

        LocalDateTime now = LocalDateTime.now();
        listing.setStatus(ListingStatus.REMOVED);
        listing.setRemovalReason(reason.trim());
        listing.setRemovedAt(now);
        listing.setRemovedBy(actor);
        listing.setReviewedAt(now);

        addNotification(new Notification(listing.getSellerUsername(), "Listing Removed",
                "Your listing '" + listing.getTitle() + "' was removed by an administrator. "
                        + "Reason: " + reason.trim(),
                "System", "LISTINGS", listing.getId()));

        int cancelled = 0;
        Set<String> handledDealIds = new HashSet<>();

        // Cancel every active request for this listing and its linked deal.
        for (Request request : requests) {
            if (!listing.getId().equals(request.getListingId()) || !request.isActive()) continue;

            request.setStatus("Cancelled");
            request.setRespondedAt(now);
            cancelled++;

            Deal linkedDeal = findDealForRequest(request.getId());
            if (linkedDeal != null && linkedDeal.isActiveOffer()) {
                linkedDeal.setStatus("Cancelled");
                linkedDeal.setCancelledAt(now);
                handledDealIds.add(linkedDeal.getId());
            }
            addNotification(new Notification(request.getBuyerUsername(), "Offer Cancelled",
                    "The listing '" + listing.getTitle() + "' was removed by an administrator, "
                            + "so your request was cancelled. Reason: " + reason.trim(),
                    "Offer", linkedDeal == null ? null : "DEALS",
                    linkedDeal == null ? null : linkedDeal.getId()));
        }

        // Defensive fallback for an active deal that has no active Request record.
        for (Deal deal : deals) {
            if (!listing.getId().equals(deal.getListingId()) || !deal.isActiveOffer()) continue;
            if (handledDealIds.contains(deal.getId())) continue;

            deal.setStatus("Cancelled");
            deal.setCancelledAt(now);
            cancelled++;
            addNotification(new Notification(deal.getBuyerUsername(), "Offer Cancelled",
                    "The listing '" + listing.getTitle() + "' was removed by an administrator, "
                            + "so your offer was cancelled. Reason: " + reason.trim(),
                    "Offer", "DEALS", deal.getId()));
        }

        logAudit(actor, "TAKE_DOWN_LISTING", "LISTING", listing.getId(),
                reason.trim() + " (cancelled " + cancelled + " active offer(s))");
        return cancelled;
    }

    // =====================================================================
    // Requests
    // =====================================================================

    public List<Request> getAllRequests() {
        return requests.stream()
                .sorted(Comparator.comparing(Request::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<Request> getRequestsForBuyer(String username) {
        return requests.stream()
                .filter(r -> r.getBuyerUsername() != null
                        && r.getBuyerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<Request> getRequestsForSeller(String username) {
        return requests.stream()
                .filter(r -> r.getSellerUsername() != null
                        && r.getSellerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<Request> getRequestsForListing(String listingId) {
        return requests.stream()
                .filter(r -> listingId != null && listingId.equals(r.getListingId()))
                .collect(Collectors.toList());
    }

    public List<Request> getActiveRequestsForListing(String listingId) {
        return requests.stream()
                .filter(r -> listingId != null && listingId.equals(r.getListingId()))
                .filter(Request::isActive)
                .collect(Collectors.toList());
    }

    public Request findRequest(String id) {
        if (id == null) return null;
        return requests.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
    }

    public void addRequest(Request request) {
        if (request.getId() == null) {
            request.setId("RQ" + requestIdCounter.getAndIncrement());
        }
        requests.add(request);
    }

    /** The 0..1 Deal created from a Request, if any. */
    public Deal findDealForRequest(String requestId) {
        if (requestId == null) return null;
        return deals.stream()
                .filter(d -> requestId.equals(d.getRequestId()))
                .findFirst()
                .orElse(null);
    }

    // =====================================================================
    // Chats
    // =====================================================================

    public List<Chat> getAllChats() {
        return new ArrayList<>(chats);
    }

    public Chat findChat(String id) {
        if (id == null) return null;
        return chats.stream().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
    }

    public List<Chat> getChatsForListing(String listingId) {
        return chats.stream()
                .filter(c -> listingId != null && listingId.equals(c.getListingId()))
                .collect(Collectors.toList());
    }

    public List<Chat> getChatsForBuyer(String username) {
        return chats.stream()
                .filter(c -> c.getBuyerUsername() != null
                        && c.getBuyerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public List<Chat> getChatsForSeller(String username) {
        return chats.stream()
                .filter(c -> c.getSellerUsername() != null
                        && c.getSellerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public void addChat(Chat chat) {
        if (chat.getId() == null) {
            chat.setId("C" + chatIdCounter.getAndIncrement());
        }
        chats.add(chat);
    }

    // =====================================================================
    // Deals
    // =====================================================================

    public List<Deal> getDealsForBuyer(String username) {
        return deals.stream()
                .filter(d -> d.getBuyerUsername() != null
                        && d.getBuyerUsername().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Deal::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<Deal> getDealsForSeller(String username) {
        return deals.stream()
                .filter(d -> d.getSellerUsername() != null
                        && d.getSellerUsername().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Deal::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<Deal> getDealsForListing(String listingId) {
        return deals.stream()
                .filter(d -> listingId != null && listingId.equals(d.getListingId()))
                .collect(Collectors.toList());
    }

    public List<Deal> getAllDeals() {
        return deals.stream()
                .sorted(Comparator.comparing(Deal::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public Deal findDeal(String id) {
        if (id == null) return null;
        return deals.stream().filter(d -> id.equals(d.getId())).findFirst().orElse(null);
    }

    public void addDeal(Deal deal) {
        if (deal.getId() == null) {
            deal.setId("D" + dealIdCounter.getAndIncrement());
        }
        deals.add(deal);
    }

    // =====================================================================
    // Notifications
    // =====================================================================

    public List<Notification> getNotificationsFor(String username) {
        return notifications.stream()
                .filter(n -> n.getUsername() != null
                        && n.getUsername().equalsIgnoreCase(username))
                .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public void addNotification(Notification n) {
        if (n.getId() == null) {
            n.setId("N" + notifIdCounter.getAndIncrement());
        }
        notifications.add(n);
    }

    public int getUnreadCount(String username) {
        return (int) notifications.stream()
                .filter(n -> n.getUsername() != null
                        && n.getUsername().equalsIgnoreCase(username))
                .filter(n -> !n.isRead())
                .count();
    }

    public void markNotificationRead(String id) {
        if (id == null) return;
        notifications.stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .ifPresent(n -> n.setRead(true));
    }

    public void markAllNotificationsRead(String username) {
        notifications.stream()
                .filter(n -> n.getUsername() != null
                        && n.getUsername().equalsIgnoreCase(username))
                .forEach(n -> n.setRead(true));
    }

    // =====================================================================
    // Ratings
    // =====================================================================

    /** Public ratings only (hidden ratings are excluded). */
    public List<Rating> getRatingsForSeller(String username) {
        return ratings.stream()
                .filter(r -> r.getSellerUsername() != null
                        && r.getSellerUsername().equalsIgnoreCase(username)
                        && !r.isHidden())
                .collect(Collectors.toList());
    }

    /** Every rating including hidden ones; used by the admin moderation screen. */
    public List<Rating> getAllRatings() {
        return ratings.stream()
                .sorted(Comparator.comparing(Rating::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public List<Rating> getAllRatingsForSeller(String username) {
        return ratings.stream()
                .filter(r -> r.getSellerUsername() != null
                        && r.getSellerUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public Rating findRating(String id) {
        if (id == null) return null;
        return ratings.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
    }

    /** Ratings are mutable in-memory objects; this keeps the API explicit. */
    public void updateRating(Rating rating) {
        // The list already holds the same object reference.
    }

    public int getPublicRatingCount(String sellerUsername) {
        return getRatingsForSeller(sellerUsername).size();
    }

    /** Average of visible (non-hidden) ratings, or 0 when there are none. */
    public double getPublicAverageRating(String sellerUsername) {
        List<Rating> visible = getRatingsForSeller(sellerUsername);
        if (visible.isEmpty()) return 0.0;
        return visible.stream().mapToInt(Rating::getRating).average().orElse(0.0);
    }

    /** Recomputes the seller's public average after a rating is added or hidden. */
    public void recalculateSellerRating(String sellerUsername) {
        User seller = findUser(sellerUsername);
        if (seller == null) return;
        List<Rating> visible = getRatingsForSeller(sellerUsername);
        seller.setRatingCount(visible.size());
        seller.setAverageRating(visible.isEmpty()
                ? 0.0
                : visible.stream().mapToInt(Rating::getRating).average().orElse(0.0));
    }

    public void addRating(Rating r) {
        if (r.getId() == null) {
            r.setId("R" + ratingIdCounter.getAndIncrement());
        }
        ratings.add(r);
        recalculateSellerRating(r.getSellerUsername());
    }

    // =====================================================================
    // Audit log
    // =====================================================================

    public void logAudit(String actorUsername, String action, String targetType,
                         String targetId, String details) {
        auditLog.add(new AuditLogEntry(actorUsername, action, targetType, targetId, details));
    }

    public List<AuditLogEntry> getAuditLog() {
        List<AuditLogEntry> copy = new ArrayList<>(auditLog);
        copy.sort(Comparator.comparing(AuditLogEntry::getTimestamp).reversed());
        return copy;
    }

    // =====================================================================
    // Validation
    // =====================================================================

    /**
     * Password strength rule used by registration, admin "Add User", and the admin
     * profile screen: at least 8 characters with at least one uppercase letter, one
     * lowercase letter and one digit.
     *
     * @return {@code "OK"} when valid, otherwise a message listing every failed rule.
     */
    public static String checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty.";
        }
        List<String> problems = new ArrayList<>();
        if (password.length() < 8) {
            problems.add("at least 8 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            problems.add("an uppercase letter");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            problems.add("a lowercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            problems.add("a digit");
        }
        if (problems.isEmpty()) {
            return "OK";
        }
        return "Password must contain " + String.join(", ", problems) + ".";
    }
}
