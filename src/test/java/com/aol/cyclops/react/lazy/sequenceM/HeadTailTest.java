package com.aol.cyclops.react.lazy.sequenceM;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import com.aol.cyclops.sequence.HeadAndTail;
import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.react.stream.traits.LazyFutureStream;

public class HeadTailTest {

	@Test
	public void headTailReplay(){
	
		ReactiveSeq<String> helloWorld = LazyFutureStream.of("hello","world","last");
		HeadAndTail<String> headAndTail = helloWorld.headAndTail();
		 String head = headAndTail.head();
		 assertThat(head,equalTo("hello"));
		
		ReactiveSeq<String> tail =  headAndTail.tail();
		assertThat(tail.headAndTail().head(),equalTo("world"));
		
	}
	@Test
	public void headTailOptional(){
	
		ReactiveSeq<String> helloWorld = LazyFutureStream.of();
		Optional<HeadAndTail<String>> headAndTail = helloWorld.headAndTailOptional();
		assertTrue(!headAndTail.isPresent());
		
	}
}
