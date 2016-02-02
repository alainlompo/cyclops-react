package com.aol.cyclops.lambda.monads;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;

import com.aol.cyclops.sequence.HeadAndTail;
import com.aol.cyclops.sequence.HotStream;
import com.aol.cyclops.sequence.Monoid;
import com.aol.cyclops.sequence.SequenceM;
import com.aol.cyclops.sequence.future.FutureOperations;
import com.aol.cyclops.sequence.streamable.Streamable;
import com.aol.cyclops.streams.StreamUtils;

public interface Traversable<T> extends Foldable<T>, Iterable<T> {
	default SequenceM<T> stream(){
		return SequenceM.fromIterable(this);
	}
	/**
	 * Convert to a Stream with the values repeated specified times
	 * 
	 * <pre>
	 * {@code 
	 * 		SequenceM.of(1,2,2)
	 * 								.cycle(3)
	 * 								.collect(Collectors.toList());
	 * 								
	 * 		//List[1,2,2,1,2,2,1,2,2]
	 * 
	 * }
	 * </pre>
	 * 
	 * @param times
	 *            Times values should be repeated within a Stream
	 * @return Stream with values repeated
	 */
	default Traversable<T> cycle(int times){
		return stream().cycle(times);
	}
	
	/**
	 * Convert to a Stream with the result of a reduction operation repeated
	 * specified times
	 * 
	 * <pre>
	 * {@code 
	 *   List<Integer> list = SequenceM.of(1,2,2))
	 *                                 .cycle(Reducers.toCountInt(),3)
	 *                                 .collect(Collectors.toList());
	 *   //List[3,3,3];
	 *   }
	 * </pre>
	 * 
	 * @param m
	 *            Monoid to be used in reduction
	 * @param times
	 *            Number of times value should be repeated
	 * @return Stream with reduced values repeated
	 */
	default Traversable<T> cycle(Monoid<T> m, int times){
		return stream().cycle(m,times);
	}
	
	/**
	 * Repeat in a Stream while specified predicate holds
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	MutableInt count = MutableInt.of(0);
	 * 	SequenceM.of(1, 2, 2).cycleWhile(next -&gt; count++ &lt; 6).collect(Collectors.toList());
	 * 
	 * 	// List(1,2,2,1,2,2)
	 * }
	 * </pre>
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	default Traversable<T> cycleWhile(Predicate<? super T> predicate){
		return stream().cycleWhile(predicate);
	}

	/**
	 * Repeat in a Stream until specified predicate holds
	 * 
	 * <pre>
	 * {@code 
	 * 	MutableInt count =MutableInt.of(0);
	 * 		SequenceM.of(1,2,2)
	 * 		 		.cycleUntil(next -> count.get()>6)
	 * 		 		.peek(i-> count.mutate(i->i+1))
	 * 		 		.collect(Collectors.toList());
	 * 
	 * 		//List[1,2,2,1,2,2,1]	
	 * }
	 * 
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	default Traversable<T> cycleUntil(Predicate<? super T> predicate){
		return stream().cycleUntil(predicate);
	}

	/**
	 * Zip 2 streams into one
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Tuple2&lt;Integer, String&gt;&gt; list = of(1, 2).zip(of(&quot;a&quot;, &quot;b&quot;, &quot;c&quot;, &quot;d&quot;)).toList();
	 * 	// [[1,&quot;a&quot;],[2,&quot;b&quot;]]
	 * }
	 * </pre>
	 * 
	 */
	default <U> Traversable<Tuple2<T, U>> zipStream(Stream<U> other){
		return stream().zipStream(other);
	}

	/**
	 * Zip 2 streams into one
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Tuple2&lt;Integer, String&gt;&gt; list = of(1, 2).zip(of(&quot;a&quot;, &quot;b&quot;, &quot;c&quot;, &quot;d&quot;)).toList();
	 * 	// [[1,&quot;a&quot;],[2,&quot;b&quot;]]
	 * }
	 * </pre>
	 * 
	 */
	default <U> Traversable<Tuple2<T, U>> zip(Seq<U> other){
		return stream().zip(other);
	}

