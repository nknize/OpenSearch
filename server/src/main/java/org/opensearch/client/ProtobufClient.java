/*
* SPDX-License-Identifier: Apache-2.0
*
* The OpenSearch Contributors require contributions made to
* this file be licensed under the Apache-2.0 license or a
* compatible open source license.
*/

package org.opensearch.client;

import org.opensearch.action.ActionListener;
import org.opensearch.common.lease.Releasable;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.settings.Settings;

import java.util.Map;

/**
 * A client provides a one stop interface for performing actions/operations against the cluster.
* <p>
* All operations performed are asynchronous by nature. Each action/operation has two flavors, the first
* simply returns an {@link org.opensearch.action.ActionFuture}, while the second accepts an
* {@link ActionListener}.
* <p>
* A client can be retrieved from a started {@link org.opensearch.node.Node}.
*
* @see org.opensearch.node.Node#client()
*
* @opensearch.internal
*/
public interface ProtobufClient extends ProtobufOpenSearchClient, Releasable {

    Setting<String> CLIENT_TYPE_SETTING_S = new Setting<>("client.type", "node", (s) -> {
        switch (s) {
            case "node":
            case "transport":
                return s;
            default:
                throw new IllegalArgumentException("Can't parse [client.type] must be one of [node, transport]");
        }
    }, Property.NodeScope);

    /**
     * The admin client that can be used to perform administrative operations.
    */
    ProtobufAdminClient admin();

    /**
     * Returns this clients settings
    */
    Settings settings();

    /**
     * Returns a new lightweight ProtobufClient that applies all given headers to each of the requests
    * issued from it.
    */
    ProtobufClient filterWithHeader(Map<String, String> headers);

    /**
     * Returns a client to a remote cluster with the given cluster alias.
    *
    * @throws IllegalArgumentException if the given clusterAlias doesn't exist
    * @throws UnsupportedOperationException if this functionality is not available on this client.
    */
    default ProtobufClient getRemoteClusterClient(String clusterAlias) {
        throw new UnsupportedOperationException("this client doesn't support remote cluster connections");
    }
}
