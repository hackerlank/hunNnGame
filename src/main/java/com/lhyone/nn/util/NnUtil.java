package com.lhyone.nn.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;
import com.lhyone.nn.dao.NnManagerDao;
import com.lhyone.nn.entity.NnRoomMultipleDic;
import com.lhyone.nn.enums.NnCardRuleEnum;
import com.lhyone.nn.pb.HunNnBean;
import com.lhyone.util.RedisUtil;

public class NnUtil {
	
	/**庄家加注积分*/
	private static List<NnRoomMultipleDic>  getListNnRoomMultipleDic(){
		String str=RedisUtil.get(NnConstans.NN_ROOM_MULTIPLE_DIC_PRE);
		if(StringUtils.isEmpty(str)){
			List<NnRoomMultipleDic> list=NnManagerDao.instance().queryNnRoomMultipleDic();
			RedisUtil.set(NnConstans.NN_ROOM_MULTIPLE_DIC_PRE,JSONObject.toJSONString(list));
			
			return list;
		}
		
		return 	JSONObject.parseArray(str, NnRoomMultipleDic.class);
	}
	
	
	/**
	 * 获取特殊牌型倍数
	 * @param type
	 * @return
	 */
	public static int getCardTypeSpecialDouble(int type){
		
		for(NnRoomMultipleDic bean:getListNnRoomMultipleDic()){
			if(bean.getNum()==type){
				
				return bean.getSpecialMultiple();
			}
			
		}
		return 1;
	}
	
	/**
	 * 获取特殊牌型倍数
	 * @param type
	 * @return
	 */
	public static int getCardTypeDouble(int type){
		
		for(NnRoomMultipleDic bean:getListNnRoomMultipleDic()){
			if(bean.getNum()==type){
				
				return bean.getMultiple();
			}
			
		}
		return 1;
	}
	 
	
	/**
	 * 检查是否存在庄家
	 * @return
	 */
	public static boolean checkIsHasLandlord(String roomNo){
		if(!RedisUtil.hexists(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo,"p1")){
			return false; 
		}
		return true;
	}
	
