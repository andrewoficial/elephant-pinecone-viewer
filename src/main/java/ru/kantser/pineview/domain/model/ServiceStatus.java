package ru.kantser.pineview.domain.model;

public enum ServiceStatus {
    ONLINE("online", "#a6e3a1"),
    OFFLINE("offline", "#f38ba8"),
    CHECKING("checking", "#fab387");

    private final String label;
    private final String colorHex;

    ServiceStatus(String label, String colorHex) {
        this.label = label;
        this.colorHex = colorHex;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
}