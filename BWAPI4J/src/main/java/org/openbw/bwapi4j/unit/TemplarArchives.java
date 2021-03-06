package org.openbw.bwapi4j.unit;

import org.openbw.bwapi4j.type.TechType;
import org.openbw.bwapi4j.type.UnitType;
import org.openbw.bwapi4j.type.UpgradeType;

public class TemplarArchives extends Building implements Mechanical, ResearchingFacility {

    private Researcher researcher;
    
    protected TemplarArchives(int id, int timeSpotted) {
        
        super(id, UnitType.Protoss_Templar_Archives, timeSpotted);
        this.researcher = new Researcher();
    }

    @Override
    public void update(int[] unitData, int index, int frame) {

        this.researcher.update(unitData, index);
        super.update(unitData, index, frame);
    }
    
    public boolean researchPsionicStorm() {
        
        return this.researcher.research(TechType.Psionic_Storm);
    }

	public boolean researchHallucination() {

		return this.researcher.research(TechType.Hallucination);
	}

	public boolean researchMaelstrom() {

		return this.researcher.research(TechType.Maelstrom);
	}

	public boolean researchMindControl() {

		return this.researcher.research(TechType.Mind_Control);
	}

	public boolean upgradeKhaydarinAmulet() {

		return this.researcher.upgrade(UpgradeType.Khaydarin_Amulet);
	}
	
	public boolean upgradeArgusTalisman() {

		return this.researcher.upgrade(UpgradeType.Argus_Talisman);
	}
	
    @Override
    public boolean isUpgrading() {
        
        return this.researcher.isUpgrading();
    }

    @Override
    public boolean isResearching() {
        
        return this.researcher.isResearching();
    }

    @Override
    public boolean cancelResearch() {
        
        return this.researcher.cancelResearch();
    }

    @Override
    public boolean cancelUpgrade() {
        
        return this.researcher.cancelUpgrade();
    }

    @Override
    public boolean canResearch(TechType techType) {
        return this.researcher.canResearch(techType);
    }

    @Override
    public boolean canUpgrade(UpgradeType upgradeType) {
        return this.researcher.canUpgrade(upgradeType);
    }

    @Override
    public boolean research(TechType techType) {
        return this.researcher.research(techType);
    }

    @Override
    public boolean upgrade(UpgradeType upgradeType) {
        return this.researcher.upgrade(upgradeType);
    }

    @Override
    public UpgradeInProgress getUpgradeInProgress() {
        return researcher.getUpgradeInProgress();
    }

    @Override
    public ResearchInProgress getResearchInProgress() {
        return researcher.getResearchInProgress();
    }
}
