/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.clustering.api;

import org.reveno.atp.clustering.util.Bits;
import org.reveno.atp.utils.Exceptions;

import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Objects;

public class InetAddress extends Address {

	private int port;
	public int getPort() {
		return port;
	}
	
	private String host;
	public String getHost() {
		return host;
	}

	/**
	 * Default constructor.
	 * @param inetAddress network address at which node is discoverable in form "host:port"
	 * @param nodeId unqiue amoung cluster Node ID, which length should be less than 10 characters
	 */
	public InetAddress(String inetAddress, String nodeId) {
		this(inetAddress, nodeId, IOMode.ASYNC);
	}

	public InetAddress(String inetAddress, IOMode addressType) {
		this(inetAddress, Base64.getEncoder().encodeToString(Bits.intToBytes(
				(int) Bits.crc32(inetAddress.getBytes()))), addressType);
	}

	public InetAddress(String inetAddress, String nodeId, IOMode addressType) {
		super(inetAddress, addressType, nodeId);
		
		String[] vals = inetAddress.split(":");
		this.host = vals[0];
		this.port = Integer.parseInt(vals[1]);
		try {
			java.net.InetAddress addr = java.net.InetAddress.getByName(host);
			this.host = addr.getHostAddress();
		} catch (UnknownHostException e) {
			throw Exceptions.runtime(e);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InetAddress that = (InetAddress) o;
		return Objects.equals(port, that.port) &&
				Objects.equals(host, that.host);
	}

	@Override
	public int hashCode() {
		return Objects.hash(port, host);
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
}
