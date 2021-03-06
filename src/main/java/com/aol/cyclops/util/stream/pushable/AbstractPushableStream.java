package com.aol.cyclops.util.stream.pushable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jooq.lambda.tuple.Tuple2;

import com.aol.cyclops.data.async.Adapter;
import com.aol.cyclops.data.async.Queue;

public abstract class AbstractPushableStream<T,X extends Adapter<T>,R extends Stream<T>> extends Tuple2<X,R> {

	
	public AbstractPushableStream(X v1,R v2) {
		super(v1, v2);
	}
	public X getInput(){
		return v1;
	}
	public R getStream(){
		return v2;
	}
	
	public <U> U visit(BiFunction<? super X,? super R,? extends U> visitor){
	    return visitor.apply(v1, v2);
	}
	
	public void peekStream(Consumer<? super R> consumer){
	    consumer.accept(v2);
	}
	
	public void peekInput(Consumer<? super X> consumer){
	    consumer.accept(v1);
	}

	private static final long serialVersionUID = 1L;

}