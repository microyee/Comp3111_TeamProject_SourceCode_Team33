package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.hkust.marketplace.utils.MarketplaceAccounts.*;

public final class MarketplaceActivity {
    public enum Decision { ACCEPT, DECLINE, COUNTER }
    public record Terms(Double price, LocalDateTime start, LocalDateTime end) {
        public static Terms sale(double price) { return new Terms(price, null, null); }
        public static Terms loan(LocalDateTime start, LocalDateTime end) { return new Terms(null, start, end); }
        public static Terms giveaway() { return new Terms(null, null, null); }

        public String display(ListingType type) {
            return switch (type) {
                case SELL -> "HK$ " + price;
                case LEND -> start + " to " + end;
                case GIVEAWAY -> "Giveaway claim";
            };
        }
    }

    private final DummyDataService data;
    private final MarketplaceAccounts accounts;
    private final Supplier<LocalDateTime> time;

    MarketplaceActivity(DummyDataService data, Supplier<LocalDateTime> time) {
        this.data = data;
        accounts = new MarketplaceAccounts(data);
        this.time = time;
    }

    public Request submit(User buyer, String listingId, Terms terms) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            LocalDateTime now = time.get();
            Listing listing = data.findListing(listingId);
            require(listing != null && data.catalog().isPublic(listing), "This listing is unavailable.");
            require(data.getActiveRequestsForListing(listingId).stream()
                    .noneMatch(r -> buyer.getUsername().equals(r.getBuyerUsername())), "You already have an active request.");
            validateTerms(listing, terms, now);

