import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

record Addr(String city, String street, int house, int floor) {
  @Override
  public String toString() {
    return String.format("City: %s, street: %s, house: %d, floor: %d", city, street, house, floor);
  }
}

class Main {
  public enum FileType {
    XML, CSV, Unknown
  }
  public static void main(String[] args) {
    final int MAX_FLOORS = 5;
    
    Scanner in = new Scanner(System.in);

    HashMap<Addr, Integer> addr_repeats_num = new HashMap<>();
    HashMap<String, int[]> each_floors_num = new HashMap<>();

    System.out.println("Enter file path (type <exit> to finish):");
    String file_path = in.nextLine();
    if(file_path.equals("exit")) return;
    FileType file_type = FileType.Unknown;
    if(file_path.toLowerCase().endsWith(".xml")) {
      file_type = FileType.XML;
    }
    else if(file_path.toLowerCase().endsWith(".csv")) {
      file_type = FileType.CSV;
    }

    switch (file_type) {
      case XML: {
        try {
          SAXParserFactory sax_factory = SAXParserFactory.newInstance();
          SAXParser sax_parser = sax_factory.newSAXParser();
          DefaultHandler handler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
              /* Count repeating records */
              if(!qName.equals("item")) return;
              Addr addr = new Addr(
                attributes.getValue(0), 
                attributes.getValue(1),
                Integer.parseInt(attributes.getValue(2)),
                Integer.parseInt(attributes.getValue(3))
              );
              
              Integer repeats = addr_repeats_num.get(addr);
              if(repeats != null) {
                addr_repeats_num.put(addr, repeats + 1);
              }
              else {
                addr_repeats_num.put(addr, 1);
              }

              /* Count how many there are houses with 1--MAX_FLOORS floors */
              if(addr.floor() > MAX_FLOORS ) return;
              int floors[] = each_floors_num.get(addr.city());
              if(floors != null) {
                floors[addr.floor() - 1]++;
              }
              else {
                int counts[] = new int[5];
                counts[addr.floor() - 1] = 1;
                each_floors_num.put(addr.city(), counts);
              }
            }
          };
          sax_parser.parse(new File(file_path), handler);
        }
        catch (IOException | SAXException | ParserConfigurationException e) {
          System.err.println("Error: " + e.getMessage());
        }
        break;
      }
      case CSV: {
        break;
      }
      case Unknown: {
        System.err.println("Error: Unknown file type");
        break;
      }
    }

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

    in.close();
  }
}