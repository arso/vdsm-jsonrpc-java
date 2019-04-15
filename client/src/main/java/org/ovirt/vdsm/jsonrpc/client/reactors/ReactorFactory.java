package org.ovirt.vdsm.jsonrpc.client.reactors;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.ovirt.vdsm.jsonrpc.client.internal.ResponseWorker;
import org.ovirt.vdsm.jsonrpc.client.reactors.stomp.SSLStompReactor;
import org.ovirt.vdsm.jsonrpc.client.reactors.stomp.StompReactor;

/**
 * Factory class which provide single instance of <code>Reactor</code>s or <code>ResponseWorker</code> within single
 * loading scope.
 *
 */
public class ReactorFactory {
    /**
     * Default timeout to clean up unprocessed events, which are in the queue more than this timeout. The value should
     * be passed from oVirt engine 4.3+, but we need to keep default value to preserve backward compatibility with 4.2.
     */
    private static final int EVENT_TIMEOUT_IN_HOURS = 3;

    private static volatile StompReactor stompReactor;
    private static volatile SSLStompReactor sslStompReactor;
    private static volatile ResponseWorker worker;

    /**
     * Provides instance of <code>Reactor</code> based on <code>ManagerProvider</code> availability and
     * type provided.
     *
     * @param provider Provides ability to get SSL context.
     * @param type <code>ReactorType</code> which will be created.
     * @return <code>NioReactor</code> reactor when provider is <code>null</code> or <code>SSLReactor</code>.
     * @throws ClientConnectionException when unexpected type value is provided or issue with constucting selector.
     */
    public static Reactor getReactor(ManagerProvider provider, ReactorType type) throws ClientConnectionException {
        if (ReactorType.STOMP.equals(type)) {
            return getStompReactor(provider);
        } else {
            throw new ClientConnectionException("Unrecognized reactor type");
        }
    }

    /**
     * @param parallelism the parallelism level using for event processing.
     * @return Single instance of <code>ResponseWorker</code>.
     */
    public static ResponseWorker getWorker(int parallelism) {
        return getWorker(parallelism, EVENT_TIMEOUT_IN_HOURS);
    }

    /**
     * @param parallelism the parallelism level using for event processing.
     * @param eventTimeoutInHours the timeout after which the events are purged from the queue.
     * @return Single instance of <code>ResponseWorker</code>.
     */
    public static ResponseWorker getWorker(int parallelism, int eventTimeoutInHours) {
        if (worker != null) {
            return worker;
        }
        synchronized (ReactorFactory.class) {
            if (worker != null) {
                return worker;
            }
            worker = new ResponseWorker(parallelism, eventTimeoutInHours);
        }
        return worker;
    }

    private static Reactor getStompReactor(ManagerProvider provider) throws ClientConnectionException {
        if (provider != null) {
            return getSslStompReactor(provider);
        }
        if (stompReactor != null) {
            return stompReactor;
        }
        synchronized (ReactorFactory.class) {
            if (stompReactor != null) {
                return stompReactor;
            }
            try {
                stompReactor = new StompReactor();
            } catch (IOException e) {
                throw new ClientConnectionException(e);
            }
        }
        return stompReactor;
    }

    private static Reactor getSslStompReactor(ManagerProvider provider) throws ClientConnectionException {
        if (sslStompReactor != null) {
            return sslStompReactor;
        }
        synchronized (ReactorFactory.class) {
            if (sslStompReactor != null) {
                return sslStompReactor;
            }
            try {
                sslStompReactor = new SSLStompReactor(provider.getSSLContext());
            } catch (IOException | GeneralSecurityException e) {
                throw new ClientConnectionException(e);
            }
        }
        return sslStompReactor;
    }
}
