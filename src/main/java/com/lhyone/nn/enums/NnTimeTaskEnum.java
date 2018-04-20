package com.lhyone.nn.enums;

public enum NnTimeTaskEnum {
	CLEAR_TIME(0,"清除时间"),
	USER_REDAY_IDLE_TIME(1,"准备倒计时"),
	PLAY_GAME_TIME(2,"游戏中倒计时"),
	SHOW_MATCH_RESULT_TIME(3,"展示比赛结果倒计时"),
	GAME_IDLE_TIME(4,"空闲时间倒计时"),
	LISTEN_TIME(5,"时间监听器");
	private int code;
	private String desc;
	
	private NnTimeTaskEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
	
	
	
}
