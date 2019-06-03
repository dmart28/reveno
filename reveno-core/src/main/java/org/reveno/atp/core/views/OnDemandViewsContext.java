package org.reveno.atp.core.views;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import org.reveno.atp.api.domain.Repository;
import org.reveno.atp.api.query.MappingContext;
import org.reveno.atp.core.api.ViewsStorage;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class OnDemandViewsContext implements MappingContext {
    protected ViewsStorage storage;
    protected ViewsManager manager;
    protected ViewsProcessor processor;
    protected Class<?> viewType;
    protected long id;
    protected Repository repository;
    protected Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked;

    public OnDemandViewsContext(ViewsProcessor processor, ViewsStorage storage, ViewsManager manager) {
        this.processor = processor;
        this.manager = manager;
        this.storage = storage;
    }

    @Override
    public <V> V get(Class<V> viewType, long id) {
        if (viewType == this.viewType && id == this.id) {
            return storage.find(viewType, id);
        }

        Class<?> entityType = manager.resolveEntityType(viewType);
        Object entity;
        if (repository == null) {
            entity = marked.get(entityType).get(id);
        } else {
            entity = repository.get(entityType, id);
        }
        if (entity != null) {
            processor.map(entityType, id, entity);
            if (repository == null) {
                marked.get(entityType).remove(id);
            }
        }
        return storage.find(viewType, id);
    }

    @Override
    public <V> List<V> link(long[] ids, Class<V> viewType) {
        return new LinkViewList<>(ids, viewType);
    }

    @Override
    public <V> Set<V> linkSet(long[] ids, Class<V> viewType) {
        return new LinkViewSet<>(ids, viewType);
    }

    @Override
    public <V> List<V> link(Stream<Long> ids, Class<V> viewType) {
        return new LinkViewList<>(ids.mapToLong(i -> i).toArray(), viewType);
    }

    @Override
    public <V> Set<V> linkSet(Stream<Long> ids, Class<V> viewType) {
        return new LinkViewSet<>(ids.mapToLong(i -> i).toArray(), viewType);
    }

    @Override
    public <V> List<V> link(LongCollection ids, Class<V> viewType) {
        return new LinkViewList<>(ids.toLongArray(), viewType);
    }

    @Override
    public <V> Set<V> linkSet(LongCollection ids, Class<V> viewType) {
        return new LinkViewSet<>(ids.toLongArray(), viewType);
    }

    @Override
    public <V> Supplier<V> link(Class<V> viewType, long id) {
        return () -> Optional.ofNullable(storage.find(viewType, id)).orElse(null);
    }

    public void marked(Map<Class<?>, Long2ObjectLinkedOpenHashMap<Object>> marked) {
        this.marked = marked;
    }

    public void repositorySource(Repository repository) {
        this.repository = repository;
    }

    public void currentId(long id) {
        this.id = id;
    }

    public void currentViewType(Class<?> viewType) {
        this.viewType = viewType;
    }


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
            V v;
            for (long id1 : ids) {
                if ((v = storage.find(viewType, id1)) != null && v.equals(o))
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
            V v;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = (v = storage.find(viewType, ids[i])) != null ? v : null;
            }
            return arr;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            if (a.length < ids.length)
                a = (T[]) Array.newInstance(a.getClass(), ids.length);
            V v;
            for (int i = 0; i < a.length; i++) {
                a[i] = (v = storage.find(viewType, ids[i])) != null ? (T) v : null;
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
            return c.stream().allMatch(this::contains);
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
            V v;
            return (v = storage.find(viewType, ids[index])) != null ? v : null;
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
            V v;
            for (int i = 0; i < ids.length; i++)
                if ((v = storage.find(viewType, ids[i])) != null && v.equals(o))
                    return i;
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            V v;
            for (int i = ids.length - 1; i <= 0; i--)
                if ((v = storage.find(viewType, ids[i])) != null && v.equals(o))
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

        @Override
        public String toString() {
            Iterator<V> it = listIterator();
            if (!it.hasNext())
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (; ; ) {
                V e = it.next();
                sb.append(e == this ? "(this Collection)" : e);
                if (!it.hasNext())
                    return sb.append(']').toString();
                sb.append(',').append(' ');
            }
        }

        private class ViewListIterator implements ListIterator<V> {
            protected int index = 0;

            public ViewListIterator(int index) {
                this.index = index;
            }

            @Override
            public boolean hasNext() {
                return ids.length - 1 >= index;
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

    protected class LinkViewSet<V> extends LinkViewList<V> implements Set<V> {

        public LinkViewSet(long[] ids, Class<V> viewType) {
            super(LongStream.of(ids).distinct().toArray(), viewType);
        }

        @Override
        public Iterator<V> iterator() {
            return new SetIterator();
        }

        @Override
        public boolean add(V e) {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("The Set is read-only!");
        }

        @Override
        public Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT);
        }

        protected class SetIterator implements Iterator<V> {
            protected int index = 0;

            @Override
            public boolean hasNext() {
                return ids.length - 1 >= index;
            }

            @Override
            public V next() {
                return LinkViewSet.this.get(index++);
            }

        }

    }

}