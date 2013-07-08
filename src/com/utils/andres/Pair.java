package com.utils.andres;

public class Pair<T, K> {

	T p1;
	K p2;

	public Pair(T p1, K p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public T getFirst(){
		return p1;
	}
	
	public K getSecond(){
		return p2;
	}

}
