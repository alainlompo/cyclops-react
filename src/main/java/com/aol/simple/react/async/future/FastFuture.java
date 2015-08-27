package com.aol.simple.react.async.future;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import uk.co.real_logic.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import uk.co.real_logic.agrona.concurrent.OneToOneConcurrentArrayQueue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;

import com.aol.cyclops.lambda.utils.ExceptionSoftener;
import com.aol.simple.react.exceptions.FilteredExecutionPathException;
/*
 * @author John McClean
 * assumptions
 * 1. only on thread may join at a time
 * 2. only map / mapAsync/ exceptionally/ allOf and anyOf are neccessary
 */
@AllArgsConstructor
public class FastFuture<T> {

	@Getter
	private volatile boolean done=false;
	private boolean isFirstAsync=false;
	@Getter
	private boolean completedExceptionally=false;
	private Object result = UNSET;
	private Object exception = UNSET;
	private static UnSet UNSET = new UnSet();
	static class UnSet{}
	@Wither
	public ExecutionPipeline builder;
	FinalPipeline pipeline;

	volatile int count=0;
	private final int max;
	
	
	
	public FastFuture(){
		max=0;
		this.builder = new ExecutionPipeline();
		this.pipeline = null;
	}
	private T result(){
		while(result==UNSET){
			Thread.yield();
		}
		return (T)result;	
	}
	private Throwable exception(){
		while(exception==UNSET){
			Thread.yield();
		}
		return (Throwable)exception;	
	}
	
	private FastFuture(FinalPipeline pipeline,int max){
		this.max = max;
		this.builder = new ExecutionPipeline();
		this.pipeline = pipeline;
	}
	public void await(){
		
		long spin=1;
		while(!done){
			LockSupport.parkNanos(spin++);
		}
		
	}
	public T join(){
	
		long spin=1;
		while(!done){
			LockSupport.parkNanos(spin++);
		}
		if(completedExceptionally)
			ExceptionSoftener.singleton.factory.getInstance().throwSoftenedException(new CompletionException(exception()));
		return result();
	}
	public static <T> FastFuture<T> completedFuture(T value){
		FastFuture<T> f = new FastFuture();
		f.result =value;
		f.done=true;
		return f;
	}
	
	public CompletableFuture<T> toCompletableFuture(){
		CompletableFuture<T> f = new CompletableFuture<>();
		this.peek(i->f.complete(i));
		return f;
	}
	public FastFuture<T> populateFromCompletableFuture(CompletableFuture<T> cf){
		cf.thenAccept(i->this.set(i));
		cf.exceptionally(t-> {
			completedExceptionally(t);
			return join();
		});
		return this;
	}
	
	private void completedExceptionally(Throwable t){

		
		for(int i =0;i<this.pipeline.firstRecover.length;i++){
			try{
				 this.set((T)pipeline.firstRecover[i].apply(t));
				
				
			}catch(Throwable e){
				this.exception =e;
			}
		}
		if(exception==UNSET)
			exception =t;
		this.completedExceptionally=true;
		this.done =true;
		throw (RuntimeException)exception();
	}
	
	public static <T>FastFuture<T> fromCompletableFuture(CompletableFuture<T> cf){
		FastFuture<T> f = new FastFuture<>();
		cf.thenAccept(i->f.set(i));
		cf.exceptionally(t-> {
			f.completedExceptionally(t);
			return f.join();
		});
		return f;
	}
	
	
	
	public static <R> FastFuture<List<R>> allOf(FastFuture... futures){
		
		FastFuture allOf = new FastFuture(FinalPipeline.empty(),futures.length);
		
		
		for(FastFuture next : futures){
			next.peek(v->{ 
					allOf.count++;
					if(allOf.count==allOf.max){
						List res = new ArrayList(futures.length);
						for(FastFuture resNext : futures)
							res.add(resNext.result());
						allOf.result =res;
						allOf.done();
					}
					
			});
		}
		return allOf;
	}
	public static <R> FastFuture<List<R>> anyOf(FastFuture... futures){
		
		FastFuture anyOf = new FastFuture();
		
		for(FastFuture next : futures){
			next.onComplete(v->{ 
				anyOf.result = true;
				anyOf.done();
			  	
			});
		}
		return anyOf;
	}
	
	
	
