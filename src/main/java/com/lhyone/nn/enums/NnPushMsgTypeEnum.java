package com.lhyone.nn.enums;

public enum NnPushMsgTypeEnum {
	INIT_ROOM_PUSH(300,"房间初始化推送"),
	JOIN_ROOM_PUSH(301,"加入房间推送"),
	APPLY_SIT_LANDLORD_PUSH(302,"申请坐庄推送"),
	USER_REDAY_PUSH(303,"用户准备推送"),
	APPLY_LEAVE_LANDLORD_PUSH(304,"庄家申请离开推送"),
	FARMER_ADD_SCORE_PUSH(305,"闲家增加积分推送"),
	SEND_CARD_PUSH(306,"发牌推送"),
	SHOW_MATCH_RESULT_PUSH(307,"展示比赛结果推送"),
	EXIT_ROOM_PUSH(308,"退出房间推送"),
	REPLACE_LANDLORD_PUSH(309,"换庄推送"),
	START_MATCH_PUSH(310,"开始比赛");
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
