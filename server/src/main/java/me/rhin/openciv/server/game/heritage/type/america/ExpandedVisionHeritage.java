package me.rhin.openciv.server.game.heritage.type.america;

import me.rhin.openciv.server.game.Player;
import me.rhin.openciv.server.game.heritage.Heritage;

public class ExpandedVisionHeritage extends Heritage {

	// TODO: Use listeners and apply stats & values through events.

	public ExpandedVisionHeritage(Player player) {
		super(player);
	}

	@Override
	public int getLevel() {
		return 1;
	}

	@Override
	public String getName() {
		return "Expanded Vision";
	}

	@Override
	public float getCost() {
		return 40;
	}

	@Override
	protected void onStudied() {
		//TODO: Not sure yet.
	}
}