package bwem.tile;

import bwem.Markable;
import bwem.StaticMarkable;
import bwem.area.typedef.AreaId;
import bwem.typedef.Altitude;
import bwem.unit.Neutral;
import org.apache.commons.lang3.mutable.MutableInt;

public class TileImpl implements Tile {

    private final static StaticMarkable staticMarkable = new StaticMarkable();
    private final Markable markable;

    private Neutral neutral;
    private Altitude lowestAltitude;
    private AreaId areaId;
    private final MutableInt internalData;
    private Tile.GroundHeight groundHeight;
    private boolean isBuildable;
    private boolean isDoodad;

    public TileImpl() {
        this.markable = new Markable(TileImpl.staticMarkable);
        this.neutral = null;
        this.lowestAltitude = new Altitude(0);
        this.areaId = new AreaId(0);
        this.internalData = new MutableInt(0);
        this.groundHeight = GroundHeight.LOW_GROUND;
        this.isBuildable = false;
        this.isDoodad = false;
    }

    public static StaticMarkable getStaticMarkable() {
        return TileImpl.staticMarkable;
    }

    public Markable getMarkable() {
        return this.markable;
    }

    @Override
    public boolean isBuildable() {
        return this.isBuildable;
    }

    @Override
    public AreaId getAreaId() {
        return this.areaId;
    }

    @Override
    public Altitude getLowestAltitude() {
        return this.lowestAltitude;
    }

    @Override
    public boolean isWalkable() {
        return (getAreaId().intValue() != 0);
    }

    @Override
    public boolean isTerrain() {
        return isWalkable();
    }

    @Override
    public GroundHeight getGroundHeight() {
        return this.groundHeight;
    }

    @Override
    public boolean isDoodad() {
        return this.isDoodad;
    }

    @Override
    public Neutral getNeutral() {
        return this.neutral;
    }

    @Override
    public int getStackedNeutralCount() {
        int stackSize = 0;
        for (Neutral stackedNeutral = getNeutral(); stackedNeutral != null; stackedNeutral = stackedNeutral.getNextStacked()) {
            ++stackSize;
        }
        return stackSize;
    }

    public void setBuildable() {
        this.isBuildable = true;
    }

    public void setGroundHeight(final int groundHeight) {
//        { bwem_assert((0 <= h) && (h <= 2)); bits.groundHeight = h; }
//        if (!((0 <= h) && (h <= 2))) {
//            throw new IllegalArgumentException();
//        }
        this.groundHeight = GroundHeight.parseGroundHeight(groundHeight);
    }

    public void setDoodad() {
        this.isDoodad = true;
    }

    public void addNeutral(final Neutral neutral) {
//        { bwem_assert(!pNeutral && pNeutral); neutral = pNeutral; }
        if (!(getNeutral() == null && neutral != null)) {
            throw new IllegalStateException();
        }
        this.neutral = neutral;
    }

    public void setAreaId(final AreaId areaId) {
//        { bwem_assert((id == -1) || !areaId && id); areaId = id; }
        if (!(areaId.intValue() == -1 || getAreaId().intValue() == 0 && areaId.intValue() != 0)) {
            throw new IllegalStateException();
        }
        this.areaId = areaId;
    }

    public void resetAreaId() {
        this.areaId = new AreaId(0);
    }

    public void setLowestAltitude(final Altitude lowestAltitude) {
//        { bwem_assert(a >= 0); this.lowestAltitude = a; }
        if (!(lowestAltitude.intValue() >= 0)) {
            throw new IllegalArgumentException();
        }
        this.lowestAltitude = lowestAltitude;
    }

    public void removeNeutral(final Neutral neutral) {
//        { bwem_assert(pNeutral &&  (pNeutral == pNeutral));
//          utils::unused(pNeutral); pNeutral = nullptr; }
        if (!(neutral != null && getNeutral().equals(neutral))) {
            throw new IllegalStateException();
        }
        this.neutral = null;
    }

    public MutableInt getInternalData() {
        return this.internalData;
    }

}
