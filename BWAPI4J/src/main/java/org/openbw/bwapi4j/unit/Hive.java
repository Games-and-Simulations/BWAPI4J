package org.openbw.bwapi4j.unit;

import org.openbw.bwapi4j.type.UnitType;

public class Hive extends Lair {

    protected Hive(int id, int timeSpotted) {
        
        super(id, UnitType.Zerg_Hive, timeSpotted);
    }

    @Override
    public boolean morph(UnitType type) {
        return false;
    }
}
