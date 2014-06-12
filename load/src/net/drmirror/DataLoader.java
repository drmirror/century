package net.drmirror;

import static net.drmirror.Util.createPoint;
import static net.drmirror.Util.generateStationId;
import static net.drmirror.Util.parseInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteError;
import com.mongodb.BulkWriteException;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class DataLoader {

    private static class RecordParser {

        // had to encapsulate this because SimpleDateFormat is not thread-safe 
        private SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");

        private Parser addParser = new Parser.MasterParser();
        
        public RecordParser() {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        
        public BasicDBObject parseRecord (String line) {
            String usaf = line.substring(4,10);
            String wban = line.substring(10,15);
            Date ts = null;
            try {
              ts = df.parse(line.substring(15,27));
            } catch (ParseException ex) {
              throw new RuntimeException(ex);
            }
            String source = line.substring(27,28);
            String latStr = line.substring(28,34);
            Integer lat = parseInt(latStr);
            String lonStr = line.substring(34,41);
            Integer lon = parseInt(lonStr);
            
            String type = line.substring(41,46).trim();
            String elevStr = line.substring(46,51);
            int elev = parseInt(elevStr);
            
            String call = line.substring(51,56).trim();
            String qc = line.substring(56,60);
            
            String tempStr = line.substring(87,92);
            String tempQuality = line.substring(92,93);
            double temp = (double)parseInt(tempStr) / 10.0;

            String dewStr = line.substring(93,98);
            String dewQuality = line.substring(98,99);
            double dew = (double)parseInt(dewStr) / 10.0;

            String pressStr = line.substring(99,104);
            String pressQuality = line.substring(104,105);
            double press = (double)parseInt(pressStr) / 10.0;
            
            int wd = parseInt(line.substring(60,63));
            String wdq = line.substring(63,64);
            String wt = line.substring(64,65);
            double ws = (double)parseInt(line.substring(65,69)) / 10.0;
            String wsq = line.substring(69,70);
            
            int skyh = parseInt(line.substring(70,75));
            String skyq = line.substring(75,76);
            String skyd = line.substring(76,77);
            String skyc = line.substring(77,78);

            int visd = parseInt(line.substring(78,84));
            String visdq = line.substring(84,85);
            String visv = line.substring(85,86);
            String visvq = line.substring(86,87);
            
            String st = generateStationId(usaf, wban, latStr, lonStr);
            BasicDBObject d = new BasicDBObject();
            d.append("_id", new BasicDBObject("st", st).append("ts", ts));
            if (!"999999".equals(usaf)) { d.append("usaf",usaf); }
            if (!"99999".equals(wban))  { d.append("wban",wban); }
            BasicDBObject p = createPoint(lon, lat);
            if (p != null) d.append ("position", p);
            d.append("elevation", elev);
            d.append("callLetters", call);
            d.append("qualityControlProcess", qc);
            
            d.append("dataSource", source).append ("type", type);
            
            d.append("airTemperature", new BasicDBObject("value",temp).append("quality",tempQuality))
              .append("dewPoint", new BasicDBObject("value",dew).append("quality",dewQuality))
              .append("pressure", new BasicDBObject("value",press).append("quality", pressQuality))
              .append("wind", new BasicDBObject("direction", 
                                 new BasicDBObject("angle", wd).append("quality",wdq))
                              .append("type",wt)
                              .append("speed",new BasicDBObject ("rate",ws)
                                                    .append("quality",wsq)))
              .append("visibility", new BasicDBObject("distance",
                                     new BasicDBObject ("value", visd)
                                         . append ("quality", visdq))
                                    .append ("variability",
                                       new BasicDBObject("value", visv)
                                         .append("quality", visvq)))
              .append("skyCondition", new BasicDBObject("ceilingHeight",
                                        new BasicDBObject("value", skyh)
                                         .append("quality", skyq)
                                         .append("determination", skyd))
                                      .append("cavok", skyc));
            
            if (line.length() > 108 && line.substring(105,108).equals("ADD"))
                addParser.parse(line, 108, d);
            
            return d;
        }
             
    }
    
    private static abstract class Loader extends Thread {
        
        private int numSplits = 1;
        private int mySplit = 0;
        
        private int batchSize = 10000;
        private boolean useBulk = true;
        private RecordParser parser = new RecordParser();
        
        private List<BasicDBObject> buffer;
        private List<BasicDBObject> duplicates;
        protected DBCollection data;

        public Loader(MongoClient client) {
            this (client, 1000);
        }
        
        public Loader (MongoClient client, int batchSize) {
            this.batchSize = batchSize;
            DB db = client.getDB("ncdc");
            data = db.getCollection("data");
            buffer = new ArrayList<BasicDBObject>(batchSize);
            duplicates = new ArrayList<BasicDBObject>();
        }

        public void insert (String record) {
            BasicDBObject d = parser.parseRecord(record);
            buffer.add(d);
            if (buffer.size() >= batchSize) {
            	try {
            		if (useBulk) bulkInsert(data, buffer); else plainInsert(data, buffer);
            	} catch (BulkWriteException ex) {
            		List<BulkWriteError> errors = ex.getWriteErrors();
            		for(BulkWriteError error : errors) {
            			if (error.getCode() == 11000) {
            				duplicates.add(buffer.get(error.getIndex()));
            			} else {
            				throw ex;
            			}
            		}
            	} finally {
            		buffer.clear();
            	}
            }
        }
        
        public void finish() {
            if (buffer.size() > 0) {
                if (useBulk) bulkInsert(data, buffer); else plainInsert(data, buffer);
                buffer.clear();
                System.out.println("Duplicates: " + duplicates.size());
            }
        }
        
        private void bulkInsert (DBCollection data, List<BasicDBObject> buffer) {
            BulkWriteOperation op = data.initializeUnorderedBulkOperation();
            for (BasicDBObject o : buffer) {
                op.insert(o);
            }
            op.execute();
        }

        private void plainInsert (DBCollection data, List<BasicDBObject> buffer) {
            data.insert (buffer.toArray(new BasicDBObject[]{}));
        }
        
        protected void loadFile (String filename) {
            if (filename.matches(".*?-[0-9]-[0-9]$")) {
                int length = filename.length();
                numSplits = Integer.parseInt(filename.substring(length-1));
                mySplit = Integer.parseInt(filename.substring(length-3, length-2));
                filename = filename.substring(0,length-4);
            } else {
                numSplits = 1;
                mySplit = 0;
            }
            try {
                BufferedReader in = filename.endsWith(".gz")
                    ? new BufferedReader (
                          new InputStreamReader (
                              new GZIPInputStream (
                                  new FileInputStream (filename))))
                    : new BufferedReader (new FileReader(filename));
                int i=0;          
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    if (i % numSplits == mySplit) insert(line);
                    i = (i+1) % numSplits;
                }
                in.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

    }
    
    private static class PoolLoader extends Loader {

        private FilePool pool = null;
        
        public PoolLoader(MongoClient client, FilePool pool) {
            this (client, pool, 1000);
        }
        
        public PoolLoader(MongoClient client, FilePool pool, int batchSize) {
            super(client, batchSize);
            this.pool = pool;
        }
        
        @Override
        public void run() {
            while (true) {
                String filename = pool.getFile();
                if (filename == null) break;
                System.out.println(filename);
                loadFile (filename);
            }
            finish();
        }
    }
    
    private static class FileLoader extends Loader {
     
        private String filename;
        
        public FileLoader (MongoClient c, String filename, int batchSize) {
            super (c, batchSize);
            this.filename = filename;
        }

        public void run() {
            System.out.println(filename);
            loadFile (filename);
            finish();
        }
        
    }
    
    private static abstract class FilePool {
        
        protected String dir;
        protected List<String> files;
        
        protected FilePool (String dir, List<String> files) {
            this.dir = dir;
            this.files = new LinkedList<String>(files);
        }
        
        public abstract String getFile();
        
    }
    
    private static class RandomFilePool extends FilePool {
        
        // The last file in the list is about 100 times larger than the other ones.
        // This flag indicates that this file should be processed first, to achieve
        // maximum parallelism on that one.
        private boolean lastFileFirst = false;
        
        public RandomFilePool (String dir, List<String> files, int numThreads) {
            super (dir, files);
            if (numThreads > 1) {
                lastFileFirst = true;
                splitShips (numThreads);
            }
        }
        
        private void splitShips (int numThreads) {
            int numSplits = Math.min(numThreads, 8);
            String shipFile = files.get(files.size()-1);
            if (shipFile.startsWith("999999-99999-")) {
                files.remove(files.size()-1);
                for (int i=0; i<numSplits; i++) {
                    files.add(shipFile + "-" + i + "-" + numSplits);
                }
            }
        }
        
        public synchronized String getFile() {
            if (!files.isEmpty()) {
                String result = null;
                if (lastFileFirst) {
                    result = files.get(files.size()-1);
                    if (result.startsWith("999999-99999-")) {
                        files.remove (files.size()-1);
                        return dir + "/" + result;
                    } else {
                        lastFileFirst = false;
                    }
                }
                int index = (int)(Math.random() * (double)files.size());
                result = files.get(index);
                files.remove(index);
                return dir + "/" + result;
            } else {
                return null;
            }
        }
        
    }
    
    
    private static class SequentialFilePool extends FilePool {
        
        public SequentialFilePool (String dir, List<String> files, boolean notUsed) {
            super (dir, files);
        }
        
        public synchronized String getFile() {
            if (!files.isEmpty()) {
                return dir + "/" + files.remove(0);
            } else {
                return null;
            }
        }
        
    }
    
    
    public static void main (String[] args) throws Exception {

        String dirname = args.length > 0 ? args[0] : ".";
        int numThreads = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        int batchSize  = args.length > 2 ? Integer.parseInt(args[2]) : 400;

        //MongoClientOptions options = MongoClientOptions.builder()
        //  .writeConcern(WriteConcern.UNACKNOWLEDGED).build();
        MongoClient c = new MongoClient("localhost");

        File dir = new File(dirname);
        String[] flist = dir.list();
        List<String> files = new LinkedList<String>(Arrays.asList(flist));
        Collections.sort(files);

        FilePool pool = new RandomFilePool(dirname, files, numThreads);
        List<Loader> loaders = new ArrayList<Loader>();
        for (int i=0; i<numThreads; i++) {
            loaders.add(new PoolLoader(c, pool, batchSize));
        }
        
        for (Loader l : loaders) l.start();
        for (Loader l : loaders) l.join();
        
        c.close();
    }
    
}
