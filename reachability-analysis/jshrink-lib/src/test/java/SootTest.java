import edu.ucla.cs.jshrinklib.TestUtils;
import edu.ucla.cs.jshrinklib.util.ClassFileUtils;
import org.junit.Test;
import soot.G;
import soot.SootClass;

import java.io.*;

import static org.junit.Assert.assertTrue;

public class SootTest {
    @Test
    public void testClassReferenceInSoot() throws IOException {
        // soot seems not handling X.class correctly when converting a class file to Jimple and convert it back
        ClassLoader classLoader = SootTest.class.getClassLoader();
        String classPath = new File(classLoader.getResource("soot-error" + File.separator + "A.class").getFile()).getParentFile().getAbsolutePath();
        SootClass sootClass = TestUtils.getSootClass(classPath, "A");

        // first soot rewrite
        File outputFile = new File(classPath + File.separator + "A.class");
        File tmpFile = new File(classPath + File.separator + "A.temp");
        outputFile.renameTo(tmpFile);
        ClassFileUtils.writeClass(sootClass, outputFile);

        // must reset Soot before reloading the same class, otherwise we will get the cached Soot class
        G.reset();

        // reload the same class and do second soot rewrite
        sootClass = TestUtils.getSootClass(classPath, "A");
        ClassFileUtils.writeClass(sootClass, outputFile);

        // run the class and we will get a bytecode validation error
        String[] cmd = new String[] {"java", "-cp", classPath, "A"};
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Process process = processBuilder.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String error = "";
        String line;
        while ((line = br.readLine()) != null) {
            error += line;
        }
        assertTrue(error.contains("ClassNotFoundException"));

        // restore the original class file
        outputFile.delete();
        tmpFile.renameTo(outputFile);
    }
}
