package com.lhyone.nn.enums;

public enum NnChipEnum {

	$50(1,50,""),
	$100(2,100,""),
	$200(3,200,""),
	$300(4,300,""),
	$1000(5,1000,""),
	$5000(6,5000,""),
	$10000(7,10000,"");
	
	private int code;
	
	private int gold;
	
	private String desc;

	private NnChipEnum(int code, int gold, String desc) {
		this.code = code;
		this.gold = gold;
		this.desc = desc;
	}
	
	public static boolean checkIsTrue(int code){
		
		for(NnChipEnum enums:NnChipEnum.values()){
			if(enums.code==code){
				return true;
			}
		}
		return false;
	}
	public static boolean checkGoldIsTrue(int gold){
		
		for(NnChipEnum enums:NnChipEnum.values()){
			if(enums.gold==gold){
				return true;
			}
		} 
		return false;
	}
	public static int getChip(int code){
		for(NnChipEnum enums:NnChipEnum.values()){
			if(enums.code==code){
				return enums.getGold();
			}
		}
		return $50.gold;
	}
	
	public int getCode() {
		return code;
	}

	public int getGold() {
		return gold;
	}

	public String getDesc() {
		return desc;
	}
	
	
}