            Request request = new Request();
            request.setListingId(listingId);
            request.setListingTitle(listing.getTitle());
            request.setBuyerUsername(buyer.getUsername());
            request.setSellerUsername(listing.getSellerUsername());
            request.setType(listing.getType());
            request.setCreatedAt(now);
            setTerms(request, terms);
            data.addRequest(request);
            if (listing.getType() == ListingType.GIVEAWAY) listing.setStatus(ListingStatus.RESERVED);
            record(request, buyer, "Request submitted", terms);
            return request;
        }
    }

    public void respond(User actor, String requestId, Decision decision, Terms counter) {
        synchronized (data) {
            UserRole role = actor == null ? null : actor.getRole();
            require(role == UserRole.BUYER || role == UserRole.SELLER, "Use a buyer or seller account.");
            accounts.requireAccount(actor, role);
            Request request = data.findRequest(requestId);
            boolean seller = role == UserRole.SELLER;
            require(request != null && actor.getUsername().equals(seller ? request.getSellerUsername()
                    : request.getBuyerUsername()), "Request unavailable.");
            require((seller ? "Pending" : "Countered").equals(request.getStatus()), "Request no longer awaits your response.");

            Listing listing = data.findListing(request.getListingId());
            LocalDateTime now = time.get();
            switch (decision) {
                case DECLINE -> {
                    closeRequest(request, "Declined", now);
                    releaseClaim(listing, request);
                    record(request, actor, "Request declined", null);
                }
                case COUNTER -> {
                    require(seller && request.getType() != ListingType.GIVEAWAY, "You cannot counter this request.");
                    requireAvailable(listing, request);
                    validateTerms(listing, counter, now);
                    request.setCounterPrice(counter.price());
                    request.setCounterBorrowStartAt(counter.start());
                    request.setCounterBorrowEndAt(counter.end());
                    request.setStatus("Countered");
                    request.setRespondedAt(now);
                    record(request, actor, "Counteroffer sent", counter);
                }
                case ACCEPT -> {
                    requireAvailable(listing, request);
                    Terms agreed = seller ? proposed(request) : counterTerms(request);
                    validateTerms(listing, agreed, now);
                    accept(request, listing, agreed, now);
                    record(request, actor, "Request accepted", agreed);
                }
            }
        }
    }

    public void withdraw(User buyer, String requestId) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Request request = data.findRequest(requestId);
            require(request != null && buyer.getUsername().equals(request.getBuyerUsername()) && request.isActive(),
                    "This request can no longer be withdrawn.");
            closeRequest(request, "Cancelled", time.get());
            releaseClaim(data.findListing(request.getListingId()), request);
            record(request, buyer, "Request withdrawn", null);
        }
    }

    private void requireAvailable(Listing listing, Request request) {
        ListingStatus expected = request.getType() == ListingType.GIVEAWAY ? ListingStatus.RESERVED : ListingStatus.LIVE;
        require(listing != null && listing.getStatus() == expected && listing.getType() == request.getType()
                        && request.getSellerUsername().equals(listing.getSellerUsername())
                        && available(data.findUser(request.getBuyerUsername()))
                        && available(data.findUser(request.getSellerUsername()))
                        && data.getDealsForListing(listing.getId()).stream().noneMatch(d -> "Accepted".equals(d.getStatus())),
                "The listing or a participant is unavailable.");
    }

    private static void validateTerms(Listing listing, Terms terms, LocalDateTime now) {
        require(terms != null, "Enter the request terms.");
        switch (listing.getType()) {
            case SELL -> require(validPrice(terms.price()) && terms.start() == null && terms.end() == null,
                    "Enter a positive HKD amount with up to two decimal places.");
            case LEND -> {
                require(terms.price() == null && terms.start() != null && terms.end() != null
                                && !terms.start().isBefore(now), "Choose a start at or after the current time and an end time.");
                Duration duration = Duration.between(terms.start(), terms.end());
                require(duration.compareTo(Duration.ofSeconds(5)) >= 0 && listing.getLendingDays() != null
                                && duration.compareTo(Duration.ofDays(listing.getLendingDays())) <= 0,
                        "The borrowing period must be at least 5 seconds and within the listing duration.");
            }
            case GIVEAWAY -> require(terms.price() == null && terms.start() == null && terms.end() == null,
                    "A giveaway claim has no price or borrowing period.");
        }
    }

    static boolean validPrice(Double money) {
        return money != null && Double.isFinite(money) && money > 0
                && java.math.BigDecimal.valueOf(money).stripTrailingZeros().scale() <= 2;
    }

    private void accept(Request request, Listing listing, Terms agreed, LocalDateTime now) {
        setTerms(request, agreed);
        request.setStatus("Accepted");
        request.setRespondedAt(now);
        Deal deal = data.findDealForRequest(request.getId());
        if (deal == null) deal = data.createDeal(request, "Accepted", null);
        deal.setStatus("Accepted");
        deal.setCategory(listing.getCategory());
        deal.setOfferPrice(agreed.price());
        deal.setBorrowStartAt(agreed.start());
        deal.setBorrowEndAt(agreed.end());
        listing.setStatus(ListingStatus.RESERVED);
        if (request.getType() == ListingType.LEND && !agreed.start().isAfter(now)) startLoan(deal, listing);
        for (Request other : data.getActiveRequestsForListing(listing.getId())) {
            closeRequest(other, "Declined", now);
            record(other, data.findUser(other.getSellerUsername()), "Request closed because another request was accepted", null);
        }
    }

    private static void setTerms(Request request, Terms terms) {
        request.setOfferPrice(terms.price());
        request.setBorrowStartAt(terms.start());
        request.setBorrowEndAt(terms.end());
    }

    public static Terms proposed(Request request) {
        return new Terms(request.getOfferPrice(), request.getBorrowStartAt(), request.getBorrowEndAt());
    }

    public static Terms counterTerms(Request request) {
        return new Terms(request.getCounterPrice(), request.getCounterBorrowStartAt(), request.getCounterBorrowEndAt());
    }

    private void closeRequest(Request request, String status, LocalDateTime now) {
        request.setStatus(status);
        request.setRespondedAt(now);
        Deal legacy = data.findDealForRequest(request.getId());
        if (legacy != null && "Pending".equals(legacy.getStatus())) {
            legacy.setStatus("Cancelled");
            legacy.setCancelledAt(now);
        }
    }

    private void releaseClaim(Listing listing, Request request) {
        if (listing != null && request.getType() == ListingType.GIVEAWAY && listing.getStatus() == ListingStatus.RESERVED
                && data.getActiveRequestsForListing(listing.getId()).isEmpty()
                && data.getDealsForListing(listing.getId()).stream().noneMatch(d -> "Accepted".equals(d.getStatus()))) {
            listing.setStatus(ListingStatus.LIVE);
        }
    }

    void cancelUnavailable(User user) {
        LocalDateTime now = time.get();
        advanceLoans();
        for (Request request : data.getAllRequests()) {
            if (!request.isActive() || !involves(user, request.getBuyerUsername(), request.getSellerUsername())) continue;
            closeRequest(request, "Cancelled", now);
            releaseClaim(data.findListing(request.getListingId()), request);
            record(request, user, "Request cancelled because an account became unavailable", null);
        }
        for (Deal deal : data.getAllDeals()) {
            if (!deal.isActiveOffer() || deal.isLoanStarted()
                    || !involves(user, deal.getBuyerUsername(), deal.getSellerUsername())) continue;
            boolean reserved = "Accepted".equals(deal.getStatus());
            deal.setStatus("Cancelled");
            deal.setCancelledAt(now);
            Request request = data.findRequest(deal.getRequestId());
            if (request != null) closeRequest(request, "Cancelled", now);
            Listing listing = data.findListing(deal.getListingId());
            if (reserved && listing != null && listing.getStatus() == ListingStatus.RESERVED) listing.setStatus(ListingStatus.LIVE);
            String other = user.getUsername().equals(deal.getBuyerUsername()) ? deal.getSellerUsername() : deal.getBuyerUsername();
            notify(other, "Deal cancelled", deal.getListingTitle() + ": a participant's account is unavailable.",
                    "Deal", "DEALS", deal.getId());
        }
    }

    private static boolean involves(User user, String buyer, String seller) {
        return user.getUsername().equalsIgnoreCase(buyer) || user.getUsername().equalsIgnoreCase(seller);
    }

    public void finish(User actor, String dealId) {
        synchronized (data) {
            UserRole role = actor == null ? null : actor.getRole();
            require(role == UserRole.BUYER || role == UserRole.SELLER, "Use a buyer or seller account.");
            accounts.requireAccount(actor, role);
            Deal deal = data.findDeal(dealId);
            boolean buyer = role == UserRole.BUYER;
            require(deal != null && actor.getUsername().equals(buyer ? deal.getBuyerUsername() : deal.getSellerUsername()),
                    "Deal unavailable.");
            advanceLoans();
            require("Accepted".equals(deal.getStatus()), "This deal is no longer active.");
            if (!buyer) require(deal.getDealType() == ListingType.SELL, "Only a sale can be marked sold.");
            if (deal.getDealType() == ListingType.LEND) require(deal.isLoanStarted(), "The loan has not started.");
            String outcome = deal.getDealType() == ListingType.LEND ? "Returned" : "Completed";
            complete(deal, outcome, time.get());
        }
    }

    private void complete(Deal deal, String outcome, LocalDateTime now) {
        deal.setStatus(outcome);
        deal.setCompletedAt(now);
        Listing listing = data.findListing(deal.getListingId());
        if (listing != null && (listing.getStatus() == ListingStatus.RESERVED || listing.getStatus() == ListingStatus.LENT)) {
            listing.setStatus(switch (deal.getDealType()) {
                case SELL -> ListingStatus.SOLD;
                case LEND -> ListingStatus.LIVE;
                case GIVEAWAY -> ListingStatus.GIVEN;
            });
        }
        for (String username : List.of(deal.getBuyerUsername(), deal.getSellerUsername())) {
            notify(username, outcome, deal.getListingTitle(), "Deal", "DEALS", deal.getId());
        }
    }

    /** Advances scheduled loans on the FX thread, including while another portal is open. */
    public boolean advanceLoans() {
        synchronized (data) {
            boolean changed = false;
            LocalDateTime now = time.get();
            for (Deal deal : data.getAllDeals()) {
                if (deal.getDealType() != ListingType.LEND || !"Accepted".equals(deal.getStatus())
                        || deal.getBorrowStartAt() == null || deal.getBorrowEndAt() == null) continue;
                LocalDateTime end = deal.getBorrowEndAt();
                if (!now.isBefore(end)) {
                    complete(deal, "Auto-returned", end);
                    changed = true;
                } else if (!now.isBefore(deal.getBorrowStartAt())) {
                    if (!deal.isLoanStarted()) {
                        startLoan(deal, data.findListing(deal.getListingId()));
                        changed = true;
                    }
                    Duration loan = Duration.between(deal.getBorrowStartAt(), end);
                    Duration reminder = loan.dividedBy(2).compareTo(Duration.ofDays(1)) < 0
                            ? loan.dividedBy(2) : Duration.ofDays(1);
                    if (!deal.isReminderSent() && !now.isBefore(end.minus(reminder))) {
                        deal.setReminderSent(true);
                        notify(deal.getBuyerUsername(), "Borrowing due soon", deal.getListingTitle() + " is due at " + end,
                                "Reminder", "DEALS", deal.getId());
                        changed = true;
                    }
                }
            }
            return changed;
        }
    }

    private void startLoan(Deal deal, Listing listing) {
        deal.setLoanStarted(true);
        if (listing != null && listing.getStatus() == ListingStatus.RESERVED) listing.setStatus(ListingStatus.LENT);
    }

    public Chat openChat(User buyer, String listingId) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            for (Chat chat : data.getChatsForListing(listingId)) {
                if (buyer.getUsername().equals(chat.getBuyerUsername())) return chat;
            }
            Listing listing = data.findListing(listingId);
            require(listing != null && (data.catalog().isPublic(listing)
                    || data.getRequestsForBuyer(buyer.getUsername()).stream().anyMatch(r -> listingId.equals(r.getListingId()))
                    || data.getDealsForBuyer(buyer.getUsername()).stream().anyMatch(d -> listingId.equals(d.getListingId()))),
                    "This listing is unavailable.");
            return chatFor(listingId, listing.getTitle(), buyer.getUsername(), listing.getSellerUsername());
        }
    }

    public Chat chatForRequest(User actor, String requestId) {
        synchronized (data) {
            Request request = data.findRequest(requestId);
            require(request != null, "Request unavailable.");
            requireParticipant(actor, request.getBuyerUsername(), request.getSellerUsername());
            return chatFor(request.getListingId(), request.getListingTitle(), request.getBuyerUsername(), request.getSellerUsername());
        }
    }

    public List<Chat> chats(User actor) {
        synchronized (data) {
            UserRole role = actor == null ? null : actor.getRole();
            require(role == UserRole.BUYER || role == UserRole.SELLER, "Sign in as a buyer or seller.");
            accounts.requireAccount(actor, role);
            return role == UserRole.BUYER ? data.getChatsForBuyer(actor.getUsername()) : data.getChatsForSeller(actor.getUsername());
        }
    }

    public List<Message> messages(User actor, String chatId, String query) {
        synchronized (data) {
            Chat chat = requireChat(actor, chatId);
            String keyword = clean(query).toLowerCase(Locale.ROOT);
            return chat.getMessages().stream().filter(m -> MarketplaceCatalog.contains(m.getContent(), keyword)
                    || MarketplaceCatalog.contains(m.getSenderUsername(), keyword)).toList();
        }
    }

    public void send(User actor, String chatId, String text) {
        synchronized (data) {
            Chat chat = requireChat(actor, chatId);
            String recipient = actor.getUsername().equals(chat.getBuyerUsername()) ? chat.getSellerUsername() : chat.getBuyerUsername();
            require(available(data.findUser(recipient)), "The other participant is unavailable. You can still view the chat.");
            String message = requiredText(text, "Message", 2000);
            data.addMessage(chat, actor.getUsername(), message, time.get());
            notify(recipient, "New message from @" + actor.getUsername(), chat.getListingTitle(), "Chat", "CHAT", chatId);
        }
    }

    private Chat requireChat(User actor, String chatId) {
        Chat chat = data.findChat(chatId);
        require(chat != null, "Chat unavailable.");
        requireParticipant(actor, chat.getBuyerUsername(), chat.getSellerUsername());
        return chat;
    }

    private void requireParticipant(User actor, String buyer, String seller) {
        require(actor != null && involves(actor, buyer, seller), "This conversation is private.");
        accounts.requireAccount(actor, actor.getRole());
    }

    private Chat chatFor(String listingId, String title, String buyer, String seller) {
        for (Chat chat : data.getChatsForListing(listingId)) {
            if (buyer.equals(chat.getBuyerUsername())) return chat;
        }
        Chat chat = new Chat();
        chat.setListingId(listingId);
        chat.setListingTitle(title);
        chat.setBuyerUsername(buyer);
        chat.setSellerUsername(seller);
        chat.setCreatedAt(time.get());
        data.addChat(chat);
        return chat;
    }

    private void record(Request request, User actor, String action, Terms terms) {
        Chat chat = chatFor(request.getListingId(), request.getListingTitle(), request.getBuyerUsername(), request.getSellerUsername());
        String detail = terms == null ? action : action + " · " + terms.display(request.getType());
        data.addMessage(chat, actor.getUsername(), detail, time.get());
        String other = actor.getUsername().equals(request.getBuyerUsername()) ? request.getSellerUsername() : request.getBuyerUsername();
        notify(other, action, request.getListingTitle() + " · " + detail, "Request", "REQUESTS", request.getId());
    }

    public Rating rate(User buyer, String dealId, int stars, String comment) {
        synchronized (data) {
            accounts.requireAccount(buyer, UserRole.BUYER);
            Deal deal = data.findDeal(dealId);
            require(deal != null && buyer.getUsername().equals(deal.getBuyerUsername()) && deal.isFinished(),
                    "You can review your own completed deals.");
            require(!deal.isRated() && data.getAllRatings().stream().noneMatch(r -> dealId.equals(r.getDealId())),
                    "This deal already has a review.");
            require(stars >= 1 && stars <= 5, "Choose a rating between 1 and 5.");
            Rating rating = new Rating();
            rating.setDealId(dealId);
            rating.setBuyerUsername(buyer.getUsername());
            rating.setSellerUsername(deal.getSellerUsername());
            rating.setListingTitle(deal.getListingTitle());
            rating.setRating(stars);
            rating.setComment(limitedText(comment, "Review", 1500));
            rating.setCreatedAt(time.get());
            data.addRating(rating);
            deal.setRated(true);
            notify(deal.getSellerUsername(), "New review", deal.getListingTitle() + " received " + stars + " stars.",
                    "Rating", "RATINGS", rating.getId());
            return rating;
        }
    }

    public void readNotification(User actor, String id) {
        synchronized (data) {
            accounts.requireAccount(actor, actor == null ? null : actor.getRole());
            Notification notification = data.getNotificationsFor(actor.getUsername()).stream()
                    .filter(n -> n.getId().equals(id)).findFirst().orElse(null);
            require(notification != null, "Notification unavailable.");
            notification.setRead(true);
        }
    }

    private void notify(String username, String title, String message, String category, String screen, String id) {
        data.addNotification(new Notification(username, title, message, category, screen, id));
    }
}
