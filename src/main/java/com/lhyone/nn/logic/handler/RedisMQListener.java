package com.lhyone.nn.logic.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lhyone.nn.enums.RedisMQEnum;
import com.lhyone.util.RedisUtil;

import redis.clients.jedis.JedisPubSub;

public class RedisMQListener extends JedisPubSub {
	 private static Logger logger = LogManager.getLogger(RedisMQListener.class);
	@Override
	public void unsubscribe() {
		super.unsubscribe();
	}

	@Override
	public void unsubscribe(String... channels) {
		super.unsubscribe(channels);
	}

	@Override
	public void subscribe(String... channels) {
		super.subscribe(channels);
	}

	@Override
	public void psubscribe(String... patterns) {
		super.psubscribe(patterns);
	}

	@Override
	public void punsubscribe() {
		super.punsubscribe();
	}

	@Override
	public void punsubscribe(String... patterns) {
		super.punsubscribe(patterns);
	}

	@Override
	public void onMessage(String channel, String message) {
		System.out.println("channel:" + channel + "receives message :"
				+ message);
		try{
			//如果是剔除房间用户
			if(RedisMQEnum.CLOSE_ROOM_CHANNEL.getCode().equals(channel)){
				HunNnManager.colseRoom(message);
			}
			if("test_1234_1234".equals(channel)){
				HunNnManager.colseRoom(message);
			}
		}catch(Exception e){
			logger.error("redisMq 监听器错误...",e.getMessage(),e);
			e.printStackTrace();
		}
		
		
	
//		this.unsubscribe();
	}

	@Override
	public void onPMessage(String pattern, String channel, String message) {
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		System.out.println("channel:" + channel + "is been subscribed:"
				+ subscribedChannels);
	}

	@Override
	public void onPUnsubscribe(String pattern, int subscribedChannels) {
	}

	@Override
	public void onPSubscribe(String pattern, int subscribedChannels) {
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		System.out.println("channel:" + channel + "is been unsubscribed:"
				+ subscribedChannels);
	}
}
