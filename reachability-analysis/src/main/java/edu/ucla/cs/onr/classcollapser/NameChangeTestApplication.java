package edu.ucla.cs.onr.classcollapser;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class NameChangeTestApplication {
    public static void main(String[] args) {
//        String path = "/Users/zonghengma/Documents/UCLA/capstone_new/test/try/A.class";
        String path = "/Users/zonghengma/Documents/UCLA/capstone_new/test/asm/B.class";
//        String path = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/commons-lang/target/classes/org/apache/commons/lang3/time/FastDateParser.class";
//        String path = "/Users/zonghengma/Documents/UCLA/capstone_new/prgms/commons-lang/target/classes/org/apache/commons/lang3/time/FastDateParser$ISO8601TimeZoneStrategy.class";
        try {
            FileInputStream is = new FileInputStream(path);
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(cr, 0);
            NameChangeClassWriter nccw = new NameChangeClassWriter(Opcodes.ASM5, cw, null, null);
            cr.accept(nccw, 0);
            System.out.println(cw.toByteArray());
            FileUtils.writeByteArrayToFile(new File("/Users/zonghengma/Documents/UCLA/capstone_new/test/asm/B_new.class"), cw.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
