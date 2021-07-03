package me.rhin.openciv.shared.packet.type;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import me.rhin.openciv.shared.packet.Packet;

public class ResearchFinishPacket extends Packet {

	private int techID;

	@Override
	public void write(Json json) {
		super.write(json);
		json.writeValue("techID", techID);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		this.techID = jsonData.getInt("techID");
	}

	public void setTechID(int techID) {
		this.techID = techID;
	}

	public int getTechID() {
		return techID;
	}
}
