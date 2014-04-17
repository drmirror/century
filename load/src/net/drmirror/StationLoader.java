package net.drmirror;

import static net.drmirror.Util.createPoint;
import static net.drmirror.Util.generateStationId;
import static net.drmirror.Util.parseInt;
import static net.drmirror.Util.parseDate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;

import org.mongodb.Document;
import org.mongodb.MongoClient;
import org.mongodb.MongoClients;
import org.mongodb.MongoCollection;
import org.mongodb.MongoDatabase;
import org.mongodb.connection.ServerAddress;


public class StationLoader {

    public static Document decodeStation(String line) {
        String usaf = line.substring(0,6);
        String wban = line.substring(7,12);
        String name = line.substring(13,42).trim();
        String ctry = line.substring(46,48).trim();
        Integer lat  = parseInt(line.substring(58,64));
        Integer lon  = parseInt(line.substring(65,72));
        Integer elev = parseInt(line.substring(73,79));
        Date    begin = parseDate(line.substring(83,91));
        Date    end   = parseDate(line.substring(92,100));
        
        Document d = new Document();
        d.append("st", generateStationId (usaf, wban, lat, lon));
        if (!usaf.equals("999999")) d.append("usaf", usaf);
        if (!wban.equals("99999")) d.append("wban", wban);
        d.append ("name", name);
        d.append ("country", ctry);
        Document pos = createPoint (lon, lat);
        if (pos != null) d.append ("position", pos);
        if (elev != null && elev != -99999) d.append("elevation", (double)elev/10.0);
        if (begin != null) d.append ("begin", begin);
        if (end != null) d.append ("end", end);
        return d;
    }
    
    public static void main(String[] args) throws Exception {
        
        MongoClient c = MongoClients.create(new ServerAddress("localhost", 27017));
        MongoDatabase db = c.getDatabase("ncdc");
        MongoCollection<Document> coll = db.getCollection("station");       
        
        BufferedReader in = new BufferedReader 
                (new FileReader("/Users/drmirror/Downloads/ncdc/ish-history.dat"));
        in.readLine(); // header
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            Document d = decodeStation (line);
            if (d != null) coll.insert(d);
        }
        
        in.close();
        c.close();
    }
    
}
