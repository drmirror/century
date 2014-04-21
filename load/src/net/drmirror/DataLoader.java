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

import org.mongodb.Document;
import org.mongodb.MongoClient;
import org.mongodb.MongoClients;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.mongodb.connection.ServerAddress;


public class DataLoader {

    private static class RecordParser {

        // had to encapsulate this because SimpleDateFormat is not thread-safe 
        private SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");

        private Parser addParser = new Parser.MasterParser();
        
        public RecordParser() {
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        
        public Document parseRecord (String line) {
            String usaf = line.substring(4,10);
            String wban = line.substring(10,15);
            Date ts = null;
            try {
              ts = df.parse(line.substring(15,27));
            } catch (ParseException ex) {
              throw new RuntimeException(ex);
            }
            String source = line.substring(27,28);
            Integer lat = parseInt(line.substring(28,34));
            Integer lon = parseInt(line.substring(34,41));
            
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
            
            String st = generateStationId(usaf, wban, lat, lon);
            Document d = new Document("st", st);
            d.append("ts", ts);
            if (!"999999".equals(usaf)) d.append("usaf",usaf);
            if (!"99999".equals(wban))  d.append("wban",wban);
            Document p = createPoint(lon, lat);
            if (p != null) d.append ("position", p);
            d.append("elevation", elev);
            d.append("callLetters", call);
            d.append("qualityControlProcess", qc);
            
            d.append("dataSource", source).append ("type", type);
            
            d.append("airTemperature", new Document("value",temp).append("quality",tempQuality))
              .append("dewPoint", new Document("value",dew).append("quality",dewQuality))
              .append("pressure", new Document("value",press).append("quality", pressQuality))
              .append("wind", new Document("direction", 
                                 new Document("angle", wd).append("quality",wdq))
                              .append("type",wt)
                              .append("speed",new Document ("rate",ws)
                                                    .append("quality",wsq)))
              .append("visibility", new Document("distance",
                                     new Document ("value", visd)
                                         . append ("quality", visdq))
                                    .append ("variability",
                                       new Document("value", visv)
                                         .append("quality", visvq)))
              .append("skyCondition", new Document("ceilingHeight",
                                        new Document("value", skyh)
                                         .append("quality", skyq)
                                         .append("determination", skyd))
                                      .append("cavok", skyc));
            
            if (line.length() > 108 && line.substring(105,108).equals("ADD"))
                addParser.parse(line, 108, d);
            
            return d;
        }
             
    }
    
    private static abstract class Loader extends Thread {
        
        private int batchSize = 10000;
        private RecordParser parser = new RecordParser();
        
        private List<Document> buffer;
        protected MongoCollection<Document> data;

        public Loader (MongoClient client) {
            this (client, 1000);
        }
        
        public Loader (MongoClient client, int batchSize) {
            this.batchSize = batchSize;
            MongoDatabase db = client.getDatabase("ncdc");
            data = db.getCollection("data");
            buffer = new ArrayList<Document>(batchSize);
        }

        public void insert (String record) {
            Document d = parser.parseRecord(record);
            buffer.add(d);
            if (buffer.size() >= batchSize) {
                data.insert(buffer);
                buffer.clear();
            }
        }
        
        public void finish() {
            if (buffer.size() > 0) {
                data.insert(buffer);
                buffer.clear();
            }
        }

        protected void loadFile (String filename) {
            try {
                BufferedReader in = filename.endsWith(".gz")
                    ? new BufferedReader (
                          new InputStreamReader (
                              new GZIPInputStream (
                                  new FileInputStream (filename))))
                    : new BufferedReader (new FileReader(filename));
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    insert(line);
                }
                in.close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

    }
    
    private static class PoolLoader extends Loader {

        private FilePool pool = null;
        
        public PoolLoader (MongoClient client, FilePool pool) {
            this (client, pool, 1000);
        }
        
        public PoolLoader (MongoClient client, FilePool pool, int batchSize) {
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
        
        // The last file in the list is about 100 times bigger than the others.
        // This option makes this file the first one to be given to a worker
        // thread, to achieve maximum parallelism on that one.
        private boolean lastFileFirst = false;
        
        public RandomFilePool (String dir, List<String> files, boolean lastFileFirst) {
            super (dir, files);
            this.lastFileFirst = lastFileFirst;
        }
        
        public synchronized String getFile() {
            if (!files.isEmpty()) {
                String result = null;
                if (lastFileFirst) {
                    result = files.get(files.size()-1);
                    files.remove (files.size()-1);
                    lastFileFirst = false;
                } else {
                    int index = (int)(Math.random() * (double)files.size());
                    result = files.get(index);
                    files.remove(index);
                }
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
        int numThreads = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int batchSize  = args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        MongoClient c = MongoClients.create(new ServerAddress("century-standalone",27017));

        File dir = new File(dirname);
        String[] flist = dir.list();
        List<String> files = new LinkedList<String>(Arrays.asList(flist));
        Collections.sort(files);

        FilePool pool = new RandomFilePool(dirname, files, numThreads > 1);
        List<Loader> loaders = new ArrayList<Loader>();
        for (int i=0; i<numThreads; i++) {
            loaders.add(new PoolLoader(c, pool, batchSize));
        }
        
        for (Loader l : loaders) l.start();
        for (Loader l : loaders) l.join();
        
        c.close();
        
    }
    
}
