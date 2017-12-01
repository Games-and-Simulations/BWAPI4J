package bwem;

import bwem.map.Map;
import bwem.typedef.CPPath;
import bwem.typedef.Pred;
import bwem.typedef.Index;
import bwem.area.Area;
import bwem.tile.MiniTile;
import bwem.unit.Neutral;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.MutablePair;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.WalkPosition;

//////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                          //
//                                  class ChokePoint
//                                                                                          //
//////////////////////////////////////////////////////////////////////////////////////////////
//
// ChokePoints are frontiers that BWEM automatically computes from Brood War's maps
// A ChokePoint represents (part of) the frontier between exactly 2 Areas. It has a form of line.
// A ChokePoint doesn't contain any MiniTile: All the MiniTiles whose positions are returned by its Geometry()
// are just guaranteed to be part of one of the 2 Areas.
// Among the MiniTiles of its Geometry, 3 particular ones called nodes can also be accessed using Pos(middle), Pos(end1) and Pos(end2).
// ChokePoints play an important role in BWEM:
//   - they define accessibility between Areas.
//   - the Paths provided by Map::GetPath are made of ChokePoints.
// Like Areas and Bases, the number and the addresses of ChokePoint instances remain unchanged.
//
// Pseudo ChokePoints:
// Some Neutrals can be detected as blocking Neutrals (Cf. Neutral::Blocking).
// Because only ChokePoints can serve as frontiers between Areas, BWEM automatically creates a ChokePoint
// for each blocking Neutral (only one in the case of stacked blocking Neutral).
// Such ChokePoints are called pseudo ChokePoints and they behave differently in several ways.
//
// ChokePoints inherit utils::Markable, which provides marking ability
// ChokePoints inherit utils::UserData, which provides free-to-use data.
//
//////////////////////////////////////////////////////////////////////////////////////////////

public final class ChokePoint {

	// ChokePoint::middle denotes the "middle" MiniTile of Geometry(), while
	// ChokePoint::end1 and ChokePoint::end2 denote its "ends".
	// It is guaranteed that, among all the MiniTiles of Geometry(), ChokePoint::middle has the highest altitude value (Cf. MiniTile::Altitude()).
    public enum Node {

        end1(0),
        middle(1),
        end2(2),
        node_count(3)
        ;

        private final int val;

        private Node(int val) {
            this.val = val;
        }

        public int intVal() {
            return this.val;
        }

    }

    private Graph m_pGraph;
    private final boolean m_pseudo;
    private final Index m_index;
    private final MutablePair<Area, Area> m_Areas;
    private WalkPosition[] m_nodes;
    private List<MutablePair<WalkPosition, WalkPosition>> m_nodesInArea;
    private final List<WalkPosition> m_Geometry;
    private boolean m_blocked;
    private Neutral m_pBlockingNeutral;
    private ChokePoint m_pPathBackTrace = null;

