package com.turtlemod.app;

public enum TurtleStatus {
    IDLE(0, "Idle", 0xAAAAAA),
    RUNNING(1, "Running", 0x55FF55),
    PAUSED(2, "Paused", 0xFFFF55),
    OUT_OF_FUEL(3, "Out of Fuel", 0xFF5555),
    NO_TOOL(4, "Tool Required", 0xFFAA00),
    BLOCKED(5, "Path Blocked", 0xFF3333),
    COMPLETED(6, "Program Complete", 0x55FFFF);

    private final int id;
    private final String label;
    private final int color;

    TurtleStatus(int id, String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public static TurtleStatus fromIndex(int index) {
        for (TurtleStatus s : values()) {
            if (s.id == index) return s;
        }
        return IDLE;
    }
}
