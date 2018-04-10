package com.lhyone.nn.enums;

public enum NnPushMsgTypeEnum {
	INIT_ROOM_PUSH(1000,"房间初始化推送"),
	JOIN_ROOM_PUSH(1001,"加入房间推送"),
	APPLY_SIT_LANDLORD_PUSH(1002,"申请坐庄推送"),
	USER_REDAY_PUSH(1003,"用户准备推送"),
	APPLY_LEAVE_LANDLORD_PUSH(1004,"庄家申请离开推送"),
	FARMER_ADD_SCORE_PUSH(1005,"闲家增加积分推送"),
	SEND_CARD_PUSH(1006,"发牌推送"),
	SHOW_MATCH_RESULT_PUSH(1007,"展示比赛结果推送"),
	EXIT_ROOM_PUSH(1008,"退出房间推送"),
	REPLACE_LANDLORD_PUSH(1009,"换庄推送");
	private int code;
	
	private String desc;

	private NnPushMsgTypeEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}
	/**
	 * 是否包含该类型
	 * @param pushType
	 * @return
	 */
	public static boolean isInclude(NnPushMsgTypeEnum pushType){
		for(NnPushMsgTypeEnum enums:NnPushMsgTypeEnum.values()){
			
			if(enums==pushType){
				
				return true;
			}
		}
		return false;
		
	}
	
	public int getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
}
