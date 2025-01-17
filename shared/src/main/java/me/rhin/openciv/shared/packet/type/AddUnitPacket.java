package me.rhin.openciv.shared.packet.type;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import me.rhin.openciv.shared.packet.Packet;

public class AddUnitPacket extends Packet {

	int tileGridX, tileGridY;
	private String unitName;
	private int unitID;
	private String playerOwner;
	private int unitMovement;

	@Override
	public void write(Json json) {
		super.write(json);
		json.writeValue("tileGridX", tileGridX);
		json.writeValue("tileGridY", tileGridY);
		json.writeValue("unitName", unitName);
		json.writeValue("unitID", unitID);
		json.writeValue("playerOwner", playerOwner);
		json.writeValue("unitMovement", unitMovement);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		this.tileGridX = jsonData.getInt("tileGridX");
		this.tileGridY = jsonData.getInt("tileGridY");
		this.unitName = jsonData.getString("unitName");
		this.unitID = jsonData.getInt("unitID");
		this.playerOwner = jsonData.getString("playerOwner");
		this.unitMovement = jsonData.getInt("unitMovement");
	}

	public void setUnit(String playerOwner, String unitName, int unitID, int tileGridX, int tileGridY) {
		this.tileGridX = tileGridX;
		this.tileGridY = tileGridY;
		this.unitName = unitName;
		this.unitID = unitID;
		this.playerOwner = playerOwner;
		this.unitMovement = -1;
	}

	public void setUnitMovement(int unitMovement) {
		this.unitMovement = unitMovement;
	}

	public int getTileGridX() {
		return tileGridX;
	}

	public int getTileGridY() {
		return tileGridY;
	}

	public String getUnitName() {
		return unitName;
	}

	public int getUnitID() {
		return unitID;
	}

	public String getPlayerOwner() {
		return playerOwner;
	}

	public int getUnitMovement() {
		return unitMovement;
	}
}
