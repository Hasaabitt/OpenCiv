package me.rhin.openciv.server.game.state;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import me.rhin.openciv.server.Server;
import me.rhin.openciv.server.game.AbstractPlayer;
import me.rhin.openciv.server.game.GameState;
import me.rhin.openciv.server.game.Player;
import me.rhin.openciv.server.game.ai.AIPlayer;
import me.rhin.openciv.server.game.ai.type.BarbarianPlayer;
import me.rhin.openciv.server.game.ai.type.CityStatePlayer;
import me.rhin.openciv.server.game.ai.type.CityStatePlayer.CityStateType;
import me.rhin.openciv.server.game.city.City;
import me.rhin.openciv.server.game.city.wonders.GameWonders;
import me.rhin.openciv.server.game.map.GameMap;
import me.rhin.openciv.server.game.map.tile.Tile;
import me.rhin.openciv.server.game.map.tile.Tile.TileTypeWrapper;
import me.rhin.openciv.server.game.map.tile.TileType;
import me.rhin.openciv.server.game.map.tile.TileType.TileProperty;
import me.rhin.openciv.server.game.options.GameOptionType;
import me.rhin.openciv.server.game.unit.AttackableEntity;
import me.rhin.openciv.server.game.unit.DeleteUnitOptions;
import me.rhin.openciv.server.game.unit.RangedUnit;
import me.rhin.openciv.server.game.unit.Unit;
import me.rhin.openciv.server.game.unit.type.Builder.BuilderUnit;
import me.rhin.openciv.server.game.unit.type.Settler.SettlerUnit;
import me.rhin.openciv.server.game.unit.type.TransportShipUnit;
import me.rhin.openciv.server.game.unit.type.Warrior.WarriorUnit;
import me.rhin.openciv.server.game.unit.type.WorkBoat.WorkBoatUnit;
import me.rhin.openciv.server.listener.BuyProductionItemListener;
import me.rhin.openciv.server.listener.CombatPreviewListener;
import me.rhin.openciv.server.listener.DisconnectListener;
import me.rhin.openciv.server.listener.EndTurnListener;
import me.rhin.openciv.server.listener.FetchPlayerListener;
import me.rhin.openciv.server.listener.NextTurnListener;
import me.rhin.openciv.server.listener.PlayerFinishLoadingListener;
import me.rhin.openciv.server.listener.PlayerListRequestListener;
import me.rhin.openciv.server.listener.RangedAttackListener;
import me.rhin.openciv.server.listener.RequestEndTurnListener;
import me.rhin.openciv.server.listener.SelectUnitListener;
import me.rhin.openciv.server.listener.SetProductionItemListener;
import me.rhin.openciv.server.listener.SettleCityListener;
import me.rhin.openciv.server.listener.TileStatlineListener;
import me.rhin.openciv.server.listener.UnitDisembarkListener;
import me.rhin.openciv.server.listener.UnitEmbarkListener;
import me.rhin.openciv.server.listener.UnitFinishedMoveListener.UnitFinishedMoveEvent;
import me.rhin.openciv.server.listener.UnitMoveListener;
import me.rhin.openciv.server.listener.UpgradeUnitListener;
import me.rhin.openciv.server.listener.WorkTileListener;
import me.rhin.openciv.shared.packet.type.AddUnitPacket;
import me.rhin.openciv.shared.packet.type.BuyProductionItemPacket;
import me.rhin.openciv.shared.packet.type.CombatPreviewPacket;
import me.rhin.openciv.shared.packet.type.EndTurnPacket;
import me.rhin.openciv.shared.packet.type.FetchPlayerPacket;
import me.rhin.openciv.shared.packet.type.GameStartPacket;
import me.rhin.openciv.shared.packet.type.MoveUnitPacket;
import me.rhin.openciv.shared.packet.type.NextTurnPacket;
import me.rhin.openciv.shared.packet.type.PlayerDisconnectPacket;
import me.rhin.openciv.shared.packet.type.PlayerListRequestPacket;
import me.rhin.openciv.shared.packet.type.RangedAttackPacket;
import me.rhin.openciv.shared.packet.type.RemoveTileTypePacket;
import me.rhin.openciv.shared.packet.type.RequestEndTurnPacket;
import me.rhin.openciv.shared.packet.type.SelectUnitPacket;
import me.rhin.openciv.shared.packet.type.SetProductionItemPacket;
import me.rhin.openciv.shared.packet.type.SetUnitHealthPacket;
import me.rhin.openciv.shared.packet.type.SettleCityPacket;
import me.rhin.openciv.shared.packet.type.TileStatlinePacket;
import me.rhin.openciv.shared.packet.type.TurnTimeLeftPacket;
import me.rhin.openciv.shared.packet.type.UnitDisembarkPacket;
import me.rhin.openciv.shared.packet.type.UnitEmbarkPacket;
import me.rhin.openciv.shared.packet.type.UpgradeUnitPacket;
import me.rhin.openciv.shared.packet.type.WorkTilePacket;
import me.rhin.openciv.shared.stat.Stat;
import me.rhin.openciv.shared.stat.StatLine;
import me.rhin.openciv.shared.util.MathHelper;

