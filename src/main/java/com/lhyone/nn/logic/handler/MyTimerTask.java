package com.lhyone.nn.logic.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.lhyone.nn.enums.NnTimeTaskEnum;
import com.lhyone.nn.pb.HunNnBean;
import com.lhyone.nn.pb.HunNnBean.ReqMsg;
import com.lhyone.nn.util.NnConstans;
import com.lhyone.nn.util.NnUtil;
import com.lhyone.nn.vo.GameTimoutVo;
import com.lhyone.util.RedisUtil;

public class MyTimerTask implements Runnable{
	 private static Logger logger = LogManager.getLogger(MyTimerTask.class);
		
	private HunNnBean.ReqMsg reqMsg;
	private int type;
	private long startTime;
	public MyTimerTask(HunNnBean.ReqMsg reqMsg,int type,long startTime){
		this.reqMsg=reqMsg;
		this.type=type;
		this.startTime=startTime;
	}
	
	public void run() {
		nnTask();	
	}
	 
	private void nnTask(){
		
	try{
		//标识位
		boolean flag=false;
		//庄家准备
		if(NnTimeTaskEnum.USER_REDAY_TIME.getCode()==type){
			//如果庄家在规定的时间没有准备,剔除庄家
			
			//删除庄家缓存
			HunNnBean.PositionInfo.Builder landlordPosition=HunNnBean.PositionInfo.newBuilder();
			landlordPosition.setPosition(1);
			NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(),1);
			RedisUtil.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE+reqMsg.getRoomNo());
			//清除用户准备倒计时
			RedisUtil.hdel(NnConstans.NN_ROOM_USER_REDAY_TIME_PRE+reqMsg.getRoomNo(), reqMsg.getUserId()+"");
			//设置新的庄家
			HunNnManager.setLandlord(reqMsg.getRoomNo());
			
		}
		//闲家加分倒计时
		if(NnTimeTaskEnum.PLAY_GAME_TIME.getCode()==type){
			//倒计时结束后进行发牌操作
			HunNnManager.sendCard(reqMsg);
			
		}
		//展示比赛结果
		if(NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode()==type){
			//时间到了触发一段空闲时间，主要是推送庄家准备倒计时,清除上局比赛的用户缓存数据
			HunNnManager.setLandlordTimerTask(reqMsg);
			
		}
		//游戏空闲倒计时
		if(NnTimeTaskEnum.GAME_IDLE_TIME.getCode()==type){
			HunNnManager.idleTimeTask(reqMsg);
			
		}
		}catch(Exception e){
			logger.info(e.getMessage(),e);
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 获取游戏超时时间
	 * @param reqMsg
	 * @return
	 */
	private static GameTimoutVo getGameTimoutVo(ReqMsg reqMsg){
		
		String str=RedisUtil.hget(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo());
		
		if(str!=null){
			return JSONObject.parseObject(str, GameTimoutVo.class);
		}
		return null;
	}
	
	
	private static boolean isRunTimer(long time,int type){
		
		long curTime=System.currentTimeMillis();
		long diffTime=Math.abs((curTime-time)/1000);
		long timer=0;
		
		if(NnTimeTaskEnum.PLAY_GAME_TIME.getCode()==type){
			timer=NnConstans.PLAY_GAME_TIME;
		}else if(NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode()==type){
			timer=NnConstans.SHOW_MATCH_RESULT_TIME;
		}else if(NnTimeTaskEnum.GAME_IDLE_TIME.getCode()==type){
			timer=NnConstans.GAME_IDLE_TIME;
		}else if(NnTimeTaskEnum.USER_REDAY_TIME.getCode()==type){
			timer=NnConstans.USER_REDAY_TIME;
		}
		
		//延迟两秒
		if(diffTime<(timer+2)){
			return true;
		}
		return false;
	}

}
