import com.example.DocParser;

public class Main {
    public static void main(String[] args) {
        String filePath = "/path/to/13F_document.txt";
        String fileType = "13F";
        DocParser parser = new DocParser(filePath, fileType);
        Map<String, String> parsedData = parser.parse();
        for (Map.Entry<String, String> entry : parsedData.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