//FIXME: Instead of the civ game listening for everything. Just split them off into the respective classes. (EX: CombatPreviewListener in the Unit class)
//Or just use reflection so we don't have to implement 20+ classes.
public class InGameState extends GameState implements DisconnectListener, SelectUnitListener, UnitMoveListener,
		SettleCityListener, PlayerFinishLoadingListener, NextTurnListener, SetProductionItemListener, EndTurnListener,
		PlayerListRequestListener, FetchPlayerListener, CombatPreviewListener, WorkTileListener, RangedAttackListener,
		BuyProductionItemListener, RequestEndTurnListener, TileStatlineListener, UnitEmbarkListener,
		UnitDisembarkListener, UpgradeUnitListener {

	private int currentTurn;
	private int turnTimeLeft;
	private ScheduledExecutorService executor;
	private Runnable turnTimeRunnable;
	private GameMap map;
	private GameWonders gameWonders;

	public InGameState() {

		this.map = new GameMap();
		this.gameWonders = new GameWonders();

		Server.getInstance().getEventManager().addListener(DisconnectListener.class, this);
		Server.getInstance().getEventManager().addListener(SelectUnitListener.class, this);
		Server.getInstance().getEventManager().addListener(UnitMoveListener.class, this);
		Server.getInstance().getEventManager().addListener(SettleCityListener.class, this);
		Server.getInstance().getEventManager().addListener(PlayerFinishLoadingListener.class, this);
		Server.getInstance().getEventManager().addListener(NextTurnListener.class, this);
		Server.getInstance().getEventManager().addListener(SetProductionItemListener.class, this);
		Server.getInstance().getEventManager().addListener(EndTurnListener.class, this);
		Server.getInstance().getEventManager().addListener(PlayerListRequestListener.class, this);
		Server.getInstance().getEventManager().addListener(FetchPlayerListener.class, this);
		Server.getInstance().getEventManager().addListener(CombatPreviewListener.class, this);
		Server.getInstance().getEventManager().addListener(WorkTileListener.class, this);
		Server.getInstance().getEventManager().addListener(RangedAttackListener.class, this);
		Server.getInstance().getEventManager().addListener(BuyProductionItemListener.class, this);
		Server.getInstance().getEventManager().addListener(RequestEndTurnListener.class, this);
		Server.getInstance().getEventManager().addListener(TileStatlineListener.class, this);
		Server.getInstance().getEventManager().addListener(UnitEmbarkListener.class, this);
		Server.getInstance().getEventManager().addListener(UnitDisembarkListener.class, this);
		Server.getInstance().getEventManager().addListener(UpgradeUnitListener.class, this);
	}

	@Override
	public void onStateBegin() {
		this.currentTurn = 0;
		this.turnTimeLeft = 0;

		this.executor = Executors.newScheduledThreadPool(1);

		this.turnTimeRunnable = new Runnable() {
			public void run() {
				try {
					if (!playersLoaded())
						return;
					if (turnTimeLeft <= 0) {
						Server.getInstance().getEventManager().fireEvent(new NextTurnEvent());
					}

					TurnTimeLeftPacket turnTimeLeftPacket = new TurnTimeLeftPacket();
					turnTimeLeftPacket.setTime(turnTimeLeft);

					Json json = new Json();
					for (Player player : players)
						player.sendPacket(json.toJson(turnTimeLeftPacket));

					turnTimeLeft--;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		executor.scheduleAtFixedRate(turnTimeRunnable, 0, 1, TimeUnit.SECONDS);

		loadGame();
	}

	@Override
	public void onStateEnd() {
		Server.getInstance().getEventManager().clearListenersFromObject(this);
	}

	@Override
	public void stop() {
		// Revert back to lobby state.
	}

	// TODO: On player connection, don't allow him to join yet. Since we haven't
	// implemented hot joining.

	@Override
	public void onDisconnect(WebSocket conn) {
		Player removedPlayer = getPlayerByConn(conn);

		if (removedPlayer == null)
			return;

		Server.getInstance().getEventManager().removeListener(NextTurnListener.class, removedPlayer);
		players.remove(removedPlayer);

		for (Player player : players) {

			PlayerDisconnectPacket packet = new PlayerDisconnectPacket();
			packet.setPlayerName(removedPlayer.getName());
			Json json = new Json();
			player.sendPacket(json.toJson(packet));
		}

		if (players.size() < 1) {
			// TODO: Change State back to lobby. Or with popup window?
		}
	}

	// TODO: Move to map class
	@Override
	public void onUnitSelect(WebSocket conn, SelectUnitPacket packet) {

		// FIXME: Use a hashmap to get by unit name?
		Unit unit = map.getTiles()[packet.getGridX()][packet.getGridY()].getUnitFromID(packet.getUnitID());
		if (unit == null)
			return;
		Player player = getPlayerByConn(conn);
		if (!unit.getPlayerOwner().equals(player))
			return;

		player.setSelectedUnit(unit);
		unit.setSelected(true);

		Json json = new Json();
		conn.send(json.toJson(packet));
	}

	@Override
	public void onUnitMove(WebSocket conn, MoveUnitPacket packet) {

		Tile prevTile = map.getTiles()[packet.getPrevGridX()][packet.getPrevGridY()];

		Unit unit = prevTile.getUnitFromID(packet.getUnitID());

		if (unit == null) {
			System.out.println("Error: Unit is NULL");
			return;
		}

		// NOTE: We assume player owner is a MPPlayer
		Player playerOwner = (Player) unit.getPlayerOwner();

		Tile targetTile = map.getTiles()[packet.getTargetGridX()][packet.getTargetGridY()];
		Tile originalTargetTile = map.getTiles()[packet.getTargetGridX()][packet.getTargetGridY()];
		Json json = new Json();

		if (!targetTile.equals(unit.getTargetTile()))
			unit.setTargetTile(targetTile);

		// If were moving onto a unit or city. Stop the unit just outside the target
		// unit. Unless if were at war & we can kill or capture a unit, move onto the
		// tile.

		boolean stopOutside = false;

		if (targetTile.getAttackableEntity() != null) {
			AttackableEntity targetEntity = targetTile.getAttackableEntity();
			stopOutside = true;

			// If we are a capturable unit & were moving to a friendly NON-capturable unit
			// w/o existing units in it. Walk on it. (E.g. Builder moving to warrior)
			if (unit.isUnitCapturable(playerOwner) && targetEntity.getPlayerOwner().equals(playerOwner)
					&& (!(targetEntity instanceof City)) && !targetEntity.isUnitCapturable(playerOwner)
					&& targetEntity.getTile().getUnits().size() < 2) {
				stopOutside = false;

				// System.out.println("1");
			}

			// If the targetEntity dies & were at war w/ it. Walk on it. (E.g. Warrior
			// attacks 1% HP Enemy Warror)
			if (!targetEntity.surviveAttack(unit) && targetEntity.getPlayerOwner().getDiplomacy().atWar(playerOwner)) {
				stopOutside = false;

				// System.out.println("2");
			}

			// If the targetEntity is capturable, & if it's ours OR an enemies & moving unit
			// isn't capturable. Walk on it. (E.g. Warrior walks on enemy/friendly builder)
			if (targetEntity.isUnitCapturable(targetEntity.getPlayerOwner())
					&& (targetEntity.getPlayerOwner().equals(playerOwner)
							|| targetEntity.getPlayerOwner().getDiplomacy().atWar(playerOwner))
					&& !unit.isUnitCapturable(playerOwner)) {

				// System.out.println("3");
				stopOutside = false;
			}

			// If were moving into a friendly city. & There is room for a capturable & non
			// capturable unit. Walk on it. (E.g. Warrior walk into city w/ just 1 builder)
			if ((targetEntity instanceof City) && targetEntity.getPlayerOwner().equals(unit.getPlayerOwner())) {
				int capturableUnits = 0;
				int nonCaptureableUnits = 0;

				for (Unit cityUnit : targetEntity.getTile().getUnits()) {
					if (cityUnit.isUnitCapturable(playerOwner))
						capturableUnits++;
					else
						nonCaptureableUnits++;
				}

				if (capturableUnits < 1 && unit.isUnitCapturable(playerOwner)
						|| nonCaptureableUnits < 1 && !unit.isUnitCapturable(playerOwner)) {
					stopOutside = false;

					// System.out.println("4");
				}
			}
		}

		if (stopOutside) {
			targetTile = unit.getCameFromTiles()[targetTile.getGridX()][targetTile.getGridY()];
			if (targetTile != null) {
				unit.setTargetTile(targetTile);
			} else
				targetTile = unit.getStandingTile();
		}

		// Move to target tile
		if (unit.getMovement() >= unit.getPathMovement() && !unit.getStandingTile().equals(targetTile)) {

			unit.moveToTargetTile();
			playerOwner.setSelectedUnit(null);
			unit.reduceMovement(unit.getPathMovement());

			packet.setMovementCost(unit.getPathMovement());
			packet.setLocation(unit.getStandingTile().getGridX(), unit.getStandingTile().getGridY());

			for (Player player : players) {
				player.sendPacket(json.toJson(packet));
			}
		}

		// If the tile we are moving on has a capturable unit.
		if (targetTile.getCaptureableUnit(unit) != null) {
			Unit targetUnit = targetTile.getCaptureableUnit(unit);
			targetUnit.capture(playerOwner);
		}

		// Handle the targetTile being a enemy unit.
		if (originalTargetTile.getEnemyAttackableEntity(playerOwner) != null)
			if (!originalTargetTile.getEnemyAttackableEntity(playerOwner).getPlayerOwner()
					.equals(unit.getPlayerOwner())) {
				// We are about to attack this unit on the tile
				AttackableEntity targetEntity = originalTargetTile.getEnemyAttackableEntity(playerOwner);
				unit.attackEntity(targetEntity);
			}

		if (unit.getHealth() > 0) {

			// Handle capturing barbarian camps
			if (unit.getStandingTile().containsTileType(TileType.BARBARIAN_CAMP)) {
				unit.captureBarbarianCamp();
			}

			// Handle capturing ruins
			if (unit.getStandingTile().containsTileType(TileType.RUINS)) {
				Tile campTile = unit.getStandingTile();
				campTile.removeTileType(TileType.RUINS);

				RemoveTileTypePacket removeTileTypePacket = new RemoveTileTypePacket();
				removeTileTypePacket.setTile(TileType.RUINS.name(), campTile.getGridX(), campTile.getGridY());

				for (Player player : Server.getInstance().getPlayers())
					player.sendPacket(json.toJson(removeTileTypePacket));

				// TODO: Capture ruin sound effect.
				// FIXME: Players don't get gold if they don't have a city.
				unit.getPlayerOwner().getStatLine().addValue(Stat.GOLD, 50);
				playerOwner.updateOwnedStatlines(false);
			}

			Server.getInstance().getEventManager().fireEvent(new UnitFinishedMoveEvent(prevTile, unit));
		}

	}

	// TODO: Move to game map class?
	@Override
	public void onSettleCity(WebSocket conn, SettleCityPacket settleCityPacket) {
		// FIXME: The server needs to check if the unit has movement & is too close to
		// other cities.

		Tile tile = map.getTiles()[settleCityPacket.getGridX()][settleCityPacket.getGridY()];
		SettlerUnit unit = null;

		for (Unit currentUnit : tile.getUnits())
			if (currentUnit instanceof SettlerUnit)
				unit = (SettlerUnit) currentUnit;

		// The player is actually trying to hack if this is triggered
		if (unit == null)
			return;

		unit.settleCity();
	}

	@Override
	public void onPlayerFinishLoading(WebSocket conn) {
		getPlayerByConn(conn).finishLoading();
	}

	@Override
	public void onNextTurn() {

		Json json = new Json();

		currentTurn++;
		turnTimeLeft = getUpdatedTurnTime();

		NextTurnPacket turnTimeUpdatePacket = new NextTurnPacket();
		turnTimeUpdatePacket.setTurnTime(turnTimeLeft);
		for (Player player : players) {
			player.sendPacket(json.toJson(turnTimeUpdatePacket));
		}

		TurnTimeLeftPacket turnTimeLeftPacket = new TurnTimeLeftPacket();
		turnTimeLeftPacket.setTime(turnTimeLeft);

		for (Player player : players)
			player.sendPacket(json.toJson(turnTimeLeftPacket));

		for (Player player : players)
			player.setTurnDone(false);
	}

	// TODO: Move to producibleItemManager
	@Override
	public void onSetProductionItem(WebSocket conn, SetProductionItemPacket packet) {
		City targetCity = getCityFromName(packet.getCityName());

		// TODO: Verify if the item can be produced.

		if (targetCity == null)
			return;

		targetCity.getProducibleItemManager().setProducingItem(packet.getItemName());

		Json json = new Json();
		conn.send(json.toJson(packet));
	}

	// TODO: Move to producibleItemManager
	@Override
	public void onBuyProductionItem(WebSocket conn, BuyProductionItemPacket packet) {

		// TODO: Verify if the player owns that city.
		City targetCity = getCityFromName(packet.getCityName());

		// TODO: Verify if the item can be produced.

		if (targetCity == null)
			return;

		targetCity.getProducibleItemManager().buyProducingItem(packet.getItemName());

		// Json json = new Json();
		// conn.send(json.toJson(packet));
	}

	@Override
	public void onEndTurn(WebSocket conn, EndTurnPacket packet) {
		Server.getInstance().getEventManager().fireEvent(new NextTurnEvent());
	}

	@Override
	public void onRequestEndTurn(WebSocket conn, RequestEndTurnPacket packet) {
		Player player = Server.getInstance().getPlayerByConn(conn);
		player.setTurnDone(true);

		packet.setPlayerName(player.getName());

		Json json = new Json();
		for (Player otherPlayer : players)
			otherPlayer.sendPacket(json.toJson(packet));

		boolean playersTurnsDone = true;
		for (Player otherPlayer : players)
			if (!otherPlayer.isTurnDone())
				playersTurnsDone = false;

		if (playersTurnsDone)
			Server.getInstance().getEventManager().fireEvent(new NextTurnEvent());
	}

	@Override
	public void onPlayerListRequested(WebSocket conn, PlayerListRequestPacket packet) {
		System.out.println("[SERVER] Player list requested");
		for (Player player : players) {
			packet.addPlayer(player.getName(), player.getCiv().getName().toUpperCase(), false);
		}

		for (AIPlayer aiPlayer : aiPlayers) {
			packet.addPlayer(aiPlayer.getName(), aiPlayer.getCiv().getName().toUpperCase(), true);
		}

		Json json = new Json();
		conn.send(json.toJson(packet));
	}

	@Override
	public void onPlayerFetch(WebSocket conn, FetchPlayerPacket packet) {
		System.out.println("[SERVER] Fetching player...");
		Player player = getPlayerByConn(conn);
		packet.setPlayerName(player.getName());
		Json json = new Json();
		conn.send(json.toJson(packet));
	}

	// TODO: Split this up into the Unit class
	@Override
	public void onCombatPreview(WebSocket conn, CombatPreviewPacket packet) {

		AttackableEntity attackingEntity = map.getTiles()[packet.getUnitGridX()][packet.getUnitGridY()]
				.getUnitFromID(packet.getUnitID());

		AttackableEntity targetEntity = map.getTiles()[packet.getTargetGridX()][packet.getTargetGridY()]
				.getAttackableEntityFromID(packet.getTargetID());

		if (attackingEntity == null || targetEntity == null)
			return;

		if (attackingEntity instanceof RangedUnit) {
			packet.setUnitDamage(0);
		} else
			packet.setUnitDamage(attackingEntity.getDamageTaken(targetEntity, false));

		packet.setTargetDamage(targetEntity.getDamageTaken(attackingEntity, true));

		Json json = new Json();
		conn.send(json.toJson(packet));
	}

	@Override
	public void onRangedAttack(WebSocket conn, RangedAttackPacket packet) {

		AttackableEntity attackingEntity = map.getTiles()[packet.getUnitGridX()][packet.getUnitGridY()]
				.getUnitFromID(packet.getUnitID());
		AttackableEntity targetEntity = map.getTiles()[packet.getTargetGridX()][packet.getTargetGridY()]
				.getAttackableEntityFromID(packet.getTargetID());

		if (!(attackingEntity instanceof RangedUnit))
			return;

		RangedUnit rangedUnit = (RangedUnit) attackingEntity;
		rangedUnit.rangeAttack(targetEntity);
	}

	@Override
	public void onWorkTile(WebSocket conn, WorkTilePacket packet) {

		// TODO: !! We need to do territory & ownership &research checks like in
		// clientside.

		Tile tile = map.getTiles()[packet.getGridX()][packet.getGridY()];

		for (Unit unit : new ArrayList<>(tile.getUnits())) {
			if (unit instanceof BuilderUnit) {
				BuilderUnit builder = (BuilderUnit) unit;
				builder.setBuilding(true);
				builder.setImprovement(packet.getImprovementType());
				builder.reduceMovement(2);
			}

			if (unit instanceof WorkBoatUnit) {
				WorkBoatUnit workBoat = (WorkBoatUnit) unit;
				workBoat.improveTile(packet.getImprovementType());
				workBoat.reduceMovement(2);
			}
		}
	}

	@Override
	public void onRequestTileStatline(WebSocket conn, TileStatlinePacket packet) {

		if (packet.getGridX() >= map.getWidth() || packet.getGridY() >= map.getHeight())
			return;

		Tile tile = map.getTiles()[packet.getGridX()][packet.getGridY()];

		packet.setGeographyName(tile.getGeograpgyName());

		StatLine tileStatLine = (tile.getTerritory() != null ? tile.getTerritory().getTileStatLine(tile)
				: tile.getStatLine());

		for (Stat stat : tileStatLine.getStatValues().keySet()) {
			packet.addStat(stat.name(), tileStatLine.getStatValues().get(stat).getValue());
		}

		Json json = new Json();
		getPlayerByConn(conn).sendPacket(json.toJson(packet));
	}

	@Override
	public void onUnitEmbark(WebSocket conn, UnitEmbarkPacket packet) {
		Player unitPlayer = Server.getInstance().getPlayerByConn(conn);
		Unit unit = map.getTiles()[packet.getTileGridX()][packet.getTileGridY()].getUnitFromID(packet.getUnitID());

		Tile embarkTile = null;

		for (Tile tile : unit.getTile().getAdjTiles()) {
			if (tile.containsTileType(TileType.SHALLOW_OCEAN) && tile.getUnits().size() < 1)
				embarkTile = tile;
		}

		if (embarkTile == null)
			return;

		TransportShipUnit transportShip = new TransportShipUnit(unitPlayer, unit, embarkTile, unit.getTile());
		embarkTile.addUnit(transportShip);

		unit.deleteUnit(DeleteUnitOptions.SERVER_DELETE, DeleteUnitOptions.KEEP_LISTENERS);

		AddUnitPacket addUnitPacket = new AddUnitPacket();
		addUnitPacket.setUnit(unitPlayer.getName(), transportShip.getName(), transportShip.getID(),
				embarkTile.getGridX(), embarkTile.getGridY());

		Json json = new Json();
		for (Player player : Server.getInstance().getPlayers())
			player.sendPacket(json.toJson(addUnitPacket));
	}

	@Override
	public void onUnitDisembark(WebSocket conn, UnitDisembarkPacket packet) {
		Player unitPlayer = Server.getInstance().getPlayerByConn(conn);

		Unit transportUnit = map.getTiles()[packet.getTileGridX()][packet.getTileGridY()]
				.getUnitFromID(packet.getUnitID());

		if (!(transportUnit instanceof TransportShipUnit))
			return;

		TransportShipUnit transportShip = (TransportShipUnit) transportUnit;

		Tile disembarkTile = null;
		ArrayList<Tile> disembarkTileCanidates = new ArrayList<>();
		Tile prevTile = transportShip.getPrevTile();

		for (Tile tile : transportShip.getTile().getAdjTiles()) {
			if (!tile.containsTileProperty(TileProperty.WATER) && tile.getUnits().size() < 1
					&& tile.getMovementCost(transportShip.getStandingTile()) < 1000)
				disembarkTileCanidates.add(tile);
		}

		if (disembarkTileCanidates.size() < 1)
			return;

		disembarkTile = disembarkTileCanidates.get(0);

		// > 1 tells us we went right < 1 tells us we went left.
		float embarkXDiff = transportShip.getTile().getGridX() - prevTile.getGridX();

		// Pick a tile that has the same + or - sign as embark diff
		for (Tile tile : disembarkTileCanidates) {

			float disembarkXDiff = tile.getGridX() - transportShip.getTile().getGridX();

			if (embarkXDiff < 0 && disembarkXDiff < 0) {
				disembarkTile = tile;
				break;
			}

			if (embarkXDiff > 0 && disembarkXDiff > 0) {
				disembarkTile = tile;
				break;
			}
		}

		if (disembarkTile == null)
			return;

		transportUnit.deleteUnit(DeleteUnitOptions.SERVER_DELETE);

		Unit unit = transportShip.getTransportUnit();
		unit.setMovement(0);

		disembarkTile.addUnit(unit);
		unitPlayer.addOwnedUnit(unit);
		unit.setStandingTile(disembarkTile);

		AddUnitPacket addUnitPacket = new AddUnitPacket();
		addUnitPacket.setUnit(unitPlayer.getName(), unit.getName(), unit.getID(), disembarkTile.getGridX(),
				disembarkTile.getGridY());
		addUnitPacket.setUnitMovement(0);

		Json json = new Json();
		for (Player player : Server.getInstance().getPlayers())
			player.sendPacket(json.toJson(addUnitPacket));
	}

	@Override
	public void onUnitUpgrade(WebSocket conn, UpgradeUnitPacket packet) {
		Tile tile = map.getTiles()[packet.getTileGridX()][packet.getTileGridY()];
		Unit oldUnit = tile.getUnitFromID(packet.getUnitID());

		oldUnit.upgrade();
	}

	@Override
	public String toString() {
		return "InGame";
	}

	public GameWonders getWonders() {
		return gameWonders;
	}

	public int getCurrentTurn() {
		return currentTurn;
	}

	public GameMap getMap() {
		return map;
	}

	public City getCityFromName(String cityName) {

		for (AbstractPlayer player : Server.getInstance().getAbstractPlayers()) {
			for (City city : player.getOwnedCities()) {
				if (city.getName().equals(cityName))
					return city;
			}

		}

		return null;
	}

	private boolean playersLoaded() {

		for (Player player : players)
			if (!player.isLoaded())
				return false;

		return true;
	}

	private int getMaxPlayerCities() {
		int maxCities = 0;

		for (AbstractPlayer player : Server.getInstance().getAbstractPlayers()) {
			maxCities = player.getOwnedCities().size();
		}

		return maxCities;
	}

	private int getMaxPlayerUnits() {
		int maxUnits = 0;
		for (Player player : players)
			if (player.getOwnedUnits().size() > maxUnits)
				maxUnits = player.getOwnedUnits().size();
		return maxUnits;
	}

	private int getUpdatedTurnTime() {

		int turnLengthOffset = Server.getInstance().getGameOptions().getOption(GameOptionType.TURN_LENGTH_OFFSET);

		if (turnLengthOffset < 0) {
			int cityMultiplier = 1;
			int unitMultiplier = 1;
			return 25 + getMaxPlayerCities() * cityMultiplier + getMaxPlayerUnits() * unitMultiplier;
		} else if (turnLengthOffset == 0) {
			// Infinite turn length
			return Integer.MAX_VALUE;
		} else {
			return 30 + (5 * turnLengthOffset);
		}
	}

	private void loadGame() {
		System.out.println("[SERVER] Starting game...");

		// Add AI
		Random rnd = new Random();
		for (int i = 0; i < Server.getInstance().getGameOptions().getOption(GameOptionType.CITY_STATE_AMOUNT); i++) {
			CityStateType type = CityStateType.values()[rnd.nextInt(CityStateType.values().length)];
			getAIPlayers().add(new CityStatePlayer(type));
		}

		getAIPlayers().add(new BarbarianPlayer());

		map.generateTerrain();

		// Start the game
		Json json = new Json();
		GameStartPacket gameStartPacket = new GameStartPacket();
		for (Player player : players) {
			player.sendPacket(json.toJson(gameStartPacket));
		}

		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			Rectangle rect = map.getMapPartition().get(i);

			// TODO: Be able to get river tiles AND tiles of a specific type
			ArrayList<Tile> riverTiles = new ArrayList<>(map.getTileIndexer().getAdjacentRiverTiles());

			// Check if the tile is within our bounds.
			int minX = (int) (rect.getX());
			int minY = (int) (rect.getY());
			int maxX = (int) (rect.getX() + rect.getWidth());
			int maxY = (int) (rect.getY() + rect.getHeight());

			int iterations = 0;
			while (true) {
				for (Tile tile : riverTiles) {
					if (tile.getGridX() > minX && tile.getGridX() < maxX && tile.getGridY() > minY
							&& tile.getGridY() < maxY) {

						boolean adjToBias = false;

						if (player.getCiv().getBiasTileType() != null) {
							for (Tile adjTile : tile.getAdjTiles())
								if (adjTile.containsTileType(player.getCiv().getBiasTileType()))
									adjToBias = true;
						} else
							adjToBias = true; // If we don't have a bias, just set we are adj no matter what.

						boolean adjToTundra = tile.getBaseTileType() == TileType.TUNDRA
								|| tile.getBaseTileType() == TileType.TUNDRA_HILL; // Can still be false.
						for (Tile adjTile : tile.getAdjTiles()) {
							if (adjTile.getBaseTileType() == TileType.TUNDRA
									|| adjTile.getBaseTileType() == TileType.TUNDRA_HILL)
								adjToTundra = true;
						}

						// TODO: Implement impassable tile property
						if (adjToBias && !adjToTundra) {
							if (!tile.containsTileProperty(TileProperty.WATER)
									&& !tile.containsTileType(TileType.MOUNTAIN)
									&& !tile.containsTileType(TileType.BARBARIAN_CAMP))
								player.setSpawnPos(tile.getGridX(), tile.getGridY());
							break;

						}
					}
				}

				if (player.hasSpawnPos() || iterations == 0)
					break;

				iterations++;
			}

			// FIXME: We might not be able to spawn the player within the bounds.
			if (!player.hasSpawnPos()) {
				System.out.println("No suitable spawn pos for: " + player.getName());
				while (true) {
					// Just pick a random tile on the map

					int rndX = rnd.nextInt(maxX - minX + 1) + minX;
					int rndY = rnd.nextInt(maxY - minY + 1) + minY;
					Tile tile = map.getTiles()[rndX][rndY];
					if (hasSafeTile(tile)) {
						player.setSpawnPos(tile.getGridX(), tile.getGridY());
						break;
					}
				}
			}
		}

		// Debug code
		if (players.size() > 1) {
			// players.get(0).setSpawnPos(players.get(1).getSpawnX() + 2,
			// players.get(1).getSpawnY() + 2);
		}

		// Give players a warrior unit
		for (Player player : players) {
			Tile tile = map.getTiles()[player.getSpawnX()][player.getSpawnY()];
			tile.addUnit(new SettlerUnit(player, tile));

			for (Tile adjTile : tile.getAdjTiles()) {
				if (!adjTile.getBaseTileType().hasProperty(TileProperty.WATER)
						&& !adjTile.containsTileType(TileType.MOUNTAIN)) {
					adjTile.addUnit(new WarriorUnit(player, adjTile));
					break;
				}
			}
		}

		// Spawn city state AI
		for (AIPlayer aiPlayer : Server.getInstance().getAIPlayers()) {
			if (aiPlayer instanceof CityStatePlayer) {
				CityStatePlayer cityStatePlayer = (CityStatePlayer) aiPlayer;

				// Pick random tile to spawn AI
				Tile tile = null;

				while (tile == null || tile.containsTileProperty(TileProperty.WATER)
						|| tile.containsTileType(TileType.MOUNTAIN) || tile.getUnits().size() > 0
						|| tile.getNearbyUnits().size() > 0) {
					int x = rnd.nextInt(map.getWidth());
					int y = rnd.nextInt(map.getHeight());
					tile = map.getTiles()[x][y];
				}

				Unit settlerUnit = new SettlerUnit(cityStatePlayer, tile);
				tile.addUnit(settlerUnit);

				// Unit archerUnit = new ArcherUnit(cityStatePlayer, tile);
				// tile.addUnit(archerUnit);

				Unit warriorUnit = new WarriorUnit(cityStatePlayer,
						map.getTiles()[tile.getGridX() + 1][tile.getGridY()]);
				map.getTiles()[tile.getGridX() + 1][tile.getGridY()].addUnit(warriorUnit);

				aiPlayer.setSpawnPos(tile.getGridX(), tile.getGridY());
			}
		}

		// FIXME: Rewrite this.
		ArrayList<AbstractPlayer> allPlayers = new ArrayList<>();
		allPlayers.addAll(players);
		allPlayers.addAll(aiPlayers);
		for (AbstractPlayer player : allPlayers) {

			if (player instanceof BarbarianPlayer)
				continue;

			// Add two luxuries around the player
			int assignedLuxTiles = 0;
			int assignedResourceTiles = 0;
			int loopLimit = 500;
			while ((assignedLuxTiles < 3 || assignedResourceTiles < 4) && loopLimit > 0) {

				int randX = rnd.nextInt(5) - 3;
				int randY = rnd.nextInt(5) - 3;

				int x = MathHelper.clamp(player.getSpawnX() + randX, 0, map.getWidth());
				int y = MathHelper.clamp(player.getSpawnY() + randY, 0, map.getHeight());
				Tile tile = map.getTiles()[x][y];

				TileType newTileType = null;

				if (assignedLuxTiles < 3) {
					newTileType = TileType.getRandomLandLuxuryTile();
				} else
					newTileType = TileType.getRandomResourceTile();

				if (!tile.containsTileType(newTileType.getSpawnTileTypes()))
					continue;

				for (TileTypeWrapper tileWrapper : tile.getTileTypeWrappers())
					if (tileWrapper.getTileType().hasProperty(TileProperty.LUXURY)
							|| tileWrapper.getTileType().hasProperty(TileProperty.RESOURCE)
							|| tileWrapper.getTileType() == TileType.BARBARIAN_CAMP)
						continue;

				tile.setTileType(newTileType);

				if (newTileType.hasProperty(TileProperty.RESOURCE))
					assignedResourceTiles++;
				else
					assignedLuxTiles++;

				loopLimit--;
			}
		}
	}

	private boolean hasSafeTile(Tile tile) {
		// If the tile itself isn't safe, return false.
		if (tile.containsTileProperty(TileProperty.WATER) || tile.containsTileType(TileType.MOUNTAIN))
			return false;

		boolean hasSafeTile = false;
		for (Tile adjTile : tile.getAdjTiles())
			if (!adjTile.containsTileProperty(TileProperty.WATER) && !adjTile.containsTileType(TileType.MOUNTAIN))
				hasSafeTile = true;

		return hasSafeTile;
	}
}
