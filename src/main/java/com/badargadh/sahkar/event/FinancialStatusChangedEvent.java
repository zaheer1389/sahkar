package com.badargadh.sahkar.event;

import org.springframework.context.ApplicationEvent;

/**
 * Event triggered whenever a financial month is Opened or Closed.
 */
public class FinancialStatusChangedEvent extends ApplicationEvent {
    
    public FinancialStatusChangedEvent(Object source) {
        super(source);
    }
}