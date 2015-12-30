package org.reveno.atp.commons;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

import java.io.Serializable;
import java.util.Map;
import java.util.Arrays;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 *
 *
 * A type-specific hash map with a fast, small-footprint implementation.
 *
 * <P>Instances of this class use a hash table to represent a map. The table is filled up to a specified <em>load factor</em>, and then doubled in size to accommodate new entries. If the table is
 * emptied below <em>one fourth</em> of the load factor, it is halved in size. However, halving is not performed when deleting entries from an iterator, as it would interfere with the iteration
 * process.
 *
 * <p>Note that {@link #clear()} does not modify the hash table size. Rather, a family of {@linkplain #trim() trimming methods} lets you control the size of the table; this is particularly useful if
 * you reuse instances of this class.
 *
 * @see Hash
 * @see HashCommon */
public class Long2LongOpenHashMap extends AbstractLong2LongMap implements java.io.Serializable, Cloneable, Hash {
    private static final long serialVersionUID = 0L;
    private static final boolean ASSERTS = false;
    /** The array of keys. */
    protected long[] key;
    /** The array of values. */
    protected long[] value;
    /** The mask for wrapping a position counter. */
    protected int mask;
    /** Whether this set contains the key zero. */
    protected boolean containsNullKey;
    /** The current table size. */
    protected int n;
    /** Threshold after which we rehash. It must be the table size times {@link #f}. */
    protected int maxFill;
    /** Number of entries in the set (including the key zero, if present). */
    protected int size;
    /** The acceptable load factor. */
    protected final float f;
    /** Cached set of entries. */
    protected volatile FastEntrySet entries;
    /** Cached set of keys. */
    protected volatile LongSet keys;
    /** Cached collection of values. */
    protected volatile LongCollection values;

    /** Creates a new hash map.
     *
     * <p>The actual table size will be the least power of two greater than <code>expected</code>/<code>f</code>.
     *
     * @param expected the expected number of elements in the hash set.
     * @param f the load factor. */

    public Long2LongOpenHashMap( final int expected, final float f ) {
        if ( f <= 0 || f > 1 ) throw new IllegalArgumentException( "Load factor must be greater than 0 and smaller than or equal to 1" );
        if ( expected < 0 ) throw new IllegalArgumentException( "The expected number of elements must be nonnegative" );
        this.f = f;
        n = arraySize( expected, f );
        mask = n - 1;
        maxFill = maxFill( n, f );
        key = new long[ n + 1 ];
        value = new long[ n + 1 ];
    }

    /** Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
     *
     * @param expected the expected number of elements in the hash map. */
    public Long2LongOpenHashMap( final int expected ) {
        this( expected, DEFAULT_LOAD_FACTOR );
    }

    /** Creates a new hash map with initial expected {@link Hash#DEFAULT_INITIAL_SIZE} entries and {@link Hash#DEFAULT_LOAD_FACTOR} as load factor. */
    public Long2LongOpenHashMap() {
        this( DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR );
    }

    /** Creates a new hash map copying a given one.
     *
     * @param m a {@link Map} to be copied into the new hash map.
     * @param f the load factor. */
    public Long2LongOpenHashMap( final Map<? extends Long, ? extends Long> m, final float f ) {
        this( m.size(), f );
        putAll( m );
    }

    /** Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor copying a given one.
     *
     * @param m a {@link Map} to be copied into the new hash map. */
    public Long2LongOpenHashMap( final Map<? extends Long, ? extends Long> m ) {
        this( m, DEFAULT_LOAD_FACTOR );
    }

    /** Creates a new hash map copying a given type-specific one.
     *
     * @param m a type-specific map to be copied into the new hash map.
     * @param f the load factor. */
    public Long2LongOpenHashMap( final Long2LongMap m, final float f ) {
        this( m.size(), f );
        putAll( m );
    }

    /** Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor copying a given type-specific one.
     *
     * @param m a type-specific map to be copied into the new hash map. */
    public Long2LongOpenHashMap( final Long2LongMap m ) {
        this( m, DEFAULT_LOAD_FACTOR );
    }

    /** Creates a new hash map using the elements of two parallel arrays.
     *
     * @param k the array of keys of the new hash map.
     * @param v the array of corresponding values in the new hash map.
     * @param f the load factor.
     * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have different lengths. */
    public Long2LongOpenHashMap( final long[] k, final long[] v, final float f ) {
        this( k.length, f );
        if ( k.length != v.length ) throw new IllegalArgumentException( "The key array and the value array have different lengths (" + k.length + " and " + v.length + ")" );
        for ( int i = 0; i < k.length; i++ )
            this.put( k[ i ], v[ i ] );
    }

    /** Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor using the elements of two parallel arrays.
     *
     * @param k the array of keys of the new hash map.
     * @param v the array of corresponding values in the new hash map.
     * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have different lengths. */
    public Long2LongOpenHashMap( final long[] k, final long[] v ) {
        this( k, v, DEFAULT_LOAD_FACTOR );
    }

    private int realSize() {
        return containsNullKey ? size - 1 : size;
    }

    private void ensureCapacity( final int capacity ) {
        final int needed = arraySize( capacity, f );
        if ( needed > n ) rehash( needed );
    }

    private void tryCapacity( final long capacity ) {
        final int needed = (int)Math.min( 1 << 30, Math.max( 2, HashCommon.nextPowerOfTwo( (long)Math.ceil( capacity / f ) ) ) );
        if ( needed > n ) rehash( needed );
    }

    private long removeEntry( final int pos ) {
        final long oldValue = value[ pos ];
        size--;
        shiftKeys( pos );
        if ( size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE ) rehash( n / 2 );
        return oldValue;
    }

    private long removeNullEntry() {
        containsNullKey = false;
        final long oldValue = value[ n ];
        size--;
        if ( size < maxFill / 4 && n > DEFAULT_INITIAL_SIZE ) rehash( n / 2 );
        return oldValue;
    }

    /** {@inheritDoc} */
    public void putAll( Map<? extends Long, ? extends Long> m ) {
        if ( f <= .5 ) ensureCapacity( m.size() ); // The resulting map will be sized for m.size() elements
        else tryCapacity( size() + m.size() ); // The resulting map will be tentatively sized for size() + m.size() elements
        super.putAll( m );
    }

    private int insert( final long k, final long v ) {
        int pos;
        if ( ( ( k ) == ( 0 ) ) ) {
            if ( containsNullKey ) return n;
            containsNullKey = true;
            pos = n;
        }
        else {
            long curr;
            final long[] key = this.key;
            // The starting point.
            if ( !( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) {
                if ( ( ( curr ) == ( k ) ) ) return pos;
                while ( !( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) )
                    if ( ( ( curr ) == ( k ) ) ) return pos;
            }
        }
        key[ pos ] = k;
        value[ pos ] = v;
        if ( size++ >= maxFill ) rehash( arraySize( size + 1, f ) );
        if ( ASSERTS ) checkTable();
        return -1;
    }

    public long put( final long k, final long v ) {
        final int pos = insert( k, v );
        if ( pos < 0 ) return defRetValue;
        final long oldValue = value[ pos ];
        value[ pos ] = v;
        return oldValue;
    }

    public Long put( final Long ok, final Long ov ) {
        final long v = ( ( ov ).longValue() );
        final int pos = insert( ( ( ok ).longValue() ), v );
        if ( pos < 0 ) return ( null );
        final long oldValue = value[ pos ];
        value[ pos ] = v;
        return ( Long.valueOf( oldValue ) );
    }

    private long addToValue( final int pos, final long incr ) {
        final long oldValue = value[ pos ];
        value[ pos ] = oldValue + incr;
        return oldValue;
    }

    /** Adds an increment to value currently associated with a key.
     *
     * <P>Note that this method respects the {@linkplain #defaultReturnValue() default return value} semantics: when called with a key that does not currently appears in the map, the key will be
     * associated with the default return value plus the given increment.
     *
     * @param k the key.
     * @param incr the increment.
     * @return the old value, or the {@linkplain #defaultReturnValue() default return value} if no value was present for the given key. */
    public long addTo( final long k, final long incr ) {
        int pos;
        if ( ( ( k ) == ( 0 ) ) ) {
            if ( containsNullKey ) return addToValue( n, incr );
            pos = n;
            containsNullKey = true;
        }
        else {
            long curr;
            final long[] key = this.key;
            // The starting point.
            if ( !( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) {
                if ( ( ( curr ) == ( k ) ) ) return addToValue( pos, incr );
                while ( !( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) )
                    if ( ( ( curr ) == ( k ) ) ) return addToValue( pos, incr );
            }
        }
        key[ pos ] = k;
        value[ pos ] = defRetValue + incr;
        if ( size++ >= maxFill ) rehash( arraySize( size + 1, f ) );
        if ( ASSERTS ) checkTable();
        return defRetValue;
    }

    /** Shifts left entries with the specified hash code, starting at the specified position, and empties the resulting free entry.
     *
     * @param pos a starting position. */
    protected final void shiftKeys( int pos ) {
        // Shift entries with the same hash.
        int last, slot;
        long curr;
        final long[] key = this.key;
        for ( ;; ) {
            pos = ( ( last = pos ) + 1 ) & mask;
            for ( ;; ) {
                if ( ( ( curr = key[ pos ] ) == ( 0 ) ) ) {
                    key[ last ] = ( 0 );
                    return;
                }
                slot = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( curr ) ) & mask;
                if ( last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos ) break;
                pos = ( pos + 1 ) & mask;
            }
            key[ last ] = curr;
            value[ last ] = value[ pos ];
        }
    }

    public long remove( final long k ) {
        if ( ( ( k ) == ( 0 ) ) ) {
            if ( containsNullKey ) return removeNullEntry();
            return defRetValue;
        }
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return defRetValue;
        if ( ( ( k ) == ( curr ) ) ) return removeEntry( pos );
        while ( true ) {
            if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return defRetValue;
            if ( ( ( k ) == ( curr ) ) ) return removeEntry( pos );
        }
    }

    public Long remove( final Object ok ) {
        final long k = ( ( ( (Long)( ok ) ).longValue() ) );
        if ( ( ( k ) == ( 0 ) ) ) {
            if ( containsNullKey ) return ( Long.valueOf( removeNullEntry() ) );
            return ( null );
        }
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return ( null );
        if ( ( ( curr ) == ( k ) ) ) return ( Long.valueOf( removeEntry( pos ) ) );
        while ( true ) {
            if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return ( null );
            if ( ( ( curr ) == ( k ) ) ) return ( Long.valueOf( removeEntry( pos ) ) );
        }
    }

    public Long get( final Long ok ) {
        final long k = ( ( ok ).longValue() );
        if ( ( ( k ) == ( 0 ) ) ) return containsNullKey ? ( Long.valueOf( value[ n ] ) ) : ( null );
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return ( null );
        if ( ( ( k ) == ( curr ) ) ) return ( Long.valueOf( value[ pos ] ) );
        // There's always an unused entry.
        while ( true ) {
            if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return ( null );
            if ( ( ( k ) == ( curr ) ) ) return ( Long.valueOf( value[ pos ] ) );
        }
    }

    public long get( final long k ) {
        if ( ( ( k ) == ( 0 ) ) ) return containsNullKey ? value[ n ] : defRetValue;
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return defRetValue;
        if ( ( ( k ) == ( curr ) ) ) return value[ pos ];
        // There's always an unused entry.
        while ( true ) {
            if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return defRetValue;
            if ( ( ( k ) == ( curr ) ) ) return value[ pos ];
        }
    }

    public boolean containsKey( final long k ) {
        if ( ( ( k ) == ( 0 ) ) ) return containsNullKey;
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return false;
        if ( ( ( k ) == ( curr ) ) ) return true;
        // There's always an unused entry.
        while ( true ) {
            if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return false;
            if ( ( ( k ) == ( curr ) ) ) return true;
        }
    }

    public boolean containsValue( final long v ) {
        final long value[] = this.value;
        final long key[] = this.key;
        if ( containsNullKey && ( ( value[ n ] ) == ( v ) ) ) return true;
        for ( int i = n; i-- != 0; )
            if ( !( ( key[ i ] ) == ( 0 ) ) && ( ( value[ i ] ) == ( v ) ) ) return true;
        return false;
    }

    /* Removes all elements from this map.
     *
     * <P>To increase object reuse, this method does not change the table size. If you want to reduce the table size, you must use {@link #trim()}. */
    public void clear() {
        if ( size == 0 ) return;
        size = 0;
        containsNullKey = false;
        Arrays.fill( key, ( 0 ) );
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** A no-op for backward compatibility.
     *
     * @param growthFactor unused.
     * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled when they are too full. */
    @Deprecated
    public void growthFactor( int growthFactor ) {}

    /** Gets the growth factor (2).
     *
     * @return the growth factor of this set, which is fixed (2).
     * @see #growthFactor(int)
     * @deprecated Since <code>fastutil</code> 6.1.0, hash tables are doubled when they are too full. */
    @Deprecated
    public int growthFactor() {
        return 16;
    }

    /** The entry class for a hash map does not record key and value, but rather the position in the hash table of the corresponding entry. This is necessary so that calls to
     * {@link java.util.Map.Entry#setValue(Object)} are reflected in the map */
    final class MapEntry implements Long2LongMap.Entry, Map.Entry<Long, Long> {
        // The table index this entry refers to, or -1 if this entry has been deleted.
        int index;

        MapEntry( final int index ) {
            this.index = index;
        }

        MapEntry() {}

        public Long getKey() {
            return ( Long.valueOf( key[ index ] ) );
        }

        public long getLongKey() {
            return key[ index ];
        }

        public Long getValue() {
            return ( Long.valueOf( value[ index ] ) );
        }

        public long getLongValue() {
            return value[ index ];
        }

        public long setValue( final long v ) {
            final long oldValue = value[ index ];
            value[ index ] = v;
            return oldValue;
        }

        public Long setValue( final Long v ) {
            return ( Long.valueOf( setValue( ( ( v ).longValue() ) ) ) );
        }

        @SuppressWarnings("unchecked")
        public boolean equals( final Object o ) {
            if ( !( o instanceof Map.Entry ) ) return false;
            Map.Entry<Long, Long> e = (Map.Entry<Long, Long>)o;
            return ( ( key[ index ] ) == ( ( ( e.getKey() ).longValue() ) ) ) && ( ( value[ index ] ) == ( ( ( e.getValue() ).longValue() ) ) );
        }

        public int hashCode() {
            return it.unimi.dsi.fastutil.HashCommon.long2int( key[ index ] ) ^ it.unimi.dsi.fastutil.HashCommon.long2int( value[ index ] );
        }

        public String toString() {
            return key[ index ] + "=>" + value[ index ];
        }
    }

    /** An iterator over a hash map. */
    private class MapIterator {
        /** The index of the last entry returned, if positive or zero; initially, {@link #n}. If negative, the last entry returned was that of the key of index {@code - pos - 1} from the
         * {@link #wrapped} list. */
        int pos = n;
        /** The index of the last entry that has been returned (more precisely, the value of {@link #pos} if {@link #pos} is positive, or {@link Integer#MIN_VALUE} if {@link #pos} is negative). It is
         * -1 if either we did not return an entry yet, or the last returned entry has been removed. */
        int last = -1;
        /** A downward counter measuring how many entries must still be returned. */
        int c = size;
        /** A boolean telling us whether we should return the entry with the null key. */
        boolean mustReturnNullKey = Long2LongOpenHashMap.this.containsNullKey;
        /** A lazily allocated list containing keys of entries that have wrapped around the table because of removals. */
        LongArrayList wrapped;

        public boolean hasNext() {
            return c != 0;
        }

        public int nextEntry() {
            if ( !hasNext() ) throw new NoSuchElementException();
            c--;
            if ( mustReturnNullKey ) {
                mustReturnNullKey = false;
                return last = n;
            }
            final long key[] = Long2LongOpenHashMap.this.key;
            for ( ;; ) {
                if ( --pos < 0 ) {
                    // We are just enumerating elements from the wrapped list.
                    last = Integer.MIN_VALUE;
                    final long k = wrapped.getLong( -pos - 1 );
                    int p = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask;
                    while ( !( ( k ) == ( key[ p ] ) ) )
                        p = ( p + 1 ) & mask;
                    return p;
                }
                if ( !( ( key[ pos ] ) == ( 0 ) ) ) return last = pos;
            }
        }

        /** Shifts left entries with the specified hash code, starting at the specified position, and empties the resulting free entry.
         *
         * @param pos a starting position. */
        private final void shiftKeys( int pos ) {
            // Shift entries with the same hash.
            int last, slot;
            long curr;
            final long[] key = Long2LongOpenHashMap.this.key;
            for ( ;; ) {
                pos = ( ( last = pos ) + 1 ) & mask;
                for ( ;; ) {
                    if ( ( ( curr = key[ pos ] ) == ( 0 ) ) ) {
                        key[ last ] = ( 0 );
                        return;
                    }
                    slot = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( curr ) ) & mask;
                    if ( last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos ) break;
                    pos = ( pos + 1 ) & mask;
                }
                if ( pos < last ) { // Wrapped entry.
                    if ( wrapped == null ) wrapped = new LongArrayList( 2 );
                    wrapped.add( key[ pos ] );
                }
                key[ last ] = curr;
                value[ last ] = value[ pos ];
            }
        }

        public void remove() {
            if ( last == -1 ) throw new IllegalStateException();
            if ( last == n ) {
                containsNullKey = false;
            }
            else if ( pos >= 0 ) shiftKeys( last );
            else {
                // We're removing wrapped entries.
                Long2LongOpenHashMap.this.remove( wrapped.getLong( -pos - 1 ) );
                last = -1; // Note that we must not decrement size
                return;
            }
            size--;
            last = -1; // You can no longer remove this entry.
            if ( ASSERTS ) checkTable();
        }

        public int skip( final int n ) {
            int i = n;
            while ( i-- != 0 && hasNext() )
                nextEntry();
            return n - i - 1;
        }
    }

    private class EntryIterator extends MapIterator implements ObjectIterator<Long2LongMap.Entry> {
        private MapEntry entry;

        public Long2LongMap.Entry next() {
            return entry = new MapEntry( nextEntry() );
        }

        @Override
        public void remove() {
            super.remove();
            entry.index = -1; // You cannot use a deleted entry.
        }
    }

    private class FastEntryIterator extends MapIterator implements ObjectIterator<Long2LongMap.Entry> {
        private final MapEntry entry = new MapEntry();

        public MapEntry next() {
            entry.index = nextEntry();
            return entry;
        }
    }

    private final class MapEntrySet extends AbstractObjectSet<Long2LongMap.Entry> implements FastEntrySet, Serializable {
        public ObjectIterator<Long2LongMap.Entry> iterator() {
            return new EntryIterator();
        }

        public ObjectIterator<Long2LongMap.Entry> fastIterator() {
            return new FastEntryIterator();
        }

        @SuppressWarnings("unchecked")
        public boolean contains( final Object o ) {
            if ( !( o instanceof Map.Entry ) ) return false;
            final Map.Entry<Long, Long> e = (Map.Entry<Long, Long>)o;
            final long k = ( ( e.getKey() ).longValue() );
            if ( ( ( k ) == ( 0 ) ) ) return ( Long2LongOpenHashMap.this.containsNullKey && ( ( value[ n ] ) == ( ( ( e.getValue() ).longValue() ) ) ) );
            long curr;
            final long[] key = Long2LongOpenHashMap.this.key;
            int pos;
            // The starting point.
            if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return false;
            if ( ( ( k ) == ( curr ) ) ) return ( ( value[ pos ] ) == ( ( ( e.getValue() ).longValue() ) ) );
            // There's always an unused entry.
            while ( true ) {
                if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return false;
                if ( ( ( k ) == ( curr ) ) ) return ( ( value[ pos ] ) == ( ( ( e.getValue() ).longValue() ) ) );
            }
        }

        @SuppressWarnings("unchecked")
        public boolean remove( final Object o ) {
            if ( !( o instanceof Map.Entry ) ) return false;
            final Map.Entry<Long, Long> e = (Map.Entry<Long, Long>)o;
            final long k = ( ( e.getKey() ).longValue() );
            final long v = ( ( e.getValue() ).longValue() );
            if ( ( ( k ) == ( 0 ) ) ) {
                if ( containsNullKey && ( ( value[ n ] ) == ( v ) ) ) {
                    removeNullEntry();
                    return true;
                }
                return false;
            }
            long curr;
            final long[] key = Long2LongOpenHashMap.this.key;
            int pos;
            // The starting point.
            if ( ( ( curr = key[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask ] ) == ( 0 ) ) ) return false;
            if ( ( ( curr ) == ( k ) ) ) {
                if ( ( ( value[ pos ] ) == ( v ) ) ) {
                    removeEntry( pos );
                    return true;
                }
                return false;
            }
            while ( true ) {
                if ( ( ( curr = key[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) ) return false;
                if ( ( ( curr ) == ( k ) ) ) {
                    if ( ( ( value[ pos ] ) == ( v ) ) ) {
                        removeEntry( pos );
                        return true;
                    }
                }
            }
        }

        public int size() {
            return size;
        }

        public void clear() {
            Long2LongOpenHashMap.this.clear();
        }
    }

    public FastEntrySet long2LongEntrySet() {
        if ( entries == null ) entries = new MapEntrySet();
        return entries;
    }

    /** An iterator on keys.
     *
     * <P>We simply override the {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and possibly their type-specific counterparts) so that they return keys
     * instead of entries. */
    private final class KeyIterator extends MapIterator implements LongIterator {
        public KeyIterator() {
            super();
        }

        public long nextLong() {
            return key[ nextEntry() ];
        }

        public Long next() {
            return ( Long.valueOf( key[ nextEntry() ] ) );
        }
    }

    private final class KeySet extends AbstractLongSet {
        public LongIterator iterator() {
            return new KeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean contains( long k ) {
            return containsKey( k );
        }

        public boolean remove( long k ) {
            final int oldSize = size;
            Long2LongOpenHashMap.this.remove( k );
            return size != oldSize;
        }

        public void clear() {
            Long2LongOpenHashMap.this.clear();
        }
    }

    public LongSet keySet() {
        if ( keys == null ) keys = new KeySet();
        return keys;
    }

    /** An iterator on values.
     *
     * <P>We simply override the {@link java.util.ListIterator#next()}/{@link java.util.ListIterator#previous()} methods (and possibly their type-specific counterparts) so that they return values
     * instead of entries. */
    private final class ValueIterator extends MapIterator implements LongIterator {
        public ValueIterator() {
            super();
        }

        public long nextLong() {
            return value[ nextEntry() ];
        }

        public Long next() {
            return ( Long.valueOf( value[ nextEntry() ] ) );
        }
    }

    public LongCollection values() {
        if ( values == null ) values = new AbstractLongCollection() {
            public LongIterator iterator() {
                return new ValueIterator();
            }

            public int size() {
                return size;
            }

            public boolean contains( long v ) {
                return containsValue( v );
            }

            public void clear() {
                Long2LongOpenHashMap.this.clear();
            }
        };
        return values;
    }

    /** A no-op for backward compatibility. The kind of tables implemented by this class never need rehashing.
     *
     * <P>If you need to reduce the table size to fit exactly this set, use {@link #trim()}.
     *
     * @return true.
     * @see #trim()
     * @deprecated A no-op. */
    @Deprecated
    public boolean rehash() {
        return true;
    }

    /** Rehashes the map, making the table as small as possible.
     *
     * <P>This method rehashes the table to the smallest size satisfying the load factor. It can be used when the set will not be changed anymore, so to optimize access speed and size.
     *
     * <P>If the table size is already the minimum possible, this method does nothing.
     *
     * @return true if there was enough memory to trim the map.
     * @see #trim(int) */
    public boolean trim() {
        final int l = arraySize( size, f );
        if ( l >= n || size > maxFill( l, f ) ) return true;
        try {
            rehash( l );
        }
        catch ( OutOfMemoryError cantDoIt ) {
            return false;
        }
        return true;
    }

    /** Rehashes this map if the table is too large.
     *
     * <P>Let <var>N</var> be the smallest table size that can hold <code>max(n,{@link #size()})</code> entries, still satisfying the load factor. If the current table size is smaller than or equal to
     * <var>N</var>, this method does nothing. Otherwise, it rehashes this map in a table of size <var>N</var>.
     *
     * <P>This method is useful when reusing maps. {@linkplain #clear() Clearing a map} leaves the table size untouched. If you are reusing a map many times, you can call this method with a typical
     * size to avoid keeping around a very large table just because of a few large transient maps.
     *
     * @param n the threshold for the trimming.
     * @return true if there was enough memory to trim the map.
     * @see #trim() */
    public boolean trim( final int n ) {
        final int l = HashCommon.nextPowerOfTwo( (int)Math.ceil( n / f ) );
        if ( this.n <= l ) return true;
        try {
            rehash( l );
        }
        catch ( OutOfMemoryError cantDoIt ) {
            return false;
        }
        return true;
    }

    /** Rehashes the map.
     *
     * <P>This method implements the basic rehashing strategy, and may be overriden by subclasses implementing different rehashing strategies (e.g., disk-based rehashing). However, you should not
     * override this method unless you understand the internal workings of this class.
     *
     * @param newN the new size */

    protected void rehash( final int newN ) {
        final long key[] = this.key;
        final long value[] = this.value;
        final int mask = newN - 1; // Note that this is used by the hashing macro
        final long newKey[] = new long[ newN + 1 ];
        final long newValue[] = new long[ newN + 1 ];
        int i = n, pos;
        for ( int j = realSize(); j-- != 0; ) {
            while ( ( ( key[ --i ] ) == ( 0 ) ) );
            if ( !( ( newKey[ pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( key[ i ] ) ) & mask ] ) == ( 0 ) ) ) while ( !( ( newKey[ pos = ( pos + 1 ) & mask ] ) == ( 0 ) ) );
            newKey[ pos ] = key[ i ];
            newValue[ pos ] = value[ i ];
        }
        newValue[ newN ] = value[ n ];
        n = newN;
        this.mask = mask;
        maxFill = maxFill( n, f );
        this.key = newKey;
        this.value = newValue;
    }

    /** Returns a deep copy of this map.
     *
     * <P>This method performs a deep copy of this hash map; the data stored in the map, however, is not cloned. Note that this makes a difference only for object keys.
     *
     * @return a deep copy of this map. */

    public Long2LongOpenHashMap clone() {
        Long2LongOpenHashMap c;
        try {
            c = (Long2LongOpenHashMap)super.clone();
        }
        catch ( CloneNotSupportedException cantHappen ) {
            throw new InternalError();
        }
        c.keys = null;
        c.values = null;
        c.entries = null;
        c.containsNullKey = containsNullKey;
        c.key = key.clone();
        c.value = value.clone();
        return c;
    }

    /** Returns a hash code for this map.
     *
     * This method overrides the generic method provided by the superclass. Since <code>equals()</code> is not overriden, it is important that the value returned by this method is the same value as
     * the one returned by the overriden method.
     *
     * @return a hash code for this map. */
    public int hashCode() {
        int h = 0;
        for ( int j = realSize(), i = 0, t = 0; j-- != 0; ) {
            while ( ( ( key[ i ] ) == ( 0 ) ) )
                i++;
            t = it.unimi.dsi.fastutil.HashCommon.long2int( key[ i ] );
            t ^= it.unimi.dsi.fastutil.HashCommon.long2int( value[ i ] );
            h += t;
            i++;
        }
        // Zero / null keys have hash zero.
        if ( containsNullKey ) h += it.unimi.dsi.fastutil.HashCommon.long2int( value[ n ] );
        return h;
    }

    private void writeObject( java.io.ObjectOutputStream s ) throws java.io.IOException {
        final long key[] = this.key;
        final long value[] = this.value;
        final MapIterator i = new MapIterator();
        s.defaultWriteObject();
        for ( int j = size, e; j-- != 0; ) {
            e = i.nextEntry();
            s.writeLong( key[ e ] );
            s.writeLong( value[ e ] );
        }
    }

    private void readObject( java.io.ObjectInputStream s ) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        n = arraySize( size, f );
        maxFill = maxFill( n, f );
        mask = n - 1;
        final long key[] = this.key = new long[ n + 1 ];
        final long value[] = this.value = new long[ n + 1 ];
        long k;
        long v;
        for ( int i = size, pos; i-- != 0; ) {
            k = s.readLong();
            v = s.readLong();
            if ( ( ( k ) == ( 0 ) ) ) {
                pos = n;
                containsNullKey = true;
            }
            else {
                pos = (int)it.unimi.dsi.fastutil.HashCommon.mix( ( k ) ) & mask;
                while ( !( ( key[ pos ] ) == ( 0 ) ) )
                    pos = ( pos + 1 ) & mask;
            }
            key[ pos ] = k;
            value[ pos ] = v;
        }
        if ( ASSERTS ) checkTable();
    }

    private void checkTable() {}
}