package com.philippbro.springboot.hazelcast.caching;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class MemoryAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    public static final Queue<ILoggingEvent> LOG_MESSAGES = new ConcurrentLinkedQueue<>();

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        event.prepareForDeferredProcessing();
        LOG_MESSAGES.add(event);
    }
}
