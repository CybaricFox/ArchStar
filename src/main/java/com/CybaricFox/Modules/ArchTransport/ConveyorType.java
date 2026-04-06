package com.CybaricFox.Modules.ArchTransport;

//What type of conveyor is this?
public enum ConveyorType {
    CONVEYOR, //Normal Conveyor
    ROUTER, //Multi-Output
    IMPORT, //Imports items into the conveyor
    EXPORT, //Exports items into blocks
    NOT_SET //Type not set
}
