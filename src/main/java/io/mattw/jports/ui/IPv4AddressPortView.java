package io.mattw.jports.ui;

import io.mattw.jports.IPv4AddressPort;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class IPv4AddressPortView extends HBox {

    private static BrowserUtil browser = new BrowserUtil();
    private static Tooltip tooltip = new Tooltip("Open in Browser");

    private IPv4AddressPort addressPort;

    public IPv4AddressPortView(IPv4AddressPort addressPort, String checkedURL) {
        this.addressPort = addressPort;

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(4, 8, 4, 8));
        setMaxHeight(34);
        setMinHeight(34);
        setPrefHeight(34);

        Label label = new Label(addressPort.getFullAddress());
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setPrefWidth(0);
        HBox.setHgrow(label, Priority.ALWAYS);

        if (checkedURL != null) {
            ImageView iv = new ImageView();
            iv.setImage(new Image("/io/mattw/jports/ui/external-link.png"));
            iv.setFitHeight(15);
            iv.setFitWidth(15);

            Button link = new Button();
            link.setGraphic(iv);
            link.setTooltip(tooltip);
            link.setStyle("-fx-cursor: hand; -fx-base: transparent;");
            link.setMaxHeight(15);
            link.setPrefHeight(15);
            link.setOnAction(ae -> browser.open(checkedURL));
            getChildren().addAll(label, link);
        } else {
            getChildren().add(label);
        }
    }

    public IPv4AddressPort getAddressPort() {
        return addressPort;
    }
}
