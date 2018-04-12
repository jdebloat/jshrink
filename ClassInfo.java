import java.util.ArrayList;

public class ClassInfo {
    public ArrayList<String> classUsed;
    public String name;
    public String filePath;

    public ClassInfo(String fp) {
//        name = n;
        filePath = fp;
        classUsed = new ArrayList<>();
//        NClassVisitor visitor = new NClassVisitor(classUsed);
//        cr.accept(visitor, 0);
    }
}