	public void set(T result){
	
		try{
			
			final Object use = result;
			if(pipeline.functions.length==0){
				done();
				this.result = (T)use;
				return;
			}
			Function op = pipeline.functions[0];
			if(this.isFirstAsync){
				
				this.pipeline.executors[0].execute(()->{
					set(()->(T)op.apply(use),1);
				});
				
			}else{
				set(()->(T)op.apply(use),1);
			
			}
		}catch(Throwable t){
			
			exception = t;
			completedExceptionally =true;
			handleOnComplete(true);
			done=true;
			
			
		}
		
	}
	public void set(Supplier<T> result,int index){
		try{
			
			Object current = result.get();
			
		//	current = op.apply(current);
			
			
			final Object use = current;
			if(index<pipeline.functions.length){
					Function op = pipeline.functions[index];
					this.pipeline.executors[index].execute(()->{
						set(()->(T)op.apply(use),index+1);
					});
					return;
			}
			
			this.result = current;
			done();
			
			
		}catch(Throwable t){
			
			exception = t;
			completedExceptionally =true;
			handleOnComplete(true);
			done=true;
				
		}
		
		
	}
	private boolean done(){
		handleOnComplete(true);
		this.completedExceptionally=false;
		return this.done =true;
		
	}
	
	public void clearFast() {
		result =UNSET;
		this.done=false;	
	}
	
	public void cancel(boolean cancel){
		
	}
	public <R> FastFuture<R> thenCompose(Function<T,CompletableFuture<R>> fn){
		return (FastFuture)this.withBuilder(builder.thenCompose((Function)fn));
	}
	public <R> FastFuture<R> thenComposeAsync(Function<T,CompletableFuture<R>> fn,Executor exec){
		return (FastFuture)this.withBuilder(builder.thenComposeAsync((Function)fn, exec));
	}
	public <R> FastFuture<R> thenApplyAsync(Function<T,R> fn,Executor exec){
		
		return (FastFuture)this.withBuilder(builder.thenApplyAsync(fn, exec));
	}
	public  FastFuture<T> peek(Consumer<T> c){
		
		return (FastFuture)this.withBuilder( builder.peek(c));
		
	}
	public <R> FastFuture<R> thenApply(Function<T,R> fn){
		
		return  (FastFuture)this.withBuilder(builder.thenApply(fn));

	}
	
	public <X extends Throwable> FastFuture<T> exceptionally(Function<X,T> fn){
		
		return (FastFuture)this.withBuilder(builder.exceptionally(fn));
	}
	public <X extends Throwable> FastFuture<T> whenComplete(BiConsumer<T,X> fn){
		return (FastFuture)this.withBuilder(builder.whenComplete(fn));
	}
	public FastFuture<T> build() {
		
		return new FastFuture(this.builder.toFinalPipeline(),0);
	}
	
	OneToOneConcurrentArrayQueue<Consumer<OnComplete>> queue = new OneToOneConcurrentArrayQueue<>(5);
	Consumer<OnComplete> essential;
	
	public void essential(Consumer<OnComplete> fn){
		this.essential=fn;
	}
	public void onComplete(Consumer<OnComplete> fn){
		while(!queue.offer(fn))
			queue.poll();
		if(done)
			handleOnComplete(false);
	}
	private void handleOnComplete(boolean force){
		
		if( ( !queue.isEmpty() || essential!=null) && (force || done)){
			OnComplete c = new OnComplete(!completedExceptionally && done ? result() : null,
					completedExceptionally ? exception() : null,this.completedExceptionally);
			if(essential!=null){
				essential.accept(c);
				essential =null;
			}
			while(!queue.isEmpty()){
				Consumer<OnComplete>  next = queue.poll();
				if(next!=null)
					next.accept(c);
			}
			
		}
	
	}
	@AllArgsConstructor
	public static class OnComplete{
		public final Object result;
		public final Throwable exception;
		public final boolean exceptionally;
	}
	
}