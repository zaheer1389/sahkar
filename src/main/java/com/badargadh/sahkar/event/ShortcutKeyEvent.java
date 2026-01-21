package com.badargadh.sahkar.event;
import org.springframework.context.ApplicationEvent;

public class ShortcutKeyEvent extends ApplicationEvent {
    private final String command; // "SAVE" or "TOGGLE_FULL"

    public ShortcutKeyEvent(Object source, String command) {
        super(source);
        this.command = command;
    }

    public String getCommand() { return command; }
}