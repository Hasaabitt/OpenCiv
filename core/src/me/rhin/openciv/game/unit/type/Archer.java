package me.rhin.openciv.game.unit.type;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;

import me.rhin.openciv.Civilization;
import me.rhin.openciv.asset.TextureEnum;
import me.rhin.openciv.game.AbstractAction;
import me.rhin.openciv.game.city.City;
import me.rhin.openciv.game.map.tile.Tile;
import me.rhin.openciv.game.map.tile.Tile.TileTypeWrapper;
import me.rhin.openciv.game.map.tile.TileType;
import me.rhin.openciv.game.map.tile.TileType.TileProperty;
import me.rhin.openciv.game.unit.RangedUnit;
import me.rhin.openciv.game.unit.Unit;
import me.rhin.openciv.game.unit.UnitItem;
import me.rhin.openciv.game.unit.UnitParameter;
import me.rhin.openciv.listener.LeftClickListener;
import me.rhin.openciv.listener.RelativeMouseMoveListener;
import me.rhin.openciv.listener.RightClickListener;
import me.rhin.openciv.listener.SelectUnitListener;
import me.rhin.openciv.listener.UnitActListener.UnitActEvent;
import me.rhin.openciv.shared.packet.type.RangedAttackPacket;
import me.rhin.openciv.shared.packet.type.SelectUnitPacket;
import me.rhin.openciv.ui.window.type.UnitCombatWindow;
import me.rhin.openciv.util.ClickType;

public class Archer extends UnitItem {

	public Archer(City city) {
		super(city);
	}

	public static class ArcherUnit extends Unit implements RangedUnit, LeftClickListener, RightClickListener,
			SelectUnitListener, RelativeMouseMoveListener {

		private UntargetAction untargetAction;
		private boolean targeting;
		private Unit rangedTarget;

		public ArcherUnit(UnitParameter unitParameter) {
			super(unitParameter, TextureEnum.UNIT_ARCHER);

			this.untargetAction = new UntargetAction(this);

			customActions.add(new TargetAction(this));
			customActions.add(untargetAction);

			this.canAttack = true;
			this.targeting = false;

			Civilization.getInstance().getEventManager().addListener(LeftClickListener.class, this);
			Civilization.getInstance().getEventManager().addListener(RightClickListener.class, this);
			Civilization.getInstance().getEventManager().addListener(SelectUnitListener.class, this);
			Civilization.getInstance().getEventManager().addListener(RelativeMouseMoveListener.class, this);
		}

		@Override
		public int getMovementCost(Tile prevTile, Tile tile) {
			if (tile.containsTileProperty(TileProperty.WATER))
				return 1000000;
			else
				return tile.getMovementCost(prevTile);
		}

		@Override
		public int getCombatStrength() {
			return 14;
		}

		@Override
		public int getRangedCombatStrength() {
			return 7;
		}

		@Override
		public boolean isRangedUnit() {
			return true;
		}

		@Override
		public boolean hasRangedTarget() {
			return rangedTarget != null;
		}

		@Override
		public void onLeftClick(float x, float y) {

			if (rangedTarget == null || getCurrentMovement() < 1)
				return;

			reduceMovement(2);
			getPlayerOwner().unselectUnit();

			RangedAttackPacket packet = new RangedAttackPacket();
			packet.setUnit(getID(), standingTile.getGridX(), standingTile.getGridY());
			packet.setTargetUnit(rangedTarget.getID(), rangedTarget.getStandingTile().getGridX(),
					rangedTarget.getStandingTile().getGridY());

			Civilization.getInstance().getNetworkManager().sendPacket(packet);

			addAction(untargetAction);
		}

		@Override
		public void onRightClick(ClickType clickType, int x, int y) {
			if (untargetAction.canAct())
				addAction(untargetAction);
		}

		@Override
		public void onSelectUnit(SelectUnitPacket packet) {
			if (untargetAction.canAct())
				addAction(untargetAction);
		}

		@Override
		public void onRelativeMouseMove(float x, float y) {
			Tile tile = Civilization.getInstance().getGame().getPlayer().getHoveredTile();

			if (!targeting || tile == null || tile.getUnits().size() < 1 || !tile.hasRangedTarget()) {
				rangedTarget = null;
				getTargetSelectionSprite().setColor(Color.YELLOW);
				Civilization.getInstance().getWindowManager().closeWindow(UnitCombatWindow.class);
				return;
			}

			if (!tile.getNextUnit().getPlayerOwner().equals(getPlayerOwner())
					&& (rangedTarget == null || !rangedTarget.equals(tile.getNextUnit()))) {
				rangedTarget = tile.getNextUnit();

				getTargetSelectionSprite().setPosition(tile.getVectors()[0].x - tile.getWidth() / 2,
						tile.getVectors()[0].y + 4);
				getTargetSelectionSprite().setSize(tile.getWidth(), tile.getHeight());
				getTargetSelectionSprite().setColor(Color.RED);

				// Popup combat preview window
				Civilization.getInstance().getWindowManager().closeWindow(UnitCombatWindow.class);
				Civilization.getInstance().getWindowManager().addWindow(new UnitCombatWindow(this, rangedTarget));
			}
		}

		@Override
		public void setRangedTarget(Unit rangedTarget) {
			this.rangedTarget = rangedTarget;
		}

		@Override
		public void setTargeting(boolean targeting) {
			this.targeting = targeting;
		}

		@Override
		public boolean isTargeting() {
			return targeting;
		}
	}

