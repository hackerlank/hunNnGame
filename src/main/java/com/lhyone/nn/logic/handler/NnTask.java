package com.lhyone.nn.logic.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lhyone.nn.enums.NnReqMsgTypeEnum;
import com.lhyone.nn.enums.NnRspCodeEnum;
import com.lhyone.nn.pb.HunNnBean;
import com.lhyone.nn.util.CommUtil;
import com.lhyone.nn.util.NnConstans;
import com.lhyone.util.RedisUtil;

import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * Created by Think on 2017/8/15.
 */
public class NnTask implements Runnable {
	private static Logger logger=LogManager.getLogger(NnTask.class);
    private ChannelHandlerContext ctx;
    private HunNnBean.ReqMsg reqMsg;
    public NnTask(ChannelHandlerContext ctx, HunNnBean.ReqMsg reqMsg){
        this.ctx=ctx;
        this.reqMsg=reqMsg;
    }
    public void run() {
    	try {
    	
    		HunNnBean.RspMsg rspMsg=HunNnBean.RspMsg.newBuilder().build();
            
            int msgType=reqMsg.getMsgType();
            if(msgType!=0){
        		boolean isAuth = CommUtil.authcheck(reqMsg.getToken(),reqMsg.getUserId());
                if(!isAuth){
                	rspMsg=HunNnBean.RspMsg.newBuilder()
                			.setCode(NnRspCodeEnum.$0001.getCode())
                			.setMsg(NnRspCodeEnum.$0001.getMsg()).build();
                	ctx.writeAndFlush(rspMsg);
                	return ;
                }
            	
            }
           if(!checkIsRepeatCommit(reqMsg.getUserId(), reqMsg.getTimestamp())){
        	   rspMsg=HunNnBean.RspMsg.newBuilder()
           			.setCode(NnRspCodeEnum.$0005.getCode())
           			.setMsg(NnRspCodeEnum.$0005.getMsg()).build();
        	   ctx.writeAndFlush(rspMsg);
        	   return;
           }
            
           NnManager(rspMsg, msgType);
            
		} catch (Exception e) {
			e.printStackTrace();
		}

    }
    
    
    private void NnManager(HunNnBean.RspMsg rspMsg, int msgType){
    	
    	try {
			
	         if(NnReqMsgTypeEnum.JOIN_ROOM.getCode()==msgType){//加入房间
	        		HunNnManager.joinRoom(reqMsg, ctx);
	        }else if(NnReqMsgTypeEnum.APPLY_SIT_LANDLORD.getCode()==msgType){//申请坐庄
	        		HunNnManager.applyLandlord(reqMsg, ctx);
	        }else if(NnReqMsgTypeEnum.USER_REDAY.getCode()==msgType){//用户准备
	        		HunNnManager.landlordReday(reqMsg, ctx);
	        }else if(NnReqMsgTypeEnum.APPLY_LEAVE_LANDLORD.getCode()==msgType){//庄家退出房间
	        		HunNnManager.landlordExitRoom(reqMsg,ctx);
	        }else if(NnReqMsgTypeEnum.FARMER_ADD_SCORE.getCode()==msgType){//闲家加分
	        		HunNnManager.farmerAddChip(reqMsg, ctx);
	        }else if(NnReqMsgTypeEnum.INIT_ROOM.getCode()==msgType){//房间初始化
	        		HunNnManager.initRoom(reqMsg,ctx);
	        }else if(NnReqMsgTypeEnum.EXIT_ROOM.getCode()==msgType){//退出房间
	        	HunNnManager.exitRoom(reqMsg,ctx);
	       }
    	} catch (Exception e) {
    		e.printStackTrace();
		}
        
    }
    

	/**
	 * 检查是否是重复提交
	 * @param userId
	 * @return
	 */
	private static boolean checkIsRepeatCommit(long userId,long timestamp){

		boolean flag=RedisUtil.hexists(NnConstans.NN_USER_THREAD_LOCK_CACHE_PRE, userId+"");
		return true;
//		if(!flag){
//			RedisUtil.hset(NnConstans.NN_USER_THREAD_LOCK_CACHE_PRE, userId+"", System.currentTimeMillis()+"");
//			return true;
//		}
//		
//		String orgTimestamp=RedisUtil.hget(NnConstans.NN_USER_THREAD_LOCK_CACHE_PRE, userId+"");
//		if(timestamp>=Long.parseLong(orgTimestamp)){
//			RedisUtil.hset(NnConstans.NN_USER_THREAD_LOCK_CACHE_PRE, userId+"", System.currentTimeMillis()+"");
//			return true;
//		}
//		return false;
	}
}
