/* Main libs */
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.*;

/* XML libs */
import javax.xml.parsers.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/* CSV libs */
import com.univocity.parsers.csv.*;
import com.univocity.parsers.common.record.Record;

record Addr(String city, String street, int house, int floor) {
  @Override
  public String toString() {
    return String.format("City: %s, street: %s, house: %d, floor: %d", city, street, house, floor);
  }
}

interface CityCounter {
  public void iterate(Addr addr);
}
/* Counts repeated addresses and number of floors *max_floors* in every city */
class RepeatsAndFloorsCityCounter implements CityCounter {
  public RepeatsAndFloorsCityCounter(int max_floors) {
    this.max_floors = max_floors;
    this.addr_repeats_num = new HashMap<>();
    this.each_floors_num = new HashMap<>();
  }
  @Override
  public void iterate(Addr addr) {
    /* Increment number of repeats or set it to 1 */
    addr_repeats_num.compute(addr, (key, value) -> value == null ? 1 : value + 1);

    /* Count how many there are houses with from 1 to *max_floors* floors */
    if(addr.floor() > max_floors) return;
    each_floors_num.computeIfAbsent(addr.city(), key -> new int[max_floors])[addr.floor() - 1]++;
  }

  public final HashMap<Addr, Integer> get_addr_repeats_num() { return addr_repeats_num; }
  public final HashMap<String, int[]> get_each_floors_num() { return each_floors_num; }

  private final int max_floors;

  private HashMap<Addr, Integer> addr_repeats_num;
  private HashMap<String, int[]> each_floors_num;
}

interface CityFileParser {
  public void parse(File file);
}
class XMLCityFileParser implements CityFileParser {
  public XMLCityFileParser(CityCounter city_counter) {
    counter = city_counter;

    try {
      sax_factory = SAXParserFactory.newInstance();
      sax_parser = sax_factory.newSAXParser();
    } 
    catch (ParserConfigurationException | SAXException e) {
      System.err.println("XML parser configuration error: " + e.getMessage());
    }

    handler = new DefaultHandler() {
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if(!qName.equals("item")) return;
        Addr addr = new Addr(
          attributes.getValue(0), 
          attributes.getValue(1),
          Integer.parseInt(attributes.getValue(2)),
          Integer.parseInt(attributes.getValue(3))
        );
        
        counter.iterate(addr); 
      }
    };
  }
  @Override
  public void parse(File file) {
    try {
      sax_parser.parse(file, handler);
    }
    catch (IOException | SAXException e) {
      System.err.println("XML parsing error: " + e.getMessage());
    }
  }

  private SAXParserFactory sax_factory;
  private SAXParser sax_parser;
  private DefaultHandler handler;

  private CityCounter counter;
}
class CSVCityFileParser implements CityFileParser {
  public CSVCityFileParser(CityCounter city_counter) {
    counter = city_counter;
  }
  @Override
  public void parse(File file) {
    CsvParserSettings settings = new CsvParserSettings();

    settings.setHeaderExtractionEnabled(true); /* Skip header */
    settings.setLineSeparatorDetectionEnabled(true); /* Read big files withoud loading them in memory */
    settings.getFormat().setDelimiter(';'); /* Sets delimiter to be *;* */
    CsvParser parser = new CsvParser(settings);

    for (Record row : parser.iterateRecords(file)) {

      Addr addr = new Addr(
        row.getString("city"),
        row.getString("street"),
        row.getInt("house"),
        row.getInt("floor")
      );
      counter.iterate(addr);
    }
  }

  private CityCounter counter;
}

class Main {
  public enum FileType {
    XML, CSV, Unknown
  }
  public static void main(String[] args) {
    final int MAX_FLOORS = 5;

    Scanner in = new Scanner(System.in);

    while(true) {
      System.out.println("Enter file path (type <exit> to finish):");
      String file_path = in.next();
      if(file_path.equals("exit")) break;
      FileType file_type = FileType.Unknown;
      if(file_path.toLowerCase().endsWith(".xml")) {
        file_type = FileType.XML;
      }
      else if(file_path.toLowerCase().endsWith(".csv")) {
        file_type = FileType.CSV;
      }

      Instant time_start = Instant.now();

      var counter = new RepeatsAndFloorsCityCounter(MAX_FLOORS);
      switch (file_type) {
        case XML: {
          XMLCityFileParser parser = new XMLCityFileParser(counter);
          parser.parse(new File(file_path));
          break;
        }
        case CSV: {
          CSVCityFileParser parser = new CSVCityFileParser(counter);
          parser.parse(new File(file_path));
          break;
        }
        case Unknown: {
          System.err.println("Error: Unknown file type");
          break;
        }
      }
      Instant time_end = Instant.now();

      /* Printing results */
      HashMap<Addr, Integer> addr_repeats_num = counter.get_addr_repeats_num();
      HashMap<String, int[]> each_floors_num = counter.get_each_floors_num();
      for(Map.Entry<Addr, Integer> entry : addr_repeats_num.entrySet()) {
        int val = entry.getValue();
        if(val > 1)
          System.out.println(entry.getKey() + "; repeated " + val + " times");
      }
      System.out.println("");
      for(Map.Entry<String, int[]> entry : each_floors_num.entrySet()) {
        System.out.println("City: " + entry.getKey());
        for(int i = 0; i < MAX_FLOORS; i++) {
          System.out.println((i + 1) + " floors: " + entry.getValue()[i]);
        }
      }
      System.out.println("Completed in: " + Duration.between(time_start, time_end).toMillis() + " ms");
    }

    in.close();
  }
}