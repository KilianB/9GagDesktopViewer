package de.ipatexi.GagDesktopApp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A bidirectional map allowing to lookup keys and values. 
 * Keys AND values have to be unique. 
 * 
 * 
 * @author Kilian
 *
 * @param <K>
 * @param <V>
 */
public class BidirectionalHashMap<K,V> implements Map<K,V> {

	private Map<K,V> map;
	private Map<V,K> reverseMap;
	
	
	public BidirectionalHashMap() {
		this(16,0.75f);
	}
	
	public BidirectionalHashMap(int initialCapacity) {
		this(initialCapacity,0.75f);
	}
	
	public BidirectionalHashMap(int initialCapacity,float loadFactor) {
		this.map = new HashMap<>(initialCapacity,loadFactor);
		this.reverseMap = new HashMap<>(initialCapacity,loadFactor);
	}
	
	
	
	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return reverseMap.containsKey(value);
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}
	
	public K getKey(Object value) {
		return reverseMap.get(value);
	}

	@Override
	public V put(K key, V value) {
		//TODO this will break if we have duplicate values ...?
		reverseMap.remove(value);
		reverseMap.put(value, key);
		return map.put(key, value);
	}

	@Override
	public V remove(Object key) {
		if(map.containsKey(key)) {
			V removedValue = map.remove(key);
			reverseMap.remove(removedValue);
			return removedValue;
		}
		return null;
	}

	public K removeKey(Object value) {
		if(reverseMap.containsKey(value)) {
			K removedValue = reverseMap.remove(value);
			map.remove(removedValue);
			return removedValue;
		}
		return null;
	}
	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.entrySet().forEach(e -> put(e.getKey(),e.getValue()));
	}

	@Override
	public void clear() {
		map.clear();
		reverseMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return reverseMap.keySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}
}