	// TODO: Move to individual class
	public static class TargetAction extends AbstractAction {

		public TargetAction(Unit unit) {
			super(unit);
		}

		@Override
		public boolean act(float delta) {

			((ArcherUnit) unit).setTargeting(true);

			boolean isHill = unit.getStandingTile().getBaseTileType() == TileType.GRASS_HILL
					|| unit.getStandingTile().getBaseTileType() == TileType.DESERT_HILL
					|| unit.getStandingTile().getBaseTileType() == TileType.PLAINS_HILL;

			for (Tile tile : unit.getStandingTile().getAdjTiles()) {

				boolean denyVisibility = false;
				for (TileTypeWrapper wrapper : tile.getTileTypeWrappers())
					if (wrapper.getTileType().getMovementCost() > 1 && !tile.equals(unit.getStandingTile())) {
						denyVisibility = true;
					}

				// FIXME: confusing name. maybe like, setRangedVisibiltiy ?
				tile.setRangedTarget(true);

				if (denyVisibility && !isHill)
					continue;

				for (Tile adjTile : tile.getAdjTiles()) {

					if (adjTile == null)
						continue;

					adjTile.setRangedTarget(true);
				}
			}

			Civilization.getInstance().getEventManager().fireEvent(new UnitActEvent(unit));
			unit.removeAction(this);
			return true;
		}

		@Override
		public boolean canAct() {

			ArcherUnit archerUnit = (ArcherUnit) unit;

			if (archerUnit.getCurrentMovement() < 1 || archerUnit.isTargeting()) {
				return false;
			}

			return true;
		}

		@Override
		public String getName() {
			return "Target";
		}
	}

	public static class UntargetAction extends AbstractAction {

		public UntargetAction(Unit unit) {
			super(unit);
		}

		@Override
		public boolean act(float delta) {

			ArcherUnit archerUnit = (ArcherUnit) unit;

			archerUnit.setTargeting(false);
			archerUnit.setRangedTarget(null);

			for (Tile tile : unit.getStandingTile().getAdjTiles()) {

				// TODO: Check if this overrides other archers ranged targets.
				tile.setRangedTarget(false);

				for (Tile adjTile : tile.getAdjTiles()) {

					if (adjTile == null)
						continue;

					adjTile.setRangedTarget(false);
				}
			}

			Civilization.getInstance().getEventManager().fireEvent(new UnitActEvent(unit));
			unit.removeAction(this);
			return true;
		}

		@Override
		public boolean canAct() {
			return ((ArcherUnit) unit).isTargeting();
		}

		@Override
		public String getName() {
			return "Untarget";
		}
	}

	@Override
	public int getProductionCost() {
		return 40;
	}

	@Override
	public boolean meetsProductionRequirements() {
		return true;
		// return
		// city.getPlayerOwner().getResearchTree().hasResearched(ArcheryTech.class);
	}

	@Override
	public String getName() {
		return "Archer";
	}

	@Override
	public TextureEnum getTexture() {
		return TextureEnum.UNIT_ARCHER;
	}
}