package me.rhin.openciv.game.city.building.type;

import me.rhin.openciv.asset.TextureEnum;
import me.rhin.openciv.game.city.City;
import me.rhin.openciv.game.city.building.Building;
import me.rhin.openciv.game.research.type.MetalCastingTech;
import me.rhin.openciv.shared.stat.Stat;

public class Workshop extends Building {

	public Workshop(City city) {
		super(city);
		
		this.statLine.addValue(Stat.MAINTENANCE, 2);
		this.statLine.addValue(Stat.PRODUCTION_GAIN, 2);
	}

	@Override
	public TextureEnum getTexture() {
		return TextureEnum.BUILDING_WORKSHOP;
	}

	@Override
	public boolean meetsProductionRequirements() {
		return city.getPlayerOwner().getResearchTree().hasResearched(MetalCastingTech.class);
	}

	@Override
	public String getDesc() {
		return "Improves production in the city.\n+2 Production\n+10% Production gain";
	}

	@Override
	public float getGoldCost() {
		return 250;
	}

	@Override
	public float getBuildingProductionCost() {
		return 120;
	}

	@Override
	public String getName() {
		return "Workshop";
	}

}
