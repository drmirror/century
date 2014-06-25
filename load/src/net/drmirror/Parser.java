package net.drmirror;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;

/**
 * A parser for the optional part of a weather record, which
 * consists of various sections, or blocks, introduced by a
 * marker header. 
 *
 * @author: Andre Spiegel <andre.spiegel@mongodb.com>
 */
public abstract class Parser {

    public abstract void parse (String data, int index, BasicDBObject d);
    public abstract int endIndex();
    
    public static class MasterParser extends Parser {
        private SkippingParser[] parserArray = new SkippingParser[] {
            new BlockParser(
                "AA4", "liquidPrecipitation",
                new DimensionParser ("period", 3, 5),
                new DimensionParser ("depth", 5, 9),
                new CodeParser      ("condition", 9, 10),
                new CodeParser      ("quality", 10, 11)
            ),
            new BlockParser(
                "AB1", "liquidPrecipitationMonthlyTotal",
                new DimensionParser ("depth", 3, 8),
                new CodeParser ("condition", 8, 9),
                new CodeParser ("quality", 9, 10)
            ),
            new BlockParser(
                "AC1", "precipitationObservationHistory",
                new CodeParser ("duration", 3, 4),
                new CodeParser ("characteristic", 4, 5),
                new CodeParser ("quality", 5, 6)
            ),
            new BlockParser(
                "AD1", "liquidPrecipitationGreatestAmount24HoursMonthly",
                new DimensionParser ("depth", 3, 8),
                new CodeParser ("condition", 8, 9),
                new CodeParser ("occurence1", 9, 13),
                new CodeParser ("occurence2", 13, 17),
                new CodeParser ("occurence3", 17, 21),
                new CodeParser ("quality", 21, 22)
            ),
            new BlockParser(
                "AE1", "liquidPrecipitationNumberOfDaysWithAmount",
                new GroupParser ("days001",
                        new DimensionParser ("value", 3, 5),
                        new CodeParser ("quality", 5, 6)
                ),
                new GroupParser ("days010",
                        new DimensionParser ("value", 6, 8),
                        new CodeParser ("quality", 8, 9)
                ),
                new GroupParser ("days050",
                        new DimensionParser ("value", 9, 11),
                        new CodeParser ("quality", 11, 12)
                ),
                new GroupParser ("days100",
                        new DimensionParser ("value", 12, 14),
                        new CodeParser ("quality", 14, 15)
                )
            ),
            new BlockParser(
                "AG1", "precipitationEstimatedObservation",
                new CodeParser ("discrepancy", 3, 4),
                new DimensionParser ("estimatedWaterDepth", 4, 7)
            ),
            new BlockParser(
                "AH6", "liquidPrecipitationMaxShortDurationMonthly",
                new DimensionParser ("period", 3, 6),
                new DimensionParser ("depth", 10, 6, 10),
                new CodeParser ("condition", 10, 11),
                new CodeParser ("endingDateTime", 11, 17),
                new CodeParser ("quality", 17, 18)
            ),
            new BlockParser(
                "AI6", "liquidPrecipitationMaxShortDurationMonthly",
                new DimensionParser ("period", 3, 6),
                new DimensionParser ("depth", 10, 6, 10),
                new CodeParser ("condition", 10, 11),
                new CodeParser ("endingDateTime", 11, 17),
                new CodeParser ("quality", 17, 18)
            ),
            new BlockParser(
                "AJ1", "snowDepth",
                new GroupParser ("depth",
                    new DimensionParser ("value", 3, 7),
                    new CodeParser ("condition", 7, 8),
                    new CodeParser ("quality", 8, 9)
                ),
                new GroupParser ("equivalentWaterDepth",
                    new DimensionParser ("value", 10, 9, 15),
                    new CodeParser ("condition", 15, 16),
                    new CodeParser ("quality", 16, 17)
                )
            ),
            new BlockParser(
                "AK1", "snowDepthMaxMonthly",
                new DimensionParser ("depth", 3, 7),
                new CodeParser ("condition", 7, 8),
                new CodeParser ("dates", 8, 14),
                new CodeParser ("quality", 14, 15)
            ),
            new BlockParser(
                "AL4", "snowAccumulation",
                new DimensionParser ("period", 3, 5),
                new DimensionParser ("depth", 5, 8),
                new CodeParser ("condition", 8, 9),
                new CodeParser ("quality", 9, 10)
            ),
            new BlockParser(
                "AM1", "snowAccumulationGreatestAmount24HoursMonthly",
                new DimensionParser ("depth", 10, 3, 7),
                new CodeParser ("condition", 7, 8),
                new CodeParser ("occurence1", 8, 12),
                new CodeParser ("occurence2", 12, 16),
                new CodeParser ("occurence3", 16, 20),
                new CodeParser ("quality", 20, 21)
            ),
            new BlockParser(
                "AN1", "snowAccumulationMonthly",
                new DimensionParser ("period", 3, 6),
                new DimensionParser ("depth", 10, 6, 10),
                new CodeParser ("condition", 10, 11),
                new CodeParser ("quality", 11, 12)
            ),
            new BlockParser(
                "AO4", "liquidPrecipitationOccurence",
                new DimensionParser ("period", 3, 5),
                new DimensionParser ("depth", 10, 5, 9),
                new CodeParser ("condition", 9, 10),
                new CodeParser ("quality", 10, 11)
            ),
            new BlockParser(
                    "AP4", "liquidPrecipitation15Minutes",
                    new DimensionParser ("gauge", 10, 3, 7),
                    new CodeParser ("condition", 7, 8),
                    new CodeParser ("quality", 8, 9)
            ),
            new BlockParser(
                    "AU9", "presentWeatherObservationASOS",
                    new CodeParser ("intensityProximity", 3, 4),
                    new CodeParser ("descriptor", 4, 5),
                    new CodeParser ("precipitation", 5, 7),
                    new CodeParser ("obscuration", 7, 8),
                    new CodeParser ("otherWeatherPhenomena", 8, 9),
                    new CodeParser ("combinationIndicator", 9, 10),
                    new CodeParser ("quality", 10, 11)
            ),
            new BlockParser(
                    "AW4", "presentWeatherObservation",
                    new CodeParser ("condition", 3, 5),
                    new CodeParser ("quality", 5, 6)
            ),
            new BlockParser(
                    "AX6", "pastWeatherObservationSummaryOfDay",
                    new GroupParser("atmosphericCondition",
                        new CodeParser("value", 3, 5),
                        new CodeParser("quality", 5, 6)
                    ),
                    new GroupParser("period",
                        new DimensionParser("value", 6, 8),
                        new CodeParser("quality", 8, 9)
                    )
            ),
            new BlockParser(
                    "AY2", "pastWeatherObservationManual",
                    new GroupParser("atmosphericCondition",
                        new CodeParser("value", 3, 4),
                        new CodeParser("quality", 4, 5)
                    ),
                    new GroupParser("period",
                        new DimensionParser("value", 5, 7),
                        new CodeParser("quality", 7, 8)
                    )
            ),
            new BlockParser(
                    "AZ2", "pastWeatherObservation",
                    new GroupParser("atmosphericCondition",
                         new CodeParser("value", 3, 4),
                         new CodeParser("quality", 4, 5)
                    ),
                    new GroupParser("period",
                         new DimensionParser("value", 5, 7),
                         new CodeParser("quality", 7, 8)
                    )
            ),
            new SkippingParser("CB2", 13),
            new SkippingParser("CF3", 9),
            new SkippingParser("CG3", 11),
            new SkippingParser("CH2", 18),
            new SkippingParser("CI1", 31),
            new SkippingParser("CN1", 21),
            new SkippingParser("CN2", 21),
            new SkippingParser("CN3", 19),
            new SkippingParser("CN4", 19),
            new SkippingParser("CO1", 8),
            new SkippingParser("CO9", 11),
            new SkippingParser("CR1", 10),
            new SkippingParser("CT3", 10),
            new SkippingParser("CU3", 16),
            new SkippingParser("CW1", 17),
            new SkippingParser("CX3", 29),
            new BlockParser(
                    "ED1", "runwayVisualRange",
                    new DimensionParser("angle", 3, 5), // SCALING FACTOR .1
                    new CodeParser("designator", 5, 6),
                    new DimensionParser("visibility", 6, 10),
                    new CodeParser("quality", 10, 11)
            ),
            new BlockParser(
                    "GA6", "skyCoverLayer",
                    new GroupParser(
                        "coverage",
                        new CodeParser ("value", 3, 5),
                        new CodeParser ("quality", 5, 6)
                    ),
                    new GroupParser(
                        "baseHeight",
                        new DimensionParser ("value", 6, 12),
                        new CodeParser ("quality", 12, 13)
                    ),
                    new GroupParser(
                        "cloudType",
                        new CodeParser ("value", 13, 15),
                        new CodeParser ("quality", 15, 16)
                    )
            ),
            new BlockParser(
                "GD6", "skyCoverSummationState",
                new GroupParser(
                        "coverage",
                        new CodeParser ("value", 3, 4),
                        new CodeParser ("value2", 4, 6),
                        new CodeParser ("quality", 6, 7)
                ),
                new GroupParser(
                        "height",
                        new DimensionParser("value", 7, 13),
                        new CodeParser ("quality", 13, 14)
                )
            ),
            new BlockParser(
                "GE1", "skyConditionObservationSimple",
                new CodeParser("convectiveCloud", 3, 4),
                new CodeParser("verticalDatum", 4, 10),
                new DimensionParser("baseHeightUpperRange", 10, 16),
                new DimensionParser("baseHeightLowerRange", 16, 22)
            ),
            new BlockParser(
                "GF1", "skyConditionObservation",
                new GroupParser(
                        "totalCoverage",
                        new CodeParser("value", 3, 5),
                        new CodeParser("opaque", 5, 7),
                        new CodeParser("quality", 7, 8)
                ),
                new GroupParser(
                        "lowestCloudCoverage",
                        new CodeParser("value", 8, 10),
                        new CodeParser("quality", 10, 11)
                ),
                new GroupParser(
                        "lowCloudGenus",
                        new CodeParser("value", 11, 13),
                        new CodeParser("quality", 13, 14)
                ),
                new GroupParser(
                        "lowestCloudBaseHeight",
                        new DimensionParser("value", 14, 19),
                        new CodeParser("quality", 19, 20)
                ),
                new GroupParser(
                        "midCloudGenus",
                        new CodeParser("value", 20, 22),
                        new CodeParser("quality", 22, 23)
                ),
                new GroupParser(
                        "highCloudGenus",
                        new CodeParser("value", 23, 25),
                        new CodeParser("quality", 25, 26)
                )
            ),
            new BlockParser (
                "GG6", "belowStationCloudLayer",
                new GroupParser(
                        "coverage",
                        new CodeParser("value", 3, 5),
                        new CodeParser("quality", 5, 6)
                ),
                new GroupParser(
                        "topHeight",
                        new DimensionParser("value", 6, 11),
                        new CodeParser("quality", 11, 12)
                ),
                new GroupParser(
                        "type",
                        new CodeParser("value", 12, 14),
                        new CodeParser("quality", 14, 15)
                ),
                new GroupParser(
                        "top",
                        new CodeParser("value", 15, 17),
                        new CodeParser("quality", 17, 18)
                )
            ),
            new BlockParser(
                "GH1", "hourlySolarRadiation",
                new GroupParser(
                        "average",
                        new DimensionParser("value", 10, 3, 8),
                        new CodeParser("quality", 8, 9),
                        new CodeParser("qualityFlag", 9, 10)
                ),
                new GroupParser(
                        "minimum",
                        new DimensionParser("value", 10, 10, 15),
                        new CodeParser("quality", 15, 16),
                        new CodeParser("qualityFlag", 16, 17)
                ),
                new GroupParser(
                        "maximum",
                        new DimensionParser("value", 10, 17, 22),
                        new CodeParser("quality", 22, 23),
                        new CodeParser("qualityFlag", 23, 24)
                ),
                new GroupParser(
                        "standardDeviation",
                        new DimensionParser("value", 10, 24, 29),
                        new CodeParser("quality", 30, 31),
                        new CodeParser("qualityFlag", 31, 32)
                )
            ),
            new BlockParser(
                "GJ1", "sunshineDuration",
                new DimensionParser ("value", 3, 7),
                new CodeParser ("quality", 7, 8)
            ),
            new BlockParser(
                "GK1", "sunshinePercent",
                new DimensionParser ("value", 3, 6),
                new CodeParser ("quality", 6, 7)
            ),
            new BlockParser(
                "GL1", "sunshineMonth",
                new DimensionParser ("value", 3, 8),
                new CodeParser ("quality", 8, 9)
            ),
            new SkippingParser ("GM1", 33),
            new SkippingParser ("GN1", 31),
            new SkippingParser ("GO1", 22),
            new SkippingParser ("GP1", 34),
            new SkippingParser ("GQ1", 17),
            new BlockParser (
                "GR1", "extraterrestrialRadiation",
                new DimensionParser ("period", 3, 7),
                new GroupParser(
                    "onHorizontalSurface",
                    new DimensionParser ("value", 7, 11),
                    new CodeParser ("quality", 11, 12)
                ),
                new GroupParser(
                    "normalToSun",
                    new DimensionParser ("value", 12, 16),
                    new CodeParser ("quality", 16, 17)
                )
            ),
            new BlockParser (
                "HL1", "hail",
                new DimensionParser ("size", 10, 3, 6),
                new CodeParser ("quality", 6, 7)
            ),
            new SkippingParser ("IA1", 6),
            new SkippingParser ("IA2", 12),
            new SkippingParser ("IB1", 30),
            new SkippingParser ("IB2", 16),
            new SkippingParser ("IC1", 28),
            new BlockParser (
                "KA4", "extremeAirTemperature",
                new DimensionParser ("period", 10, 3, 6),
                new CodeParser ("code", 6, 7),
                new DimensionParser ("value", 10, 7, 12),
                new CodeParser ("quantity", 12, 13)
            ),
            new BlockParser (
                "KB3", "averageAirTemperature",
                new DimensionParser ("period", 3, 6),
                new CodeParser ("code", 6, 7),
                new DimensionParser ("value", 100, 7, 12),
                new DimensionParser ("quantity", 12, 13)
            ),
            new BlockParser (
                "KC2", "extremeAirTemperatureMonth",
                new CodeParser ("code", 3, 4),
                new CodeParser ("condition", 4, 5),
                new DimensionParser ("value", 10, 5, 10),
                new CodeParser ("dates", 10, 16),
                new CodeParser ("quality", 16, 17)
            ),
            new SkippingParser ("KD2", 12),
            new SkippingParser ("KE1", 15),
            new SkippingParser ("KF1", 9),
            new SkippingParser ("KG2", 14),
            new BlockParser (
                "MA1", "atmosphericPressureObservation",
                new GroupParser ("altimeterSetting",
                    new DimensionParser ("value", 10, 3, 8),
                    new CodeParser ("quality", 8, 9)
                ),
                new GroupParser ("stationPressure",
                    new DimensionParser ("value", 10, 9, 14),
                    new CodeParser ("quality", 14, 15)
                )
            ),
            new BlockParser (
                "MD1", "atmosphericPressureChange",
                new GroupParser ("tendency",
                    new CodeParser ("code", 3, 4),
                    new CodeParser ("quality", 4, 5)
                ),
                new GroupParser ("quantity3Hours",
                    new DimensionParser ("value", 10, 5, 8),
                    new CodeParser ("quality", 8, 9)
                ),
                new GroupParser ("quantity24Hours",
                    new DimensionParser ("value", 10, 9, 13),
                    new CodeParser ("quality", 13, 14)
                )
            ),
            new SkippingParser ("ME1", 9),
            new SkippingParser ("MF1", 15),
            new SkippingParser ("MG1", 15),
            new SkippingParser ("MH1", 15),
            new SkippingParser ("MK1", 27),
            new BlockParser (
                "MV7", "presentWeatherInVicinity",
                new CodeParser("condition", 3, 5),
                new CodeParser("quality", 5, 6)
            ),
            new BlockParser (
                "MW7", "presentWeatherObservationManual",
                new CodeParser("condition", 3, 5),
                new CodeParser("quality", 5, 6)
            ),
            new SkippingParser ("OA3", 11),
            new SkippingParser ("OB2", 39),
            new SkippingParser ("OD3", 14),
            new SkippingParser ("OE3", 19),
            new BlockParser (
                "RH3", "relativeHumidity",
                new DimensionParser ("period", 3, 6),
                new CodeParser ("code", 6, 7),
                new DimensionParser ("percentage", 7, 10),
                new CodeParser ("derived", 10, 11),
                new CodeParser ("quality", 11, 12)
            ),
            new BlockParser (
                "SA1", "seaSurfaceTemperature",
                new DimensionParser ("value", 10, 3, 7),
                new CodeParser ("quality", 7, 8)
            ),
            new BlockParser (
                "ST1", "soilTemperature",
                new CodeParser ("type", 3, 4),
                new GroupParser (
                    "temperature",
                    new DimensionParser("value", 10, 4, 9),
                    new CodeParser ("quality", 9, 10)
                ),
                new GroupParser (
                    "depth",
                    new DimensionParser("value", 10, 10, 14),
                    new CodeParser ("quality", 14, 15)
                ),
                new GroupParser (
                    "cover",
                    new CodeParser ("code", 15, 17),
                    new CodeParser ("quality", 17, 18)
                ),
                new GroupParser (
                    "subPlot",
                    new DimensionParser ("number", 18, 19),
                    new CodeParser ("quality", 19, 20)
                )
            ),
            new BlockParser (
                "UA1", "waveMeasurement",
                new CodeParser ("method", 3, 4),
                new GroupParser (
                    "waves",
                    new DimensionParser ("period", 4, 6),
                    new DimensionParser ("height", 10, 6, 9),
                    new CodeParser ("quality", 9, 10)
                ),
                new GroupParser (
                    "seaState",
                    new CodeParser ("code", 10, 12),
                    new CodeParser ("quality", 12, 13)
                )
            ),
            new SkippingParser ("UG2", 12),
            new SkippingParser ("WA1", 9),
            new SkippingParser ("WD1", 23),
            new SkippingParser ("WG1", 14),
            new SkippingParser ("WJ1", 22)
        };

