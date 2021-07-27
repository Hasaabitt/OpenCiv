package me.rhin.openciv.ui.window.type;

import com.badlogic.gdx.utils.Align;

import me.rhin.openciv.Civilization;
import me.rhin.openciv.asset.TextureEnum;
import me.rhin.openciv.game.heritage.Heritage;
import me.rhin.openciv.listener.ResizeListener;
import me.rhin.openciv.shared.stat.Stat;
import me.rhin.openciv.ui.background.ColoredBackground;
import me.rhin.openciv.ui.button.type.CloseWindowButton;
import me.rhin.openciv.ui.button.type.PickHeritageButton;
import me.rhin.openciv.ui.button.type.PickResearchButton;
import me.rhin.openciv.ui.label.CustomLabel;
import me.rhin.openciv.ui.window.AbstractWindow;

public class PickHeritageWindow extends AbstractWindow implements ResizeListener {

	private Heritage heritage;
	private ColoredBackground coloredBackground;
	private CustomLabel titleLabel;
	private ColoredBackground icon;
	private CustomLabel descLabel;
	private CustomLabel turnsLabel;
	private PickHeritageButton pickResearchButton;
	private CloseWindowButton closeWindowButton;

	public PickHeritageWindow(Heritage heritage) {
		super.setBounds(viewport.getWorldWidth() / 2 - 270 / 2, viewport.getWorldHeight() / 2 - 300 / 2, 270, 300);
		this.heritage = heritage;

		this.coloredBackground = new ColoredBackground(TextureEnum.UI_LIGHT_GRAY.sprite(), 0, 0, getWidth(),
				getHeight());
		addActor(coloredBackground);

		this.titleLabel = new CustomLabel("Study " + heritage.getName(), Align.center, 0, getHeight() - 20, getWidth(), 15);
		addActor(titleLabel);

		this.icon = new ColoredBackground(heritage.getIcon(), getWidth() / 2 - 32 / 2, getHeight() - 55, 32, 32);
		addActor(icon);

		this.descLabel = new CustomLabel(heritage.getDesc(), Align.left, 5, getHeight() - 87, getWidth(), 15);
		addActor(descLabel);

		int turns = (int) Math.ceil(heritage.getCost()
				/ Civilization.getInstance().getGame().getPlayer().getStatLine().getStatValue(Stat.HERITAGE_GAIN));

		this.turnsLabel = new CustomLabel(turns + " Turns", Align.center, 0, 50, getWidth(), 15);
		addActor(turnsLabel);

		this.pickResearchButton = new PickHeritageButton(heritage, 0, 5, 100, 35);
		addActor(pickResearchButton);

		this.closeWindowButton = new CloseWindowButton(this.getClass(), "Cancel", getWidth() - 100, 5, 100, 35);
		addActor(closeWindowButton);

		Civilization.getInstance().getEventManager().addListener(ResizeListener.class, this);
	}

	@Override
	public void onResize(int width, int height) {
		super.setPosition(width / 2 - 270 / 2, height / 2 - 300 / 2);
	}

	@Override
	public boolean disablesInput() {
		return true;
	}

	@Override
	public boolean disablesCameraMovement() {
		return false;
	}

	@Override
	public boolean closesOtherWindows() {
		return false;
	}

	@Override
	public boolean closesGameDisplayWindows() {
		return false;
	}

	@Override
	public boolean isGameDisplayWindow() {
		return false;
	}

}