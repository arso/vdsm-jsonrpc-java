package org.ovirt.vdsm.jsonrpc.client.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.JsonNode;
import org.ovirt.vdsm.jsonrpc.client.BrokerCommandCallback;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcRequest;
import org.ovirt.vdsm.jsonrpc.client.JsonRpcResponse;

/**
 * <code>Call</code> holds single response and uses {@link BatchCall}
 * as internal implementation to promote code reuse.
 *
 */
public class Call implements Future<JsonRpcResponse>, JsonRpcCall {

    private final BatchCall batchCall;

    public Call(JsonRpcRequest req) {
        this.batchCall = new BatchCall(Arrays.asList(req));
    }

    public Call(JsonRpcRequest req, BrokerCommandCallback callback) {
        this.batchCall = new BatchCall(Arrays.asList(req), callback);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.batchCall.cancel(mayInterruptIfRunning);
    }

    @Override
    public void addResponse(JsonRpcResponse response) {
        this.batchCall.addResponse(response);
    }

    private JsonRpcResponse extractResponse(List<JsonRpcResponse> list) {
        return list.get(0);
    }

    public JsonNode getId() {
        return this.batchCall.getId().get(0);
    }

    @Override
    public JsonRpcResponse get() throws InterruptedException {
        return extractResponse(this.batchCall.get());
    }

    @Override
    public JsonRpcResponse get(long timeout, TimeUnit unit)
            throws InterruptedException,
            TimeoutException {
        return extractResponse(this.batchCall.get(timeout, unit));
    }

    @Override
    public boolean isCancelled() {
        return this.batchCall.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.batchCall.isDone();
    }

    @Override
    public BrokerCommandCallback getCallback() {
        return batchCall.getCallback();
    }
}
