package me.rhin.openciv.ui.screen.type;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;

import me.rhin.openciv.Civilization;
import me.rhin.openciv.listener.GameStartListener;
import me.rhin.openciv.listener.LeftClickListener.LeftClickEvent;
import me.rhin.openciv.listener.MouseMoveListener.MouseMoveEvent;
import me.rhin.openciv.listener.PlayerConnectListener;
import me.rhin.openciv.listener.PlayerDisconnectListener;
import me.rhin.openciv.listener.PlayerListRequestListener;
import me.rhin.openciv.shared.listener.EventManager;
import me.rhin.openciv.shared.packet.type.MapRequestPacket;
import me.rhin.openciv.shared.packet.type.PlayerConnectPacket;
import me.rhin.openciv.shared.packet.type.PlayerDisconnectPacket;
import me.rhin.openciv.shared.packet.type.PlayerListRequestPacket;
import me.rhin.openciv.ui.button.ButtonManager;
import me.rhin.openciv.ui.button.type.MPStartButton;
import me.rhin.openciv.ui.label.CustomLabel;
import me.rhin.openciv.ui.overlay.TitleOverlay;
import me.rhin.openciv.ui.screen.AbstractScreen;
import me.rhin.openciv.ui.screen.ScreenEnum;

public class ServerLobbyScreen extends AbstractScreen
		implements PlayerConnectListener, PlayerDisconnectListener, PlayerListRequestListener, GameStartListener {

	private EventManager eventManager;
	private TitleOverlay titleOverlay;
	private ButtonManager buttonManager;

	private CustomLabel connectedPlayersTitleLabel;
	private HashMap<String, CustomLabel> connectedPlayersLabels;

	public ServerLobbyScreen() {
		this.eventManager = Civilization.getInstance().getEventManager();
		eventManager.clearEvents();

		this.titleOverlay = new TitleOverlay();

		eventManager.addListener(PlayerConnectListener.class, this);
		eventManager.addListener(PlayerDisconnectListener.class, this);
		eventManager.addListener(PlayerListRequestListener.class, this);
		eventManager.addListener(GameStartListener.class, this);

		this.buttonManager = new ButtonManager(getStage());

		connectedPlayersLabels = new HashMap<>();

		connectedPlayersTitleLabel = new CustomLabel("Connected Players: ", 0, viewport.getWorldHeight() / 1.1F,
				viewport.getWorldWidth(), 20);
		connectedPlayersTitleLabel.setAlignment(Align.center);
		stage.addActor(connectedPlayersTitleLabel);

		// FIXME: Only show this button to the first player in the player list.
		buttonManager.addButton(new MPStartButton(viewport.getWorldWidth() / 2 - 150 / 2, 50, 150, 45));

		requestPlayerList();

		overrideGlClear();
	}

	@Override
	public void show() {
		super.show();
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0.253F, 0.304F, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		titleOverlay.act();
		titleOverlay.draw();
		super.render(delta);
		eventManager.fireEvent(MouseMoveEvent.INSTANCE);
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (button == Input.Buttons.LEFT) {
			eventManager.fireEvent(new LeftClickEvent(screenX, screenY));
		}
		return false;
	}

	@Override
	public void onPlayerConnect(PlayerConnectPacket packet) {
		Gdx.app.log(Civilization.LOG_TAG, packet.getPlayerName() + " has connected to the lobby");

		CustomLabel playerLabel = new CustomLabel(packet.getPlayerName(), 0,
				viewport.getWorldHeight() - 100 - (connectedPlayersLabels.size() * 40), viewport.getWorldWidth(), 20);
		playerLabel.setAlignment(Align.center);
		stage.addActor(playerLabel);
		connectedPlayersLabels.put(packet.getPlayerName(), playerLabel);
	}

	@Override
	public void onPlayerListRequested(PlayerListRequestPacket packet) {
		for (String playerName : packet.getPlayerList()) {
			if (playerName == null)
				continue;

			CustomLabel playerLabel = new CustomLabel(playerName, 0,
					viewport.getWorldHeight() - 100 - (connectedPlayersLabels.size() * 40), viewport.getWorldWidth(),
					20);
			playerLabel.setAlignment(Align.center);
			stage.addActor(playerLabel);
			connectedPlayersLabels.put(playerName, playerLabel);
		}
	}

	private void requestPlayerList() {
		Civilization.getInstance().getNetworkManager().sendPacket(new PlayerListRequestPacket());
	}

	@Override
	public void onPlayerDisconnect(PlayerDisconnectPacket packet) {
		CustomLabel removedLabel = connectedPlayersLabels.get(packet.getPlayerName());
		for (Actor actor : stage.getActors()) {
			if (removedLabel.equals(actor))
				actor.addAction(Actions.removeActor());
		}

		// Repositon our labels
		for (int i = 0; i < connectedPlayersLabels.size(); i++) {
			CustomLabel label = (CustomLabel) connectedPlayersLabels.values().toArray()[i];

			label.setPosition(0, viewport.getWorldHeight() - 100 - (i * 40));
		}

		connectedPlayersLabels.remove(packet.getPlayerName());
	}

	@Override
	public void onGameStart() {
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				Civilization.getInstance().getScreenManager().setScreen(ScreenEnum.IN_GAME);
				Civilization.getInstance().getNetworkManager().sendPacket(new MapRequestPacket());
			}
		});
	}

}
