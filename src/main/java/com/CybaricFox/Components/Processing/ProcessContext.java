package com.CybaricFox.Components.Processing;

import java.util.ArrayList;

public class ProcessContext {
    //How much has this recipe progressed in ticks?
    public int progress = 0;
    //How much progress is required to finish the process?
    public int progressThreshold;
    //The items that are required as input
    public ArrayList<String> targetInputIds;
    //A parallel array that contains the quantities of each item
    public ArrayList<Integer> targetInputQuantities;
    //The items that are returned as output
    public ArrayList<String> targetOutputIds;
    //A parallel array that contains the quantities of each item
    public ArrayList<Integer> targetOutputQuantities;

    public ProcessContext(ArrayList<String> targetInputIds, ArrayList<Integer> targetInputQuantities, ArrayList<String> targetOutputIds, ArrayList<Integer> targetOutputQuantities, int progressThreshold) {
        this.targetInputIds = targetInputIds;
        this.targetInputQuantities = targetInputQuantities;
        this.targetOutputIds = targetOutputIds;
        this.targetOutputQuantities = targetOutputQuantities;
        this.progressThreshold = progressThreshold;
    }
}
