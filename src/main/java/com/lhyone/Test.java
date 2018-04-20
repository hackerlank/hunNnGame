package com.lhyone;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lhyone.nn.enums.RedisMQEnum;
import com.lhyone.nn.logic.handler.HunNnManager;
import com.lhyone.nn.logic.handler.RedisSubMQThread;
import com.lhyone.nn.util.NnConstans;
import com.lhyone.util.RedisUtil;

public class Test {
	public final static ScheduledExecutorService executorTask = Executors.newScheduledThreadPool(10);
	public final static Map<String, Future> futures = new HashMap<>();
	public final static CountDownLatch countDownLatch = new CountDownLatch(1);
	
	private static Logger logger = LogManager.getLogger(HunNnManager.class);
	public static void main(String[] args) throws InterruptedException {
//		String roomNo="100200";
//		roomNo=roomNo+1;
//		 Future future =executorTask.scheduleWithFixedDelay(new Job(roomNo), 0, 1, TimeUnit.MILLISECONDS);
//		 futures.put(roomNo, future);
//		 Thread.sleep(10000);
//		 roomNo=roomNo+1;
//		 Future future1 =executorTask.scheduleWithFixedDelay(new Job(roomNo), 0, 1, TimeUnit.MILLISECONDS);
//		 futures.put(roomNo, future1);
////		
//		String curStat=RedisUtil.hget(NnConstans.NN_ROOM_CUR_STATUS_PRE, "200245");
//		System.out.println(curStat);
		
//		logger.info("sssssssssssssssssssss");
		new RedisSubMQThread("test_1234_1234").start();
	}
}