	/**
	 * zip 3 Streams into one
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Tuple3&lt;Integer, Integer, Character&gt;&gt; list = of(1, 2, 3, 4, 5, 6).zip3(of(100, 200, 300, 400), of('a', 'b', 'c')).collect(Collectors.toList());
	 * 
	 * 	// [[1,100,'a'],[2,200,'b'],[3,300,'c']]
	 * }
	 * 
	 * </pre>
	 */
	default <S, U> Traversable<Tuple3<T, S, U>> zip3(Stream<? extends S> second, Stream<? extends U> third){
		return stream().zip3(second, third);
	}

	/**
	 * zip 4 Streams into 1
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Tuple4&lt;Integer, Integer, Character, String&gt;&gt; list = of(1, 2, 3, 4, 5, 6).zip4(of(100, 200, 300, 400), of('a', 'b', 'c'), of(&quot;hello&quot;, &quot;world&quot;))
	 * 			.collect(Collectors.toList());
	 * 
	 * }
	 * // [[1,100,'a',&quot;hello&quot;],[2,200,'b',&quot;world&quot;]]
	 * </pre>
	 */
	default <T2, T3, T4> Traversable<Tuple4<T, T2, T3, T4>> zip4(Stream<T2> second, Stream<T3> third, Stream<T4> fourth){
		return stream().zip4(second, third, fourth);
	}

	/**
	 * Add an index to the current Stream
	 * 
	 * <pre>
	 * {@code 
	 * assertEquals(asList(new Tuple2("a", 0L), new Tuple2("b", 1L)), of("a", "b").zipWithIndex().toList());
	 * }
	 * </pre>
	 */
	default Traversable<Tuple2<T, Long>> zipWithIndex(){
		return stream().zipWithIndex();
	}
	
	/**
	 * Create a sliding view over this Sequence
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;List&lt;Integer&gt;&gt; list = SequenceM.of(1, 2, 3, 4, 5, 6).sliding(2).collect(Collectors.toList());
	 * 
	 * 	assertThat(list.get(0), hasItems(1, 2));
	 * 	assertThat(list.get(1), hasItems(2, 3));
	 * 
	 * }
	 * 
	 * </pre>
	 * 
	 * @param windowSize
	 *            Size of sliding window
	 * @return SequenceM with sliding view
	 */
	default Traversable<List<T>> sliding(int windowSize){
		return stream().sliding(windowSize);
	}

	/**
	 * Create a sliding view over this Sequence
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;List&lt;Integer&gt;&gt; list = SequenceM.of(1, 2, 3, 4, 5, 6).sliding(3, 2).collect(Collectors.toList());
	 * 
	 * 	assertThat(list.get(0), hasItems(1, 2, 3));
	 * 	assertThat(list.get(1), hasItems(3, 4, 5));
	 * 
	 * }
	 * 
	 * </pre>
	 * 
	 * @param windowSize
	 *            number of elements in each batch
	 * @param increment
	 *            for each window
	 * @return SequenceM with sliding view
	 */
	default Traversable<List<T>> sliding(int windowSize, int increment){
		return stream().sliding(windowSize, increment);
	}
	
	/**
	 * Group elements in a Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;List&lt;Integer&gt;&gt; list = SequenceM.of(1, 2, 3, 4, 5, 6).grouped(3).collect(Collectors.toList());
	 * 
	 * 	assertThat(list.get(0), hasItems(1, 2, 3));
	 * 	assertThat(list.get(1), hasItems(4, 5, 6));
	 * 
	 * }
	 * </pre>
	 * 
	 * @param groupSize
	 *            Size of each Group
	 * @return Stream with elements grouped by size
	 */
	default Traversable<List<T>> grouped(int groupSize){
		return stream().grouped(groupSize);
	}

	default <K, A, D> Traversable<Tuple2<K, D>> grouped(Function<? super T, ? extends K> classifier, Collector<? super T, A, D> downstream){
		return stream().grouped(classifier,downstream);
	}

	default <K> Traversable<Tuple2<K, Seq<T>>> grouped(Function<? super T, ? extends K> classifier){
		return stream().grouped(classifier);
	}
	

