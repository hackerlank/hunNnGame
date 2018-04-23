package com.lhyone.nn.logic.handler;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.lhyone.nn.dao.NnManagerDao;
import com.lhyone.nn.entity.BugUser;
import com.lhyone.nn.entity.GoldRecord;
import com.lhyone.nn.entity.JoinRoomUser;
import com.lhyone.nn.entity.NnApplyLandlord;
import com.lhyone.nn.entity.NnRoom;
import com.lhyone.nn.entity.NnRoomMatchUser;
import com.lhyone.nn.entity.NnRoomMatchUserDetail;
import com.lhyone.nn.entity.UserCostRecord;
import com.lhyone.nn.entity.UserMatchRecord;
import com.lhyone.nn.enums.GameTypeEnum;
import com.lhyone.nn.enums.GoldTypeEnum;
import com.lhyone.nn.enums.NnCardRuleEnum;
import com.lhyone.nn.enums.NnCardTypeTransEnum;
import com.lhyone.nn.enums.NnChipEnum;
import com.lhyone.nn.enums.NnPushMsgTypeEnum;
import com.lhyone.nn.enums.NnRoomMatchStatusEnum;
import com.lhyone.nn.enums.NnRspCodeEnum;
import com.lhyone.nn.enums.NnRspMsgTypeEnum;
import com.lhyone.nn.enums.NnTimeTaskEnum;
import com.lhyone.nn.enums.NnUserRoleEnum;
import com.lhyone.nn.enums.NnWrokEnum;
import com.lhyone.nn.enums.NnYesNoEnum;
import com.lhyone.nn.enums.NumEnum;
import com.lhyone.nn.pb.HunNnBean;
import com.lhyone.nn.pb.HunNnBean.ReqMsg;
import com.lhyone.nn.util.LocalCacheUtil;
import com.lhyone.nn.util.NnCardUtil;
import com.lhyone.nn.util.NnConstans;
import com.lhyone.nn.util.NnUtil;
import com.lhyone.nn.vo.DbVo;
import com.lhyone.nn.vo.GameTimoutVo;
import com.lhyone.nn.vo.UserCacheVo;
import com.lhyone.util.RedisUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class HunNnManager {
	private static Logger logger = LogManager.getLogger(HunNnManager.class);

	/**
	 * @param reqMsg
	 * @param ctx
	 */
	public static void initChannel(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {
		if (!ServerManager.channels.contains(ctx.channel())) {
			ServerManager.channels.add(ctx.channel());
			RedisUtil.hset(NnConstans.NN_USER_CHANNEL_PRE, reqMsg.getUserId() + "", ctx.channel().id().asLongText());
		}
	}

	/**
	 * 删除渠道
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void delChannel(ChannelHandlerContext ctx) {

		if (ServerManager.channels.contains(ctx.channel())) {
			ServerManager.channels.remove(ctx.channel());
		}
	}

	/**
	 * 删除渠道
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void delChannel(ChannelHandlerContext ctx, HunNnBean.ReqMsg reqMsg) {

		if (ServerManager.channels.contains(ctx.channel())) {
			RedisUtil.hdel(NnConstans.NN_CHANNEL_PRE, ctx.channel().id().asLongText());
			RedisUtil.hdel(NnConstans.NN_USER_CHANNEL_PRE, reqMsg.getUserId() + "");
		}
	}

	/** 推送单个消息 */
	public static void pushsingle(HunNnBean.RspMsg rspMsg, ChannelHandlerContext ctx) {
		System.out.println();
		logger.info("推送单个信息开是,推送channeId:{},操作类型:{},信息长度为:{},响应码为：{}", ctx.channel().id().asLongText(), rspMsg.getOperateType(), rspMsg.toString().length(), rspMsg.getCode());
		Channel channel = ServerManager.channels.find(ctx.channel().id());
		if (!channel.isActive() && !channel.isOpen()) {
			channel.close();
		} else {
			channel.writeAndFlush(rspMsg);
		}
	}

	/**
	 * 加入房间
	 * 
	 * @param reqMsg
	 * @return
	 * @throws Exception
	 */
	public static void joinRoom(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) throws Exception {
		logger.info("加入房间开始...");
		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		rspMsg.setOperateType(NnRspMsgTypeEnum.JOIN_ROOM_FEEDBACK.getCode());
		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();
		Jedis redis = RedisUtil.getJedis();
		Transaction tx = redis.multi();

		try {
			boolean flag=RedisUtil.exists(NnConstans.NN_ROOM_PRE+reqMsg.getRoomNo());
			
			if(!flag){
				rspMsg.setCode(NnRspCodeEnum.$1001.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1001.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
				
			}
			// 判断是否在房间内
			long i = NnManagerDao.instance().checkNnUserIsJoinRoom(reqMsg.getUserId());

			if (i > 0) {
				rspMsg.setCode(NnRspCodeEnum.$1002.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1002.getMsg());
				logger.info(NnRspCodeEnum.$1002.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
			// 增加房间用户
			JoinRoomUser njru = new JoinRoomUser();
			njru.setUserId(reqMsg.getUserId());
			njru.setRoomId(getRoomId(reqMsg.getRoomNo()));
			njru.setRoomNo(reqMsg.getRoomNo());
			njru.setGameType(GameTypeEnum.HUNDRED_NIU.getType());
			njru.setStatus(2);// 默认值
			njru.setCreateDate(new Date());

			NnManagerDao.instance().addNnRoomUser(njru);

			// 3.判断房间是否已经开赛
			HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(reqMsg.getRoomNo());

			// 6.用户加入房间
			HunNnBean.UserInfo.Builder uinfo = HunNnBean.UserInfo.newBuilder();
			UserCacheVo uv = getUserInfo(reqMsg.getUserId());
			uinfo.setUserId(reqMsg.getUserId());
			uinfo.setNickName(uv.getUserName());//
			uinfo.setHeadUrl(uv.getHeadImgUrl());//
			uinfo.setMark(uv.getMark());
			uinfo.setGender(uv.getGender());
			uinfo.setPlayerType(NnUserRoleEnum.GUEST.getCode());// 设置用角色为游客
			uinfo.setRoomNo(reqMsg.getRoomNo());
			uinfo.setUserGold((int) NnManagerDao.instance().getUserGold(reqMsg.getUserId()));
			uinfo.setIp(ctx.channel().remoteAddress().toString().replaceAll("(/)|:(.*)", ""));

			// 增加房间用户信息缓存
			tx.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(uinfo.build()));

			// 增加所有用户
			tx.sadd(NnConstans.NN_ROOM_ALL_USER_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");// 牛牛所有用户

			// 8.更新用户房间缓存信息

			roomInfo.setRoomCurPersonCount(roomInfo.getRoomCurPersonCount() + 1);// 设置房间当前人数

			if (roomInfo.getRoomMaxPersonCount() <= roomInfo.getRoomCurPersonCount()) {

				roomInfo.setRoomCurStatus(NnYesNoEnum.YES.getCode());// 当前人数已满
			}

			tx.hset(NnConstans.NN_ROOM_PRE + roomInfo.getRoomNo(), "roomInfo", JsonFormat.printToString(roomInfo.build()));

			tx.exec();// 执行事务提交
			// 9.封装返回结果信息

			/** 用户无用信息清除 */
			{
				rspData.setUser(uinfo);
			}

			String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());
			int restTime = getRestTime(reqMsg.getRoomNo());
			/** 房间无用信息清除 */
			{
				roomInfo.clearCardDouble();
				rspData.setCurRoomStatus(Integer.parseInt(curStatus));
				rspData.setRestTime(restTime);
				rspData.setRoom(roomInfo);// 增加返回对象

			}

			List<HunNnBean.PositionInfo> listPosition=NnUtil.getPositionInfo(reqMsg.getRoomNo());
			
			for(HunNnBean.PositionInfo up:listPosition){
				up=up.toBuilder().clearUserChip().clearListGold().clearUids().clearCard().build();
			}
			
			String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());
			if(StringUtils.isNotEmpty(landlordUserId)){
				HunNnBean.UserInfo.Builder landLordUser = getCurUser(Long.parseLong(landlordUserId), reqMsg.getRoomNo());
				rspData.setLandlord(landLordUser);
				
			}
			rspData.addAllPosition(listPosition);

			rspMsg.setData(rspData);
			rspMsg.setOperateType(NnRspMsgTypeEnum.JOIN_ROOM_FEEDBACK.getCode());
			rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

			pushsingle(rspMsg.build(), ctx);// 推送单个用户

			batchSendJoinRoom(reqMsg);// 批量加入房间推送
			logger.info("加入房间结束...");
		} catch (Exception e) {
			NnManagerDao.instance().deleteRoomUser(reqMsg.getUserId(), reqMsg.getRoomNo(), GameTypeEnum.HUNDRED_NIU.getType());
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			rspMsg.setOperateType(NnRspMsgTypeEnum.JOIN_ROOM_FEEDBACK.getCode());
			rspMsg.setCode(NnRspCodeEnum.$9999.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$9999.getMsg());
			pushsingle(rspMsg.build(), ctx);
		} finally {
			redis.close();
		}
	}

	/**
	 * 获取剩余时间
	 * 
	 * @param reqMsg
	 * @return
	 */
	private static int getRestTime(String roomNo) {
		String sysTimeStr=RedisUtil.get(NnConstans.NN_SYS_CUR_TIME_CACHE);
		long sysTime=Long.parseLong(sysTimeStr);
		int restTime = 0;
		GameTimoutVo timeOutVo = getGameTimoutVo(roomNo);
		if (timeOutVo.getRestTimeType()==NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode()) {
			restTime = (int) (NnConstans.USER_REDAY_IDLE_TIME - (sysTime-timeOutVo.getRestTime()) / 1000);
		}
		if (timeOutVo.getRestTimeType()==NnTimeTaskEnum.PLAY_GAME_TIME.getCode()) {
			restTime = (int) (NnConstans.PLAY_GAME_TIME - (sysTime- timeOutVo.getRestTime()) / 1000);
		}
		if (timeOutVo.getRestTimeType()==NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode()) {
			restTime = (int) (NnConstans.SHOW_MATCH_RESULT_TIME - (sysTime-timeOutVo.getRestTime()) / 1000);
		}
		return restTime;
	}

	/**
	 * 批量加入房间推送
	 * 
	 * @param reqMsg
	 */
	private static void batchSendJoinRoom(ReqMsg reqMsg) {
		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(reqMsg.getRoomNo());

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());

		HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(roomNo);

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspData.setRoom(roomInfo);
		rspMsg.setOperateType(NnPushMsgTypeEnum.JOIN_ROOM_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode()); 
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		for (String key : userSet) {

			if (reqMsg.getUserId() != Long.parseLong(key)) {
				map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
			}

		}
		batchSendMsg(map);
	}

	/**
	 * 
	 * 
	 * @return
	 */
	private static UserCacheVo getUserInfo(long userId) {
		try{
			String	str=RedisUtil.get(NnConstans.REDIS_USER_PRE + userId);
			UserCacheVo uv=	JSONObject.parseObject(str, UserCacheVo.class);
			return uv;
		}catch (Exception e) {
			e.printStackTrace();
			return new UserCacheVo();
		}
		
	}
	/**
	 * 获所有用户集合
	 * 
	 * @param roomNo
	 * @return
	 */
	public static Set<String> getAllUserSet(String roomNo) {
		return RedisUtil.smembers(NnConstans.NN_ROOM_ALL_USER_PRE + roomNo);
	}

	/**
	 * 获取用户信息
	 * 
	 * @param userId
	 * @return
	 */
	public static HunNnBean.UserInfo.Builder getCurUser(long userId, String roomNo) {
		HunNnBean.UserInfo.Builder user = HunNnBean.UserInfo.newBuilder();
		try {
			JsonFormat.merge(RedisUtil.hget(NnConstans.NN_ROOM_USER_INFO_PRE + roomNo, userId + ""), user);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			return null;
		}
		return user;
	}

	/**
	 * 获取房间信息
	 * 
	 * @param roomNo
	 * @return
	 */
	public static HunNnBean.RoomInfo.Builder getRoomInfo(String roomNo) {
		HunNnBean.RoomInfo.Builder roomInfo = HunNnBean.RoomInfo.newBuilder();
		if (!StringUtils.isEmpty(roomNo)) {
			String roomStr = RedisUtil.hget(NnConstans.NN_ROOM_PRE + roomNo, "roomInfo");
			try {
				JsonFormat.merge(roomStr, roomInfo);
			} catch (ParseException e) {
				logger.error(e.getMessage(), e);
				e.printStackTrace();
			}
		}
		return roomInfo;
	}

	/**
	 * 获取房间信息
	 * 
	 * @param roomNo
	 * @return
	 */
	private static long getRoomId(String roomNo) {
		String str = RedisUtil.hget(NnConstans.NN_ROOM_PRE + roomNo, "roomVo");

		if (StringUtils.isNoneEmpty(str)) {

			NnRoom nnRoomVo = (NnRoom) (JSONObject.parseObject(str, NnRoom.class));

			return nnRoomVo.getId();
		}

		return 0;
	}

	/**
	 * 获取房间信息
	 * 
	 * @param roomNo
	 * @return
	 */
	private static NnRoom getRoomVo(String roomNo) {
		String str = RedisUtil.hget(NnConstans.NN_ROOM_PRE + roomNo, "roomVo");

		if (StringUtils.isNoneEmpty(str)) {

			NnRoom nnRoomVo = (NnRoom) (JSONObject.parseObject(str, NnRoom.class));

			return nnRoomVo;
		}

		return new NnRoom();
	}

	/**
	 * 申请坐庄
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void applyLandlord(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		rspMsg.setOperateType(NnRspMsgTypeEnum.APPLY_SIT_LANDLORD_FEEDBACK.getCode());

		// 判断是否申请坐庄
		boolean flag =NnManagerDao.instance().checkIsApplyLandlordUser(reqMsg.getRoomNo(), reqMsg.getUserId());

		if (flag) {
			rspMsg.setCode(NnRspCodeEnum.$1003.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$1003.getMsg());
			pushsingle(rspMsg.build(), ctx);
			return;
		}

		// 判断金币是否充足

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());

		if (userInfo.getUserGold() < NnConstans.NN_LANDLORD_MIN_GOLD) {
			rspMsg.setCode(NnRspCodeEnum.$1107.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$1107.getMsg());
			pushsingle(rspMsg.build(), ctx);
			return;
		}
		userInfo.clearIsReday();
		userInfo.setIsApplyLandlord(NnYesNoEnum.YES.getCode());
		RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));

		long roomId=getRoomId(reqMsg.getRoomNo());
		
		NnApplyLandlord nal=new NnApplyLandlord();
		nal.setRoomNo(reqMsg.getRoomNo());
		nal.setRoomId(roomId);
		nal.setUserId(reqMsg.getUserId());
		nal.setStatus(NumEnum.ONE.getNumInteger());
		nal.setCreateDate(new Date());
		
		NnManagerDao.instance().addApplyLandlordUser(nal);

		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		pushsingle(rspMsg.build(), ctx);
		
		flag=NnManagerDao.instance().checkIsHasSetLandlord(reqMsg.getRoomNo());
		// 判断当前是否有庄家
		if (!flag) {
			setLandlord(reqMsg.getRoomNo());
		}

	}
	
	public static void sysDataInit(String roomNo){
		
		try {
			
			//判断当前房间号是否存在
		
			if(!RedisUtil.exists(NnConstans.NN_ROOM_PRE+roomNo)){
				return;
			}
			boolean flag=NnManagerDao.instance().checkIsHasSetLandlord(roomNo);
			// 判断当前是否有庄家
			if (!flag) {
				setLandlord(roomNo);
			}else{
				HunNnBean.ReqMsg.Builder reqMsg=HunNnBean.ReqMsg.newBuilder();
				
				reqMsg.setRoomNo(roomNo);
				int restTime=getRestTime(roomNo);
				GameTimoutVo timeVo=getGameTimoutVo(roomNo);
				
				//如果是准备,则证明有庄家
				if(timeVo.getRestTimeType()==NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode()){
					String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + roomNo);
					reqMsg.setUserId(Long.parseLong(landlordUserId));
					// 增加个人准备倒计时
					ServerManager.executorTask.schedule(new MyTimerTask(reqMsg.build(), NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode(),timeVo.getRestTime()),restTime, TimeUnit.SECONDS);
					
					
				}else if(timeVo.getRestTimeType()==NnTimeTaskEnum.PLAY_GAME_TIME.getCode()){
					
					ServerManager.executorTask.schedule(new MyTimerTask(reqMsg.build(), NnTimeTaskEnum.PLAY_GAME_TIME.getCode(),timeVo.getRestTime()), restTime, TimeUnit.SECONDS);
					Future<?> future=ServerManager.executorTask.scheduleAtFixedRate(new TimeTask(reqMsg.build(), NnTimeTaskEnum.PLAY_GAME_TIME.getCode()), 1, 1, TimeUnit.SECONDS);
				    ServerManager.futures.put(reqMsg.getRoomNo(), future);
				}else if(timeVo.getRestTimeType()==NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode()){
					
					ServerManager.executorTask.schedule(new MyTimerTask(reqMsg.build(), NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode(),timeVo.getRestTime()), restTime, TimeUnit.SECONDS);
					
				}
				
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}
	

	/**
	 * 设置庄家
	 * 
	 * @param reqMsg
	 */
	public static void setLandlord(String roomNo) {

		
		if (NnManagerDao.instance().getApplyLandlordCount(roomNo) <= 0) {
			ReqMsg.Builder reqMsg = ReqMsg.newBuilder().setRoomNo(roomNo);
			
			
			GameTimoutVo timeout = new GameTimoutVo();
			timeout.setRestTime(0);
			timeout.setRestTimeType(NnTimeTaskEnum.CLEAR_TIME.getCode());
			RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));
			// 更新当前状态为空闲状态
			RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() + "");
			HunNnBean.RoomInfo.Builder roomInfo=getRoomInfo(roomNo);
			roomInfo.clearCurLandlordTimes();
			
			RedisUtil.hset(NnConstans.NN_ROOM_PRE+ reqMsg.getRoomNo() ,"roomInfo", JsonFormat.printToString(roomInfo.build()));
			RedisUtil.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE+reqMsg.getRoomNo());
			batchSendLandlord(reqMsg.build());
			//无庄清除定时器
			return;
		}


		Long landlordUserId= NnManagerDao.instance().getNextLandlordUserId(roomNo);
		
		if(landlordUserId==null){
			return ;
		}
		ReqMsg.Builder reqMsg = ReqMsg.newBuilder().setUserId(landlordUserId).setRoomNo(roomNo);

		NnManagerDao.instance().setLandlord(roomNo, reqMsg.getUserId());
		// 移除庄家的申请队列