    public ChokePoint(
            final Graph pGraph,
            final Index idx,
            final Area area1, Area area2,
            final List<WalkPosition> Geometry,
            final Neutral pBlockingNeutral
    ) {
        m_pGraph = pGraph;
        m_index = idx;
        m_Areas = new MutablePair<>(area1, area2);
        m_Geometry = Geometry;
        m_pBlockingNeutral = pBlockingNeutral;
        m_blocked = pBlockingNeutral != null;
        m_pseudo = pBlockingNeutral != null;

//        bwem_assert(!Geometry.empty());
        if (!(!Geometry.isEmpty())) {
            throw new IllegalArgumentException();
        }

        // Ensures that in the case where several neutrals are stacked, m_pBlockingNeutral points to the bottom one:
        if (m_pBlockingNeutral != null) {
            m_pBlockingNeutral = GetMap().getData().getTile(m_pBlockingNeutral.TopLeft()).getNeutral();
        }

        m_nodes = new WalkPosition[ChokePoint.Node.node_count.intVal()];
        m_nodes[ChokePoint.Node.end1.intVal()] = Geometry.get(0);
        m_nodes[ChokePoint.Node.end2.intVal()] = Geometry.get(Geometry.size() - 1);

        m_nodesInArea = new ArrayList<>(ChokePoint.Node.node_count.intVal());
        for (int i = 0; i < ChokePoint.Node.node_count.intVal(); ++i) {
            m_nodesInArea.add(new MutablePair<>(new WalkPosition(0, 0), new WalkPosition(0, 0)));
        }

        int i = Geometry.size() / 2;
        while ((i > 0)
                && (GetMap().getData().getMiniTile(Geometry.get(i - 1)).getAltitude().intValue()
                    > GetMap().getData().getMiniTile(Geometry.get(i)).getAltitude().intValue())) {
            --i;
        }
        while ((i < Geometry.size() - 1)
                && (GetMap().getData().getMiniTile(Geometry.get(i + 1)).getAltitude().intValue()
                    > GetMap().getData().getMiniTile(Geometry.get(i)).getAltitude().intValue())) {
            ++i;
        }
        m_nodes[ChokePoint.Node.middle.intVal()] = Geometry.get(i);

        for (int n = 0; n < ChokePoint.Node.node_count.intVal(); ++n) {
            final List<Area> tmpAreaList = new ArrayList<>();
            tmpAreaList.add(area1);
            tmpAreaList.add(area2);
            for (final Area pArea : tmpAreaList) {
                final WalkPosition nodeInArea = GetGraph().GetMap().BreadthFirstSearch(
                    m_nodes[n],
                    new Pred() { // findCond
                        @Override
                        public boolean isTrue(Object... args) {
                            Object ttile = args[0];
                            Object tpos = args[1];
                            Object tmap = args[args.length - 1];
                            if (ttile instanceof MiniTile && tpos instanceof WalkPosition && tmap instanceof Map) {
                                MiniTile miniTile = (MiniTile) ttile;
                                WalkPosition w = (WalkPosition) tpos;
                                TilePosition t = (w.toPosition()).toTilePosition();
                                Map map = (Map) tmap;
                                return (miniTile.getAreaId().equals(pArea.Id()) && map.getData().getTile(t, check_t.no_check).getNeutral() == null);
                            } else {
                                throw new IllegalArgumentException();
                            }
                        }
                    },
                    new Pred() { // visitCond
                        @Override
                        public boolean isTrue(Object... args) {
                            Object ttile = args[0];
                            Object tpos = args[1];
                            Object tmap = args[args.length - 1];
                            if (ttile instanceof MiniTile && tpos instanceof WalkPosition) {
                                MiniTile miniTile = (MiniTile) ttile;
                                WalkPosition w = (WalkPosition) tpos;
                                TilePosition t = (w.toPosition()).toTilePosition();
                                Map map = (Map) tmap;
                                return (miniTile.getAreaId().equals(pArea.Id()) || (Blocked() && (miniTile.isBlocked() || map.getData().getTile(t, check_t.no_check).getNeutral() != null)));
                            } else {
                                throw new IllegalArgumentException("Invalid argument list.");
                            }
                        }
                    }
                );

                /**
                 * Note: In the original C++ code, "nodeInArea" is a reference to a "WalkPosition" in
                 * "nodesInArea" which changes! Change that object here (after the call to "BreadthFirstSearch")...
                 */
                final WalkPosition left = m_nodesInArea.get(n).getLeft();
                final WalkPosition right = m_nodesInArea.get(n).getRight();
                final MutablePair<WalkPosition, WalkPosition> replacementPair
                        = new MutablePair<>(new WalkPosition(left.getX(), left.getY()), new WalkPosition(right.getX(), right.getY()));
                if (pArea.equals(m_Areas.getLeft())) {
                    replacementPair.setLeft(nodeInArea);
                } else {
                    replacementPair.setRight(nodeInArea);
                }
                m_nodesInArea.set(n, replacementPair);
            }
        }
    }

    public ChokePoint(Graph pGraph, Index idx, Area area1, Area area2, List<WalkPosition> Geometry) {
        this(pGraph, idx, area1, area2, Geometry, null);
    }

	// Tells whether this ChokePoint is a pseudo ChokePoint, i.e., it was created on top of a blocking Neutral.
	public boolean IsPseudo() {
        return m_pseudo;
    }

	// Returns the two Areas of this ChokePoint.
	public MutablePair<Area, Area> GetAreas() {
        return m_Areas;
    }

	// Returns the center of this ChokePoint.
	public WalkPosition Center() {
        return Pos(Node.middle);
    }

	// Returns the position of one of the 3 nodes of this ChokePoint (Cf. node definition).
	// Note: the returned value is contained in Geometry()
    public WalkPosition Pos(Node n) {
//        bwem_assert(n < node_count);
        if (!(n.intVal() < Node.node_count.intVal())) {
            throw new IllegalArgumentException();
        }
        return m_nodes[n.intVal()];
    }

	// Pretty much the same as Pos(n), except that the returned MiniTile position is guaranteed to be part of pArea.
	// That is: Map::GetArea(PosInArea(n, pArea)) == pArea.
    public WalkPosition PosInArea(Node n, Area pArea) {
//        bwem_assert((pArea == m_Areas.getLeft()) || (pArea == m_Areas.getRight()));
        if (!(pArea.equals(m_Areas.getLeft()) || pArea.equals(m_Areas.getRight()))) {
            throw new IllegalArgumentException();
        }
        return pArea.equals(m_Areas.getLeft())
                ? m_nodesInArea.get(n.intVal()).getLeft()
                : m_nodesInArea.get(n.intVal()).getRight();
    }

	// Returns the set of positions that defines the shape of this ChokePoint.
	// Note: none of these MiniTiles actually belongs to this ChokePoint (a ChokePoint doesn't contain any MiniTile).
	//       They are however guaranteed to be part of one of the 2 Areas.
	// Note: the returned set contains Pos(middle), Pos(end1) and Pos(end2).
	// If IsPseudo(), returns {p} where p is the position of a walkable MiniTile near from BlockingNeutral()->Pos().
	public List<WalkPosition> Geometry() {
        return m_Geometry;
    }

