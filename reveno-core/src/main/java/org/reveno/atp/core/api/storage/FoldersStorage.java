package org.reveno.atp.core.api.storage;

import org.reveno.atp.core.api.channel.Channel;

public interface FoldersStorage {

    Channel channel(String address);

    Folder nextFolder(String prefix);

    Folder getLastFolder(String prefix);

    FolderItem[] getItems(Folder folder);

    FolderItem getItem(String name, Folder folder);

    FolderItem createItem(Folder folder, String name);


    class FolderItem implements Comparable<FolderItem> {

        private final String itemAddress;
        private final String name;

        public FolderItem(String name, String itemAddress) {
            this.name = name;
            this.itemAddress = itemAddress;
        }

        public String getItemAddress() {
            return itemAddress;
        }

        public String getName() {
            return name;
        }

        @Override
        public int compareTo(FolderItem other) {
            return getItemAddress().compareTo(other.getItemAddress());
        }

    }

    public static class Folder implements Comparable<Folder> {

        private final String groupAddress;
        private final String prefix;

        public Folder(String groupAddress, String prefix) {
            this.groupAddress = groupAddress;
            this.prefix = prefix;
        }

        public String getGroupAddress() {
            return groupAddress;
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public int compareTo(Folder other) {
            return getGroupAddress().compareTo(other.getGroupAddress());
        }
    }

}
