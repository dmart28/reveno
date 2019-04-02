package org.reveno.atp.core.api;

public interface ViewsStorage {

	<View> View find(Class<View> viewType, long id);

	<View> void insert(long id, View view);
	
	<View> void remove(Class<View> viewType, long id);

	void clearAll();
	
}
