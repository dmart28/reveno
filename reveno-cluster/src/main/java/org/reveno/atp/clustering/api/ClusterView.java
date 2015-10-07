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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ClusterView {

	private final long viewId;
	public long viewId() {
		return viewId;
	}
	
	private final List<Address> members;
	public List<Address> members() {
		return Collections.unmodifiableList(members);
	}
	
	public ClusterView(long viewId, List<Address> members) {
		this.viewId = viewId;
		this.members = members;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClusterView that = (ClusterView) o;
		return Objects.equals(viewId, that.viewId) &&
				Objects.equals(members, that.members);
	}

	@Override
	public int hashCode() {
		return Objects.hash(viewId, members);
	}

	@Override
	public String toString() {
		return "ClusterView{" +
				"viewId=" + viewId +
				", members=" + members +
				'}';
	}
}
