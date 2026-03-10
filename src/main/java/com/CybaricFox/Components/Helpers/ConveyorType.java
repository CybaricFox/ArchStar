package com.CybaricFox.Components.Helpers;

//What type of conveyor is this?
public enum ConveyorType {
    CONVEYOR, //Normal Conveyor
    ROUTER, //Multi-Output
    IMPORT, //Imports items into the conveyor
    EXPORT, //Exports items into blocks
    NOTSET //Type not set
}
