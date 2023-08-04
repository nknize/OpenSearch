/*
* SPDX-License-Identifier: Apache-2.0
*
* The OpenSearch Contributors require contributions made to
* this file be licensed under the Apache-2.0 license or a
* compatible open source license.
*/

/*
* Modifications Copyright OpenSearch Contributors. See
* GitHub history for details.
*/

package org.opensearch.common.transport;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import org.opensearch.common.io.stream.ProtobufWriteable;
import org.opensearch.common.network.InetAddresses;

import java.io.IOException;

/** not used
 * A bounded transport address is a tuple of {@link TransportAddress}, one array that represents
* the addresses the transport is bound to, and the other is the published one that represents the address clients
* should communicate on.
*
* @opensearch.internal
*/
public class ProtobufBoundTransportAddress implements ProtobufWriteable {

    private TransportAddress[] boundAddresses;

    private TransportAddress publishAddress;

    public ProtobufBoundTransportAddress(CodedInputStream in) throws IOException {
        int boundAddressLength = in.readInt32();
        boundAddresses = new TransportAddress[boundAddressLength];
        for (int i = 0; i < boundAddressLength; i++) {
            boundAddresses[i] = new TransportAddress(in);
        }
        publishAddress = new TransportAddress(in);
    }

    public ProtobufBoundTransportAddress(TransportAddress[] boundAddresses, TransportAddress publishAddress) {
        if (boundAddresses == null || boundAddresses.length < 1) {
            throw new IllegalArgumentException("at least one bound address must be provided");
        }
        this.boundAddresses = boundAddresses;
        this.publishAddress = publishAddress;
    }

    public TransportAddress[] boundAddresses() {
        return boundAddresses;
    }

    public TransportAddress publishAddress() {
        return publishAddress;
    }

    @Override
    public void writeTo(CodedOutputStream out) throws IOException {
        out.writeInt32NoTag(boundAddresses.length);
        for (TransportAddress address : boundAddresses) {
            address.writeTo(out);
        }
        publishAddress.writeTo(out);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("publish_address {");
        String hostString = publishAddress.address().getHostString();
        String publishAddressString = publishAddress.toString();
        if (InetAddresses.isInetAddress(hostString) == false) {
            publishAddressString = hostString + '/' + publishAddress.toString();
        }
        builder.append(publishAddressString);
        builder.append("}, bound_addresses ");
        boolean firstAdded = false;
        for (TransportAddress address : boundAddresses) {
            if (firstAdded) {
                builder.append(", ");
            } else {
                firstAdded = true;
            }

            builder.append("{").append(address).append("}");
        }
        return builder.toString();
    }
}
