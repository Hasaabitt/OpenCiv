package me.rhin.openciv.game.research.type;

import com.badlogic.gdx.graphics.g2d.Sprite;

import me.rhin.openciv.asset.TextureEnum;
import me.rhin.openciv.game.research.ResearchTree;
import me.rhin.openciv.game.research.Technology;
import me.rhin.openciv.game.research.TreePosition;

public class IronWorkingTech extends Technology {

	public IronWorkingTech(ResearchTree researchTree) {
		super(researchTree, new TreePosition(3, 0));
		
		requiredTechs.add(BronzeWorkingTech.class);
	}

	@Override
	public int getScienceCost() {
		return 195;
	}

	@Override
	public String getName() {
		return "Iron Working";
	}

	@Override
	public Sprite getIcon() {
		return TextureEnum.UNIT_SWORDSMAN.sprite();
	}

	@Override
	public String getDesc() {
		return "- Unlocks swordsman \n- Unlocks Hero Epic \n- Unlocks Colossus";
	}

}