//		NnManagerDao.instance().deleteApplyLandlorder(reqMsg.getRoomNo(),reqMsg.getUserId());

		// 设置庄家缓存
		NnUtil.setLandlord(reqMsg.getRoomNo(), reqMsg.getUserId());
		// 当有人申请坐庄时，增加庄准备倒计时

		// 设置庄家
		RedisUtil.set(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());
		// 增加准备倒计时
		{
			long userRedayTime = System.currentTimeMillis();
			// 设置空闲状态
			GameTimoutVo timeout = new GameTimoutVo();
			timeout.setRestTime(userRedayTime);
			timeout.setRestTimeType(NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode());
			RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));
			// 增加个人准备倒计时
			{
				ServerManager.executorTask.schedule(new MyTimerTask(reqMsg.build(), NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode(),timeout.getRestTime()), NnConstans.USER_REDAY_IDLE_TIME, TimeUnit.SECONDS);
			}
			userInfo.setRedayTime(NnConstans.USER_REDAY_IDLE_TIME);
		
		}
		
		userInfo.setPlayerType(NnUserRoleEnum.LANDLORD.getCode());
		RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));

		batchSendLandlord(reqMsg.build());

	}

	/**
	 * 抢庄推送
	 * 
	 * @param reqMsg
	 */
	public static void batchSendLandlord(HunNnBean.ReqMsg reqMsg) {

		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);
		
		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + roomNo);
		HunNnBean.UserInfo.Builder landLordUser=HunNnBean.UserInfo.newBuilder();
		if(StringUtils.isNotEmpty(landlordUserId)){
			landLordUser = getCurUser(Long.parseLong(landlordUserId), roomNo);
			
		}
		
		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RoomInfo.Builder roomInfo= getRoomInfo(reqMsg.getRoomNo());
		
		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		rspData.setRoom(roomInfo);
		rspData.setRestTime(getRestTime(reqMsg.getRoomNo()));

		rspData.setLandlord(landLordUser);
		
		
		

		for (String key : userSet) {
			HunNnBean.UserInfo.Builder userInfo = getCurUser(Long.parseLong(key), roomNo);
			userInfo.clearToken();
			userInfo.clearRoomNo();
			userInfo.clearCard();
			rspData.setUser(userInfo);
			rspData.setCurRoomStatus(Integer.parseInt(curStatus));
			rspMsg.setOperateType(NnPushMsgTypeEnum.APPLY_SIT_LANDLORD_PUSH.getCode());
			rspMsg.setData(rspData);
			rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());
			
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());

		}
		batchSendMsg(map);

	}

	/**
	 * 庄家准备
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void landlordReday(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		rspMsg.setOperateType(NnRspMsgTypeEnum.USER_REDAY_FEEDBACK.getCode());
		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		// 判断庄家是否已经点击准备
		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());

		if (userInfo.getIsReday() == NnYesNoEnum.YES.getCode()) {
			rspMsg.setCode(NnRspCodeEnum.$0005.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$0005.getMsg());
			pushsingle(rspMsg.build(), ctx);
			return;
		}
		// 清除用户准备倒计时
//		RedisUtil.hdel(NnConstans.NN_ROOM_USER_REDAY_TIME_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");
		userInfo.setLandlordTimes(userInfo.getLandlordTimes() + 1);
		// 设置庄家已准备
		userInfo.setIsReday(NnYesNoEnum.YES.getCode());
		userInfo.setBaseGold(0);

		RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));

		// 增加游戏进行中的倒计时[主要是闲家压筹码]

		// 设置庄家
		RedisUtil.set(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");

		// 设置房间状态
		HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(reqMsg.getRoomNo());
		roomInfo.setRoomCurMatchStatus(NnYesNoEnum.YES.getCode());
		roomInfo.setRoomCurMatchCount(roomInfo.getRoomCurMatchCount() + 1);
		roomInfo.setCurLandlordTimes(roomInfo.getCurLandlordTimes()+1);
		RedisUtil.hset(NnConstans.NN_ROOM_PRE+ reqMsg.getRoomNo() ,"roomInfo", JsonFormat.printToString(roomInfo.build()));

		rspData.setUser(userInfo);

		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		pushsingle(rspMsg.build(), ctx);

		// 更新当前状态为空闲状态
		RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() + "");

		batchSendLanlordReday(reqMsg);
		
	}
	
	/**
	 * 推送加注筹码
	 * @param reqMsg
	 * @param position
	 */
	public static void batchSendLanlordReday(HunNnBean.ReqMsg reqMsg){

		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		HunNnBean.RoomInfo.Builder roomInfo=getRoomInfo(roomNo);
		rspData.setRoom(roomInfo);
		rspData.setRestTime(getRestTime(reqMsg.getRoomNo()));
		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspMsg.setOperateType(NnPushMsgTypeEnum.USER_REDAY_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		for (String key : userSet) {
				map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
		}
		batchSendMsg(map);
		
	}
	

	/**
	 * 闲家增加筹码
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void farmerAddChip(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {
		
		long a=System.currentTimeMillis();
		if (LocalCacheUtil.hexist(NnConstans.NN_ROOM_LOCK_REQ+reqMsg.getRoomNo(), reqMsg.getUserId()+"")) {
			System.out.println("重复操作》》》》》》》》");
			return;
		}
		try{
			// 枷锁
			LocalCacheUtil.hset(NnConstans.NN_ROOM_LOCK_REQ+reqMsg.getRoomNo(), reqMsg.getUserId()+"",System.currentTimeMillis()+"");
		
			HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();
	
			rspMsg.setOperateType(NnRspMsgTypeEnum.FARMER_ADD_SCORE_FEEDBACK.getCode());
	
			// 判断加注时间是否结束
	
			String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());
	
			if (NnRoomMatchStatusEnum.PLAY_GAME_STATUS.getCode() != Integer.parseInt(curStatus)) {
				rspMsg.setCode(NnRspCodeEnum.$1006.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1006.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
	
			// 判断用户金币是否充足
			HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());
			
			if(userInfo.getPlayerType()==NnUserRoleEnum.LANDLORD.getCode()){
				rspMsg.setCode(NnRspCodeEnum.$0003.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$0003.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
			if(!NnChipEnum.checkGoldIsTrue(reqMsg.getChipGold())){
				rspMsg.setCode(NnRspCodeEnum.$0003.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$0003.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
			
			int totalChipGold= NnUtil.getCardTypeDouble(NnCardTypeTransEnum.NIU_NIU.getDestType())*(userInfo.getBaseGold()+reqMsg.getChipGold());
			if(userInfo.getUserGold()<totalChipGold){
				rspMsg.setCode(NnRspCodeEnum.$1102.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1102.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
				
			}
			
	
			if (userInfo.getUserGold() < reqMsg.getChipGold()) {
				rspMsg.setCode(NnRspCodeEnum.$1107.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1107.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
	
			HunNnBean.PositionInfo.Builder position = NnUtil.getPostion(reqMsg.getRoomNo(), reqMsg.getPosition());
	
			position.addListGold(reqMsg.getChipGold());
			position.setTotalGold(position.getTotalGold() + reqMsg.getChipGold());
			position.setPerGold(reqMsg.getChipGold());
			if (!position.getUidsList().contains(userInfo.getUserId())) {
				position.addUids(userInfo.getUserId());
			}
			
			HunNnBean.UserChip.Builder userChip=HunNnBean.UserChip.newBuilder();
			userChip.setGold(reqMsg.getChipGold());
			userChip.setUserId(reqMsg.getUserId());
			position.addUserChip(userChip.build());
			// 更新位置缓存
			NnUtil.setPosition(position, reqMsg.getRoomNo(), reqMsg.getPosition());
			
			NnManagerDao.instance().subUserGold(userInfo.getUserId(), reqMsg.getChipGold());
			
			logger.info("加注前用户id:{},加注前用户总金币:{},加注前总筹码:{},加注筹码:{}", userInfo.getUserId(), userInfo.getUserGold(), userInfo.getBaseGold(), userInfo.getPerGold());
			userInfo.setPerGold(reqMsg.getChipGold());
			userInfo.setBaseGold(userInfo.getBaseGold() + reqMsg.getChipGold());
			userInfo.setPlayerType(NnUserRoleEnum.FARMER.getCode());
			userInfo.setUserGold((userInfo.getUserGold() - userInfo.getPerGold()));
			logger.info("加注后用户id:{},加注后用户总金币:{},加注后总筹码:{},加注筹码:{}", userInfo.getUserId(), userInfo.getUserGold(), userInfo.getBaseGold(), userInfo.getPerGold());
			
			
			//设置加注位置
			HunNnBean.PositionInfo.Builder uPosition = HunNnBean.PositionInfo.newBuilder();
			uPosition.setPosition(reqMsg.getPosition());
			uPosition.setTotalGold(reqMsg.getChipGold());
			uPosition.setPerGold(reqMsg.getChipGold());
			uPosition.addListGold(reqMsg.getChipGold());
			uPosition.addUserChip(userChip);
			
			
			List<HunNnBean.PositionInfo> uPositions=new ArrayList<HunNnBean.PositionInfo>(userInfo.getUPositionsList());
			
			Map<Integer,HunNnBean.PositionInfo> upmap=new HashMap<Integer,HunNnBean.PositionInfo>();
			
			for(HunNnBean.PositionInfo up:uPositions){
				upmap.put(up.getPosition(), up);
			}
			
			if(upmap.containsKey(reqMsg.getPosition())){
				uPosition =upmap.get(reqMsg.getPosition()).toBuilder();
				uPosition
				.setTotalGold(uPosition.getTotalGold()+userChip.getGold())
				.setPerGold(userChip.getGold())
				.addListGold(userChip.getGold())
				.addUserChip(userChip.build());
				upmap.put(reqMsg.getPosition(), uPosition.build());
			}else{
				upmap.put(reqMsg.getPosition(), uPosition.build());
			}
			
			userInfo.clearUPositions();
			userInfo.addAllUPositions(upmap.values());
			
			RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));
//			
			{
				
				HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();
				
				for(HunNnBean.PositionInfo up:userInfo.getUPositionsList()){
					up=up.toBuilder().clearUserChip().clearUids().clearListGold().build();
				}
				position=position.clearUserChip().clearUids().clearListGold();
				rspData.setUser(userInfo);
				rspData.addPosition(position);
				rspMsg.setData(rspData);
				rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());
				pushsingle(rspMsg.build(), ctx);
				
			}
			long b=System.currentTimeMillis();
			System.out.println("执行时间============"+(b-a));
//			batchSendAddChip(reqMsg,position);
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			LocalCacheUtil.hdel(NnConstans.NN_ROOM_LOCK_REQ+reqMsg.getRoomNo(), reqMsg.getUserId()+"");
			
		}
	}
	
	
	
	
	/**
	 * 推送加注筹码
	 * @param reqMsg
	 * @param position
	 */
	public static void batchSendAddChip(HunNnBean.ReqMsg reqMsg,HunNnBean.PositionInfo.Builder position){
		
		position.clearCard();
		position.clearListGold();
		position.clearUids();

		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);


		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		rspData.setRestTime(getRestTime(reqMsg.getRoomNo()));
		HunNnBean.UserInfo.Builder userInfo=getCurUser(reqMsg.getUserId(), roomNo);
				
		
		rspData.setUser(userInfo);
		rspData.addPosition(position);
		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspMsg.setOperateType(NnPushMsgTypeEnum.FARMER_ADD_SCORE_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		for (String key : userSet) {
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
		}
		batchSendMsg(map);
		
	}
	/**
	 * 推送加注筹码
	 * @param reqMsg
	 * @param position
	 */
	public static void batchSendPostion(HunNnBean.ReqMsg reqMsg){
		

		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);


		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		List<HunNnBean.PositionInfo> list=NnUtil.getPositionInfo(reqMsg.getRoomNo());
		for(HunNnBean.PositionInfo up:list){
			up=up.toBuilder().clearUserChip().clearListGold().clearUids().clearCard().build();
		}
		
		
		rspData.addAllPosition(list);
		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspMsg.setOperateType(NnPushMsgTypeEnum.FARMER_ADD_SCORE_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		for (String key : userSet) {
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
		}
		batchSendMsg(map);
		
	}
	
	

	/**
	 * 发牌
	 * 
	 * @param reqMsg
	 */
	public static void sendCard(HunNnBean.ReqMsg reqMsg) {


		try {
			// 枷锁

			// 获取庄家位置，判断庄家是否为bug用户

			long landlordUserId = NnUtil.getPostion(reqMsg.getRoomNo(), 1).getUidsList().get(0);

			HunNnBean.UserInfo.Builder landLordUser = getCurUser(landlordUserId, reqMsg.getRoomNo());

			BugUser nbu = NnManagerDao.instance().getBugUser(landlordUserId + "");

			String[] sers = new String[] { "1", "2", "3", "4", "5" };

			RedisUtil.del(NnConstans.NN_BUG_USER_PRE + reqMsg.getRoomNo());
			Map<String, List<Integer>> userCardMap = null;
			if (null != nbu) {
				RedisUtil.hset(NnConstans.NN_BUG_USER_PRE + reqMsg.getRoomNo(), landlordUserId + "", System.currentTimeMillis() + "");
				userCardMap = NnCardUtil.getCard(nbu.getBugType(), "1", nbu.getWinRate(), sers);
			} else {
				userCardMap = NnCardUtil.getSimpleCard(sers);
			}
			Map<String, HunNnBean.PositionInfo> userMap = NnUtil.getPositionInfoToMap(reqMsg.getRoomNo());
			for (String key : userCardMap.keySet()) {

				if (userMap.containsKey(key)) {

					HunNnBean.PositionInfo.Builder position = userMap.get(key).toBuilder();
					List<Integer> cardList = userCardMap.get(key);
					HunNnBean.CardInfo.Builder cardInfo = HunNnBean.CardInfo.newBuilder();

					Integer cardType = NnCardUtil.getCardType(cardList);
					cardInfo.setCardType(cardType);
					cardInfo.addAllNum(cardList);
					position.setCard(cardInfo);

					NnUtil.setPosition(position, reqMsg.getRoomNo(), Integer.parseInt(key));

					// 如果是庄家位置
					if ("1" == key) {
						landLordUser.setCard(cardInfo);
						RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(landLordUser.build()));

					}
				}

			}
		

			// 设置当前当前房间状态为展示比赛结果
			RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.SHOW_MATCH_RESULT_STATUS.getCode() + "");

			// 增加展示比赛结果定时任务
			{
				GameTimoutVo timeout = new GameTimoutVo();
				timeout.setRestTime(System.currentTimeMillis());
				timeout.setRestTimeType(NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode());
				RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));
				ServerManager.executorTask.schedule(new MyTimerTask(reqMsg, NnTimeTaskEnum.SHOW_MATCH_RESULT_TIME.getCode(),timeout.getRestTime()), NnConstans.SHOW_MATCH_RESULT_TIME, TimeUnit.SECONDS);
				NnUtil.delNnRoomSendGoldTimer(reqMsg.getRoomNo());
			}
			
			// 推送发牌操作
			batchSendCard(reqMsg);

			// 计算比赛结果
			ServerManager.executor.execute(new NnWork(reqMsg, NnWrokEnum.SHOW_MATCH_RESULT.getCode()));

			
		} finally {
			RedisUtil.hdel(NnConstans.NN_ROOM_LOCK_REQ, reqMsg.getRoomNo());
		}

	}

	/**
	 * 发牌推送
	 * 
	 * @param reqMsg
	 */
	public static void batchSendCard(HunNnBean.ReqMsg reqMsg) {

		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), roomNo);
		userInfo.clearToken();
		userInfo.clearRoomNo();
		userInfo.clearCard();

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		rspData.setRestTime(getRestTime(reqMsg.getRoomNo()));

		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());
		HunNnBean.UserInfo.Builder landLordUser = getCurUser(Long.parseLong(landlordUserId), roomNo);
		rspData.setLandlord(landLordUser);
		
		List<HunNnBean.PositionInfo> list=NnUtil.getPositionInfo(reqMsg.getRoomNo());
		
		for(HunNnBean.PositionInfo up:list){
			up=up.toBuilder().clearUserChip().clearListGold().clearUids().clearCard().build();
		}
		rspData.addAllPosition(list);
		rspData.setUser(userInfo);
		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspMsg.setOperateType(NnPushMsgTypeEnum.SEND_CARD_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		for (String key : userSet) {
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());

		}
		batchSendMsg(map);

	}

	/**
	 * 计算比赛结果
	 * 计算规则用户金币以实际输赢为准，每个位置显示的输赢为末日输赢
	 * 实际结果用户的输赢可能由于用户金币不足导致和每个位置不同
	 * 
	 * @param roomNo
	 */
	public static void showMatchResult(String roomNo) {
		
		try {
			long time1=System.currentTimeMillis();
			HunNnBean.PositionInfo.Builder landlordPosition = NnUtil.getPostion(roomNo, 1);
			HunNnBean.PositionInfo.Builder position2 = NnUtil.getPostion(roomNo, 2);
			HunNnBean.PositionInfo.Builder position3 = NnUtil.getPostion(roomNo, 3);
			HunNnBean.PositionInfo.Builder position4 = NnUtil.getPostion(roomNo, 4);
			HunNnBean.PositionInfo.Builder position5 = NnUtil.getPostion(roomNo, 5);
	
			int landlordDoubleType = NnCardUtil.getCardType(landlordPosition.getCard().getNumList());
			int landlordDouble = NnUtil.getCardTypeDouble(landlordDoubleType);
	
			// 获取房间的信息
			HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(roomNo);
	
			for (HunNnBean.CardTypeDouble doubleType : roomInfo.getCardDoubleList()) {
	
				if (doubleType.getCardType() == landlordDoubleType) {
					landlordDouble = NnUtil.getCardTypeSpecialDouble(doubleType.getCardType());
				}
			}
	
			HunNnBean.CardInfo.Builder landlordCard=HunNnBean.CardInfo.newBuilder(landlordPosition.getCard());
			landlordCard.setCardDouble(landlordDouble);
			landlordPosition.setCard(landlordCard);
			
			
			HunNnBean.UserInfo.Builder landlordUser = getCurUser(landlordPosition.getUids(0), roomNo);
	
			
			calculation(roomNo, roomInfo, landlordUser, landlordDouble, landlordPosition, position2);
			calculation(roomNo, roomInfo, landlordUser, landlordDouble, landlordPosition, position3);
			calculation(roomNo, roomInfo, landlordUser, landlordDouble, landlordPosition, position4);
			calculation(roomNo, roomInfo, landlordUser, landlordDouble, landlordPosition, position5);
	
			
			
			 List<Long> uids=getPositionAllUsers(position2,position3,position4,position5);
			
			 Map<Long,HunNnBean.UserInfo.Builder> allUserMap=new HashMap<Long,HunNnBean.UserInfo.Builder>();
			//计算每个用户位置总盈亏
			 for(Long uid:uids){
				 
				 HunNnBean.UserInfo.Builder curUser= HunNnBean.UserInfo.newBuilder();
				 curUser= getCurUser(uid, roomNo);
				 curUser.clearUPositions();
				 
				 Map<Integer,HunNnBean.PositionInfo> upchipMap=new HashMap<Integer,HunNnBean.PositionInfo>();
				 if(position2.getUidsList().contains(uid)){
					 calUserWinGold(uid, position2, curUser, allUserMap,upchipMap);
				 }
				 if(position3.getUidsList().contains(uid)){
					 calUserWinGold(uid, position3, curUser, allUserMap,upchipMap);
				 }
				 if(position4.getUidsList().contains(uid)){
					 calUserWinGold(uid, position4, curUser, allUserMap,upchipMap);
				 }
				 if(position5.getUidsList().contains(uid)){
					 calUserWinGold(uid, position5, curUser, allUserMap,upchipMap);
				 }
				 
				 for(Integer p:upchipMap.keySet()){
					 curUser.addUPositions(upchipMap.get(p));
				 }
				 allUserMap.put(uid, curUser);
				
			 }
			
			//重新计算闲家用户金币，主要是为了防止用户金币不足情况
			 
			 for(long uid:allUserMap.keySet()){
				 
				 HunNnBean.UserInfo.Builder user=allUserMap.get(uid);
				 long winGold=user.getUserGold()+user.getWinGold()+user.getBaseGold();
				 if(winGold<0){
					 landlordUser.setWinGold((int)(landlordUser.getWinGold()+winGold));
					 user.setWinGold(-(int)(user.getUserGold()+user.getBaseGold()));
					 allUserMap.put(uid, user);
				 }
				 
			 }
	//		 
			
			
			
			// 等到庄家总金币
			long totalGold = NnManagerDao.instance().getUserGold(landlordUser.getUserId());
			landlordUser.setUserGold(totalGold);
			landlordUser.setTotalGold(totalGold);
			
			if (landlordUser.getTotalGold() + landlordUser.getWinGold() < 0) {
				// 计算闲家赢家用户的总金币数
				int userWinTotalGold = 0;
				
				calUserWinTotalGold(position2,userWinTotalGold);
				calUserWinTotalGold(position3,userWinTotalGold);
				calUserWinTotalGold(position4,userWinTotalGold);
				calUserWinTotalGold(position5,userWinTotalGold);
				
	
				int userRealWinTotalGold = 0;
				
				calUserRealWinTotalGold(position2, userRealWinTotalGold,userWinTotalGold,landlordUser,allUserMap);
				calUserRealWinTotalGold(position3, userRealWinTotalGold,userWinTotalGold,landlordUser,allUserMap);
				calUserRealWinTotalGold(position4, userRealWinTotalGold,userWinTotalGold,landlordUser,allUserMap);
				calUserRealWinTotalGold(position5, userRealWinTotalGold,userWinTotalGold,landlordUser,allUserMap);
				
				landlordUser.setWinGold(-userRealWinTotalGold);
				
			}
			
			
			//计算每个位置总盈亏
			
			calPositionTotalWinGold(position2);
			calPositionTotalWinGold(position3);
			calPositionTotalWinGold(position4);
			calPositionTotalWinGold(position5);
			
			//设置庄家位置总盈亏
			landlordPosition.setWinGold(landlordUser.getWinGold());
			
			 allUserMap.put(landlordUser.getUserId(), landlordUser);
			 
			// 更新缓存
			for (Long key: allUserMap.keySet()) {
				RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + roomNo, key + "", JsonFormat.printToString(allUserMap.get(key).build()));
	
			}
			
			NnUtil.setPosition(landlordPosition, roomNo, 1);
			NnUtil.setPosition(position2, roomNo, 2);
			NnUtil.setPosition(position3, roomNo, 3);
			NnUtil.setPosition(position4, roomNo, 4);
			NnUtil.setPosition(position5, roomNo, 5);
	
			NnRoom room = getRoomVo(roomNo);
	
			// 封装缓存数据
	
			List<DbVo> listDb = new ArrayList<DbVo>();
	
	
			
			for ( Long key: allUserMap.keySet()) {
				HunNnBean.UserInfo.Builder user=allUserMap.get(key);
				DbVo dbVo = new DbVo();
	
				dbVo.setUserId(user.getUserId());
				dbVo.setWinGold(user.getWinGold()+user.getBaseGold());
	
				String orderNo = UUID.randomUUID().toString();
				GoldRecord gr = new GoldRecord();
				gr.setUserId(user.getUserId());
				gr.setOrderNo(orderNo);
				gr.setGoldCount(user.getCostGold());
				gr.setCostType(GoldTypeEnum.HUNDRED_NIU_COST.getType());
				gr.setBindId(room.getCreateUserId());
				gr.setCreateDate(new Timestamp(System.currentTimeMillis()));
				
				dbVo.setGoldRecord(gr);
	
				UserCostRecord ucr = new UserCostRecord();
				ucr.setUserId(user.getUserId());
				ucr.setRoomNo(room.getRoomNo());
				ucr.setRoomId(room.getId());
				ucr.setOrderNo(orderNo);
				ucr.setGameType(GoldTypeEnum.HUNDRED_NIU_COST.getType());
				ucr.setCostGold(room.getCostGold().intValue());
				ucr.setIsDel(NnYesNoEnum.NO.getCode());
				ucr.setBindId(room.getId());
				ucr.setCreateDate(new Date());
	
				dbVo.setUserCostRecord(ucr);
	
				NnRoomMatchUser nrmu = new NnRoomMatchUser();
				nrmu.setUserId(user.getUserId());
				nrmu.setRoomId(room.getId());
				nrmu.setRoomNo(room.getRoomNo());
				nrmu.setMatchNum(roomInfo.getRoomCurMatchCount());
				nrmu.setTotalGold(user.getTotalGold());
				nrmu.setBaseGold(user.getBaseGold());
				nrmu.setCostGold(user.getCostGold());
				nrmu.setPlayerRole(user.getPlayerType());
				nrmu.setWinGold(user.getWinGold());
				if (user.getWinGold() >= 0) {
					nrmu.setIsWin(NnYesNoEnum.YES.getCode());
				} else {
					nrmu.setIsWin(NnYesNoEnum.NO.getCode());
				}
				
	
				HunNnBean.CardInfo card = HunNnBean.CardInfo.getDefaultInstance();
				if (landlordPosition.getUidsList().contains(user.getUserId())) {
					card = landlordPosition.getCard();
				} else if (position2.getUidsList().contains(user.getUserId())) {
					card = position2.getCard();
				} else if (position3.getUidsList().contains(user.getUserId())) {
					card = position3.getCard();
				} else if (position4.getUidsList().contains(user.getUserId())) {
					card = position4.getCard();
				} else if (position5.getUidsList().contains(user.getUserId())) {
					card = position5.getCard();
				}
				String cards = "";
				for (int num : card.getNumList()) {
					cards += num + ",";
	
				}
				nrmu.setCards(cards.substring(0, cards.length()));
				nrmu.setCardType(NnCardUtil.getCardType(card.getNumList()));
				nrmu.setOrderNo(orderNo);
				if (RedisUtil.hexists(NnConstans.NN_BUG_USER_PRE + roomNo, user.getUserId() + "")) {
					nrmu.setIsBug(NnYesNoEnum.YES.getCode());
				} else {
					nrmu.setIsBug(NnYesNoEnum.NO.getCode());
				}
				nrmu.setCreateDate(new Date());
	
				dbVo.setNnRoomMatchUser(nrmu);
	
				UserMatchRecord umr = new UserMatchRecord();
				umr.setUserId(user.getUserId());
				umr.setWinGold(user.getWinGold());
				umr.setRoomId(room.getId());
				umr.setOrderNo(orderNo);
				umr.setGameType(GameTypeEnum.HUNDRED_NIU.getType());
				umr.setBindId(room.getId());
				umr.setCreateDate(new Date());
	
				dbVo.setUserMatchRecord(umr);
	
				listDb.add(dbVo);
				//
			}
	
			
			List<NnRoomMatchUserDetail> list=new ArrayList<NnRoomMatchUserDetail>();
			List<HunNnBean.PositionInfo> listPosition=NnUtil.getPositionInfo(roomNo);
			
			if(allUserMap!=null){
				
				
			
			for(HunNnBean.PositionInfo bean:listPosition){
				
				if(bean.getPosition()!=1){
					for(HunNnBean.UserChip uchip: bean.getUserChipList()){
						HunNnBean.UserInfo.Builder user=allUserMap.get(uchip.getUserId());
						NnRoomMatchUserDetail nrmu = new NnRoomMatchUserDetail();
						nrmu.setUserId(user.getUserId());
						nrmu.setRoomId(room.getId());
						nrmu.setRoomNo(room.getRoomNo());
						nrmu.setMatchNum(roomInfo.getRoomCurMatchCount());
						nrmu.setBaseGold(uchip.getGold());
						nrmu.setPlayerRole(user.getPlayerType());
						nrmu.setWinGold(uchip.getWinGold());
						nrmu.setDoublex(1);
						nrmu.setCostGold(uchip.getCostGold());
						if (uchip.getWinGold() >= 0) {
							nrmu.setIsWin(NnYesNoEnum.YES.getCode());
						} else {
							nrmu.setIsWin(NnYesNoEnum.NO.getCode());
						}
						
						HunNnBean.CardInfo card =bean.getCard();
						String cards = "";
						for (int num : card.getNumList()) {
							cards += num + ",";
	
						}
						nrmu.setCards(cards.substring(0, cards.length()));
						nrmu.setCardType(NnCardUtil.getCardType(card.getNumList()));
						nrmu.setCreateDate(new Date());
						nrmu.setPosition(bean.getPosition());
						list.add(nrmu);
					}
					
				}else{
					
					
					HunNnBean.UserInfo.Builder user=getCurUser(bean.getUids(0), roomNo);
					NnRoomMatchUserDetail nrmu = new NnRoomMatchUserDetail();
					nrmu.setUserId(user.getUserId());
					nrmu.setRoomId(room.getId());
					nrmu.setRoomNo(room.getRoomNo());
					nrmu.setMatchNum(roomInfo.getRoomCurMatchCount());
					nrmu.setBaseGold(user.getBaseGold());
					nrmu.setCostGold(user.getCostGold());
					nrmu.setPlayerRole(user.getPlayerType());
					nrmu.setWinGold(user.getWinGold());
					nrmu.setDoublex(1);
					if (user.getWinGold() >= 0) {
						nrmu.setIsWin(NnYesNoEnum.YES.getCode());
					} else {
						nrmu.setIsWin(NnYesNoEnum.NO.getCode());
					}
					
					HunNnBean.CardInfo card =bean.getCard();
					String cards = "";
					for (int num : card.getNumList()) {
						cards += num + ",";
	
					}
					nrmu.setCards(cards.substring(0, cards.length()-1));
					nrmu.setCardType(NnCardUtil.getCardType(card.getNumList()));
					nrmu.setCreateDate(new Date());
					nrmu.setPosition(1);
					list.add(nrmu);
				}
				
			}
			
			}
			
			NnManagerDao.instance().addDb(listDb);
			NnManagerDao.instance().addMatchDetail(list);
			
			
			//更新用户金币
			
			// 更新缓存
			for (Long key: allUserMap.keySet()) {
				long userGold=NnManagerDao.instance().getUserGold(key);
				HunNnBean.UserInfo.Builder userInfo=allUserMap.get(key);
				userInfo.setUserGold(userGold);
				RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + roomNo, key + "", JsonFormat.printToString(allUserMap.get(key).build()));
	
			}
			long time2=System.currentTimeMillis();
			System.out.println("计算比赛结果耗时:"+(time2-time1));
			// 推送比赛结果,主要是计算庄家输赢和自己输赢
			batchSendMatchResult(roomNo);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			e.printStackTrace();
		}
	}
	
	private static void calUserWinGold(Long uid,HunNnBean.PositionInfo.Builder position,HunNnBean.UserInfo.Builder curUser,Map<Long,HunNnBean.UserInfo.Builder> allUserMap, Map<Integer,HunNnBean.PositionInfo>  upchipMap){
		
		
		for(HunNnBean.UserChip uchip:position.getUserChipList()){
			 if(uchip.getUserId()==uid){
				 
				 
				 if(upchipMap.containsKey(position.getPosition())){
					 HunNnBean.PositionInfo.Builder pp= upchipMap.get(position.getPosition()).toBuilder();
					 pp.setWinGold(pp.getWinGold()+uchip.getWinGold());
					 pp.setTotalGold(pp.getTotalGold()+uchip.getGold());
					 pp.addUserChip(uchip);
					 
					 upchipMap.put(position.getPosition(), pp.build());
				 }else{
					 HunNnBean.PositionInfo.Builder pp=HunNnBean.PositionInfo.newBuilder();
					 pp.setPosition(position.getPosition());
					 pp.setWinGold(pp.getWinGold()+uchip.getWinGold());
					 pp.setTotalGold(uchip.getGold());
					 pp.addUserChip(uchip);
					 
					 upchipMap.put(position.getPosition(), pp.build());
				 }
				 curUser.setWinGold(curUser.getWinGold()+uchip.getWinGold()-uchip.getCostGold());
				 curUser.setCostGold(curUser.getCostGold()+uchip.getWinGold());
				 allUserMap.put(uid, curUser);
			 }
			 
		 }
	}
	
	/**
	 * 获取该位置用户
	 * @param positions
	 * @return
	 */
	private static List<Long> getPositionAllUsers(HunNnBean.PositionInfo.Builder... positions){
		
		List<Long> uids=new ArrayList<Long>();
		for(HunNnBean.PositionInfo.Builder position:positions){
			uids.addAll(position.getUidsList());
		}
		return uids;
	}

	/**
	 * 计算用户总赢得金币
	 * @param position
	 * @param userWinTotalGold
	 */
	private static void calUserWinTotalGold(HunNnBean.PositionInfo.Builder position,int userWinTotalGold){
		
		for (HunNnBean.UserChip userChip : position.getUserChipList()) {
			
			if(userChip.getWinGold()>=0){
				userWinTotalGold = userWinTotalGold + userChip.getWinGold()-userChip.getCostGold();
			}
			
		}
		
	}
	
	/**
	 * 计算用户总赢得金币
	 * @param position
	 * @param userWinTotalGold
	 */
	private static void calUserRealWinTotalGold(HunNnBean.PositionInfo.Builder position,
			int userRealWinTotalGold,int userWinTotalGold,HunNnBean.UserInfo.Builder landlordUser,
			Map<Long,HunNnBean.UserInfo.Builder> allUserMap){
		List<HunNnBean.UserChip> listChipUser=new ArrayList<HunNnBean.UserChip>();
		for (HunNnBean.UserChip userChip : position.getUserChipList()) {
			
			if(userChip.getWinGold()>=0){
				
				int m = (int) Math.floor((userChip.getWinGold() / userWinTotalGold) * landlordUser.getTotalGold());
				userRealWinTotalGold = userRealWinTotalGold + m;
//				userChip=userChip.toBuilder().setWinGold(m).build();
				
				//去除盈利金币小于服务费的情况
				if(allUserMap.containsKey(userChip.getUserId())){
					
					int diffGold=userChip.getWinGold()-userChip.getCostGold()-m;
					HunNnBean.UserInfo.Builder user=allUserMap.get(userChip);
					user.setWinGold(user.getWinGold()-diffGold);
					allUserMap.put(userChip.getUserId(), user);
				}
				
//				listChipUser.add(userChip);
			}else{
				break;
			}
			
		}
		position.clearUserChip();
		position.addAllUserChip(listChipUser);
		
	}
	
	/**
	 * 计算每个位置总盈亏
	 * @param position
	 */
	private static void calPositionTotalWinGold(HunNnBean.PositionInfo.Builder position){
		int posionTotalWinGold=0;
		for (HunNnBean.UserChip userChip : position.getUserChipList()) {
			posionTotalWinGold+=userChip.getWinGold();
		}
		position.setWinGold(posionTotalWinGold);
	}
	
	
	
	/**
	 * 金币计算
	 * 
	 * @param listUser
	 * @param roomNo
	 * @param roomInfo
	 * @param landlordUser
	 * @param landlordDouble
	 * @param landlordPosition
	 * @param farmerPosition
	 */
	private static void calculation(String roomNo, HunNnBean.RoomInfo.Builder roomInfo, HunNnBean.UserInfo.Builder landlordUser, int landlordDouble, HunNnBean.PositionInfo.Builder landlordPosition,
			HunNnBean.PositionInfo.Builder farmerPosition) {
		logger.info("比牌数据one====={}||||two======={}",JSONObject.toJSONString(landlordPosition.getCard().getNumList()),JSONObject.toJSONString(farmerPosition.getCard().getNumList()));
		
		boolean matchResult = NnCardUtil.compareCard(landlordPosition.getCard().getNumList(), farmerPosition.getCard().getNumList());
		int farmerDoubleType = NnCardUtil.getCardType(farmerPosition.getCard().getNumList());
		int farmertDouble = NnUtil.getCardTypeDouble(farmerDoubleType);
		for (HunNnBean.CardTypeDouble doubleType : roomInfo.getCardDoubleList()) {

			if (doubleType.getCardType() == farmerDoubleType) {
				farmertDouble = NnUtil.getCardTypeSpecialDouble(doubleType.getCardType());
			}
		}
		HunNnBean.CardInfo.Builder farmerCard=HunNnBean.CardInfo.newBuilder(farmerPosition.getCard());
		farmerCard.setCardDouble(matchResult?landlordDouble:farmertDouble);
		farmerPosition.setCard(farmerCard);
		
		List<HunNnBean.UserChip> listChipUser=new ArrayList<HunNnBean.UserChip>();
		
		for (HunNnBean.UserChip orgUserChip : farmerPosition.getUserChipList()) {
			HunNnBean.UserChip.Builder userChip=HunNnBean.UserChip.newBuilder(orgUserChip);
			// 积分计算[(庄家底分+闲家底分)*牛的倍数]
			// 底分
			int baseScore = userChip.getGold();
			// 庄家赢
			if (matchResult) {

				int winGold = baseScore * landlordDouble;
				int costGold=new BigDecimal(winGold*NnUtil.getCostRate()/100).setScale(BigDecimal.ROUND_UP, 0).intValue();
				userChip.setCostGold(costGold);
				int realWinGold=winGold-costGold;
				
				landlordUser.setWinGold(landlordUser.getWinGold() + realWinGold);

				userChip.setWinGold(-winGold);
				
			} else {
				int winGold = baseScore * landlordDouble;
				int costGold=new BigDecimal(winGold*NnUtil.getCostRate()/100).setScale(BigDecimal.ROUND_UP, 0).intValue();
				userChip.setCostGold(costGold);
				int realWinGold=winGold-costGold;
				
				landlordUser.setWinGold(landlordUser.getWinGold() - realWinGold);

				userChip.setWinGold(winGold);
			}
			listChipUser.add(userChip.build());
		}
		
		//更改每个position的数据
		
		farmerPosition.clearUserChip();
		farmerPosition.addAllUserChip(listChipUser);
		
		
	}
	
	

	/**
	 * 推送比赛结果
	 * 
	 * @param reqMsg
	 */
	private static void batchSendMatchResult(String roomNo) {

		Set<String> allUserSet = getAllUserSet(roomNo);

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + roomNo);
		HunNnBean.UserInfo.Builder landLordUser = getCurUser(Long.parseLong(landlordUserId), roomNo);
		List<HunNnBean.PositionInfo> listPosition = NnUtil.getPositionInfo(roomNo);
		
		
		for(HunNnBean.PositionInfo position:listPosition){
			position=position.toBuilder().clearUserChip().clearUids().clearListGold().build();
		}

		int time = getRestTime(roomNo);
		for (String key : allUserSet) {

			HunNnBean.UserInfo.Builder userInfo = getCurUser(Long.parseLong(key), roomNo);

			HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

			HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

			rspData.setRestTime(time);

			rspData.addAllPosition(listPosition);
			rspData.setUser(userInfo);
			rspData.setLandlord(landLordUser);
			rspData.setCurRoomStatus(Integer.parseInt(curStatus));
			rspMsg.setOperateType(NnPushMsgTypeEnum.SHOW_MATCH_RESULT_PUSH.getCode());
			rspMsg.setData(rspData);
			rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());

		}
