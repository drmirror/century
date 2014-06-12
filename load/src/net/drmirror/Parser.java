package net.drmirror;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;

public abstract class Parser {

    public abstract void parse (String data, int index, BasicDBObject d);
    public abstract int endIndex();

    public static class MasterParser extends Parser {
        private SkippingParser[] parserArray = new SkippingParser[] {
            new BlockParser(
                "AA4", "lP",
                new DimensionParser ("p", 3, 5),
                new DimensionParser ("d", 5, 9),
                new CodeParser      ("c", 9, 10),
                new CodeParser      ("q", 10, 11)
            ),
            new BlockParser(
                "AB1", "lPMT",
                new DimensionParser ("d", 3, 8),
                new CodeParser ("c", 8, 9),
                new CodeParser ("q", 9, 10)
            ),
            new BlockParser(
                "AC1", "pOH",
                new CodeParser ("d", 3, 4),
                new CodeParser ("c", 4, 5),
                new CodeParser ("q", 5, 6)
            ),
            new BlockParser(
                "AD1", "lPGA24HM",
                new DimensionParser ("d", 3, 8),
                new CodeParser ("c", 8, 9),
                new CodeParser ("o1", 9, 13),
                new CodeParser ("o2", 13, 17),
                new CodeParser ("o3", 17, 21),
                new CodeParser ("q", 21, 22)
            ),
            new BlockParser(
                "AE1", "lPNODWA",
                new GroupParser ("d001",
                        new DimensionParser ("v", 3, 5),
                        new CodeParser ("q", 5, 6)
                ),
                new GroupParser ("d010",
                        new DimensionParser ("v", 6, 8),
                        new CodeParser ("q", 8, 9)
                ),
                new GroupParser ("d050",
                        new DimensionParser ("v", 9, 11),
                        new CodeParser ("q", 11, 12)
                ),
                new GroupParser ("d100",
                        new DimensionParser ("v", 12, 14),
                        new CodeParser ("q", 14, 15)
                )
            ),
            new BlockParser(
                "AG1", "pEO",
                new CodeParser ("d", 3, 4),
                new DimensionParser ("eWD", 4, 7)
            ),
            new BlockParser(
                "AH6", "lPMSDM",
                new DimensionParser ("p", 3, 6),
                new DimensionParser ("d", 10, 6, 10),
                new CodeParser ("c", 10, 11),
                new CodeParser ("eDT", 11, 17),
                new CodeParser ("q", 17, 18)
            ),
            new BlockParser(
                "AI6", "lPMSDM",
                new DimensionParser ("p", 3, 6),
                new DimensionParser ("d", 10, 6, 10),
                new CodeParser ("c", 10, 11),
                new CodeParser ("eDT", 11, 17),
                new CodeParser ("q", 17, 18)
            ),
            new BlockParser(
                "AJ1", "sD",
                new GroupParser ("d",
                    new DimensionParser ("v", 3, 7),
                    new CodeParser ("c", 7, 8),
                    new CodeParser ("q", 8, 9)
                ),
                new GroupParser ("eWD",
                    new DimensionParser ("v", 10, 9, 15),
                    new CodeParser ("c", 15, 16),
                    new CodeParser ("q", 16, 17)
                )
            ),
            new BlockParser(
                "AK1", "sDMM",
                new DimensionParser ("d", 3, 7),
                new CodeParser ("c", 7, 8),
                new CodeParser ("da", 8, 14),
                new CodeParser ("q", 14, 15)
            ),
            new BlockParser(
                "AL4", "sA",
                new DimensionParser ("p", 3, 5),
                new DimensionParser ("d", 5, 8),
                new CodeParser ("c", 8, 9),
                new CodeParser ("q", 9, 10)
            ),
            new BlockParser(
                "AM1", "sAGA24HM",
                new DimensionParser ("d", 10, 3, 7),
                new CodeParser ("c", 7, 8),
                new CodeParser ("o1", 8, 12),
                new CodeParser ("o2", 12, 16),
                new CodeParser ("o3", 16, 20),
                new CodeParser ("q", 20, 21)
            ),
            new BlockParser(
                "AN1", "sAM",
                new DimensionParser ("p", 3, 6),
                new DimensionParser ("d", 10, 6, 10),
                new CodeParser ("c", 10, 11),
                new CodeParser ("q", 11, 12)
            ),
            new BlockParser(
                "AO4", "lPO",
                new DimensionParser ("p", 3, 5),
                new DimensionParser ("d", 10, 5, 9),
                new CodeParser ("c", 9, 10),
                new CodeParser ("q", 10, 11)
            ),
            new BlockParser(
                    "AP4", "lP15M",
                    new DimensionParser ("g", 10, 3, 7),
                    new CodeParser ("c", 7, 8),
                    new CodeParser ("q", 8, 9)
            ),
            new BlockParser(
                    "AU9", "pWOASOS",
                    new CodeParser ("iP", 3, 4),
                    new CodeParser ("d", 4, 5),
                    new CodeParser ("p", 5, 7),
                    new CodeParser ("o", 7, 8),
                    new CodeParser ("oWP", 8, 9),
                    new CodeParser ("cI", 9, 10),
                    new CodeParser ("q", 10, 11)
            ),
            new BlockParser(
                    "AW4", "pWO",
                    new CodeParser ("c", 3, 5),
                    new CodeParser ("q", 5, 6)
            ),
            new BlockParser(
                    "AX6", "pWOSOD",
                    new GroupParser("aC",
                        new CodeParser("v", 3, 5),
                        new CodeParser("q", 5, 6)
                    ),
                    new GroupParser("p",
                        new DimensionParser("v", 6, 8),
                        new CodeParser("q", 8, 9)
                    )
            ),
            new BlockParser(
                    "AY2", "pWOM",
                    new GroupParser("aC",
                        new CodeParser("v", 3, 4),
                        new CodeParser("q", 4, 5)
                    ),
                    new GroupParser("p",
                        new DimensionParser("v", 5, 7),
                        new CodeParser("q", 7, 8)
                    )
            ),
            new BlockParser(
                    "AZ2", "pWO",
                    new GroupParser("aC",
                         new CodeParser("v", 3, 4),
                         new CodeParser("q", 4, 5)
                    ),
                    new GroupParser("p",
                         new DimensionParser("v", 5, 7),
                         new CodeParser("q", 7, 8)
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
                    "ED1", "rVR",
                    new DimensionParser("a", 3, 5), // SCALING FACTOR .1
                    new CodeParser("d", 5, 6),
                    new DimensionParser("v", 6, 10),
                    new CodeParser("q", 10, 11)
            ),
            new BlockParser(
                    "GA6", "sC",
                    new GroupParser(
                        "c",
                        new CodeParser ("v", 3, 5),
                        new CodeParser ("q", 5, 6)
                    ),
                    new GroupParser(
                        "bH",
                        new DimensionParser ("v", 6, 12),
                        new CodeParser ("q", 12, 13)
                    ),
                    new GroupParser(
                        "cT",
                        new CodeParser ("v", 13, 15),
                        new CodeParser ("q", 15, 16)
                    )
            ),
            new BlockParser(
                "GD6", "sCSS",
                new GroupParser(
                        "c",
                        new CodeParser ("v", 3, 4),
                        new CodeParser ("v2", 4, 6),
                        new CodeParser ("q", 6, 7)
                ),
                new GroupParser(
                        "h",
                        new DimensionParser("v", 7, 13),
                        new CodeParser ("q", 13, 14)
                )
            ),
            new BlockParser(
                "GE1", "sCOS",
                new CodeParser("cC", 3, 4),
                new CodeParser("vD", 4, 10),
                new DimensionParser("bHUR", 10, 16),
                new DimensionParser("bHLR", 16, 22)
            ),
            new BlockParser(
                "GF1", "sCO",
                new GroupParser(
                        "tC",
                        new CodeParser("v", 3, 5),
                        new CodeParser("o", 5, 7),
                        new CodeParser("q", 7, 8)
                ),
                new GroupParser(
                        "lCC",
                        new CodeParser("v", 8, 10),
                        new CodeParser("q", 10, 11)
                ),
                new GroupParser(
                        "lCG",
                        new CodeParser("v", 11, 13),
                        new CodeParser("q", 13, 14)
                ),
                new GroupParser(
                        "lCBH",
                        new DimensionParser("v", 14, 19),
                        new CodeParser("q", 19, 20)
                ),
                new GroupParser(
                        "mCG",
                        new CodeParser("v", 20, 22),
                        new CodeParser("q", 22, 23)
                ),
                new GroupParser(
                        "hCG",
                        new CodeParser("value", 23, 25),
                        new CodeParser("quality", 25, 26)
                )
            ),
            new BlockParser (
                "GG6", "bSCL",
                new GroupParser(
                        "c",
                        new CodeParser("v", 3, 5),
                        new CodeParser("q", 5, 6)
                ),
                new GroupParser(
                        "tH",
                        new DimensionParser("v", 6, 11),
                        new CodeParser("q", 11, 12)
                ),
                new GroupParser(
                        "t",
                        new CodeParser("v", 12, 14),
                        new CodeParser("q", 14, 15)
                ),
                new GroupParser(
                        "t",
                        new CodeParser("v", 15, 17),
                        new CodeParser("q", 17, 18)
                )
            ),
            new BlockParser(
                "GH1", "hSR",
                new GroupParser(
                        "a",
                        new DimensionParser("v", 10, 3, 8),
                        new CodeParser("q", 8, 9),
                        new CodeParser("qF", 9, 10)
                ),
                new GroupParser(
                        "m",
                        new DimensionParser("v", 10, 10, 15),
                        new CodeParser("q", 15, 16),
                        new CodeParser("qF", 16, 17)
                ),
                new GroupParser(
                        "max",
                        new DimensionParser("v", 10, 17, 22),
                        new CodeParser("q", 22, 23),
                        new CodeParser("qF", 23, 24)
                ),
                new GroupParser(
                        "sD",
                        new DimensionParser("v", 10, 24, 29),
                        new CodeParser("q", 30, 31),
                        new CodeParser("qF", 31, 32)
                )
            ),
            new BlockParser(
                "GJ1", "sD",
                new DimensionParser ("v", 3, 7),
                new CodeParser ("q", 7, 8)
            ),
            new BlockParser(
                "GK1", "sP",
                new DimensionParser ("v", 3, 6),
                new CodeParser ("q", 6, 7)
            ),
            new BlockParser(
                "GL1", "sM",
                new DimensionParser ("v", 3, 8),
                new CodeParser ("q", 8, 9)
            ),
            new SkippingParser ("GM1", 33),
            new SkippingParser ("GN1", 31),
            new SkippingParser ("GO1", 22),
            new SkippingParser ("GP1", 34),
            new SkippingParser ("GQ1", 17),
            new BlockParser (
                "GR1", "eR",
                new DimensionParser ("p", 3, 7),
                new GroupParser(
                    "oHS",
                    new DimensionParser ("v", 7, 11),
                    new CodeParser ("q", 11, 12)
                ),
                new GroupParser(
                    "nTS",
                    new DimensionParser ("v", 12, 16),
                    new CodeParser ("q", 16, 17)
                )
            ),
            new BlockParser (
                "HL1", "h",
                new DimensionParser ("s", 10, 3, 6),
                new CodeParser ("q", 6, 7)
            ),
            new SkippingParser ("IA1", 6),
            new SkippingParser ("IA2", 12),
            new SkippingParser ("IB1", 30),
            new SkippingParser ("IB2", 16),
            new SkippingParser ("IC1", 28),
            new BlockParser (
                "KA4", "eAT",
                new DimensionParser ("p", 10, 3, 6),
                new CodeParser ("c", 6, 7),
                new DimensionParser ("v", 10, 7, 12),
                new CodeParser ("q", 12, 13)
            ),
            new BlockParser (
                "KB3", "aAT",
                new DimensionParser ("p", 3, 6),
                new CodeParser ("c", 6, 7),
                new DimensionParser ("v", 100, 7, 12),
                new DimensionParser ("q", 12, 13)
            ),
            new BlockParser (
                "KC2", "eATM",
                new CodeParser ("cd", 3, 4),
                new CodeParser ("c", 4, 5),
                new DimensionParser ("v", 10, 5, 10),
                new CodeParser ("d", 10, 16),
                new CodeParser ("q", 16, 17)
            ),
            new SkippingParser ("KD2", 12),
            new SkippingParser ("KE1", 15),
            new SkippingParser ("KF1", 9),
            new SkippingParser ("KG2", 14),
            new BlockParser (
                "MA1", "aPO",
                new GroupParser ("aS",
                    new DimensionParser ("v", 10, 3, 8),
                    new CodeParser ("q", 8, 9)
                ),
                new GroupParser ("sP",
                    new DimensionParser ("v", 10, 9, 14),
                    new CodeParser ("q", 14, 15)
                )
            ),
            new BlockParser (
                "MD1", "aPC",
                new GroupParser ("t",
                    new CodeParser ("c", 3, 4),
                    new CodeParser ("q", 4, 5)
                ),
                new GroupParser ("q3H",
                    new DimensionParser ("v", 10, 5, 8),
                    new CodeParser ("q", 8, 9)
                ),
                new GroupParser ("q24H",
                    new DimensionParser ("v", 10, 9, 13),
                    new CodeParser ("q", 13, 14)
                )
            ),
            new SkippingParser ("ME1", 9),
            new SkippingParser ("MF1", 15),
            new SkippingParser ("MG1", 15),
            new SkippingParser ("MH1", 15),
            new SkippingParser ("MK1", 27),
            new BlockParser (
                "MV7", "pWIV",
                new CodeParser("c", 3, 5),
                new CodeParser("q", 5, 6)
            ),
            new BlockParser (
                "MW7", "pWOM",
                new CodeParser("c", 3, 5),
                new CodeParser("q", 5, 6)
            ),
            new SkippingParser ("OA3", 11),
            new SkippingParser ("OB2", 39),
            new SkippingParser ("OD3", 14),
            new SkippingParser ("OE3", 19),
            new BlockParser (
                "RH3", "rH",
                new DimensionParser ("p", 3, 6),
                new CodeParser ("c", 6, 7),
                new DimensionParser ("%", 7, 10),
                new CodeParser ("d", 10, 11),
                new CodeParser ("q", 11, 12)
            ),
            new BlockParser (
                "SA1", "sST",
                new DimensionParser ("v", 10, 3, 7),
                new CodeParser ("q", 7, 8)
            ),
            new BlockParser (
                "ST1", "sT",
                new CodeParser ("t", 3, 4),
                new GroupParser (
                    "t",
                    new DimensionParser("v", 10, 4, 9),
                    new CodeParser ("q", 9, 10)
                ),
                new GroupParser (
                    "d",
                    new DimensionParser("v", 10, 10, 14),
                    new CodeParser ("q", 14, 15)
                ),
                new GroupParser (
                    "c",
                    new CodeParser ("c", 15, 17),
                    new CodeParser ("q", 17, 18)
                ),
                new GroupParser (
                    "sP",
                    new DimensionParser ("n", 18, 19),
                    new CodeParser ("q", 19, 20)
                )
            ),
            new BlockParser (
                "UA1", "wM",
                new CodeParser ("m", 3, 4),
                new GroupParser (
                    "w",
                    new DimensionParser ("p", 4, 6),
                    new DimensionParser ("h", 10, 6, 9),
                    new CodeParser ("q", 9, 10)
                ),
                new GroupParser (
                    "sS",
                    new CodeParser ("c", 10, 12),
                    new CodeParser ("q", 12, 13)
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
