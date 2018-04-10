package com.lhyone.nn.enums;

public enum NnWrokEnum {

	SHOW_MATCH_RESULT(1,"展示比赛结果");
	private int code;
	
	private String desc;

	private NnWrokEnum(int code, String desc) {
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