//		System.out.println(map);
		batchSendMsg(map);

	}


	/**
	 * 退出房间
	 * 
	 * @param reqMsg
	 * @return
	 */
	public static void exitRoom(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();
		// 判断是否庄家，如果是庄家则提示非法操作

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());
		
		
		
		if (userInfo.getPlayerType() == NnUserRoleEnum.LANDLORD.getCode()) {
			// 判断是否可以退出
			String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());

			if (NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() != Integer.parseInt(curStatus)) {
				rspMsg.setCode(NnRspCodeEnum.$1008.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1008.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
			// 删除庄家缓存
			HunNnBean.PositionInfo.Builder landlordPosition = HunNnBean.PositionInfo.newBuilder();
			landlordPosition.setPosition(1);
			NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(), 1);
			RedisUtil.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());
			
			//删除倒计时
			RedisUtil.hdel(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo());
			// 设置新的庄家
			setLandlord(reqMsg.getRoomNo());
		}else{
			// 判断此人是否参与下注，如果参与下注无法退出房间

			if (userInfo.getBaseGold() > 0) {
				rspMsg.setCode(NnRspCodeEnum.$1009.getCode());
				rspMsg.setMsg(NnRspCodeEnum.$1009.getMsg());
				pushsingle(rspMsg.build(), ctx);
				return;
			}
		}


		// 删除房间用户信息
		RedisUtil.hdel(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");

		// 移除房间用户
		RedisUtil.srem(NnConstans.NN_ROOM_ALL_USER_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "");// 牛牛所有用户

		// 更新用户房间缓存信息

		HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(reqMsg.getRoomNo());
		roomInfo.setRoomCurPersonCount(roomInfo.getRoomCurPersonCount() - 1);// 设置房间当前人数

		if (roomInfo.getRoomMaxPersonCount() > roomInfo.getRoomCurPersonCount()) {

			roomInfo.setRoomCurStatus(NnYesNoEnum.NO.getCode());// 当前人数未满
		}
		RedisUtil.hset(NnConstans.NN_ROOM_PRE + roomInfo.getRoomNo(), "roomInfo", JsonFormat.printToString(roomInfo.build()));
		NnManagerDao.instance().deleteRoomUser(reqMsg.getUserId(), reqMsg.getRoomNo(), GameTypeEnum.HUNDRED_NIU.getType());

		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());
		rspMsg.setOperateType(NnRspMsgTypeEnum.EXIT_ROOM_FEEDBACK.getCode());
		
		pushsingle(rspMsg.build(), ctx);
		
		batchSendExitRoom(reqMsg.getRoomNo());
	}
	
	
	/**
	 * 退出房间推送
	 * @param roomNo
	 */
	public static void batchSendExitRoom(String roomNo){
	
		HunNnBean.RoomInfo.Builder roomInfo=getRoomInfo(roomNo);
		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();
	
		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();
	
		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();
	
		Set<String> userSet=getAllUserSet(roomNo);

		rspData.setRoom(roomInfo);
		rspMsg.setOperateType(NnPushMsgTypeEnum.EXIT_ROOM_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());
	
		for (String key : userSet) {
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
	
		}
		batchSendMsg(map);
	}

	/**
	 * 用户房间初始化
	 * 
	 * @param reqMsg
	 */
	public static void initRoom(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();
		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();
		String roomNo = reqMsg.getRoomNo();

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);

		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + roomNo);
		if(StringUtils.isNotEmpty(landlordUserId)){
			HunNnBean.UserInfo.Builder landLordUser = getCurUser(Long.parseLong(landlordUserId), roomNo);
			rspData.setLandlord(landLordUser);
			
		}
	
		HunNnBean.RoomInfo.Builder roomInfo= getRoomInfo(reqMsg.getRoomNo());
		List<HunNnBean.PositionInfo> listPosition=NnUtil.getPositionInfo(reqMsg.getRoomNo());
		
		for(HunNnBean.PositionInfo up:listPosition){
			up=up.toBuilder().clearUserChip().clearListGold().clearUids().clearCard().build();
		}
		int time = getRestTime(roomNo);

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), roomNo);
		
		
		long clubGolds = NnManagerDao.instance().getUserGold(reqMsg.getUserId());
		userInfo.setUserGold((int) clubGolds);
		RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));

		
		rspData.setRestTime(time);
		rspData.setRoom(roomInfo);
		rspData.addAllPosition(listPosition);
		rspData.setUser(userInfo);
		
		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspMsg.setOperateType(NnRspMsgTypeEnum.INIT_ROOM_FEEDBACK.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		pushsingle(rspMsg.build(), ctx);

	}

	/**
	 * 庄家退出房间
	 * 
	 * @param reqMsg
	 * @param ctx
	 */
	public static void landlordExitRoom(HunNnBean.ReqMsg reqMsg, ChannelHandlerContext ctx) {

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();
		// 判断是否庄家，如果是庄家则提示非法操作

		HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());

		if (userInfo.getPlayerType() != NnUserRoleEnum.LANDLORD.getCode()) {
			rspMsg.setCode(NnRspCodeEnum.$0003.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$0003.getMsg());
			pushsingle(rspMsg.build(), ctx);
			return;
		}

		// 判断是否可以退出
		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());

		if (NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() == Integer.parseInt(curStatus) || NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() == Integer.parseInt(curStatus)) {
			rspMsg.setCode(NnRspCodeEnum.$1008.getCode());
			rspMsg.setMsg(NnRspCodeEnum.$1008.getMsg());
			pushsingle(rspMsg.build(), ctx);
			return;
		}
		// 删除庄家缓存
		HunNnBean.PositionInfo.Builder landlordPosition = HunNnBean.PositionInfo.newBuilder();
		landlordPosition.setPosition(1);
		NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(), 1);
		RedisUtil.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());

		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());
		pushsingle(rspMsg.build(), ctx);

		// 设置新的庄家
		setLandlord(reqMsg.getRoomNo());

	}

	public static void setLandlordTimerTask(HunNnBean.ReqMsg reqMsg) {

		// 先清除用户的缓存数据

		HunNnBean.PositionInfo.Builder landlordPosition = NnUtil.getPostion(reqMsg.getRoomNo(), 1);
		HunNnBean.PositionInfo.Builder position2 = NnUtil.getPostion(reqMsg.getRoomNo(), 2);
		HunNnBean.PositionInfo.Builder position3 = NnUtil.getPostion(reqMsg.getRoomNo(), 3);
		HunNnBean.PositionInfo.Builder position4 = NnUtil.getPostion(reqMsg.getRoomNo(), 4);
		HunNnBean.PositionInfo.Builder position5 = NnUtil.getPostion(reqMsg.getRoomNo(), 5);

		landlordPosition.clearCard().clearPerGold().clearTotalGold().clearListGold().clearPerGold();
		position2.clearCard().clearPerGold().clearTotalGold().clearUids().clearListGold().clearUserChip().clearWinGold();
		position3.clearCard().clearPerGold().clearTotalGold().clearUids().clearListGold().clearUserChip().clearWinGold();
		position4.clearCard().clearPerGold().clearTotalGold().clearUids().clearListGold().clearUserChip().clearWinGold();
		position5.clearCard().clearPerGold().clearTotalGold().clearUids().clearListGold().clearUserChip().clearWinGold();

		NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(), 1);
		NnUtil.setPosition(position2, reqMsg.getRoomNo(), 2);
		NnUtil.setPosition(position3, reqMsg.getRoomNo(), 3);
		NnUtil.setPosition(position4, reqMsg.getRoomNo(), 4);
		NnUtil.setPosition(position5, reqMsg.getRoomNo(), 5);
		
		
		

		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());

		Set<String> allUser = getAllUserSet(reqMsg.getRoomNo());

		for (String key : allUser) {
			HunNnBean.UserInfo.Builder curUser = getCurUser(Long.parseLong(key), reqMsg.getRoomNo());
			if (key.equals(landlordUserId)) {
				curUser.clearBaseGold().clearCard().clearTotalGold().clearPerGold().clearUPositions().clearWinGold().clearIsReday();

			} else {
				curUser.clearBaseGold().clearCard().clearTotalGold().clearPerGold().clearPlayerType().clearUPositions().clearWinGold();
			}
			RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), key, JsonFormat.printToString(curUser.build()));

		}

		/**
		 * 判断庄家金币是否充足， 如果不足就提出庄家,同时设置新的庄家 如果充足就推送庄家准备按钮
		 */

		HunNnBean.UserInfo.Builder landlordUser = getCurUser(Long.parseLong(landlordUserId), reqMsg.getRoomNo());
		// 庄家金币不足
		if (NnConstans.NN_LANDLORD_MIN_GOLD > landlordUser.getUserGold()||landlordUser.getLandlordTimes()>=NnConstans.NN_LANDLORD_TIMES) {

			landlordPosition.clearUids();
			NnUtil.setPosition(landlordPosition, reqMsg.getRoomNo(), 1);

			landlordUser.clearLandlordTimes();
			landlordUser.clearIsReday();
			landlordUser.clearIsApplyLandlord();
			RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), landlordUserId, JsonFormat.printToString(landlordUser.build()));
			// 更新当前状态为空闲状态
			RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() + "");
			
			NnManagerDao.instance().deleteApplyLandlorder(reqMsg.getRoomNo(), reqMsg.getUserId());
			
			GameTimoutVo timeout = new GameTimoutVo();
			timeout.setRestTime(0);
			timeout.setRestTimeType(NnTimeTaskEnum.CLEAR_TIME.getCode());
			RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));
			
			setLandlord(reqMsg.getRoomNo());
			
		} else {
			reqMsg = reqMsg.toBuilder().setUserId(Long.parseLong(landlordUserId)).build();
			HunNnBean.UserInfo.Builder userInfo = getCurUser(reqMsg.getUserId(), reqMsg.getRoomNo());
			// 增加准备倒计时
			{
				long userRedayTime = System.currentTimeMillis();
				GameTimoutVo timeout = new GameTimoutVo();
				timeout.setRestTime(userRedayTime);
				timeout.setRestTimeType(NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode());
				RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));
				// 增加个人准备倒计时
				{
					ServerManager.executorTask.schedule(new MyTimerTask(reqMsg, NnTimeTaskEnum.USER_REDAY_IDLE_TIME.getCode(),timeout.getRestTime()), NnConstans.USER_REDAY_IDLE_TIME, TimeUnit.SECONDS);
				}
				userInfo.setRedayTime(NnConstans.USER_REDAY_IDLE_TIME);
			}
			
			userInfo.setPlayerType(NnUserRoleEnum.LANDLORD.getCode());
			userInfo.setIsReday(NnYesNoEnum.NO.getCode());
			RedisUtil.hset(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo(), reqMsg.getUserId() + "", JsonFormat.printToString(userInfo.build()));
			// 更新当前状态为空闲状态
			RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.INIT_HOME_IDLE_STATUS.getCode() + "");

			batchSendLandlord(reqMsg);
		}

	}

	/**
	 * 当前为空闲状态处理
	 * 
	 * @param reqMsg
	 */
	public static void idleTimeTask(HunNnBean.ReqMsg reqMsg) {

		// 先更新游戏状态为游戏状态，此时玩家可以进行加注操作

		// 设置空游戏状态
		GameTimoutVo timeout = new GameTimoutVo();
		timeout.setRestTime(System.currentTimeMillis());
		timeout.setRestTimeType(NnTimeTaskEnum.PLAY_GAME_TIME.getCode());
		RedisUtil.hset(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo(), JSONObject.toJSONString(timeout));

		// 更新当前状态为空闲状态
		RedisUtil.hset(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo(), NnRoomMatchStatusEnum.PLAY_GAME_STATUS.getCode() + "");

		// 推送当前状态
		batchSendFarmerSetChip(reqMsg);

		// 设置发牌倒计时,设置金币倒计时
		{
			ServerManager.executorTask.schedule(new MyTimerTask(reqMsg, NnTimeTaskEnum.PLAY_GAME_TIME.getCode(),timeout.getRestTime()), NnConstans.PLAY_GAME_TIME, TimeUnit.SECONDS);
			Future<?> future=ServerManager.executorTask.scheduleAtFixedRate(new TimeTask(reqMsg, NnTimeTaskEnum.PLAY_GAME_TIME.getCode()), 1, 1, TimeUnit.SECONDS);
		    ServerManager.futures.put(reqMsg.getRoomNo(), future);
					
		}

	}

	/**
	 * 批量加入房间推送
	 * 
	 * @param reqMsg
	 */
	private static void batchSendFarmerSetChip(ReqMsg reqMsg) {
		String roomNo = reqMsg.getRoomNo();
		Set<String> userSet = getAllUserSet(reqMsg.getRoomNo());

		String curStatus = RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());

		HunNnBean.RoomInfo.Builder roomInfo = getRoomInfo(roomNo);

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();

		HunNnBean.RspData.Builder rspData = HunNnBean.RspData.newBuilder();

		rspData.setCurRoomStatus(Integer.parseInt(curStatus));
		rspData.setRoom(roomInfo);
		rspData.setRestTime(getRestTime(roomNo));
		rspMsg.setOperateType(NnPushMsgTypeEnum.START_MATCH_PUSH.getCode());
		rspMsg.setData(rspData);
		rspMsg.setCode(NnRspCodeEnum.$0000.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$0000.getMsg());

		String landlordUserId = RedisUtil.get(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());

		for (String key : userSet) {

			if (landlordUserId != key) {
				map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
			}

		}
		batchSendMsg(map);
	}

	/**
	 * 获取游戏超时时间
	 * 
	 * @param reqMsg
	 * @return
	 */
	private static GameTimoutVo getGameTimoutVo(String roomNo) {

		String str = RedisUtil.hget(NnConstans.NN_REST_TIME_PRE, roomNo);

		if (str != null) {
			return JSONObject.parseObject(str, GameTimoutVo.class);
		}
		return new GameTimoutVo();
	}
	
	/**
	 * 解散房间
	 * @param roomNo
	 */
	public static void colseRoom(String roomNo){
		ReqMsg reqMsg=ReqMsg.newBuilder().setRoomNo(roomNo).build();
		// 更新当前状态为空闲状态
		
		String curStat=RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, roomNo);

		if(curStat!=(NnRoomMatchStatusEnum.PLAY_GAME_STATUS.getCode()+"")){
			// 直接解散房间，推送用户退出房间，删除缓存
			batchSendClose(reqMsg);
			clearRedis(reqMsg);
		}

	}
	

	/**
	 * 推送解散房间
	 * 
	 * @param reqMsg
	 */
	private static void batchSendClose(HunNnBean.ReqMsg reqMsg) {
		Set<String> userSet = getAllUserSet(reqMsg.getRoomNo());

		Map<String, HunNnBean.RspMsg> map = new HashMap<String, HunNnBean.RspMsg>();

		HunNnBean.RspMsg.Builder rspMsg = HunNnBean.RspMsg.newBuilder();


		rspMsg.setOperateType(NnPushMsgTypeEnum.EXIT_ROOM_PUSH.getCode());
		rspMsg.setCode(NnRspCodeEnum.$1101.getCode());
		rspMsg.setMsg(NnRspCodeEnum.$1101.getMsg());

		for (String key : userSet) {
			map.put(RedisUtil.hget(NnConstans.NN_USER_CHANNEL_PRE, key), rspMsg.build());
		}
		batchSendMsg(map);

	}

	private static boolean clearRedis(HunNnBean.ReqMsg reqMsg) {
		Jedis redis = null;
		try {
			Set<String> set = getAllUserSet(reqMsg.getRoomNo());
			for (String key : set) {
				RedisUtil.hdel(NnConstans.NN_USER_CHANNEL_PRE, key);
			}
			long roomId = getRoomId(reqMsg.getRoomNo());
			redis = RedisUtil.getJedis();
			Transaction tx = redis.multi();
			// 清楚点击开始缓存

			tx.hdel(NnConstans.NN_ROOM_CUR_STATUS_PRE, reqMsg.getRoomNo());// 删除房间缓存
			tx.hdel(NnConstans.NN_REST_TIME_PRE, reqMsg.getRoomNo());// 删除房间缓存

			tx.del(NnConstans.NN_ROOM_PRE + reqMsg.getRoomNo());// 删除房间缓存
			tx.del(NnConstans.NN_ROOM_USER_INFO_PRE + reqMsg.getRoomNo());// 清除该房间下的用户
			tx.del(NnConstans.NN_ROOM_LANDLORD_USER_PRE + reqMsg.getRoomNo());// 删除庄家缓存
			tx.del(NnConstans.NN_BUG_USER_PRE + reqMsg.getRoomNo());
			tx.del(NnConstans.NN_ROOM_ALL_READY_USER_PRE + reqMsg.getRoomNo());// 所有准备用户
			tx.del(NnConstans.NN_ROOM_ALL_USER_PRE + reqMsg.getRoomNo());// 所有用户
			tx.del(NnConstans.NN_ROOM_ALL_MATCH_USER_PRE + reqMsg.getRoomNo());// 所有用户
			tx.del(NnConstans.NN_ROOM_USER_REDAY_TIME_PRE + reqMsg.getRoomNo());// 删除准备到倒计时
			tx.del(NnConstans.NN_ALL_POSITION_CACHE_PRE + reqMsg.getRoomNo());// 删除准备到倒计时
			tx.exec();
			
			NnManagerDao.instance().closeRoom(roomId);
			NnManagerDao.instance().clearRoomNo(reqMsg.getRoomNo());
			NnManagerDao.instance().deleteRoomUser(reqMsg.getRoomNo(), GameTypeEnum.HUNDRED_NIU.getType());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
			return false;
		} finally {
			redis.disconnect();
			redis.close();
		}

		return true;
	}

	/**
	 * 发送消息
	 * 
	 * @param map
	 */
	public static void sendMsg(HunNnBean.RspMsg rspMsg, String channelId) {
		System.out.println();
		try {
			for (Channel channel : ServerManager.channels) {
				if (channel.id().asLongText().equalsIgnoreCase(channelId)) {
					if (!channel.isActive() || !channel.isOpen()) {
						logger.info("该渠道已关闭,推送channeId:" + channel.id().asLongText() + ":操作类型=" + rspMsg.getOperateType() + ":推送信息长度=" + rspMsg.toByteArray().length + ":响应码=" + rspMsg.getCode());
						channel.close();
					} else {
						logger.info("推送channeId:" + channel.id().asLongText() + ":操作类型=" + rspMsg.getOperateType() + ":推送信息长度=" + rspMsg.toByteArray().length + ":响应码=" + rspMsg.getCode());
						channel.writeAndFlush(rspMsg).addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture channelFuture) throws Exception {
								if (!channelFuture.isSuccess()) {
									System.out.println(">>>>>>>>>>>>>>>>>>>>>>发送消息错误");
									channelFuture.cause().printStackTrace();
									channelFuture.channel().close();
								}
							}
						});
					}

					break;
				}
			}
		} catch (Exception ce) {
			logger.error(ce.getMessage(), ce);
			ce.printStackTrace();
		}

	}

	/**
	 * 多人发送消息
	 * 
	 * @param map
	 */
	public static void batchSendMsg(Map<String, HunNnBean.RspMsg> map) {
		System.out.println();
		try {
			for (String key : map.keySet()) {
				for (Channel channel : ServerManager.channels) {
					if (channel.id().asLongText().equalsIgnoreCase(key)) {

						if (!channel.isActive() || !channel.isOpen()) {
							logger.info("该渠道已关闭,推送channeId:" + channel.id().asLongText() + ":操作类型=" + map.get(key).getOperateType() + ":推送信息长度=" + map.get(key).toByteArray().length + ":响应码=" + map.get(key).getCode());
							channel.close();
						} else {
							// logger.info(map);
							logger.info("推送channeId:" + channel.id().asLongText() + ":操作类型=" + map.get(key).getOperateType() + ":推送信息长度=" + map.get(key).toByteArray().length + ":响应码=" + map.get(key).getCode());
							channel.writeAndFlush(map.get(key)).addListener(new ChannelFutureListener() {
								@Override
								public void operationComplete(ChannelFuture channelFuture) throws Exception {
									if (!channelFuture.isSuccess()) {
										System.out.println(">>>>>>>>>>>>>>>>>>>>>>发送消息错误");
										channelFuture.cause().printStackTrace();
										channelFuture.channel().close();
									}
								}
							});
						}

						break;
					}
				}
			}
		} catch (Exception ce) {
			logger.error(ce.getMessage(), ce);
			ce.printStackTrace();
		}

	}

}