	// If !IsPseudo(), returns false.
	// Otherwise, returns whether this ChokePoint is considered blocked.
	// Normally, a pseudo ChokePoint either remains blocked, or switches to not blocked when BlockingNeutral()
	// is destroyed and there is no remaining Neutral stacked with it.
	// However, in the case where Map::AutomaticPathUpdate() == false, Blocked() will always return true
	// whatever BlockingNeutral() returns.
	// Cf. Area::AccessibleNeighbours().
	public boolean Blocked() {
        return m_blocked;
    }

	// If !IsPseudo(), returns nullptr.
	// Otherwise, returns a pointer to the blocking Neutral on top of which this pseudo ChokePoint was created,
	// unless this blocking Neutral has been destroyed.
	// In this case, returns a pointer to the next blocking Neutral that was stacked at the same location,
	// or nullptr if no such Neutral exists.
	public Neutral BlockingNeutral() {
        return m_pBlockingNeutral;
    }

	// If AccessibleFrom(cp) == false, returns -1.
	// Otherwise, returns the ground distance in pixels between Center() and cp->Center().
	// Note: if this == cp, returns 0.
	// Time complexity: O(1)
	// Note: Corresponds to the length in pixels of GetPathTo(cp). So it suffers from the same lack of accuracy.
	//       In particular, the value returned tends to be slightly higher than expected when GetPathTo(cp).size() is high.
	public int DistanceFrom(ChokePoint cp) {
        return GetGraph().Distance(this, cp);
    }

	// Returns whether this ChokePoint is accessible from cp (through a walkable path).
	// Note: the relation is symmetric: this->AccessibleFrom(cp) == cp->AccessibleFrom(this)
	// Note: if this == cp, returns true.
	// Time complexity: O(1)
	public boolean AccessibleFrom(ChokePoint cp) {
        return (DistanceFrom(cp) >= 0);
    }

	// Returns a list of ChokePoints, which is intended to be the shortest walking path from this ChokePoint to cp.
	// The path always starts with this ChokePoint and ends with cp, unless AccessibleFrom(cp) == false.
	// In this case, an empty list is returned.
	// Note: if this == cp, returns [cp].
	// Time complexity: O(1)
	// To get the length of the path returned in pixels, use DistanceFrom(cp).
	// Note: all the possible Paths are precomputed during Map::Initialize().
	//       The best one is then stored for each pair of ChokePoints.
	//       However, only the center of the ChokePoints is considered.
	//       As a consequence, the returned path may not be the shortest one.
    public CPPath GetPathTo(ChokePoint cp) {
        return GetGraph().GetPath(this, cp);
    }

    public Map GetMap() {
        return m_pGraph.GetMap();
    }

    // Assumes pBlocking->RemoveFromTiles() has been called
    public void OnBlockingNeutralDestroyed(Neutral pBlocking) {
//        bwem_assert(pBlocking && pBlocking->Blocking());
        if (!(pBlocking != null && pBlocking.Blocking())) {
            throw new IllegalStateException();
        }

        if (m_pBlockingNeutral.equals(pBlocking)) {
            // Ensures that in the case where several neutrals are stacked, m_pBlockingNeutral points to the bottom one:
            m_pBlockingNeutral = GetMap().getData().getTile(m_pBlockingNeutral.TopLeft()).getNeutral();

            if (m_pBlockingNeutral == null) {
                if (GetGraph().GetMap().AutomaticPathUpdate().booleanValue()) {
                    m_blocked = false;
                }
            }
        }
    }

	public Index Index() {
        return m_index;
    }

    public ChokePoint PathBackTrace() {
        return m_pPathBackTrace;
    }

	public void SetPathBackTrace(ChokePoint p) {
        m_pPathBackTrace = p;
    }

    private Graph GetGraph() {
        return m_pGraph;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChokePoint)) {
            return false;
        } else {
            ChokePoint that = (ChokePoint) object;
            boolean lel = this.m_Areas.getLeft().equals(that.m_Areas.getLeft());
            boolean ler = this.m_Areas.getLeft().equals(that.m_Areas.getRight());
            boolean rer = this.m_Areas.getRight().equals(that.m_Areas.getRight());
            boolean rel = this.m_Areas.getRight().equals(that.m_Areas.getLeft());
            return (((lel && rer) || (ler && rel))); /* true if area pairs are an exact match or if one pair is reversed. */
        }
    }

    @Override
    public int hashCode() {
        int id_left = m_Areas.getLeft().Id().intValue();
        int id_right = m_Areas.getRight().Id().intValue();
        if (id_left > id_right) {
            int tmp = id_left;
            id_left = id_right;
            id_right = tmp;
        }
        return Objects.hash(id_left, id_right);
    }

}
