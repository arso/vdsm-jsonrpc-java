package org.ovirt.vdsm.jsonrpc.client.reactors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.ovirt.vdsm.jsonrpc.client.ClientConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NioListener</code> provides a way to react on incoming messages.
 *
 */
public final class ReactorListener {
    public interface EventListener extends java.util.EventListener {
        void onAcccept(ReactorClient client);
    }

    private static Logger log = LoggerFactory.getLogger(ReactorListener.class);
    private final EventListener eventListener;
    private final ServerSocketChannel channel;
    private final Reactor reactor;
    private final Selector selector;

    public ReactorListener(Reactor reactor, InetSocketAddress address, Selector selector,
            EventListener eventListener) throws IOException {

        this.eventListener = eventListener;
        this.reactor = reactor;
        this.selector = selector;
        this.channel = setupChannel(address);
    }

    private ServerSocketChannel setupChannel(InetSocketAddress address)
            throws IOException {

        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        try {
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT, this);
            serverSocketChannel.bind(address);
        } catch (ClosedChannelException e) {
            throw new RuntimeException(
                    "Connection closed unexpectedly");
        } catch (IOException e) {
            try {
                serverSocketChannel.close();
            } catch (IOException e1) {
                // ignore
            }
            throw e;
        }

        return serverSocketChannel;

    }

    public int getPort() {
        return this.channel.socket().getLocalPort();
    }

    public ReactorClient accept() {
        ReactorClient client = null;
        try {
            final SocketChannel conn = this.channel.accept();
            if (conn == null) {
                return null;
            }
            conn.configureBlocking(false);
            InetSocketAddress address = (InetSocketAddress) conn.getRemoteAddress();

            client = this.reactor.createConnectedClient(this.reactor,
                    this.selector, address.getHostName(),
                    address.getPort(), conn);
            this.eventListener.onAcccept(client);
        } catch (IOException | ClientConnectionException e) {
            log.error("Not able to accept connection", e);
        }
        return client;
    }

    public Future<Void> close() {
        final Future<Void> task = new FutureTask<>(() -> {
            try {
                channel.close();
            } catch (IOException e) {
                // Ignore
            }
            return null;
        });
        this.reactor.queueFuture(task);
        return task;
    }
}
