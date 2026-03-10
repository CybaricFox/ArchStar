package com.CybaricFox.Components.Helpers;

public enum ConveyorState {
    OPEN, //Conveyor is not transfering and not containing an item
    WAIT, //Conveyor just received its item.
    TRANSFER, //Conveyer is currently transfering an item.
    VOLATILE, //Conveyor is in volatile state and should be ignored until ready.

    //These values are used by the importer
    IMPORT_TRANSFER_IN,
    IMPORT_TRANSFER_OUT,
    IMPORT_TRANSFER_IO
}

//Conveyor starts OPEN
//Conveyors will only transfer items to OPEN or TRANSFER