	/**
	 * 获取位置信息
	 * @param roomNo
	 * @return
	 */
	public static List<HunNnBean.PositionInfo> getPositionInfo(String roomNo){
		
		if(!RedisUtil.hexists(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo,"p1")){
			return null;
		}
		List<HunNnBean.PositionInfo> list=new ArrayList<HunNnBean.PositionInfo>();
		
		
		HunNnBean.PositionInfo.Builder position=HunNnBean.PositionInfo.newBuilder();
		
		String str1=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p1");
		
		if(StringUtils.isNoneEmpty(str1)){
			
			try{
				JsonFormat.merge(str1, position);
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			list.add(position.build());
		}

		position=HunNnBean.PositionInfo.newBuilder();
		
		String str2=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p2");
		
		if(StringUtils.isNoneEmpty(str2)){
			
			try{
				JsonFormat.merge(str2, position);
			}catch (Exception e) {
				e.printStackTrace();
			}
			list.add(position.build());
		}
		position=HunNnBean.PositionInfo.newBuilder();
		String str3=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p3");
		
		if(StringUtils.isNoneEmpty(str3)){
			
			try{
				JsonFormat.merge(str3, position);
				
			}catch (Exception e) {
				e.printStackTrace();
			}
			list.add(position.build());
		}
		
		position=HunNnBean.PositionInfo.newBuilder();	
		String str4=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p4");
		
		if(StringUtils.isNoneEmpty(str4)){
			
			try{
				JsonFormat.merge(str4, position);
			}catch (Exception e) {
				e.printStackTrace();
			}
			list.add(position.build());
		}
		
		position=HunNnBean.PositionInfo.newBuilder();	
		String str5=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p5");
		
		if(StringUtils.isNoneEmpty(str5)){
			
			try{
				JsonFormat.merge(str5, position);
			}catch (Exception e) {
				e.printStackTrace();
			}
			list.add(position.build());
		}
		
		return list;
	}
	
	/**
	 * 获取位置信息map形式
	 * @param roomNo
	 * @return
	 */
	public static Map<String,HunNnBean.PositionInfo> getPositionInfoToMap(String roomNo){
		List<HunNnBean.PositionInfo>  list=getPositionInfo(roomNo);
		
		Map<String,HunNnBean.PositionInfo> map=new HashMap<String,HunNnBean.PositionInfo>();
		for(HunNnBean.PositionInfo bean:list){
			map.put(bean.getPosition()+"", bean);
		}
		
		return map;
	}
	
	/**
	 * 设置庄家缓存
	 * @param roomNo
	 * @param userId
	 */
	public static void setLandlord(String roomNo,long userId){
		HunNnBean.PositionInfo.Builder position=HunNnBean.PositionInfo.newBuilder();
		String str1=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p1");
		try{
			JsonFormat.merge(str1, position);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		position.setPosition(1);
		List<Long> listUid=new ArrayList<Long>(position.getUidsList());
		
		if(!listUid.contains(userId)){
			listUid.add(userId);
		}
		position.addAllUids(listUid);
		
		RedisUtil.hset(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo,"p1",JsonFormat.printToString(position.build()));
	}

	/**
	 * 获取位置信息
	 * @param roomNo
	 * @param p
	 * @return
	 */
	public static HunNnBean.PositionInfo.Builder getPostion(String roomNo,int p){
		
		HunNnBean.PositionInfo.Builder position=HunNnBean.PositionInfo.newBuilder();
		String str1=RedisUtil.hget(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p"+p);
		try{
			JsonFormat.merge(str1, position);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return position;
		
	}
	/**
	 * 保存位置信息
	 * @param position
	 * @param roomNo
	 * @param p
	 */
	public static void setPosition(HunNnBean.PositionInfo.Builder position,String roomNo,int p){
		
		RedisUtil.hset(NnConstans.NN_ALL_POSITION_CACHE_PRE+roomNo, "p"+p, JsonFormat.printToString(position.build()));
		
	}
	
	/**
	 * 设置房间发送金币定时计数
	 * @param roomNo
	 */
	public static void setNnRoomSendGoldTimer(String roomNo){
		String str=RedisUtil.hget(NnConstans.NN_ROOM_PRE+roomNo, "sendGoldTimer");
		int sendGoldTimer=0;
		if(StringUtils.isEmpty(str)){
			sendGoldTimer=1;
		}else{
			sendGoldTimer+=Integer.parseInt(str);
		}
		RedisUtil.hset(NnConstans.NN_ROOM_PRE+roomNo, "sendGoldTimer", sendGoldTimer+"");
		
	}
	
	/**
	 * 获取房间金币定时计数
	 * @param roomNo
	 * @return
	 */
	public static int getNnRoomSendGoldTimer(String roomNo){
		String str=RedisUtil.hget(NnConstans.NN_ROOM_PRE+roomNo, "sendGoldTimer");
		int sendGoldTimer=0;
		if(!StringUtils.isEmpty(str)){
			sendGoldTimer=Integer.parseInt(str);
		}
		return sendGoldTimer;
	}
	/**
	 * 删除房间金币定时计数
	 * @param roomNo
	 */
	public static void delNnRoomSendGoldTimer(String roomNo){
		RedisUtil.hdel(NnConstans.NN_ROOM_PRE+roomNo, "sendGoldTimer");
	}
	
	
	public static void main(String[] args) {
//		 Map<String,HunNnBean.PositionInfo> map=NnUtil.getPositionInfoToMap("200106");
//		 System.out.println(map);
		 
		 int type=NnUtil.getCardTypeDouble(NnCardRuleEnum.NIU_NIU.getCode());
	}
	
	
}
