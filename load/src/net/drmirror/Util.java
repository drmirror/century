package net.drmirror;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.mongodb.Document;

public class Util {

    private static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    
    public static Date parseDate (String str) {
        if (!str.matches("[0-9]+")) return null;
        int year = Integer.parseInt (str.substring(0,4));
        int month = Integer.parseInt(str.substring(4,6));
        int day = Integer.parseInt(str.substring(6,8));
        synchronized (calendar) {
            calendar.set(year, month-1, day);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        }
    }
    
    public static Integer parseInt(String str) {
        if (!str.matches("[-+0-9]+")) return null;
        if (str.startsWith("+")) {
            return Integer.parseInt(str.substring(1));
        } else {
            return Integer.parseInt(str);
        }
    }
    
    public static String generateStationId (String usaf, String wban, String lat, String lon) {
        if (!"999999".equals(usaf)) {
            return "u" + usaf;
        } else if (!"99999".equals(wban)) {
            return "w" + wban;
        } else if (lat != null && lon != null) {
            return "x" + lat + lon;
        } else {
            return "unknown";
        }
    } 
    
    public static Document createPoint (Integer lon, Integer lat) {
        if (lon == null || lat == null) return null;
        if (lon < -180000 || lon > 180000
          || lat < -90000 || lat > 90000) return null; 
        Document result = new Document("type", "Point");
        result.append ("coordinates", Arrays.asList(lon/1000.0, lat/1000.0));
        return result;
    }
    
    public static void main (String[] args) {
        Document d = new Document();
        d.append("a.b", 1);
        System.out.println(d);
    }
    
}
