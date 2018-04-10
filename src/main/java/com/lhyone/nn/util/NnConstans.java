package com.lhyone.nn.util;

public class NnConstans {
	
	/**庄家准备倒计时*/
	public final static int USER_REDAY_TIME=5;
	/**打牌时间*/
	public final static int PLAY_GAME_TIME=10;
	/**展示比赛结果时间*/
	public final static int SHOW_MATCH_RESULT_TIME=10;
	/**空闲时间*/
	public final static int GAME_IDLE_TIME=3;
	/**牛牛渠道*/
	public final static String NN_CHANNEL_PRE="hun_nn_channel:";
	/**牛牛用户*/
	public final static String NN_USER_CHANNEL_PRE="hun_nn_user_channel:";
	/**房间信息*/
	public final static String NN_ROOM_PRE="hun_nn_room:";
	/**房间用户缓存*/
	public final static String NN_ROOM_USER_INFO_PRE="hun_nn_room_user_info:";
	/**用户抢庄缓存*/
	public final static String NN_ROOM_LANDLORD_USER_PRE="hun_nn_room_landlord_user:";
	/**当前比赛状态*/
	public final static String NN_ROOM_CUR_STATUS_PRE="hun_nn_room_status:";
	
	/**牛牛房间用户每个人准备的倒计时*/
	public final static String NN_ROOM_USER_REDAY_TIME_PRE="hun_nn_room_user_reday:";//
	
	/**用户redis的key前缀*/
    public static final String REDIS_USER_PRE="user:";
    
    /**房间规则字典*/
    public static final String NN_ROOM_MULTIPLE_DIC_PRE="hun_nn_room_multiple_dic";
    
    /**牛牛房间bug用户*/
    public static final String NN_BUG_USER_PRE="hun_nn_room_bug_user:";
    
    /**牛牛倒计时*/
    public static final String NN_REST_TIME_PRE="hun_nn_rest_time:";
    
    /**牛牛所有用户*/
    public static final String NN_ROOM_ALL_USER_PRE="hun_nn_room_all_user:";
    
    /**牛牛所有准备用户*/
    public static final String NN_ROOM_ALL_READY_USER_PRE="hun_nn_room_all_ready_user:";
    
    /**牛牛所有比赛用户*/
    public static final String NN_ROOM_ALL_MATCH_USER_PRE="hun_nn_room_all_match_user:";
	
	/**牛牛线程锁，防止高并发用*/
	public static final String NN_ROOM_LOCK_REQ="hun_nn_room_lock";
	
	
	/**牛牛防止并发缓存*/
	public static final String NN_USER_THREAD_LOCK_CACHE_PRE="hun_nn_user_thread_cache";
	
	
	/**百人牛牛位置信息
	 * [默认有5个位置，位置1是庄家位，其他都是闲家位置]
	 * */
	public static final String NN_ALL_POSITION_CACHE_PRE="hun_nn_all_position_cache:";
	
	/**百人牛牛庄家申请队列*/
	public static final String NN_ROOM_LANDLORD_APPLY_QUEUE_CACHE="hun_nn_room_landlord_apply_queue:";
	
	/**牛牛庄家最低金币*/
	public static final long NN_LANDLORD_MIN_GOLD=12*10000;
	
	/**连庄次数*/
	public static final int NN_LANDLORD_TIMES=10;
	
	
}
