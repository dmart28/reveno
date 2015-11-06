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

import java.util.Objects;

public class Address {

	private String connectionString;
	public String getConnectionString() {
		return connectionString;
	}
	
	private IOMode addressType;
	public IOMode getAddressType() {
		return addressType;
	}

	private String nodeId;
	public String getNodeId() {
		return nodeId;
	}

	public Address(String connectionString, IOMode addressType, String nodeId) {
		this.connectionString = connectionString;
		this.addressType = addressType;
		this.nodeId = nodeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Address address = (Address) o;
		return Objects.equals(connectionString, address.connectionString) &&
				Objects.equals(addressType, address.addressType) &&
				Objects.equals(nodeId, address.nodeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionString, addressType, nodeId);
	}
}
