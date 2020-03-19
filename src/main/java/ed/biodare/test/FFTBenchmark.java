

package ed.biodare.test;

import ed.biodare.period.fft_nlls.FFTMultiAnalyser2;
import ed.robust.dom.data.TimeSeries;
import ed.robust.dom.tsprocessing.PPAResult;
import ed.robust.util.timeseries.TSGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
public class FFTBenchmark {

    
    
    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        //@Param({ "1", "4", "8", "16", "32", "64" })
        @Param({"16", "32" })
        public int threads;

        @Param({ "10" })
        int chunkSize = 10;
        int dataSize = 64*5*10;

        int length = 10*24; 
        
        List<TimeSeries> data;

        @Setup(Level.Invocation)
        public void setUp() {
            data = makeData(dataSize, length);
        }
    }
    
    
    //@Benchmark
    public List<PPAResult> fft2(ExecutionPlan params, Blackhole blackHole) {
        
        FFTMultiAnalyser2 analyser = new FFTMultiAnalyser2(params.threads);
        
        List<PPAResult> results = analyser.analyse(params.data, 20, 28, params.chunkSize);
        blackHole.consume(results);
        return results;                 
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