	/**
	 * Use classifier function to group elements in this Sequence into a Map
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Map&lt;Integer, List&lt;Integer&gt;&gt; map1 = of(1, 2, 3, 4).groupBy(i -&gt; i % 2);
	 * 	assertEquals(asList(2, 4), map1.get(0));
	 * 	assertEquals(asList(1, 3), map1.get(1));
	 * 	assertEquals(2, map1.size());
	 * 
	 * }
	 * 
	 * </pre>
	 */
	default <K> Map<K, List<T>> groupBy(Function<? super T, ? extends K> classifier){
		return stream().groupBy(classifier);
	}

	/*
	 * Return the distinct Stream of elements
	 * 
	 * <pre> {@code List<Integer> list = SequenceM.of(1,2,2,2,5,6) .distinct()
	 * .collect(Collectors.toList()); }</pre>
	 */
	default Traversable<T> distinct(){
		return stream().distinct();
	}
	
	/**
	 * Scan left using supplied Monoid
	 * 
	 * <pre>
	 * {@code  
	 * 
	 * 	assertEquals(asList("", "a", "ab", "abc"),SequenceM.of("a", "b", "c")
	 * 													.scanLeft(Reducers.toString("")).toList());
	 *         
	 *         }
	 * </pre>
	 * 
	 * @param monoid
	 * @return
	 */
	default Traversable<T> scanLeft(Monoid<T> monoid){
		return stream().scanLeft(monoid);
	}

	/**
	 * Scan left
	 * 
	 * <pre>
	 * {@code 
	 *  assertThat(of("a", "b", "c").scanLeft("", String::concat).toList().size(),
	 *         		is(4));
	 * }
	 * </pre>
	 */
	default <U> Traversable<U> scanLeft(U seed, BiFunction<U, ? super T, U> function){
		return stream().scanLeft(seed,function);
	}

	/**
	 * Scan right
	 * 
	 * <pre>
	 * {@code 
	 * assertThat(of("a", "b", "c").scanRight(Monoid.of("", String::concat)).toList().size(),
	 *             is(asList("", "c", "bc", "abc").size()));
	 * }
	 * </pre>
	 */
	default Traversable<T> scanRight(Monoid<T> monoid){
		return stream().scanRight(monoid);
	}

	/**
	 * Scan right
	 * 
	 * <pre>
	 * {@code 
	 * assertThat(of("a", "ab", "abc").map(str->str.length()).scanRight(0, (t, u) -> u + t).toList().size(),
	 *             is(asList(0, 3, 5, 6).size()));
	 * 
	 * }
	 * </pre>
	 */
	default <U> Traversable<U> scanRight(U identity, BiFunction<? super T, U, U> combiner){
		return stream().scanRight(identity,combiner);
	}

	/**
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7)).sorted().toList(),equalTo(Arrays.asList(3,4,6,7))); }
	 * </pre>
	 * 
	 */
	default Traversable<T> sorted(){
		return stream().sorted();
	}

	/**
	 * <pre>
	 * {@code 
	 * 	assertThat(SequenceM.of(4,3,6,7).sorted((a,b) -> b-a).toList(),equalTo(Arrays.asList(7,6,4,3)));
	 * }
	 * </pre>
	 * 
	 * @param c
	 *            Compartor to sort with
	 * @return Sorted Stream
	 */
	default Traversable<T> sorted(Comparator<? super T> c){
		return stream().sorted(c);
	}

	/**
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7).skip(2).toList(),equalTo(Arrays.asList(6,7))); }
	 * </pre>
	 * 
	 * 
	 * 
	 * @param num
	 *            Number of elemenets to skip
	 * @return Stream with specified number of elements skipped
	 */
	default Traversable<T> skip(long num){
		return stream().skip(num);
	}

	/**
	 * 
	 * SkipWhile drops elements from the Stream while the predicate holds, once
	 * the predicte returns true all subsequent elements are included *
	 * 
	 * <pre>
	 * {@code
	 * assertThat(SequenceM.of(4,3,6,7).sorted().skipWhile(i->i<6).toList(),equalTo(Arrays.asList(6,7)));
	 * }
	 * </pre>
	 * 
	 * @param p
	 *            Predicate to skip while true
	 * @return Stream with elements skipped while predicate holds
	 */
	default Traversable<T> skipWhile(Predicate<? super T> p){
		return stream().skipWhile(p);
	}

