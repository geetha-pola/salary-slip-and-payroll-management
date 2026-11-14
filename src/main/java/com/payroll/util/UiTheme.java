package com.payroll.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Pure JavaFX API Styling Engine for Employee Payroll Management System.
 * Fully compliant with hackathon restrictions: STRICTLY NO CSS, NO HTML.
 * Uses native JavaFX Background, Border, Font, Color, and Layout APIs.
 */
public final class UiTheme {

    // Primary Brand Colors (Corporate Navy & Slate Palette)
    public static final Color COLOR_PRIMARY_NAVY = Color.rgb(15, 23, 42);      // #0f172a
    public static final Color COLOR_SIDEBAR_NAVY = Color.rgb(30, 41, 59);      // #1e293b
    public static final Color COLOR_NAV_HOVER = Color.rgb(51, 65, 85);         // #334155
    public static final Color COLOR_NAV_ACTIVE = Color.rgb(37, 99, 235);       // #2563eb
    public static final Color COLOR_BG_LIGHT = Color.rgb(241, 245, 249);       // #f1f5f9
    public static final Color COLOR_CARD_BG = Color.WHITE;
    public static final Color COLOR_BORDER = Color.rgb(226, 232, 240);          // #e2e8f0
    public static final Color COLOR_BORDER_FOCUSED = Color.rgb(37, 99, 235);    // #2563eb

    // Functional State Colors
    public static final Color COLOR_ACTION_BLUE = Color.rgb(37, 99, 235);       // #2563eb
    public static final Color COLOR_ACTION_BLUE_HOVER = Color.rgb(29, 78, 216); // #1d4ed8
    public static final Color COLOR_DANGER_RED = Color.rgb(220, 38, 38);        // #dc2626
    public static final Color COLOR_DANGER_RED_HOVER = Color.rgb(185, 28, 28);  // #b91c1c
    public static final Color COLOR_SUCCESS_GREEN = Color.rgb(22, 163, 74);     // #16a34a
    public static final Color COLOR_MUTED_GRAY = Color.rgb(100, 116, 139);      // #64748b
    public static final Color COLOR_TEXT_DARK = Color.rgb(15, 23, 42);         // #0f172a
    public static final Color COLOR_TEXT_LIGHT = Color.rgb(248, 250, 252);     // #f8fafc

    // Typography
    public static final String FONT_FAMILY = "Segoe UI";

    private UiTheme() {
        // Utility class
    }

    // --- Container Styling ---

    public static void applyCard(Region region) {
        region.setBackground(new Background(new BackgroundFill(COLOR_CARD_BG, new CornerRadii(8), Insets.EMPTY)));
        region.setBorder(new Border(new BorderStroke(
                COLOR_BORDER,
                BorderStrokeStyle.SOLID,
                new CornerRadii(8),
                new BorderWidths(1)
        )));
    }

