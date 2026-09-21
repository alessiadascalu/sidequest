package dev.sidequest.domain;

public enum Category {
    SOCIAL("Social"),
    FITNESS_EXPLORE("Fitness / Explore"),
    FOCUS("Focus"),
    CREATIV("Creativ");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
