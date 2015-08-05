package org.reveno.atp.core.views;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.api.query.MappingContext;
import org.reveno.atp.core.api.ViewsStorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;

public class OnDemandViewsContext implements MappingContext {

	@Override
	public <V> Optional<V> get(Class<V> viewType, long id) {
		Class<?> entityType = manager.resolveEntityType(viewType);
		Object entity;
		if (repository == null) {
			entity = marked.get(entityType).get(id);
		} else {
			entity = repository.get(entityType, id);
		}
		if (entity != null) {
			processor.map(entityType, id, entity);
		}
		return storage.find(viewType, id);
	}
	
	@Override
	public <V> List<V> link(Stream<Long> ids, Class<V> viewType) {
		return new LinkViewList<V>(ids.mapToLong(i->i).toArray(), viewType);
	}

	@Override
	public <V> Set<V> linkSet(Stream<Long> ids, Class<V> viewType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> List<V> link(LongCollection ids, Class<V> viewType) {
		return new LinkViewList<V>(ids.toLongArray(), viewType);
	}

	@Override
	public <V> Set<V> linkSet(LongCollection ids, Class<V> viewType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> Supplier<V> link(Class<V> viewType, long id) {
		return () -> storage.find(viewType, id).orElseGet(() -> null);
	}
	
	protected Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked;
	public void marked(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked) {
		this.marked = marked;
	}
	
	protected Repository repository;
	public void repositorySource(Repository repository) {
		this.repository = repository;
	}
	
	public OnDemandViewsContext(ViewsProcessor processor, ViewsStorage storage, ViewsManager manager) {
		this.processor = processor;
		this.manager = manager;
		this.storage = storage;
	}

	protected ViewsStorage storage;
	protected ViewsManager manager;
	protected ViewsProcessor processor;
	
	protected class LinkViewList<V> implements List<V> {
		protected final long[] ids;
		protected final Class<V> viewType;
		
		public LinkViewList(long[] ids, Class<V> viewType) {
			this.ids = ids;
			this.viewType = viewType;
		}

		@Override
		public int size() {
			return ids.length;
		}

		@Override
		public boolean isEmpty() {
			return ids.length == 0;
		}

		@Override
		public boolean contains(Object o) {
			Optional<V> v;
			for (int i = 0; i < ids.length; i++) {
				if ((v = storage.find(viewType, ids[i])).isPresent() && v.get().equals(o))
					return true;
			}
			return false;
		}

		@Override
		public Iterator<V> iterator() {
			return new ViewListIterator(0);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] toArray() {
			V[] arr = (V[]) Array.newInstance(viewType, ids.length);
			Optional<V> v;
			for (int i = 0; i < arr.length; i++) {
				arr[i] = (v = storage.find(viewType, ids[i])).isPresent() ? v.get() : null;
			}
			return arr;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			if (a.length < ids.length) 
				a = (T[]) Array.newInstance(a.getClass(), ids.length);
			Optional<V> v;
			for (int i = 0; i < a.length; i++) {
				a[i] = (v = storage.find(viewType, ids[i])).isPresent() ? (T) v.get() : null;
			}
			return a;
		}

		@Override
		public boolean add(V e) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("The List is read-only!");
		}
		
		@Override
		public boolean containsAll(Collection<?> c) {
			return c.stream().allMatch(cc -> contains(cc));
		}

		@Override
		public boolean addAll(Collection<? extends V> c) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public boolean addAll(int index, Collection<? extends V> c) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public void clear() {
		}

		@Override
		public V get(int index) {
			Optional<V> v;
			return (v = storage.find(viewType, ids[index])).isPresent() ? v.get() : null;
		}

		@Override
		public V set(int index, V element) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public void add(int index, V element) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public V remove(int index) {
			throw new UnsupportedOperationException("The List is read-only!");
		}

		@Override
		public int indexOf(Object o) {
			Optional<V> v;
			for (int i = 0; i < ids.length; i++)
				if ((v = storage.find(viewType, ids[i])).isPresent() && v.get().equals(o))
					return i;
			return 0;
		}

		@Override
		public int lastIndexOf(Object o) {
			Optional<V> v;
			for (int i = ids.length - 1; i <= 0; i--)
				if ((v = storage.find(viewType, ids[i])).isPresent() && v.get().equals(o))
					return i;
			return 0;
		}

		@Override
		public ListIterator<V> listIterator() {
			return new ViewListIterator(0);
		}

		@Override
		public ListIterator<V> listIterator(int index) {
			if (index < 0 || index >= ids.length) 
				throw new IllegalArgumentException("index out of bounds of ids array.");
			return new ViewListIterator(index);
		}

		@Override
		public List<V> subList(int fromIndex, int toIndex) {
			long[] newIds = new long[toIndex - fromIndex + 1];
			System.arraycopy(ids, fromIndex, newIds, 0, newIds.length);
			return new LinkViewList<V>(newIds, viewType);
		}
		
		private class ViewListIterator implements ListIterator<V> {
			protected int index = 0;
			
			public ViewListIterator(int index) {
				this.index = index;
			}
			
			@Override
			public boolean hasNext() {
				return ids.length - 1 <= index;
			}

			@Override
			public V next() {
				return LinkViewList.this.get(index++);
			}

			@Override
			public boolean hasPrevious() {
				return index > 0;
			}

			@Override
			public V previous() {
				return LinkViewList.this.get(index--);
			}

			@Override
			public int nextIndex() {
				return hasNext() ? index + 1 : ids.length;
			}

			@Override
			public int previousIndex() {
				return hasPrevious() ? index - 1 : 0;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("The List is read-only!");
			}

			@Override
			public void set(V e) {
				throw new UnsupportedOperationException("The List is read-only!");
			}

			@Override
			public void add(V e) {
				throw new UnsupportedOperationException("The List is read-only!");
			}
			
		}
		
	}
	
}