package edu.ucla.cs.onr.classcollapser;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public interface IClassCollapserAnalyser {
    public void run();
    public Queue<ArrayList<String>> getCollapseList();
    public Map<String, String> getNameChangeList();
    public Set<String> getRemoveList();
    public Map<String, Set<String>> getProcessedUsedMethods();
}