	/**
	 * Drop elements from the Stream until the predicate returns true, after
	 * which all elements are included
	 * 
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7).skipUntil(i->i==6).toList(),equalTo(Arrays.asList(6,7)));}
	 * </pre>
	 * 
	 * 
	 * @param p
	 *            Predicate to skip until true
	 * @return Stream with elements skipped until predicate holds
	 */
	default Traversable<T> skipUntil(Predicate<? super T> p){
		return stream().skipUntil(p);
	}
	
	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7).limit(2).toList(),equalTo(Arrays.asList(4,3));}
	 * </pre>
	 * 
	 * @param num
	 *            Limit element size to num
	 * @return Monad converted to Stream with elements up to num
	 */
	default Traversable<T> limit(long num){
		return stream().limit(num);
	}

	/**
	 * Take elements from the Stream while the predicate holds, once the
	 * predicate returns false all subsequent elements are excluded
	 * 
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7).sorted().limitWhile(i->i<6).toList(),equalTo(Arrays.asList(3,4)));}
	 * </pre>
	 * 
	 * @param p
	 *            Limit while predicate is true
	 * @return Stream with limited elements
	 */
	default Traversable<T> limitWhile(Predicate<? super T> p){
		return stream().limitWhile(p);
	}

	/**
	 * Take elements from the Stream until the predicate returns true, after
	 * which all elements are excluded.
	 * 
	 * <pre>
	 * {@code assertThat(SequenceM.of(4,3,6,7).limitUntil(i->i==6).toList(),equalTo(Arrays.asList(4,3))); }
	 * </pre>
	 * 
	 * @param p
	 *            Limit until predicate is true
	 * @return Stream with limited elements
	 */
	default Traversable<T> limitUntil(Predicate<? super T> p){
		return stream().limitUntil(p);
	}
	
	/**
	 * True if predicate matches all elements when Monad converted to a Stream
	 * 
	 * <pre>
	 * {@code 
	 * assertThat(SequenceM.of(1,2,3,4,5).allMatch(it-> it>0 && it <6),equalTo(true));
	 * }
	 * </pre>
	 * 
	 * @param c
	 *            Predicate to check if all match
	 */
	default boolean allMatch(Predicate<? super T> c){
		return stream().allMatch(c);
	}

	/**
	 * True if a single element matches when Monad converted to a Stream
	 * 
	 * <pre>
	 * {@code 
	 * assertThat(SequenceM.of(1,2,3,4,5).anyMatch(it-> it.equals(3)),equalTo(true));
	 * }
	 * </pre>
	 * 
	 * @param c
	 *            Predicate to check if any match
	 */
	default boolean anyMatch(Predicate<? super T> c){
		return stream().anyMatch(c);
	}

	/**
	 * Check that there are specified number of matches of predicate in the
	 * Stream
	 * 
	 * <pre>
	 * {@code 
	 *  assertTrue(SequenceM.of(1,2,3,5,6,7).xMatch(3, i-> i>4 ));
	 * }
	 * </pre>
	 * 
	 */
	default boolean xMatch(int num, Predicate<? super T> c){
		return stream().xMatch(num, c);
	}

	/*
	 * <pre> {@code assertThat(of(1,2,3,4,5).noneMatch(it->
	 * it==5000),equalTo(true));
	 * 
	 * } </pre>
	 */
	default boolean noneMatch(Predicate<? super T> c){
		return stream().noneMatch(c);
	}

	/**
	 * <pre>
	 * {@code
	 *  assertEquals("123".length(),SequenceM.of(1, 2, 3).join().length());
	 * }
	 * </pre>
	 * 
	 * @return Stream as concatenated String
	 */
	default String join(){
		return stream().join();
	}

	/**
	 * <pre>
	 * {@code
	 * assertEquals("1, 2, 3".length(), SequenceM.of(1, 2, 3).join(", ").length());
	 * }
	 * </pre>
	 * 
	 * @return Stream as concatenated String
	 */
	default String join(String sep){
		return stream().join(sep);
	}

	/**
	 * <pre>
	 * {@code 
	 * assertEquals("^1|2|3$".length(), of(1, 2, 3).join("|", "^", "$").length());
	 * }
	 * </pre>
	 * 
	 * @return Stream as concatenated String
	 */
	default String join(String sep, String start, String end){
		return stream().join(sep,start,end);
	}

	/**
	 * Extract the minimum as determined by supplied function
	 * 
	 */
	default <C extends Comparable<? super C>> Optional<T> minBy(Function<? super T, ? extends C> f){
		return stream().minBy(f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.stream.Stream#min(java.util.Comparator)
	 */
	default Optional<T> min(Comparator<? super T> comparator){
		return stream().min(comparator);
	}

	/**
	 * Extract the maximum as determined by the supplied function
	 * 
	 */
	default <C extends Comparable<? super C>> Optional<T> maxBy(Function<? super T, ? extends C> f){
		return stream().maxBy(f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.stream.Stream#max(java.util.Comparator)
	 */
	default Optional<T> max(Comparator<? super T> comparator){
		return stream().max(comparator);
	}

	/**
	 * extract head and tail together, where head is expected to be present
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	SequenceM&lt;String&gt; helloWorld = SequenceM.of(&quot;hello&quot;, &quot;world&quot;, &quot;last&quot;);
	 * 	HeadAndTail&lt;String&gt; headAndTail = helloWorld.headAndTail();
	 * 	String head = headAndTail.head();
	 * 	assertThat(head, equalTo(&quot;hello&quot;));
	 * 
	 * 	SequenceM&lt;String&gt; tail = headAndTail.tail();
	 * 	assertThat(tail.headAndTail().head(), equalTo(&quot;world&quot;));
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	default HeadAndTail<T> headAndTail(){
		return stream().headAndTail();
	}
	
	/**
	 * @return First matching element in sequential order
	 * 
	 *         <pre>
	 * {@code
	 * SequenceM.of(1,2,3,4,5).filter(it -> it <3).findFirst().get();
	 * 
	 * //3
	 * }
	 * </pre>
	 * 
	 *         (deterministic)
	 * 
	 */
	default Optional<T> findFirst(){
		return stream().findFirst();
	}

	/**
	 * @return first matching element, but order is not guaranteed
	 * 
	 *         <pre>
	 * {@code
	 * SequenceM.of(1,2,3,4,5).filter(it -> it <3).findAny().get();
	 * 
	 * //3
	 * }
	 * </pre>
	 * 
	 * 
	 *         (non-deterministic)
	 */
	default Optional<T> findAny(){
		return stream().findAny();
	}
	
	/**
	 * 
	 * <pre>
	 * {@code 
	 *  assertTrue(SequenceM.of(1,2,3,4).startsWith(Arrays.asList(1,2,3)));
	 * }
	 * </pre>
	 * 
	 * @param iterable
	 * @return True if Monad starts with Iterable sequence of data
	 */
	default boolean startsWith(Iterable<T> iterable){
		return stream().startsWith(iterable);
	}

	/**
	 * <pre>
	 * {@code assertTrue(SequenceM.of(1,2,3,4).startsWith(Arrays.asList(1,2,3).iterator())) }
	 * </pre>
	 * 
	 * @param iterator
	 * @return True if Monad starts with Iterators sequence of data
	 */
	default boolean startsWith(Iterator<T> iterator){
		return stream().startsWith(iterator);
	}
	/**
	 * <pre>
	 * {@code
	 *  assertTrue(SequenceM.of(1,2,3,4,5,6)
	 * 				.endsWith(Arrays.asList(5,6)));
	 * 
	 * }
	 * 
	 * @param iterable Values to check
	 * @return true if SequenceM ends with values in the supplied iterable
	 */
	default boolean endsWith(Iterable<T> iterable){
		return stream().endsWith(iterable);
	}

	/**
	 * <pre>
	 * {@code
	 * assertTrue(SequenceM.of(1,2,3,4,5,6)
	 * 				.endsWith(Stream.of(5,6))); 
	 * }
	 * </pre>
	 * 
	 * @param stream
	 *            Values to check
	 * @return true if SequenceM endswith values in the supplied Stream
	 */
	default boolean endsWith(Stream<T> stream){
		return stream().endsWith(stream);
	}
	
	/**
	 * Lazily converts this SequenceM into a Collection. This does not trigger
	 * the Stream. E.g. Collection is not thread safe on the first iteration.
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Collection&lt;Integer&gt; col = SequenceM.of(1, 2, 3, 4, 5).peek(System.out::println).toLazyCollection();
	 * 
	 * 	col.forEach(System.out::println);
	 * }
	 * 
	 * // Will print out &quot;first!&quot; before anything else
	 * </pre>
	 * 
	 * @return
	 */
	default Collection<T> toLazyCollection(){
		return stream().toLazyCollection();
	}

	/**
	 * Lazily converts this SequenceM into a Collection. This does not trigger
	 * the Stream. E.g.
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Collection&lt;Integer&gt; col = SequenceM.of(1, 2, 3, 4, 5).peek(System.out::println).toConcurrentLazyCollection();
	 * 
	 * 	col.forEach(System.out::println);
	 * }
	 * 
	 * // Will print out &quot;first!&quot; before anything else
	 * </pre>
	 * 
	 * @return
	 */
	default Collection<T> toConcurrentLazyCollection(){
		return stream().toConcurrentLazyCollection();
	}

	/**
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Streamable&lt;Integer&gt; repeat = SequenceM.of(1, 2, 3, 4, 5, 6).map(i -&gt; i + 2).toConcurrentLazyStreamable();
	 * 
	 * 	assertThat(repeat.sequenceM().toList(), equalTo(Arrays.asList(2, 4, 6, 8, 10, 12)));
	 * 	assertThat(repeat.sequenceM().toList(), equalTo(Arrays.asList(2, 4, 6, 8, 10, 12)));
	 * }
	 * </pre>
	 * 
	 * @return Streamable that replay this SequenceM, populated lazily and can
	 *         be populated across threads
	 */
	default Streamable<T> toConcurrentLazyStreamable(){
		return stream().toConcurrentLazyStreamable();
	}
	
	/**
	 * <pre>
	 * {@code 
	 * 	assertThat(SequenceM.of(1,2,3,4)
	 * 					.map(u->{throw new RuntimeException();})
	 * 					.recover(e->"hello")
	 * 					.firstValue(),equalTo("hello"));
	 * }
	 * </pre>
	 * 
	 * @return first value in this Stream
	 */
	T firstValue();

	/**
	 * <pre>
	 * {@code 
	 *    
	 *    //1
	 *    SequenceM.of(1).single(); 
	 *    
	 *    //UnsupportedOperationException
	 *    SequenceM.of().single();
	 *     
	 *     //UnsupportedOperationException
	 *    SequenceM.of(1,2,3).single();
	 * }
	 * </pre>
	 * 
	 * @return a single value or an UnsupportedOperationException if 0/1 values
	 *         in this Stream
	 */
	default T single() {
		return stream().single();

	}

	T single(Predicate<? super T> predicate);

	/**
	 * <pre>
	 * {@code 
	 *    
	 *    //Optional[1]
	 *    SequenceM.of(1).singleOptional(); 
	 *    
	 *    //Optional.empty
	 *    SequenceM.of().singleOpional();
	 *     
	 *     //Optional.empty
	 *    SequenceM.of(1,2,3).singleOptional();
	 * }
	 * </pre>
	 * 
	 * @return An Optional with single value if this Stream has exactly one
	 *         element, otherwise Optional Empty
	 */
	default Optional<T> singleOptional() {
		return stream().singleOptional();

	}

	/**
	 * Return the elementAt index or Optional.empty
	 * 
	 * <pre>
	 * {@code
	 * 	assertThat(SequenceM.of(1,2,3,4,5).elementAt(2).get(),equalTo(3));
	 * }
	 * </pre>
	 * 
	 * @param index
	 *            to extract element from
	 * @return elementAt index
	 */
	default Optional<T> get(long index){
		return get(index);
	}
	
	
	/**
	 * Returns a stream with a given value interspersed between any two values
	 * of this stream.
	 * 
	 * 
	 * // (1, 0, 2, 0, 3, 0, 4) SequenceM.of(1, 2, 3, 4).intersperse(0)
	 * 
	 */
	default Traversable<T> intersperse(T value){
		return stream().intersperse(value);
	}

	/**
	 * Keep only those elements in a stream that are of a given type.
	 * 
	 * 
	 * // (1, 2, 3) SequenceM.of(1, "a", 2, "b",3).ofType(Integer.class)
	 * 
	 */
	@SuppressWarnings("unchecked")
	<U> Traversable<U> ofType(Class<U> type);

	

	

	/*
	 * Potentially efficient Sequence reversal. Is efficient if
	 * 
	 * - Sequence created via a range - Sequence created via a List - Sequence
	 * created via an Array / var args
	 * 
	 * Otherwise Sequence collected into a Collection prior to reversal
	 * 
	 * <pre> {@code assertThat( of(1, 2, 3).reverse().toList(),
	 * equalTo(asList(3, 2, 1))); } </pre>
	 */
	default Traversable<T> reverse(){
		return stream().reverse();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#shuffle()
	 */
	default Traversable<T> shuffle(){
		return stream().shuffle();
	}
	/**
	 * Access asynchronous terminal operations (each returns a Future)
	 * 
	 * @param exec
	 *            Executor to use for Stream execution
	 * @return Async Future Terminal Operations
	 */
	default FutureOperations<T> futureOperations(Executor exec){
		return stream().futureOperations(exec);
	}
	
	
	/**
	 * assertThat(SequenceM.of(1,2,3,4,5) .skipLast(2)
	 * .collect(Collectors.toList()),equalTo(Arrays.asList(1,2,3)));
	 * 
	 * @param num
	 * @return
	 */
	default Traversable<T> skipLast(int num){
		return stream().skipLast(num);
	}

	/**
	 * Limit results to the last x elements in a SequenceM
	 * 
	 * <pre>
	 * {@code 
	 * 	assertThat(SequenceM.of(1,2,3,4,5)
	 * 							.limitLast(2)
	 * 							.collect(Collectors.toList()),equalTo(Arrays.asList(4,5)));
	 * 
	 * }
	 * 
	 * @param num of elements to return (last elements)
	 * @return SequenceM limited to last num elements
	 */
	default Traversable<T> limitLast(int num){
		return stream().limitLast(num);
	}
	


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#onEmpty(java.lang.Object)
	 */
	default Traversable<T> onEmpty(T value){
		return stream().onEmpty(value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#onEmptyGet(java.util.function.Supplier)
	 */
	default Traversable<T> onEmptyGet(Supplier<T> supplier){
		return stream().onEmptyGet(supplier);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#onEmptyThrow(java.util.function.Supplier)
	 */
	 default <X extends Throwable> Traversable<T> onEmptyThrow(Supplier<X> supplier){
		 return stream().onEmptyThrow(supplier);
	 }
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#shuffle(java.util.Random)
	 */
	default Traversable<T> shuffle(Random random){
		return stream().shuffle(random);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#slice(long, long)
	 */
	default Traversable<T> slice(long from, long to){
		return stream().slice(from, to);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jooq.lambda.Seq#sorted(java.util.function.Function)
	 */
	default <U extends Comparable<? super U>> Traversable<T> sorted(Function<? super T, ? extends U> function){
		return stream().sorted(function);
	}
	
	/**
	 * Generate the permutations based on values in the SequenceM Makes use of
	 * Streamable to store intermediate stages in a collection
	 * 
	 * 
	 * @return Permutations from this SequenceM
	 */
	 default Traversable<SequenceM<T>> permutations(){
		 return stream().permutations();
	 }


	/**
	 * <pre>
	 * {@code
	 *   SequenceM.of(1,2,3).combinations(2)
	 *   
	 *   //SequenceM[SequenceM[1,2],SequenceM[1,3],SequenceM[2,3]]
	 * }
	 * </pre>
	 * 
	 * 
	 * @param size
	 *            of combinations
	 * @return All combinations of the elements in this stream of the specified
	 *         size
	 */
	 default Traversable<SequenceM<T>> combinations(int size){
		 return stream().combinations();
	 }

	/**
	 * <pre>
	 * {@code
	 *   SequenceM.of(1,2,3).combinations()
	 *   
	 *   //SequenceM[SequenceM[],SequenceM[1],SequenceM[2],SequenceM[3].SequenceM[1,2],SequenceM[1,3],SequenceM[2,3]
	 *   			,SequenceM[1,2,3]]
	 * }
	 * </pre>
	 * 
	 * 
	 * @return All combinations of the elements in this stream
	 */
	 default Traversable<SequenceM<T>> combinations(){
		 return stream().combinations();
	 }

	/**
	 * Execute this Stream on a schedule
	 * 
	 * <pre>
	 * {@code
	 *  //run at 8PM every night
	 *  SequenceeM.generate(()->"next job:"+formatDate(new Date()))
	 *            .map(this::processJob)
	 *            .schedule("0 20 * * *",Executors.newScheduledThreadPool(1));
	 * }
	 * </pre>
	 * 
	 * Connect to the Scheduled Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	HotStream&lt;Data&gt; dataStream = SequenceeM.generate(() -&gt; &quot;next job:&quot; + formatDate(new Date())).map(this::processJob)
	 * 			.schedule(&quot;0 20 * * *&quot;, Executors.newScheduledThreadPool(1));
	 * 
	 * 	data.connect().forEach(this::logToDB);
	 * }
	 * </pre>
	 * 
	 * 
	 * 
	 * @param cron
	 *            Expression that determines when each job will run
	 * @param ex
	 *            ScheduledExecutorService
	 * @return Connectable HotStream of output from scheduled Stream
	 */
	default HotStream<T> schedule(String cron, ScheduledExecutorService ex){
		return stream().schedule(cron, ex);
	}

	/**
	 * Execute this Stream on a schedule
	 * 
	 * <pre>
	 * {@code
	 *  //run every 60 seconds after last job completes
	 *  SequenceeM.generate(()->"next job:"+formatDate(new Date()))
	 *            .map(this::processJob)
	 *            .scheduleFixedDelay(60_000,Executors.newScheduledThreadPool(1));
	 * }
	 * </pre>
	 * 
	 * Connect to the Scheduled Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	HotStream&lt;Data&gt; dataStream = SequenceeM.generate(() -&gt; &quot;next job:&quot; + formatDate(new Date())).map(this::processJob)
	 * 			.scheduleFixedDelay(60_000, Executors.newScheduledThreadPool(1));
	 * 
	 * 	data.connect().forEach(this::logToDB);
	 * }
	 * </pre>
	 * 
	 * 
	 * @param delay
	 *            Between last element completes passing through the Stream
	 *            until the next one starts
	 * @param ex
	 *            ScheduledExecutorService
	 * @return Connectable HotStream of output from scheduled Stream
	 */
	default HotStream<T> scheduleFixedDelay(long delay, ScheduledExecutorService ex){
		return stream().scheduleFixedDelay(delay, ex);
	}

	/**
	 * Execute this Stream on a schedule
	 * 
	 * <pre>
	 * {@code
	 *  //run every 60 seconds
	 *  SequenceeM.generate(()->"next job:"+formatDate(new Date()))
	 *            .map(this::processJob)
	 *            .scheduleFixedRate(60_000,Executors.newScheduledThreadPool(1));
	 * }
	 * </pre>
	 * 
	 * Connect to the Scheduled Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	HotStream&lt;Data&gt; dataStream = SequenceeM.generate(() -&gt; &quot;next job:&quot; + formatDate(new Date())).map(this::processJob)
	 * 			.scheduleFixedRate(60_000, Executors.newScheduledThreadPool(1));
	 * 
	 * 	data.connect().forEach(this::logToDB);
	 * }
	 * </pre>
	 * 
	 * @param rate
	 *            Time in millis between job runs
	 * @param ex
	 *            ScheduledExecutorService
	 * @return Connectable HotStream of output from scheduled Stream
	 */
	default HotStream<T> scheduleFixedRate(long rate, ScheduledExecutorService ex){
		return stream().scheduleFixedRate(rate, ex);
	}

	/**
	 * [equivalent to count]
	 * 
	 * @return size
	 */
	default int size() {
		return stream().size();
	}


}
