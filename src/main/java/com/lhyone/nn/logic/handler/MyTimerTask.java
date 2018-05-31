package com.lhyone.nn.logic.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.lhyone.nn.dao.NnManagerDao;
import com.lhyone.nn.enums.NnTimeTaskEnum;
import com.lhyone.nn.enums.NnUserRoleEnum;
import com.lhyone.nn.enums.NnYesNoEnum;
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
	private long timestamp;
	public MyTimerTask(HunNnBean.ReqMsg reqMsg,int type,long timestamp){
		this.reqMsg=reqMsg;
		this.type=type;
		this.timestamp=timestamp;
	}
	
	public void run() {
		nnTask();	
	}
	 
	private void nnTask(){
		
	try{
		
		if(NnTimeTaskEnum.LISTEN_TIME.getCode()==type){
			RedisUtil.set(NnConstans.NN_SYS_CUR_TIME_CACHE, System.currentTimeMillis()+"");
		}else{	
			GameTimoutVo timeVo= getGameTimoutVo(reqMsg.getRoomNo());
			if(NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode()==type&&null!=timeVo&&timestamp==timeVo.getRestTime()){//庄家准备
			
				
				HunNnBean.UserInfo.Builder userInfo=HunNnManager.getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());
				if(null!=userInfo&&userInfo.getIsReday()==NnYesNoEnum.YES.getCode()){
					HunNnManager.idleTimeTask(reqMsg);
				}else{
					
					//如果庄家在规定的时间没有准备,剔除庄家
					//删除庄家缓存
					HunNnBean.PositionInfo.Builder landlordPosition=HunNnBean.PositionInfo.newBuilder();
					landlordPosition.setPosition(1);
					NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(),1);
					RedisUtil.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE+reqMsg.getRoomNo());
					//清除用户准备倒计时
					NnManagerDao.instance().deleteApplyLandlorder(reqMsg.getRoomNo(), reqMsg.getUserId());
					
					userInfo.setPlayerType(NnUserRoleEnum.GUEST.getCode());
					userInfo.clearIsApplyLandlord();
					userInfo.clearLandlordTimes();
					userInfo.clearIsReday();
				
					RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE+reqMsg.getRoomNo(), reqMsg.getUserId()+"", JsonFormat.printToString(userInfo.build()));
					
					HunNnBean.RoomInfo.Builder roomInfo=HunNnManager.getRoomInfo(reqMsg.getRoomNo());
					roomInfo.clearCurLandlordTimes();
					
					RedisUtil.hset(NnConstans.NN_ROOM_PRE+reqMsg.getRoomNo(), "roomInfo", JsonFormat.printToString(roomInfo.build()));
					
					//设置新的庄家
					HunNnManager.setLandlord(reqMsg.getRoomNo());
				
				}
			
			}
			//闲家加分倒计时
			if(NnTimeTaskEnum.PLAY_GAME_TIME.getCode()==type&&timestamp==timeVo.getRestTime()){
				//倒计时结束后进行发牌操作
				ServerManager.futures.get(reqMsg.getRoomNo()).cancel(true);
				HunNnManager.sendCard(reqMsg);
				
				
			}
			//展示比赛结果
			if(NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode()==type&&timestamp==timeVo.getRestTime()){
				//时间到了触发一段空闲时间，主要是推送庄家准备倒计时,清除上局比赛的用户缓存数据
				HunNnManager.setLandlordTimerTask(reqMsg);
				
			}
		}
		}catch(Exception e){
			System.out.println("定时器异常");
			logger.info(e.getMessage(),e);
			e.printStackTrace();
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
