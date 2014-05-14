package net.drmirror;

import static net.drmirror.Util.createPoint;
import static net.drmirror.Util.generateStationId;
import static net.drmirror.Util.parseDate;
import static net.drmirror.Util.parseInt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class StationLoader {

    public static BasicDBObject decodeStation(String line) {
        String usaf = line.substring(0,6);
        String wban = line.substring(7,12);
        String name = line.substring(13,42).trim();
        String ctry = line.substring(46,48).trim();
        String latStr = line.substring(58,64);
        Integer lat  = parseInt(latStr);
        String lonStr = line.substring(65,72);
        Integer lon  = parseInt(lonStr);
        Integer elev = parseInt(line.substring(73,79));
        Date    begin = parseDate(line.substring(83,91));
        Date    end   = parseDate(line.substring(92,100));
        
        BasicDBObject d = new BasicDBObject();
        d.append("st", generateStationId (usaf, wban, latStr, lonStr));
        if (!usaf.equals("999999")) d.append("usaf", usaf);
        if (!wban.equals("99999")) d.append("wban", wban);
        d.append ("name", name);
        d.append ("country", ctry);
        BasicDBObject pos = createPoint (lon, lat);
        if (pos != null) d.append ("position", pos);
        if (elev != null && elev != -99999) d.append("elevation", (double)elev/10.0);
        if (begin != null) d.append ("begin", begin);
        if (end != null) d.append ("end", end);
        return d;
    }
    
    public static void main(String[] args) throws Exception {
        
//        MongoClient c = MongoClients.create(new ServerAddress("localhost", 27017));
//        MongoDatabase db = c.getDatabase("ncdc");
//        MongoCollection<BasicDBObject> coll = db.getCollection("station");       
        
        MongoClient c = new MongoClient("localhost");
        DB db = c.getDB("ncdc");
        DBCollection coll = db.getCollection("station");
        
        BufferedReader in = new BufferedReader 
                (new FileReader("/Users/drmirror/Downloads/ncdc/ish-history.dat"));
        in.readLine(); // header
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            BasicDBObject d = decodeStation (line);
            if (d != null) coll.insert(d);
        }
        
        in.close();
        c.close();
    }
    
}