        private Map<String,Parser> parsers = new HashMap<String,Parser>();
        
        private void initParserMap() {
            for (SkippingParser bp : parserArray) {
                String marker = bp.marker();
                String prefix = marker.substring(0,2);
                char n = marker.charAt(2);
                for (char i='1'; i<=n; i++) {
                    if (!parsers.containsKey(prefix + i))
                        parsers.put(prefix + i, bp);
                }
            }
        }
        
        public MasterParser() {
            initParserMap();
        }
        
        public void parse (String data, int index, BasicDBObject d) {
            while (index < data.length()) {
                Parser currentParser = parsers.get(data.substring(index,index+3));
                if (currentParser == null) break;
                currentParser.parse(data,index,d);
                index += currentParser.endIndex();
            }
        }
        public int endIndex() {
            return -1; // we're the only Parser who doesn't have an endIndex
        }
                  
    }

    public static class SkippingParser extends Parser {
        protected String marker;
        protected int endIndex;
        public SkippingParser (String marker, int endIndex) {
            this.marker = marker;
            this.endIndex = endIndex;
        }
        public void parse (String data, int index, BasicDBObject d) {
            appendSection (data, index, d);
            // don't actually parse anything, that's done in the subclass
        }
        public String marker() {
            return marker;
        }
        public int endIndex() {
            return endIndex;
        }
        protected void appendSection (String data, int index, BasicDBObject d) {
            List<Object> sections = (List<Object>)d.get("sections");
            if (sections == null) {
                sections = new ArrayList<Object>();
                d.append("sections",sections);
            }
            sections.add(data.substring(index,index+3));
        }
    }
    
