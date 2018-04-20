package com.lhyone.nn.enums;

public enum NnRspMsgTypeEnum {
	INIT_ROOM_FEEDBACK(200,"房间初始化反馈"),
	JOIN_ROOM_FEEDBACK(201,"加入房间反馈"),
	APPLY_SIT_LANDLORD_FEEDBACK(202,"申请坐庄反馈"),
	USER_REDAY_FEEDBACK(203,"用户准备反馈"),
	APPLY_LEAVE_LANDLORD_FEEDBACK(204,"庄家申请离开反馈"),
	FARMER_ADD_SCORE_FEEDBACK(205,"闲家增加积分反馈"),
	EXIT_ROOM_FEEDBACK(206,"退出房间反馈");
	private int code;
	
	private String desc;

	private NnRspMsgTypeEnum(int code, String desc) {
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
