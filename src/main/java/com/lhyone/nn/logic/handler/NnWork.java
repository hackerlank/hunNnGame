package com.lhyone.nn.logic.handler;

import com.lhyone.nn.pb.HunNnBean;

public class NnWork implements Runnable {
	
	
	private HunNnBean.ReqMsg reqMsg;
	private int type;
	public NnWork(HunNnBean.ReqMsg reqMsg,int type){
		this.reqMsg=reqMsg;
		this.type=type;
	}
	
	public void run() {
			nnTask();
	}
	
	private void nnTask(){
		switch (type) {
		case 1:
			HunNnManager.showMatchResult(reqMsg.getRoomNo());
			break;
		default:
			break;
		}
		
	}

}
