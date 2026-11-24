package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Report;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.hkust.marketplace.controllers.PortalUi.*;

final class AdminReportsPane extends VBox {
    private final DummyDataService data = DummyDataService.getInstance();
    private final AdminDashboardController host;
    private final VBox rows = new VBox(10);
    private final CheckBox openOnly = new CheckBox("Open reports only");
    private final String highlight;

    AdminReportsPane(AdminDashboardController host, String highlight) {
        super(12);
        this.host = host;
        this.highlight = highlight;
        openOnly.setSelected(highlight == null);
        openOnly.setOnAction(event -> render());
        HBox toolbar = new HBox(openOnly);
        toolbar.getStyleClass().add("toolbar");
        ScrollPane list = scroll(rows);
        VBox.setVgrow(list, Priority.ALWAYS);
        getChildren().addAll(toolbar, list);
        render();
    }

    private void render() {
        rows.getChildren().clear();
        for (Report report : data.catalog().reports(SessionManager.getCurrentUser())) {
            if (openOnly.isSelected() && !report.isOpen()) continue;
            VBox item = card(label(report.subject() + " · " + (report.isOpen() ? "Open" : "Resolved"), "section-title"),
                    label(report.id() + " · " + report.target() + " · @" + report.reporter()
                            + " · " + time(report.submittedAt()), "muted"), label(report.reason(), "label"));
            if (report.id().equals(highlight)) item.getStyleClass().add("notif-unread");
            HBox actions = new HBox(8);
            if (report.target() == Report.Target.LISTING) {
                actions.getChildren().add(button("View listing", "btn-outline", () -> host.showAllListings(report.targetId())));
            } else {
                actions.getChildren().add(button("View rating", "btn-outline", () -> host.showRatings(report.targetId())));
            }
            if (report.isOpen()) {
                actions.getChildren().add(button("Resolve", "btn-primary", () -> textForm(this,
                        "Resolve report", "Resolve", "Resolution shared with the reporter", "", note -> {
                            data.catalog().resolve(SessionManager.getCurrentUser(), report.id(), note);
                            render();
                        })));
            } else {
                item.getChildren().add(label("Resolution: " + report.resolution() + " · " + time(report.resolvedAt()), "muted"));
            }
            item.getChildren().add(actions);
            rows.getChildren().add(item);
        }
        if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No matching reports.", "muted"));
    }
}
