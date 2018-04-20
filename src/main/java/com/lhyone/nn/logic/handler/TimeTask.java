package com.lhyone.nn.logic.handler;

import com.alibaba.fastjson.JSONObject;
import com.lhyone.nn.enums.NnTimeTaskEnum;
import com.lhyone.nn.pb.HunNnBean;
import com.lhyone.nn.util.NnConstans;
import com.lhyone.nn.util.NnUtil;
import com.lhyone.nn.vo.GameTimoutVo;
import com.lhyone.util.RedisUtil;

public class TimeTask implements Runnable {
	private HunNnBean.ReqMsg reqMsg;
	private int type;
	public TimeTask(HunNnBean.ReqMsg reqMsg,int type){
		this.reqMsg=reqMsg;
		this.type=type;
	}
	
	

	@Override
	public void run() {
		goRun();
	}
	
	private void goRun(){
		System.out.println("========================================================");
		if(NnTimeTaskEnum.PLAY_GAME_TIME.getCode()==type){
			
			GameTimoutVo timeVo=getGameTimoutVo(reqMsg.getRoomNo());
			int time=NnUtil.getNnRoomSendGoldTimer(reqMsg.getRoomNo());
			if(timeVo.getRestTimeType()==NnTimeTaskEnum.PLAY_GAME_TIME.getCode()&&time<=NnConstans.PLAY_GAME_TIME){
				HunNnManager.batchSendPostion(reqMsg);
				NnUtil.setNnRoomSendGoldTimer(reqMsg.getRoomNo());
			}
			
			
			
		}
		
		
	}
	
	/**
	 * 获取游戏超时时间
	 * @param reqMsg
	 * @return
	 */
	private static GameTimoutVo getGameTimoutVo(String roomNo){
		
		String str=RedisUtil.hget(NnConstans.NN_REST_TIME_PRE,roomNo);
		
		if(str!=null){
			return JSONObject.parseObject(str, GameTimoutVo.class);
		}
		return null;
	}

}
