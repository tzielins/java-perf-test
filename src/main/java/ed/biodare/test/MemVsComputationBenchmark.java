

package ed.biodare.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Pair;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemVsComputationBenchmark {

    
    
    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        @Param({ "1", "4", "8"})
        public int threads;

        @Param({ "false", "true" })
        public boolean memalloc;
        int chunk = 50;
        int length = 50; 
        int patternSize = 50;
        int dataSize = 64*5*chunk;
        
        List<double[]> patterns;
        List<double[]> data;

        @Setup(Level.Invocation)
        public void setUp() {
            patterns = makeDatas(patternSize, length);
            data = makeDatas(dataSize, length);
        }
    }
    
    //@Benchmark
    public long tablesCalc(ExecutionPlan params, Blackhole blackHole) {
        

        List<Callable<Double>> tasks = new ArrayList<>();
        
        for (int ix = 0; ix < params.data.size()-params.chunk; ix+= params.chunk) {
            List<double[]> part = params.data.subList(ix, ix+params.chunk);
            tasks.add( () -> convolute(part, params.patterns, params.memalloc));
        }
        
        Pair<Long, Double> res = clockTasks(tasks, params.threads);
        blackHole.consume(res);
        return res.getFirst();
    }    

    Pair<Long, Double> clockTasks(List<Callable<Double>> tasks, int threads) {
        
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        
        long sT = System.currentTimeMillis();
        
        List<Future<Double>> results = tasks.stream()
                .map( task -> pool.submit(task))
                .collect(Collectors.toList());
        
        
        pool.shutdown();
        try {
            
            double val = 0;
            
            for (Future<Double> f : results) {
                
                val+=f.get();
            }
            
            long dur = System.currentTimeMillis() - sT;
            return new Pair<>(dur, val);
        } catch (InterruptedException| ExecutionException e) {
            pool.shutdownNow();
            throw new RuntimeException("Interrupted analysis in parallel "+e.getMessage(),e);
        }
        
    }    
    

    
    double convolute(List<double[]> datas, List<double[]> patterns, boolean memalloc) {
        
        Random rnd = new Random();
        double val = 0;
        
        double[][] joined = new double[datas.get(0).length][];
        
        for (double[] pattern : patterns) {
            for (double[] data : datas) {
                for (int i = 0; i< data.length; i++) {
                    double x  = Math.cos(data[i]);
                    double y  = Math.cos(pattern[i]);
                    val += x * y;

                    if (!memalloc) {
                        x = Math.cos(data[data.length-1-i]);
                        y = Math.cos(pattern[data.length-1-i]);
                        val += x * y;
                    } else {            
                        double[] row = {x, y}; 
                        joined[i] = rnd.nextBoolean() ? row : joined[i];
                    }
                }
            }
        }
        
        for (int i = 0; i < joined.length; i++) {
            val+=joined[i] != null ? joined[i][0]*joined[i][1] : 0;
        }
        return val;
    }  
    

    
    static double[] makeData(int length) {

        Random rnd = new Random();
        double[] data = new double[length];
        
        for (int i = 0; i< data.length; i++) {
            data[i] = rnd.nextDouble()*100;
        }
        return data;
    } 
    
    static List<double[]> makeDatas(int size, int length) {

        List<double[]> datas = new ArrayList<>(size);
        
        for (int i = 0; i< size; i++) {
            datas.add(makeData(length));
        }
        return datas;
    }     
}
