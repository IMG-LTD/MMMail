package com.mmmail.platform.access;

public interface AccessGate {

    AccessDecision evaluate(AccessRequest request);
}
