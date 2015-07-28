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

package org.reveno.atp.core.api.storage;

import org.reveno.atp.core.api.channel.Channel;


public interface FoldersStorage {
	
	Channel channel(String address);

	Folder nextFolder(String prefix);

	Folder getLastFolder(String prefix);

	FolderItem[] getItems(Folder folder);

	FolderItem getItem(String name, Folder folder);

	FolderItem createItem(Folder folder, String name);

	
	public static class FolderItem implements Comparable<FolderItem> {

		private final String itemAddress;
		public String getItemAddress() {
			return itemAddress;
		}

		private final String name;
		public String getName() {
			return name;
		}

		public FolderItem(String name, String itemAddress) {
			this.name = name;
			this.itemAddress = itemAddress;
		}

		@Override
		public int compareTo(FolderItem other) {
			return getItemAddress().compareTo(other.getItemAddress());
		}

	}

	public static class Folder implements Comparable<Folder> {

		private final String groupAddress;
		public String getGroupAddress() {
			return groupAddress;
		}

		private final String prefix;
		public String getPrefix() {
			return prefix;
		}

		public Folder(String groupAddress, String prefix) {
			this.groupAddress = groupAddress;
			this.prefix = prefix;
		}

		@Override
		public int compareTo(Folder other) {
			return getGroupAddress().compareTo(other.getGroupAddress());
		}
	}

}
