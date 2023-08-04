/*
* SPDX-License-Identifier: Apache-2.0
*
* The OpenSearch Contributors require contributions made to
* this file be licensed under the Apache-2.0 license or a
* compatible open source license.
*/

package org.opensearch.transport;

import org.opensearch.action.ProtobufActionType;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ProtobufActionListenerResponseHandler;
import org.opensearch.action.ProtobufActionRequest;
import org.opensearch.action.ProtobufActionResponse;
import org.opensearch.client.ProtobufClient;
import org.opensearch.client.support.ProtobufAbstractClient;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.threadpool.ThreadPool;

/**
 * ProtobufClient that is aware of remote clusters
*
* @opensearch.internal
*/
final class ProtobufRemoteClusterAwareClient extends ProtobufAbstractClient {

    private final TransportService service;
    private final String clusterAlias;
    private final RemoteClusterService remoteClusterService;

    ProtobufRemoteClusterAwareClient(Settings settings, ThreadPool threadPool, TransportService service, String clusterAlias) {
        super(settings, threadPool);
        this.service = service;
        this.clusterAlias = clusterAlias;
        this.remoteClusterService = service.getRemoteClusterService();
    }

    @Override
    protected <Request extends ProtobufActionRequest, Response extends ProtobufActionResponse> void doExecute(
        ProtobufActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    ) {
        remoteClusterService.ensureConnected(clusterAlias, ActionListener.wrap(v -> {
            Transport.Connection connection;
            if (request instanceof RemoteClusterAwareRequest) {
                DiscoveryNode preferredTargetNode = ((RemoteClusterAwareRequest) request).getPreferredTargetNode();
                connection = remoteClusterService.getConnection(preferredTargetNode, clusterAlias);
            } else {
                connection = remoteClusterService.getConnection(clusterAlias);
            }
            service.sendRequest(
                connection,
                action.name(),
                request,
                TransportRequestOptions.EMPTY,
                new ProtobufActionListenerResponseHandler<>(listener, action.getResponseReaderTry())
            );
        }, listener::onFailure));
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public ProtobufClient getRemoteClusterClient(String clusterAlias) {
        return remoteClusterService.getRemoteClusterClientProtobuf(threadPool(), clusterAlias);
    }
}
