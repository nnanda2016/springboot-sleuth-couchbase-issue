package com.demo.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO: Add a description
 * 
 * @author Niranjan Nanda
 */
public enum InMemoryKeyValueStore {
	INSTANCE(new MapMaker()
			.concurrencyLevel(Runtime.getRuntime().availableProcessors()) // # of concurrent segments = available threads
			.initialCapacity(Runtime.getRuntime().availableProcessors() * 10) // Each segment will have 10 elements
			.makeMap());
	
	private final ConcurrentMap<Object, Object> store;
	
	private InMemoryKeyValueStore(final ConcurrentMap<Object, Object> store) {
		this.store = store;
	}
	
	/**
	 * Puts the key-value pair in the cache.
	 *
	 * @param key An object which will be stored as Key.
	 * @param value An object which will be stored as value against the key.
	 */
	public void put(final Object key, final Object value) {
		Objects.requireNonNull(key, "Provided key is null.");
		
		this.store.put(key, new ValueWrapper(value));
	}

	/**
	 * Retrieves the value for the given key.
	 *
	 * @param key The key against which the value will be searched.
	 * @return The value stored against the given key.
	 */
	public Object get(final Object key) {
		if (key != null) {
			final ValueWrapper valueWrapper = (ValueWrapper) this.store.get(key);
			if (valueWrapper != null && !valueWrapper.isStoredValueNull()) {
				return valueWrapper.getValue();
			}
		}
		
		return null;
	}

	/**
	 * Checks if the cache contains any object with provided key;
	 *
	 * @param key The key against which the cache will be checked for the presence of any value.
	 * @return TRUE if cache contains such a key, FALSE otherwise.
	 */
	public boolean containsKey(final Object key) {
		return key == null ? false : this.store.containsKey(key);
	}

	/**
	 * Deletes the value for the given key.
	 * 
	 * @param key	The key for which the value will be deleted.
	 * @return	TRUE if value is deleted; FALSE otherwise.
	 */
	public boolean delete(final Object key) {
		Objects.requireNonNull(key, "Provided key is null.");
		return this.store.remove(key) != null;
	}
	
	public Map<Object, Object> contentMap() {
		return ImmutableMap.copyOf(store);
	}
	
	/**
	 * This wrapper class was created to allow storing NULL values
	 * in the store (because {@link ConcurrentMap} does not allow
	 * null values.
	 * 
	 * @author Niranjan Nanda
	 */
	private final class ValueWrapper {
		private final Object value;
		private final boolean storedValueNull;
		
		private ValueWrapper(final Object value) {
			this.value = value;
			this.storedValueNull = value == null;
		}
		
		/**
		 * Returns the value of value.
		 *
		 * @return the value
		 */
		Object getValue() {
			return value;
		}

		/**
		 * Returns the value of isValueNull.
		 *
		 * @return the isValueNull
		 */
		boolean isStoredValueNull() {
			return storedValueNull;
		}
	}
}
