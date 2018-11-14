package edu.ucla.cs.onr.classcollapser;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;

public interface IClassCollapserAnalyser {
    public void run();
    public Queue<ArrayList<String>> getCollapseList();
    public Map<String, String> getNameChangeList();
}
