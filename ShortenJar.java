import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.*;


public class ShortenJar {
    public static void main(String[] args) {
        try {
            String jarPath = args[0];
            String outPath = args[1];
            String prgmPath = args[2];
            
            String jarTmpPath = jarPath;
            String outTmpPath = outPath + "_out";
            
            Runtime rt = Runtime.getRuntime();
            
            String OS = System.getProperty("os.name", "generic").toLowerCase();
            if (OS.indexOf("nux") >= 0) {
            	jarTmpPath = jarPath + "_out";
            	Process p = rt.exec("unzip " + jarTmpPath + " -d " + jarTmpPath);
            }
            
            ArrayList<String> paths = FilePathProcessor.process(jarTmpPath, "");
//            for (int i = 0; i < paths.size(); ++i) {
//                System.out.println(paths.get(i));
//            }

            HashMap<String, ClassInfo> classInfos = new HashMap<>();
            HashMap<String, String> nameToPath = new HashMap<>();

            for (int i = 0; i < paths.size(); ++i) {
                String thisPath = paths.get(i);
                String realPath = jarTmpPath + thisPath;

                ClassInfo thisInfo = new ClassInfo(thisPath);
                FileInputStream is = new FileInputStream(realPath);
                ClassReader cr = new ClassReader(is);
                NClassVisitor visitor = new NClassVisitor(thisInfo);
                cr.accept(visitor, 0);

                classInfos.put(thisPath, thisInfo);
                nameToPath.put(thisInfo.name, thisPath);

//                System.out.println("putting in: " + thisPath);
            }
            System.out.println("# of classes in jar: " + classInfos.size());

            ClassInfo prgmInfo = new ClassInfo(prgmPath);
            FileInputStream is = new FileInputStream(prgmPath);
            ClassReader cr = new ClassReader(is);
            NClassVisitor prgmVisitor = new NClassVisitor(prgmInfo);
            cr.accept(prgmVisitor, 0);

            System.out.println("classes used:");
            for (String s : prgmInfo.classUsed) {
                System.out.println(s);
            }

            UsedClassesProcessor ucProcessor = new UsedClassesProcessor(classInfos, nameToPath);
            ucProcessor.process(prgmInfo);

            System.out.println("class files used: (" + ucProcessor.usedClasses.size() + ") total");
            for (String s: ucProcessor.usedClasses) {
                System.out.println(s);
            }

            for (String path: ucProcessor.usedClasses) {
                String fullPath = outTmpPath + path;
                System.out.println(fullPath);
                String regex = "\\\\";
                String[] splitted = fullPath.split(regex);
                String directory = "";
                for (int i = 0; i < splitted.length - 2; ++i) {
                    directory += splitted[i];
                    directory += File.separator;
                }
                directory += splitted[splitted.length - 2];
                if (!Files.exists(Paths.get(directory))) {
                    Files.createDirectories(Paths.get(directory));
                }
                Files.copy(Paths.get(jarTmpPath + path), Paths.get(outTmpPath + path));
            }
            
            rt.exec("jar cf " + outPath + " " + outTmpPath);

        } catch (FileNotFoundException e) {
            System.out.println(e);
            System.exit(1);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
//
//    private static ArrayList<String> getClassesUsedByClass(String basePath) {
//        ArrayList<String> res = new ArrayList<>();
//    }
}



