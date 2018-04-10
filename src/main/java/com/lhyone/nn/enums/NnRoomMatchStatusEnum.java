package com.lhyone.nn.enums;

public enum NnRoomMatchStatusEnum {

	INIT_HOME_STATUS(1,"房间初始状态"),
	PLAY_GAME_STATUS(2,"游戏中"),
	SHOW_MATCH_RESULT_STATUS(3,"展示结果的状态"),
	GAME_IDLE_STATUS(4,"游戏空闲状态");
	
	
	private int code;
	private String desc;
	
	private NnRoomMatchStatusEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}
	public int getCode() {
		return code;
	}
	public String getDesc() {
		return desc;
	}
	
	
	
	
}
