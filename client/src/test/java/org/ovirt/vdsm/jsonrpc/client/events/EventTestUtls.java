package org.ovirt.vdsm.jsonrpc.client.events;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

public class EventTestUtls {

    public static final String MESSAGE_CONTENT =
            "{\"jsonrpc\": \"2.0\", \"method\": \"|testcase|test|update\", \"params\": {\"value\": 42}}";
    private static final int EVENT_TIMEOUT_IN_HOURS = 10;

    static class ResponseForkJoinWorkerThread extends ForkJoinWorkerThread {

        protected ResponseForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool);
        }
    }

    public static EventPublisher createPublisher() {
        return new EventPublisher(new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                new ForkJoinWorkerThreadFactory() {

                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        return new ResponseForkJoinWorkerThread(pool);
                    }

                },
                null,
                true),
                EVENT_TIMEOUT_IN_HOURS);
    }
}
