package com.lhyone.nn.enums;

public enum NnReqMsgTypeEnum {
	INIT_ROOM(100,"房间初始化"),
	JOIN_ROOM(101,"加入房间"),
	APPLY_SIT_LANDLORD(102,"申请坐庄"),
	USER_REDAY(103,"用户准备"),
	APPLY_LEAVE_LANDLORD(104,"庄家申请离开"),
	FARMER_ADD_SCORE(105,"闲家增加积分"),
	EXIT_ROOM(1066,"退出房间");
	private int code;
	
	private String desc;

	private NnReqMsgTypeEnum(int code, String desc) {
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
