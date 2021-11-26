

package ed.biodare.test;

import ed.biodare.period.PPAMultiAnalyser;
import ed.biodare.period.epr.EPRMultiAnalyser;
import ed.biodare.period.fft_nlls.FFTMultiAnalyser2;
import ed.biodare.period.lspr.LSPRMultiAnalyser;
import ed.biodare.period.mesa.MESAMultiAnalyser;
import ed.biodare.period.mfourfit.MFourFitMultiAnalyser;
import ed.biodare.period.sr.SRMultiAnalyser;
import ed.robust.dom.data.TimeSeries;
import ed.robust.dom.tsprocessing.PPAResult;
import ed.robust.dom.util.Pair;
import ed.robust.error.RobustFormatException;
import ed.robust.util.timeseries.TSGenerator;
import ed.robust.util.timeseries.TimeSeriesFileHandler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PPABenchmark {


    
    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        @Param({ "1", "4", "8" })
        //@Param({"8","16","24" })
        public int threads;

        @Param({ "10" })
        public int chunkSize = 10;
        public int dataSize = 64*3*10;

        public double periodMin = 18;
        public double periodMax = 36;
        //int length = 10*24; 
        
        List<TimeSeries> data;

        @Setup(Level.Invocation)
        public void setUp() throws RobustFormatException, IOException {
            data = readTSData(dataSize);
            //System.out.println("TS: "+data.size());
        }

        List<TimeSeries> readTSData(int dataSize) throws RobustFormatException, IOException {

            InputStream in = PPABenchmark.class.getResourceAsStream("p24_48-120.csv");
            List<TimeSeries> series = TimeSeriesFileHandler.readFromText(in, ",", 10);
            
            while(series.size() < dataSize) {
                
                List<TimeSeries> cpy = series.stream()
                        .map( ts -> {
                            Pair<double[],double[]> tv = ts.getTimesAndValues();
                            return new TimeSeries(tv.getLeft(), tv.getRight());
                        })
                        .collect(Collectors.toList());
                series.addAll(cpy);
            }
            
            return series.subList(0, dataSize);
        }
    }
    
    //@Benchmark
    public List<PPAResult> fft(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new FFTMultiAnalyser2(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    }
    
    //@Benchmark
    public List<PPAResult> mff(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new MFourFitMultiAnalyser(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    }
    
    //@Benchmark
    public List<PPAResult> mesa(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new MESAMultiAnalyser(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    }    
    
    //@Benchmark
    public List<PPAResult> epr(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new EPRMultiAnalyser(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    } 
    
    //@Benchmark
    public List<PPAResult> lspr(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new LSPRMultiAnalyser(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    }

    //@Benchmark
    public List<PPAResult> sr(ExecutionPlan params, Blackhole blackHole) {
        
        PPAMultiAnalyser analyser = new SRMultiAnalyser(params.threads);
        
        List<PPAResult> results = analyse(analyser, params);
        blackHole.consume(results);
        return results;                 
    }
    
    List<PPAResult> analyse(PPAMultiAnalyser analyser, ExecutionPlan params) {
        
        if (params.data.size() != params.dataSize) {
            throw new IllegalArgumentException("Data Size mismatch: "+params.data.size()+"!="+params.dataSize);
        }
        return analyser.analyse(params.data, params.periodMin, params.periodMax, params.chunkSize, params.threads);
    }
    
    
    static List<TimeSeries> makeData(int count, int length) {

        Random rnd = new Random();
        
        List<TimeSeries> datas = new ArrayList<>();
        
        for (int i=0; i< count; i++) {
            if (rnd.nextBoolean()) {
                double period = 20+rnd.nextInt(8);
                TimeSeries data = TSGenerator.makeCos(length, 1, period, 5,10);
                datas.add(TSGenerator.addNoise(data, 0.25));
            } else {
                TimeSeries data = (TSGenerator.makeLine(length, 1, 0.001, 1));
                datas.add(TSGenerator.addNoise(data, 0.25));
            }
        }
        
        return datas;
    }    
}
