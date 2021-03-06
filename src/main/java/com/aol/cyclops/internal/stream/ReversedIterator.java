package com.aol.cyclops.internal.stream;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.util.stream.Streamable;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReversedIterator<U> implements Streamable<U>{
	
	private final List<U> list;
	
	public List<U> getValue(){
		return  list;
	}
	
	public ReactiveSeq<U> stream(){
		return ReactiveSeq.fromIterator(reversedIterator());
	}
	public Iterator<U> reversedIterator(){
		
		ListIterator<U> iterator = list.listIterator(list.size());
	
		return new Iterator<U>(){

			@Override
			public boolean hasNext() {
				return iterator.hasPrevious();
			}

			@Override
			public U next() {
				return iterator.previous();
			}
			
		};
	}
		
		
	
}
