package com.CybaricFox.ComponentSystems.HelperClasses;

import java.util.ArrayList;

//This could be used as a cache but I dont know if the performace is worth the memory cost.
public class CustomProcessContext {
    public boolean isMultiInput;
    public String processID;
    public ArrayList<String> inputs;
    public ArrayList<Integer> inputQuantities;
    public ArrayList<String> outputs;
    public ArrayList<Integer> outputQuantities;

    CustomProcessContext(boolean isMultiInput, String processID, ArrayList<String> inputs, ArrayList<Integer> inputQuantities, ArrayList<String> outputs, ArrayList<Integer> outputQuantities) {
        this.isMultiInput = isMultiInput;
        this.processID = processID;
        this.inputs = inputs;
        this.inputQuantities = inputQuantities;
        this.outputs = outputs;
        this.outputQuantities = outputQuantities;
    }
}
