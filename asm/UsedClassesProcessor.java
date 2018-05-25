import java.util.HashSet;
import java.util.HashMap;

public class UsedClassesProcessor {

    private HashMap<String, ClassInfo> allClasses;
    private HashMap<String, String> nameToPath;
    public HashSet<String> usedClasses;

    public UsedClassesProcessor(HashMap<String, ClassInfo> classes, HashMap<String, String> nameToP) {
        allClasses = classes;
        usedClasses = new HashSet<>();
        nameToPath = nameToP;
    }

    public void process(ClassInfo info) {
        HashSet<String> visited = new HashSet<>();
        processOne(info, visited, true);
    }

    private void processOne(ClassInfo info, HashSet<String> visited, boolean base) {
//        System.out.println(info.filePath);
//        System.out.println(visited.size());
        if (visited.contains(info.filePath)) {
            return;
        }
        if (!base) {
            usedClasses.add(info.filePath);
        }
        visited.add(info.filePath);
        for (String name: info.classUsed) {
//            System.out.println("looking for: " + name);
            if (nameToPath.containsKey(name) && allClasses.containsKey(nameToPath.get(name))) {
                processOne(allClasses.get(nameToPath.get(name)), visited, false);
            }
        }
    }
}
