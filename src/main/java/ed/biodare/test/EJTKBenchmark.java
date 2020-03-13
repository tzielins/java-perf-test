

package ed.biodare.test;

import ed.biodare.rhythm.ejtk.BD2JTK;
import ed.biodare.rhythm.ejtk.BD2eJTK;
import ed.biodare.rhythm.ejtk.BD2eJTKRes;
import ed.biodare.rhythm.ejtk.CopyingBD2JTK;
import ed.biodare.rhythm.ejtk.IdentityBD2JTK;
import ed.biodare.rhythm.ejtk.JTKPatterns;
import ed.biodare.rhythm.ejtk.ListBD2JTK;
import ed.biodare.rhythm.ejtk.patterns.JTKPattern;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
public class EJTKBenchmark {

    
    
    @State(Scope.Benchmark)
    public static class ExecutionPlan {

        //@Param({ "1", "4", "8", "16", "32", "64" })
        @Param({ "12", "16", "24" })
        public int threads;

        int dataSize = 64*5*50;

        int length = 50; 
        
        List<JTKPattern> patterns;
        double[][] data;
        double[] zts;

        @Setup(Level.Invocation)
        public void setUp() {
            patterns = JTKPatterns.eJTKClassic();
            data = makeData(dataSize, length);
            zts = makeTimes(0, length, 1);
        }
    }
    
    //@Benchmark
    public List<BD2eJTKRes> eJTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        BD2eJTK analyser = new BD2eJTK(params.threads);
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }    

    //@Benchmark
    public List<BD2eJTKRes> JTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        BD2JTK analyser = new BD2JTK(params.threads);
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }     
    
    //@Benchmark
    public List<BD2eJTKRes> NoRefCopyingJTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        CopyingBD2JTK analyser = new CopyingBD2JTK(params.threads);
        analyser.cpyReferences = false;
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }     
    
    //@Benchmark
    public List<BD2eJTKRes> OnlyRefCopyingJTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        CopyingBD2JTK analyser = new CopyingBD2JTK(params.threads);
        analyser.cpyReferences = true;
        analyser.cpyZts = false;
        analyser.cpySeries = false;
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }    
    
    //@Benchmark
    public List<BD2eJTKRes> FullCopyingJTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        BD2JTK analyser = new CopyingBD2JTK(params.threads);
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }    

    @Benchmark
    public List<BD2eJTKRes> ListJTKRunNoCpy(ExecutionPlan params, Blackhole blackHole) {
        

        ListBD2JTK analyser = new ListBD2JTK(params.threads);
        analyser.cpySeries = false;
        analyser.cpyZts = false;
        analyser.cpyReferences = false;
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    } 
    
    @Benchmark
    public List<BD2eJTKRes> ListJTKRunFullCpy(ExecutionPlan params, Blackhole blackHole) {
        

        ListBD2JTK analyser = new ListBD2JTK(params.threads);
        
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }     
    
    @Benchmark
    public List<BD2eJTKRes> ListJTKRunRefCpy(ExecutionPlan params, Blackhole blackHole) {
        

        ListBD2JTK analyser = new ListBD2JTK(params.threads);
        analyser.cpySeries = false;
        analyser.cpyZts = false;
        analyser.cpyReferences = true;
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    } 
    
    @Benchmark
    public List<BD2eJTKRes> ListJTKRunRefCpyData(ExecutionPlan params, Blackhole blackHole) {
        

        ListBD2JTK analyser = new ListBD2JTK(params.threads);
        analyser.cpySeries = false;
        analyser.cpyZts = false;
        analyser.cpyReferences = true;
        analyser.cpyRefPatterns = false;
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    }     
    
    
    //@Benchmark
    public List<BD2eJTKRes> IdentityJTKRun(ExecutionPlan params, Blackhole blackHole) {
        

        BD2JTK analyser = new IdentityBD2JTK(params.threads);
        
        List<BD2eJTKRes> results = analyser.analyseData(params.data, params.zts, params.patterns);
        blackHole.consume(results);
        return results;
    } 
    
    static double[] makeTimes(int start, int end, int stepH) {
        
        return IntStream.range(0, end)
                .mapToDouble( i -> start+i*stepH)
                .filter( t -> t < end)
                .toArray();
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