    public static class BlockParser extends SkippingParser {
        private boolean multi;
        private String fieldName;
        private List<Parser> parsers = new ArrayList<Parser>();

        public BlockParser (String marker, String fieldName, Parser... p) {
            super (marker, -1);
            this.multi = marker.charAt(2) > '1';
            this.fieldName = fieldName;
            for (Parser x : p) {
                if(x.endIndex() > this.endIndex) this.endIndex = x.endIndex();
                parsers.add(x);
            }
        }
        public void parse (String data, int index, BasicDBObject d) {
            appendSection(data,index,d);
            BasicDBObject result = new BasicDBObject();
            for (Parser p : parsers) {
                p.parse (data, index, result);
            }
            if (multi) {
                List<Object> l = (List<Object>)d.get(fieldName);
                if (l == null) {
                    l = new ArrayList<Object>();
                    d.append(fieldName, l);
                }
                l.add(result);
            } else {
                d.append(fieldName,result);
            }
        }
    }
    
    public static class GroupParser extends Parser {
        private String fieldName;
        private List<Parser> parsers = new ArrayList<Parser>();
        private int endIndex = -1;
        public GroupParser (String fieldName, Parser... p) {
            this.fieldName = fieldName;
            for (Parser x : p) {
                if (x.endIndex() > endIndex) endIndex = x.endIndex();
                parsers.add(x);
            }
        }
        public void parse (String data, int index, BasicDBObject d) {
            BasicDBObject x = new BasicDBObject();
            d.append(fieldName, x);
            for (Parser p : parsers) p.parse (data, index, x);
        }
        public int endIndex() {
            return endIndex;
        }
    }
    
