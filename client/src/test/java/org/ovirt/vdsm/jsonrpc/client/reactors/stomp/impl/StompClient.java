package org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl;

import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_ACCEPT;
import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_DESTINATION;
import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_ID;
import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_RECEIPT;
import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_RECEIPT_ID;
import static org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.HEADER_TRANSACTION;
import static org.ovirt.vdsm.jsonrpc.client.utils.JsonUtils.UTF8;
import static org.ovirt.vdsm.jsonrpc.client.utils.JsonUtils.isEmpty;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl.Message.Command;
import org.ovirt.vdsm.jsonrpc.client.utils.LockWrapper;

public class StompClient implements Receiver {
    private final StompTransport transport;
    private final CountDownLatch connected;
    private final CountDownLatch disconnected;
    private final SelectionKey key;
    private final Map<String, Listener> listener;
    private final Map<String, String> destinations;
    private String id;
    private String transactionId;
    private final Lock lock;

    public StompClient(String host, int port) throws IOException {
        this.transport = new StompTransport(host, this);
        this.connected = new CountDownLatch(1);
        this.disconnected = new CountDownLatch(1);
        this.listener = new ConcurrentHashMap<>();
        this.destinations = new ConcurrentHashMap<>();
        this.key = this.transport.connect(port);
        this.transport.send(new Message().connect().withHeader(HEADER_ACCEPT, "1.2").build(), key);
        try {
            // TODO use connection timeout
            this.connected.await();
        } catch (InterruptedException e) {
            throw new IOException("Not connected");
        }
        this.lock = new ReentrantLock();
    }

    public void subscribe(String channel, Listener listener) {
        try (LockWrapper ignored = new LockWrapper(this.lock)) {
            if (this.listener.get(channel) != null) {
                throw new IllegalArgumentException("Already subscribed to channel: " + channel);
            }
            this.listener.put(channel, listener);
            String id = UUID.randomUUID().toString();
            this.destinations.put(channel, id);
            this.transport.send(new Message().subscribe().withHeader(HEADER_DESTINATION, channel)
                    .withHeader(HEADER_ID, id)
                    .build(), this.key);
        }
    }

    public void send(String content, String channel) {
        Map<String, String> headers = new HashMap<>();
        try (LockWrapper ignored = new LockWrapper(this.lock)) {
            if (!isEmpty(this.transactionId)) {
                headers.put(HEADER_TRANSACTION, this.transactionId);
            }
        }
        headers.put(HEADER_DESTINATION, channel);
        this.transport.send(new Message().send().withContent(content.getBytes(UTF8)).withHeaders(headers).build(),
                key);
    }

    public void unsubscribe(String channel) {
        try (LockWrapper ignored = new LockWrapper(this.lock)) {
            String id = this.destinations.remove(channel);
            this.listener.remove(channel);
            this.transport.send(new Message().unsubscribe().withHeader(HEADER_ID, id).build(), key);
        }

    }

    public void disconnect() {
        id = UUID.randomUUID().toString();
        this.transport.send(new Message().disconnect().withHeader(HEADER_RECEIPT, id).build(), key);
        try {
            // TODO message timeout
            this.disconnected.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            // we can never receive confirmation
        }
    }

    public void stop() throws IOException {
        this.transport.close();
        this.listener.clear();
        this.destinations.clear();
    }

    public void begin() {
        try (LockWrapper ignored = new LockWrapper(this.lock)) {
            if (!isEmpty(this.transactionId)) {
                throw new IllegalStateException("Already opened transaction");
            }
            this.transactionId = UUID.randomUUID().toString();
            this.transport.send(new Message().begin().withHeader(HEADER_TRANSACTION, this.transactionId).build(), key);
        }
    }

    public void commit() {
        try (LockWrapper ignored = new LockWrapper(this.lock)) {
            if (isEmpty(this.transactionId)) {
                throw new IllegalStateException("No running transaction");
            }
            this.transport.send(new Message().commit().withHeader(HEADER_TRANSACTION, this.transactionId).build(), key);
            this.transactionId = null;
        }
    }

    @Override
    public void receive(Message message, SelectionKey key) {
        if (Command.CONNECTED.toString().equals(message.getCommand())) {
            this.connected.countDown();
        } else if (Command.MESSAGE.toString().equals(message.getCommand())) {
            try (LockWrapper ignored = new LockWrapper(this.lock)) {
                String destination = message.getHeaders().get(HEADER_DESTINATION);
                Listener listener = this.listener.get(destination);
                if (listener != null) {
                    listener.update(new String(message.getContent(), UTF8));
                }
            }
        } else if (Command.ERROR.toString().equals(message.getCommand())) {
            String destination = message.getHeaders().get(HEADER_DESTINATION);
            if (destination == null) {
                return;
            }
            Listener listener = this.listener.get(destination);
            if (listener != null) {
                listener.error(message.getHeaders());
            }
        } else if (Command.RECEIPT.toString().equals(message.getCommand())) {
            String receiptId = message.getHeaders().get(HEADER_RECEIPT_ID);
            if (!isEmpty(receiptId) && id.equals(receiptId)) {
                this.disconnected.countDown();
            }
        }
    }
}
