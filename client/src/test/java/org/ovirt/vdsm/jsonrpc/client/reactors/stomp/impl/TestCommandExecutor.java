package org.ovirt.vdsm.jsonrpc.client.reactors.stomp.impl;

import java.nio.channels.SelectionKey;

public interface TestCommandExecutor {
    Message execute(Message message, SelectionKey key);
}
