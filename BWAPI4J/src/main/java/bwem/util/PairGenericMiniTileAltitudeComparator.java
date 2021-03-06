package bwem.util;

import bwem.tile.MiniTile;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Comparator;

public final class PairGenericMiniTileAltitudeComparator<T> implements Comparator<MutablePair<T, MiniTile>> {

    @Override
    public int compare(MutablePair<T, MiniTile> o1, MutablePair<T, MiniTile> o2) {
        int a1 = o1.getRight().getAltitude().intValue();
        int a2 = o2.getRight().getAltitude().intValue();
        return Integer.compare(a1, a2);
    }

}
