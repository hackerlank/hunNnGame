package com.lhyone.nn.logic.handler;

import redis.clients.jedis.Jedis;

import com.lhyone.util.RedisUtil;

public class RedisSubMQThread extends Thread{
	private final RedisMQListener redisMQListener = new RedisMQListener();

	private String[] channels;

	public RedisSubMQThread(String... channels ) {
		this.channels=channels;
	}

	@Override
	public void run() {
		Jedis jedis = null;
		try {
			jedis=RedisUtil.getJedis();
			jedis.subscribe(redisMQListener, channels);
		} catch (Exception e) {
			System.out.println(String.format("subsrcibe channel error, %s", e));
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

}
