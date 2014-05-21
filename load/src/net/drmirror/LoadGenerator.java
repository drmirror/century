package net.drmirror;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.TreeMultiset;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class LoadGenerator extends Thread {

    public static enum StationInterval {
        SINGLE, REGION, ALL
    }
    
    public static class SIConverter implements IStringConverter<StationInterval> {
        public StationInterval convert (String data) {
            return Enum.valueOf(StationInterval.class, data);
        }
    }
    
    public static enum TimeInterval {
        POINT, FUZZ, HOUR, DAY, MONTH, YEAR
    }
    
    public static final int FUZZ_SECONDS = 300;

    public static class TIConverter implements IStringConverter<TimeInterval> {
        public TimeInterval convert (String data) {
            return Enum.valueOf(TimeInterval.class, data);
        }
    }

    public static class DateConverter implements IStringConverter<Date> {
        public Date convert (String data) {
            try {
                return dateFormat.parse(data);
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    @Parameter(names = "--mongoHost")
    private static String mongoHost = "century-one";
    private static MongoClient mongoClient;
    
    private static final double NANOS_PER_MILLI = 1000000.0;
    
    static {
        try {
            mongoClient = new MongoClient(mongoHost);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private List<String> stations = new ArrayList<String>();

    public static SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyyMMdd");
    
    @Parameter(names="--stMin")
    private String stMin = "u720000";
    
    @Parameter(names="--stMax")
    private String stMax = "u730000";

    @Parameter(names="--tsMin", converter = DateConverter.class)
    private Date tsMin;
    
    @Parameter(names="--tsMax", converter = DateConverter.class)
    private Date tsMax;
    
    @Parameter(names="--stInterval", converter = SIConverter.class)
    private StationInterval stInterval = StationInterval.SINGLE;

    @Parameter(names="--tsInterval", converter = TIConverter.class)
    private TimeInterval tsInterval = TimeInterval.POINT;
    
    private DB db;
    private DBCollection data;
    
    @Parameter(names="--delay")
    private long queryDelayMillis = 100;

    @Parameter(names="--jitter")
    private long queryJitterMillis = 5;
    
    @Parameter(names="--statDelay")
    private long statDelayMillis = 5000;
    
    // statistics
    
    private Object statLock = new Object();
    
    private TreeMultiset<Long> nanos = TreeMultiset.create();
    private long sumNanos = 0;
    private int numNanos = 0;
    private long minNanos = Long.MAX_VALUE;
    private long maxNanos = 0;
    private long emaNanos = -1;
    private static final double emaFactor = 0.9;
    
    public LoadGenerator() {
        try {
            this.tsMin = dateFormat.parse("20130101");
            this.tsMax = dateFormat.parse("20140101");
            this.reportStats = true;
        } catch (ParseException ex) {
            throw new RuntimeException (ex);
        }
    }
    
    public LoadGenerator(boolean reportStats) {
        this();
        this.reportStats = reportStats;
    }
    
    private void initConnection() {
        db = mongoClient.getDB("ncdc");
        data = db.getCollection("data");
    }

    public void initStations() {
        stations = new ArrayList<String>();
        DBCollection station = db.getCollection("station");
        DBCursor c = station.find(new BasicDBObject("begin", 
                                    new BasicDBObject ("$lte", tsMax))
                                  .append("end",
                                      new BasicDBObject ("$gte", tsMin)))
                            .sort(new BasicDBObject("st", 1));
        for (DBObject o : c) {
            String st = (String)o.get("st");
            if (   st.compareTo(stMin) >= 0
                && st.compareTo(stMax) < 0) {
                stations.add(st);
            }
        }
        // System.out.printf("%d stations\n", stations.size());
    }

    private Calendar calendar = new GregorianCalendar (TimeZone.getTimeZone("UTC"));
    
    private static class Range<T> { public T min, max; }
    
    private Range<Date> getTsRange() {
        Range<Date> result = new Range<Date>();
        long start = tsMin.getTime();
        long interval = tsMax.getTime() - tsMin.getTime();
        Date time = new Date (start + (long)(Math.random() * interval));
        calendar.setTime(time);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);

        switch (tsInterval) {
        case YEAR:  calendar.set(Calendar.MONTH, 0);
        case MONTH: calendar.set(Calendar.DAY_OF_MONTH, 1);
        case DAY:   calendar.set(Calendar.HOUR_OF_DAY, 0);
                    break;
        case HOUR:  break;
        case FUZZ:  calendar.add(Calendar.SECOND, -FUZZ_SECONDS); break;
        case POINT: break;
        }
        result.min = calendar.getTime();

        switch (tsInterval) {
        case YEAR:  calendar.add(Calendar.YEAR, 1);
                    break;
        case MONTH: calendar.add(Calendar.MONTH, 1);
                    break;
        case DAY:   calendar.add(Calendar.DATE, 1);
                    break;
        case HOUR:  calendar.add(Calendar.HOUR, 1);
                    break;
        case FUZZ:  calendar.add(Calendar.SECOND, 2*FUZZ_SECONDS);
                    break;
        case POINT: break;
        }
        result.max = calendar.getTime();
        
        return result;
    }
    
    private Range<String> getStRange() {
        Range<String> result = new Range<String>();
        if (stInterval == StationInterval.ALL) {
            return null;
        }    
        int stationIndex = (int)(Math.random() * stations.size());
        String station = stations.get(stationIndex);
        if (stInterval == StationInterval.REGION) {
            result.min = station.substring(0, 3) + "0000";
            result.max = station.substring(0, 2) + (char)(station.charAt(2)+1) + "0000";
        } else {
            result.min = station;
            result.max = station;
        }
        return result;
    }
    
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private long query() {
        Range<String> stRange = getStRange();
        Range<Date> tsRange = getTsRange();

//        if (stRange == null) {
//            System.out.print("st:     ALL        ");
//        } else {
//            System.out.print("st: " + stRange.min + " " + stRange.max + "  ");
//        }
//        System.out.print("ts: " + df.format(tsRange.min) + "  " + df.format(tsRange.max));
        
        BasicDBObject query = tsRange.min.equals(tsRange.max) 
                            ? new BasicDBObject("ts", tsRange.min)
                            : new BasicDBObject("ts", new BasicDBObject("$gte", tsRange.min)
                                                           .append("$lt",  tsRange.max));
        if (stRange != null) {
            query = stRange.min.equals(stRange.max)
                  ? query.append("st", stRange.min)
                  : query.append("st", new BasicDBObject("$gte", stRange.min)
                                                 .append("$lt", stRange.max));
        }
        
        long startTime = System.nanoTime();
        DBCursor c = data.find (query);
        List<DBObject> x = c.toArray(); // drain cursor
        long endTime = System.nanoTime();

//        System.out.println("  " + c.length());
        
        return endTime - startTime;
    }
    
    private void updateStats (long nano) {
        synchronized (statLock) {
            nanos.add(nano);
            sumNanos += nano;
            numNanos += 1;
            if (nano < minNanos) minNanos = nano;
            if (nano > maxNanos) maxNanos = nano;
            if (emaNanos == -1)
                emaNanos = nano;
            else
                emaNanos = (long)(emaFactor * (double)emaNanos + (1.0-emaFactor) * (double)nano);
        }
    }
    
    private void printStatsHeader() {
         System.out.println("\n     n      avg      ema      min      max     95th     99th");   
    }
    
    private void printStats() {
        long n;
        double avg, ema, min, max, p95, p99;
        synchronized (statLock) {
            n   = numNanos;
            avg = ((double)sumNanos / (double)numNanos) / NANOS_PER_MILLI;
            ema = emaNanos / NANOS_PER_MILLI;
            min = minNanos / NANOS_PER_MILLI;
            max = maxNanos / NANOS_PER_MILLI;
            Pair p = getPercentiles (95, 99);
            p95 = p.a / NANOS_PER_MILLI;
            p99 = p.b / NANOS_PER_MILLI;
        }
        
        System.out.printf("%6d %8.3f %8.3f %8.3f %8.3f %8.3f %8.3f\n", 
                          n, avg, ema, min, max, p95, p99);    
    }
    
    private static class Pair {
        public long a, b;
    }
    
    private Pair getPercentiles (int percentA, int percentB) {
        synchronized (statLock) {
            Pair result = new Pair();
            int indexA = (int)(nanos.size() * ((double)percentA / 100.0));
            int indexB = (int)(nanos.size() * ((double)percentB / 100.0));
            Iterator<Long> it = nanos.iterator();
            int i = 0;
            while (it.hasNext()) {
                long currentNano = it.next();
                if (i == indexA) { result.a = currentNano; }
                if (i == indexB) { result.b = currentNano; break; }
                i++;
            }
            return result;
        }
        
    }

    private static void sl_e_e_p (long millis) {
        try {
            Thread.sleep(millis);
            
        } catch (InterruptedException ex) {
        }
    }
    
    private class StatReporter extends Thread {
        
        public void run() {
            int i=0;
            while (true) {
                sl_e_e_p(statDelayMillis);
                if (i % 10 == 0) printStatsHeader();
                printStats();
                i++;
            }
        }
    }

    @Parameter(names="--numClients")
    private static int numClients = 1;
    
    private boolean reportStats = false;
    
    public void run() {
        initConnection();
        initStations();
        if (reportStats) {
            StatReporter sr = new StatReporter();
            sr.start();
        }
        while (true) {
            updateStats (query());
            double delay =      (queryDelayMillis - queryJitterMillis)
                          + 2 * (queryJitterMillis * Math.random());
            sl_e_e_p((long)delay);
        }
    }
    
    public static void main (String[] args) throws Exception {
        LoadGenerator g = new LoadGenerator(true);
        JCommander jc = new JCommander(g);
        jc.parse(args);
        
        List<LoadGenerator> others = new ArrayList<LoadGenerator>();
        for (int i=1; i<numClients; i++) {
            LoadGenerator l = new LoadGenerator(false);
            JCommander c = new JCommander(l);
            c.parse(args);
            others.add (l);
        }
        
        for (LoadGenerator x : others) {
            x.start();
        }
        g.start();
    }
    
}
