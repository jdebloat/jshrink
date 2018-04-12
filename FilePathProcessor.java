import java.io.File;
import java.util.*;

public class FilePathProcessor {
    public static ArrayList<String> process(String basePath, String prefix) {
        ArrayList<String> paths = new ArrayList<>();
        File folder = new File(basePath);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; ++ i) {
            String absPath = listOfFiles[i].getAbsolutePath();
            if (listOfFiles[i].isFile() && absPath.substring(absPath.length() - 5).equals("class")) {
                paths.add(prefix + File.separator + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()){
                ArrayList<String> directory = FilePathProcessor.process(absPath,prefix + File.separator +listOfFiles[i].getName());
                paths.addAll(directory);
            }
        }
        return paths;
    }
}
