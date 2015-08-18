/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.transport;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A transport address used for IP socket address (wraps {@link java.net.InetSocketAddress}).
 */
public final class InetSocketTransportAddress implements TransportAddress {

    private static boolean resolveAddress = false;

    public static void setResolveAddress(boolean resolveAddress) {
        InetSocketTransportAddress.resolveAddress = resolveAddress;
    }

    public static boolean getResolveAddress() {
        return resolveAddress;
    }

    public static final InetSocketTransportAddress PROTO = new InetSocketTransportAddress();

    private final InetSocketAddress address;

    public InetSocketTransportAddress(StreamInput in) throws IOException {
        if (in.readByte() == 0) {
            int len = in.readByte();
            byte[] a = new byte[len]; // 4 bytes (IPv4) or 16 bytes (IPv6)
            in.readFully(a);
            InetAddress inetAddress;
            if (len == 16) {
                int scope_id = in.readInt();
                inetAddress = Inet6Address.getByAddress(null, a, scope_id);
            } else {
                inetAddress = InetAddress.getByAddress(a);
            }
            int port = in.readInt();
            this.address = new InetSocketAddress(inetAddress, port);
        } else {
            this.address = new InetSocketAddress(in.readString(), in.readInt());
        }
    }

    private InetSocketTransportAddress() {
        address = null;
    }

    public InetSocketTransportAddress(String hostname, int port) {
        this(new InetSocketAddress(hostname, port));
    }

    public InetSocketTransportAddress(InetAddress address, int port) {
        this(new InetSocketAddress(address, port));
    }

    public InetSocketTransportAddress(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public short uniqueAddressTypeId() {
        return 1;
    }

    @Override
    public boolean sameHost(TransportAddress other) {
        return other instanceof InetSocketTransportAddress &&
                address.getAddress().equals(((InetSocketTransportAddress) other).address.getAddress());
    }

    @Override
    public String getHost() {
        return address.getHostName();
    }

    @Override
    public String getAddress() {
        return address.getAddress().getHostAddress();
    }

    @Override
    public int getPort() {
        return address.getPort();
    }

    public InetSocketAddress address() {
        return this.address;
    }

    @Override
    public TransportAddress readFrom(StreamInput in) throws IOException {
        return new InetSocketTransportAddress(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        if (!resolveAddress && address.getAddress() != null) {
            out.writeByte((byte) 0);
            byte[] bytes = address().getAddress().getAddress();  // 4 bytes (IPv4) or 16 bytes (IPv6)
            out.writeByte((byte) bytes.length); // 1 byte
            out.write(bytes, 0, bytes.length);
            if (address().getAddress() instanceof Inet6Address)
                out.writeInt(((Inet6Address) address.getAddress()).getScopeId());
        } else {
            out.writeByte((byte) 1);
            out.writeString(address.getHostName());
        }
        out.writeInt(address.getPort());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InetSocketTransportAddress address1 = (InetSocketTransportAddress) o;
        return address.equals(address1.address);
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "inet[" + address + "]";
    }
}