    public static void applyStatCard(VBox card, boolean highlight) {
        Color bg = highlight ? Color.rgb(239, 246, 255) : COLOR_CARD_BG;
        Color border = highlight ? Color.rgb(191, 219, 254) : COLOR_BORDER;
        card.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(8), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
                border,
                BorderStrokeStyle.SOLID,
                new CornerRadii(8),
                new BorderWidths(highlight ? 1.5 : 1)
        )));
        card.setPadding(new Insets(16));
        card.setSpacing(6);
    }

    public static void applySidebar(VBox sidebar) {
        sidebar.setBackground(new Background(new BackgroundFill(COLOR_PRIMARY_NAVY, CornerRadii.EMPTY, Insets.EMPTY)));
        sidebar.setBorder(new Border(new BorderStroke(
                null,
                COLOR_SIDEBAR_NAVY,
                null,
                null,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.SOLID,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, 0, 0),
                Insets.EMPTY
        )));
    }

    public static void applyContentArea(StackPane pane) {
        pane.setBackground(new Background(new BackgroundFill(COLOR_BG_LIGHT, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // --- Button Styling ---

    public static void applyPrimaryButton(Button btn) {
        Background normalBg = new Background(new BackgroundFill(COLOR_ACTION_BLUE, new CornerRadii(6), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(COLOR_ACTION_BLUE_HOVER, new CornerRadii(6), Insets.EMPTY));
        btn.setBackground(normalBg);
        btn.setTextFill(COLOR_TEXT_LIGHT);
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 13));
        btn.setPadding(new Insets(9, 18, 9, 18));
        btn.setCursor(Cursor.HAND);
        btn.setBorder(Border.EMPTY);

        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setBackground(hoverBg);
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setBackground(normalBg);
        });
    }

    public static void applySecondaryButton(Button btn) {
        Background normalBg = new Background(new BackgroundFill(COLOR_CARD_BG, new CornerRadii(6), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(COLOR_BG_LIGHT, new CornerRadii(6), Insets.EMPTY));
        Border normalBorder = new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1)));
        btn.setBackground(normalBg);
        btn.setBorder(normalBorder);
        btn.setTextFill(COLOR_TEXT_DARK);
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        btn.setPadding(new Insets(7, 14, 7, 14));
        btn.setCursor(Cursor.HAND);

        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setBackground(hoverBg);
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setBackground(normalBg);
        });
    }

    public static void applyDangerButton(Button btn) {
        Background normalBg = new Background(new BackgroundFill(COLOR_DANGER_RED, new CornerRadii(6), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(COLOR_DANGER_RED_HOVER, new CornerRadii(6), Insets.EMPTY));
        btn.setBackground(normalBg);
        btn.setTextFill(COLOR_TEXT_LIGHT);
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 12));
        btn.setPadding(new Insets(7, 14, 7, 14));
        btn.setCursor(Cursor.HAND);
        btn.setBorder(Border.EMPTY);

        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setBackground(hoverBg);
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setBackground(normalBg);
        });
    }

    public static void applySidebarNavButton(Button btn, boolean isActive) {
        btn.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 13));
        btn.setPadding(new Insets(10, 14, 10, 14));
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setBorder(Border.EMPTY);

        if (btn.isDisabled()) {
            btn.setBackground(Background.EMPTY);
            btn.setTextFill(Color.rgb(148, 163, 184, 0.6));
            btn.setCursor(Cursor.DEFAULT);
            return;
        }

        btn.setCursor(Cursor.HAND);
        if (isActive) {
            btn.setBackground(new Background(new BackgroundFill(COLOR_NAV_ACTIVE, new CornerRadii(6), Insets.EMPTY)));
            btn.setTextFill(COLOR_TEXT_LIGHT);
        } else {
            btn.setBackground(Background.EMPTY);
            btn.setTextFill(Color.rgb(203, 213, 225));

            btn.setOnMouseEntered(e -> {
                btn.setBackground(new Background(new BackgroundFill(COLOR_NAV_HOVER, new CornerRadii(6), Insets.EMPTY)));
                btn.setTextFill(COLOR_TEXT_LIGHT);
            });
            btn.setOnMouseExited(e -> {
                btn.setBackground(Background.EMPTY);
                btn.setTextFill(Color.rgb(203, 213, 225));
            });
        }
    }

    // --- Form & Input Controls ---

    public static void applyInputField(TextInputControl field) {
        field.setBackground(new Background(new BackgroundFill(COLOR_CARD_BG, new CornerRadii(6), Insets.EMPTY)));
        Border normalBorder = new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1)));
        Border focusedBorder = new Border(new BorderStroke(COLOR_BORDER_FOCUSED, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5)));
        field.setBorder(normalBorder);
        field.setFont(Font.font(FONT_FAMILY, 13));
        field.setPadding(new Insets(8, 10, 8, 10));

        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            field.setBorder(isNowFocused ? focusedBorder : normalBorder);
        });
    }

    public static void applyReadOnlyField(TextInputControl field) {
        field.setEditable(false);
        field.setBackground(new Background(new BackgroundFill(COLOR_BG_LIGHT, new CornerRadii(6), Insets.EMPTY)));
        field.setBorder(new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        field.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 13));
        field.setPadding(new Insets(8, 10, 8, 10));
    }

    public static void applyComboBox(ComboBox<?> combo) {
        combo.setBackground(new Background(new BackgroundFill(COLOR_CARD_BG, new CornerRadii(6), Insets.EMPTY)));
        combo.setBorder(new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
        combo.setPadding(new Insets(2, 6, 2, 6));
    }

    public static void applyDatePicker(DatePicker picker) {
        picker.setBackground(new Background(new BackgroundFill(COLOR_CARD_BG, new CornerRadii(6), Insets.EMPTY)));
        picker.setBorder(new Border(new BorderStroke(COLOR_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1))));
    }

    // --- Label Styling ---

    public static void applyPageTitle(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 22));
        label.setTextFill(COLOR_TEXT_DARK);
    }

    public static void applyPageSubtitle(Label label) {
        label.setFont(Font.font(FONT_FAMILY, 13));
        label.setTextFill(COLOR_MUTED_GRAY);
    }

    public static void applySectionHeader(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 15));
        label.setTextFill(COLOR_PRIMARY_NAVY);
    }

    public static void applyFormLabel(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        label.setTextFill(COLOR_TEXT_DARK);
    }

    public static void applyStatTitle(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 11));
        label.setTextFill(COLOR_MUTED_GRAY);
    }

    public static void applyStatValue(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 22));
        label.setTextFill(COLOR_PRIMARY_NAVY);
    }

    public static void applyStatSubtext(Label label) {
        label.setFont(Font.font(FONT_FAMILY, 11));
        label.setTextFill(COLOR_MUTED_GRAY);
    }

    public static void applyErrorLabel(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        label.setTextFill(COLOR_DANGER_RED);
    }

    public static void applySuccessLabel(Label label) {
        label.setFont(Font.font(FONT_FAMILY, FontWeight.SEMI_BOLD, 12));
        label.setTextFill(COLOR_SUCCESS_GREEN);
    }

    public static void applyBadge(Label label, Color bg, Color text) {
        label.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(12), Insets.EMPTY)));
        label.setTextFill(text);
        label.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 11));
        label.setPadding(new Insets(4, 10, 4, 10));
    }
}