    public static abstract class ValueParser extends Parser {
        protected int startIndex, endIndex;
        protected String fieldName;
        protected ValueParser (String fieldName, int startIndex, int endIndex) {
            this.fieldName = fieldName;
            this.startIndex = startIndex; this.endIndex = endIndex;
        }
        public int endIndex() {
            return endIndex;
        }
    }
    
    public static class CodeParser extends ValueParser {
        public CodeParser (String fieldName, int startIndex, int endIndex) {
            super (fieldName, startIndex, endIndex);
        }
        public void parse (String data, int index, BasicDBObject d) {
            d.append(fieldName, data.substring(index+startIndex,index+endIndex));
        }
    }
    
    public class DimensionParser extends ValueParser {
        private int scalingFactor = 1;
        public DimensionParser (String fieldName, int startIndex, int endIndex) {
            super (fieldName, startIndex, endIndex);
        }
        public DimensionParser (String fieldName, int scalingFactor, int startIndex, int endIndex) {
            super (fieldName, startIndex, endIndex);
            this.scalingFactor = scalingFactor;
        }
        public void parse (String data, int index, BasicDBObject d) {
            int value = Util.parseInt(data.substring(index+startIndex,index+endIndex));
            if (scalingFactor == 1) {
                d.append(fieldName, value);
            } else {
                d.append (fieldName, (double)value / (double)scalingFactor);
            }
        }
    }
    
    public static void main (String[] args) {
        Parser p = new MasterParser();
        BasicDBObject result = new BasicDBObject();
        p.parse("AA112000091AA224000091AY101061AY201061GF102991001001999999001081KA1999N-00211MA1999999099241MD1710231+9999MW1021REMSYN100AAXX  01061 01010 11989 21512 10009 21078 39924 49940 57023 60002 70200 80008 333 21021 70000 91117;EQDQ01  00002PRCP24Q02  00002PRCP12", 0, result);
        System.out.println(result);
    }
    
}
