package com.mmmail.common.observability;

public interface ErrorReporter {

    void record(ObservedErrorReport report);
}
