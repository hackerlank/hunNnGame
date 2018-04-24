package com.lhyone.nn.enums;

public enum RedisMQEnum {
	CLOSE_ROOM_CHANNEL("hun_nn_close_room_channel","解散房间");
	
	private String code;
	
	private String desc;

	
	private RedisMQEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
	
	
}
