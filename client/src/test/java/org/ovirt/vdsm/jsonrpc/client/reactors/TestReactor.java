package org.ovirt.vdsm.jsonrpc.client.reactors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.ovirt.vdsm.jsonrpc.client.internal.ClientPolicy;

// This class is heavily time dependent so there is
// good number of timeouts. It is ignored due to time
// needed to run it.
@Ignore
public class TestReactor {

    private static final int TIMEOUT_SEC = 6;
    private static final String HOSTNAME = "127.0.0.1";
    private static final String DATA = "Hello World!";
    private Reactor reactorForListener;
    private Reactor reactorForClient;

    @Before
    public void setUp() throws Exception {
        this.reactorForListener = ReactorFactory.getReactor(null, ReactorType.STOMP);
        this.reactorForClient = ReactorFactory.getReactor(null, ReactorType.STOMP);
    }

    @After
    public void tearDown() {
        this.reactorForListener.close();
        this.reactorForClient.close();
    }

    @Test
    public void testConnectionBetweenListenerAndClient() throws InterruptedException,
            ExecutionException, TimeoutException,
            ClientConnectionException {
        final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        final Future<ReactorListener> futureListener = this.reactorForListener.createListener(HOSTNAME, 6669,
                client -> client.addEventListener(message -> {
                    try {
                        client.sendMessage(message);
                    } catch (ClientConnectionException e) {
                        fail();
                    }
                }));

        ReactorListener listener = futureListener.get(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertNotNull(listener);
        assertTrue(futureListener.isDone());

        ReactorClient client = this.reactorForClient.createClient(HOSTNAME, 6669);
        assertNotNull(client);

        client.addEventListener(queue::add);

        final ByteBuffer buff = ByteBuffer.allocate(DATA.length());
        buff.put(DATA.getBytes());
        buff.position(0);
        client.connect();
        client.sendMessage(buff.array());
        byte[] message = queue.poll(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertNotNull(message);
        assertArrayEquals(buff.array(), message);

        client.close();
        listener.close();
    }

    @Test
    public void testRetryConnectionBetweenListenerAndClient()
            throws InterruptedException, ExecutionException, ClientConnectionException {
        final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        final ExecutorService executorService = Executors.newCachedThreadPool();

        final Callable<ReactorClient> clientTask = () -> {
            ReactorClient client = reactorForClient.createClient(HOSTNAME, 6668);

            client.addEventListener(queue::add);
            client.setClientPolicy(new ClientPolicy(2000, 10, 10000, IOException.class));
            client.connect();
            return client;
        };

        final Callable<ReactorListener> listenerTask = () -> {
            final Future<ReactorListener> futureListener = reactorForListener.createListener(HOSTNAME, 6668,
                    client -> client.addEventListener(message -> {
                        try {
                            client.sendMessage(message);
                        } catch (ClientConnectionException e) {
                            fail();
                        }
                    }));

            return futureListener.get(TIMEOUT_SEC, TimeUnit.SECONDS);
        };
        Future<ReactorClient> clientFuture = executorService.submit(clientTask);
        Thread.sleep(2000);
        Future<ReactorListener> listenerFuture = executorService.submit(listenerTask);

        ReactorListener listener = listenerFuture.get();
        assertTrue(listenerFuture.isDone());
        assertNotNull(listener);

        ReactorClient client = clientFuture.get();
        assertTrue(clientFuture.isDone());
        assertNotNull(client);

        final ByteBuffer buff = ByteBuffer.allocate(DATA.length());
        buff.put(DATA.getBytes());
        buff.position(0);
        client.sendMessage(buff.array());
        byte[] message = queue.poll(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertNotNull(message);
        assertArrayEquals(buff.array(), message);

        client.close();
        listener.close();
    }

    @Test
    public void testNotConnectedRetry() throws InterruptedException, TimeoutException, ClientConnectionException,
            ExecutionException {
        final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
        Future<ReactorListener> futureListener = this.reactorForListener.createListener(HOSTNAME, 6667,
                client -> client.addEventListener(message -> {
                    try {
                        client.sendMessage(message);
                    } catch (ClientConnectionException e) {
                        fail();
                    }
                }));

        ReactorListener listener = futureListener.get(TIMEOUT_SEC, TimeUnit.SECONDS);
        assertNotNull(listener);
        assertTrue(futureListener.isDone());

        ReactorClient client = this.reactorForClient.createClient(HOSTNAME, 6667);
        assertNotNull(client);

        client.addEventListener(queue::add);
        client.connect();
        listener.close();

        futureListener = this.reactorForListener.createListener(HOSTNAME, 6667,
                _client -> _client.addEventListener(message -> {
                    try {
                        _client.sendMessage(message);
                    } catch (ClientConnectionException e) {
                        fail();
                    }
                }));

        listener = futureListener.get(TIMEOUT_SEC, TimeUnit.SECONDS);

        final ByteBuffer buff = ByteBuffer.allocate(DATA.length());
        buff.put(DATA.getBytes());
        buff.position(0);

        client.sendMessage(buff.array());
        byte[] message = queue.poll(TIMEOUT_SEC, TimeUnit.SECONDS);

        assertNotNull(message);
        assertArrayEquals(buff.array(), message);
    }
}
