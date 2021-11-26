/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

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
public class SmallTablesBenchmark {

    
    
    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        @Param({ "1", "4", "8" })
        public int threads;

        @Param({ "true", "false" })
        public boolean memalloc;
        int chunk = 25;
        int dataSize = 64*4*chunk;
        int patternSize = 80;

        int length = 50; 
        
        Map<Pair<Integer, Integer>, double[]> patterns;
        double[][] data;

        @Setup(Level.Invocation)
        public void setUp() {
            patterns = makePatterns(patternSize, length);
            data = makeData(dataSize, length);
        }
    }
    
    //@Benchmark
    public long tablesRun(ExecutionPlan params, Blackhole blackHole) {
        

        List<Callable<List<Pair<Pair<Integer,Integer>,Double>>>> tasks = new ArrayList<>();
        
        for (int ix = 0; ix < params.data.length; ix+=params.chunk) {
            
            final int start = ix;
            final int end = Math.min(ix+params.chunk, params.data.length);
            tasks.add( () -> calculate(params.data, start, end, params.patterns, params.memalloc));
        }
        
        Pair<Long, Double> res = clockTasks(tasks, params.threads);
        blackHole.consume(res);
        return res.getFirst();
    }    

    Pair<Long, Double> clockTasks(List<Callable<List<Pair<Pair<Integer,Integer>,Double>>>> tasks, int threads) {
        
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        
        long sT = System.currentTimeMillis();
        
        List<Future<List<Pair<Pair<Integer, Integer>, Double>>>> results = tasks.stream()
                .map( task -> pool.submit(task))
                .collect(Collectors.toList());
        
        
        pool.shutdown();
        try {
            
            double val = 0;
            
            for (Future<List<Pair<Pair<Integer, Integer>, Double>>> f : results) {
                
                val+=f.get().stream().mapToDouble( p -> p.getValue()).sum();
            }
            
            long dur = System.currentTimeMillis() - sT;
            return new Pair<>(dur, val);
        } catch (InterruptedException| ExecutionException e) {
            pool.shutdownNow();
            throw new RuntimeException("Interrupted analysis in parallel "+e.getMessage(),e);
        }
        
    }    
    
    List<Pair<Pair<Integer,Integer>,Double>> calculate(double[][] datas, int startIx, int endIx, Map<Pair<Integer, Integer>, 
            double[]> patterns, boolean memalloc) {
        
        List<Pair<Pair<Integer,Integer>,Double>> pairs = new ArrayList<>();
        
        for (int i =startIx; i< endIx; i++) {
            double[] data = datas[i];
            
            Pair<Pair<Integer,Integer>,Double> res = patterns.entrySet().stream()
                    .map( e -> new Pair<>(e.getKey(), convolute(data, e.getValue(), memalloc)))
                    .max(Comparator.comparing(p -> p.getValue()))
                    .get();
            pairs.add(res);
        }
        return pairs;
    }
    
    double convolute(double[] data, double[] pattern, boolean memalloc) {
        double val = 0;
        
        double[][] joined = new double[data.length][];
        
        for (int i = 0;i<data.length;i++) {
            double x  = Math.cos(data[i]);
            double y  = Math.cos(pattern[i]);
            val += x * y;
            
            if (!memalloc) {
                x = Math.cos(data[data.length-1-i]);
                y = Math.cos(pattern[data.length-1-i]);
                val += x * y;
            } else {            
                double[] row = {x, y}; // {Math.cos(data[i]), Math.cos(pattern[i])};
                joined[i] = row;
            }
        }
        
        for (int i = 0; i < joined.length; i++) {
            val+=joined[i] != null ? joined[i][0] : 0;
        }
        return val;
    }  
    
    static Map<Pair<Integer, Integer>, double[]> makePatterns(int count, int length) {
        
        Random rnd = new Random();
        Map<Pair<Integer, Integer>, double[]> patterns = new HashMap<>();
        
        while (patterns.size() < count) {
            Pair<Integer,Integer> key = new Pair<>(rnd.nextInt(100), rnd.nextInt(100));
            double[] val = new double[length];
            for (int i = 0; i< length; i++) val[i] = 0.1+rnd.nextDouble()*10;
            patterns.put(key, val);
        }
        
        return patterns;
    }
    
    static double[][] makeData(int count, int length) {

        Random rnd = new Random();
        double[][] data = new double[count][];
        
        for (int i = 0; i< count; i++) {
            double[] row = new double[length];
            for (int k =0;k < length; k++) row[k] = 0.1+rnd.nextDouble()*10;
            data[i] = row;
        }
        return data;
    }    
}
