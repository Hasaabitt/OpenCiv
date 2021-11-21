package me.rhin.openciv.game.research.type;

import com.badlogic.gdx.graphics.g2d.Sprite;

import me.rhin.openciv.asset.TextureEnum;
import me.rhin.openciv.game.research.ResearchTree;
import me.rhin.openciv.game.research.Technology;

public class PotteryTech extends Technology {

	public PotteryTech(ResearchTree researchTree) {
		super(researchTree);
	}

	@Override
	public int getScienceCost() {
		return 35;
	}

	@Override
	public String getName() {
		return "Pottery";
	}

	@Override
	public Sprite getIcon() {
		return TextureEnum.BUILDING_GRANARY.sprite();
	}

	@Override
	public String getDesc() {
		return "- Unlocks granary building \n" + "- Unlocks shrine building";
	}
}
