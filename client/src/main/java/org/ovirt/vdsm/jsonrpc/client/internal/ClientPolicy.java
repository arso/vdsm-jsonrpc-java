package org.ovirt.vdsm.jsonrpc.client.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java bean which provide information how retry logic should work.
 *
 */
public class ClientPolicy {
    private final int retryTimeOut;
    private final int retryNumber;
    private volatile int incomingHeartbeat;
    private volatile int outgoingHeartbeat;
    private List<Class<? extends Exception>> exceptions;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private AtomicBoolean isIncomingHeartbeat = new AtomicBoolean();
    private AtomicBoolean isOutgoingHeartbeat = new AtomicBoolean();
    private String identifier;

    /**
     * Create policy using provided values.
     *
     * @param retryTimeOut
     *            - <code>Integer</code> value which is used as timeout between operation retry combined with
     *            <code>TimeUnit</code> which is set to milliseconds by default.
     * @param retryNumber
     *            - <code>Integer</code> value which defines number of retry attempts.
     * @param incomingHeartbeat
     *            - <code>Integer</code> value which defines incoming heart beat.
     * @param outgoingHeartbeat
     *            - <code>Integer</code> value which defines outgoing heart beat.
     * @param retryableExceptions
     *            - <code>List</code> of retryable exceptions.
     */
    public ClientPolicy(int retryTimeOut, int retryNumber, int incomingHeartbeat,
            int outgoingHeartbeat, List<Class<? extends Exception>> retryableExceptions) {
        this.retryNumber = retryNumber;
        this.retryTimeOut = retryTimeOut;
        setIncomingHeartbeat(incomingHeartbeat);
        setOutgoingHeartbeat(outgoingHeartbeat);
        this.exceptions = Collections.unmodifiableList(retryableExceptions);
    }

    public ClientPolicy(int retryTimeOut, int retryNumber, int incomingHeartbeat) {
        this(retryTimeOut, retryNumber, incomingHeartbeat, 0, new ArrayList<Class<? extends Exception>>());
    }

    public ClientPolicy(int retryTimeOut, int retryNumber, int incomingHeartbeat, int outgoingHeartbeat) {
        this(retryTimeOut,
                retryNumber,
                incomingHeartbeat,
                outgoingHeartbeat,
                new ArrayList<Class<? extends Exception>>());
    }

    public ClientPolicy(int retryTimeOut,
            int retryNumber,
            int incomingHeartbeat,
            Class<? extends Exception> retryableException) {
        this(retryTimeOut,
                retryNumber,
                incomingHeartbeat,
                0,
                new ArrayList<Class<? extends Exception>>(Arrays.asList(retryableException)));
    }

    public ClientPolicy(int retryTimeOut,
            int retryNumber,
            int incomingHeartbeat,
            int outgoingHeartbeat,
            Class<? extends Exception> retryableException) {
        this(retryTimeOut,
                retryNumber,
                incomingHeartbeat,
                outgoingHeartbeat,
                new ArrayList<Class<? extends Exception>>(Arrays.asList(retryableException)));
    }

    public int getRetryTimeOut() {
        return this.retryTimeOut;
    }

    public int getRetryNumber() {
        return this.retryNumber;
    }

    public int getIncomingHeartbeat() {
        return this.incomingHeartbeat;
    }

    public int getOutgoingHeartbeat() {
        return this.outgoingHeartbeat;
    }

    public final void setOutgoingHeartbeat(int outgoingHeartbeat) {
        this.outgoingHeartbeat = outgoingHeartbeat;
        if (outgoingHeartbeat != 0) {
            this.isOutgoingHeartbeat.set(true);
        } else {
            this.isOutgoingHeartbeat.set(false);
        }
    }

    public final void setIncomingHeartbeat(int incomingHeartbeat) {
        this.incomingHeartbeat = incomingHeartbeat;
        if (incomingHeartbeat != 0) {
            this.isIncomingHeartbeat.set(true);
        } else {
            this.isIncomingHeartbeat.set(false);
        }
    }

    public List<Class<? extends Exception>> getExceptions() {
        return this.exceptions;
    }

    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier){
        this.identifier = identifier;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public boolean isIncomingHeartbeat() {
        return this.isIncomingHeartbeat.get();
    }

    public void setIncomingHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat && this.incomingHeartbeat != 0) {
            this.isIncomingHeartbeat.set(isHeartbeat);
            return;
        }
        this.isIncomingHeartbeat.set(false);
    }

    public boolean isOutgoingHeartbeat() {
        return this.isOutgoingHeartbeat.get();
    }

    public void setOutgoingHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat && this.outgoingHeartbeat != 0) {
            this.isOutgoingHeartbeat.set(isHeartbeat);
            return;
        }
        this.isOutgoingHeartbeat.set(false);
    }

    @Override
    public ClientPolicy clone() {
        return new ClientPolicy(this.retryTimeOut, this.retryNumber, this.incomingHeartbeat, this.outgoingHeartbeat,
                this.exceptions);
    }
}
