package com.lhyone.nn.vo;

public class GameTimoutVo {
	
	private long playGameTime;
	private long showMatchResultTime;
	private long idleTime;
	public long getPlayGameTime() {
		return playGameTime;
	}
	public void setPlayGameTime(long playGameTime) {
		this.playGameTime = playGameTime;
	}
	public long getShowMatchResultTime() {
		return showMatchResultTime;
	}
	public void setShowMatchResultTime(long showMatchResultTime) {
		this.showMatchResultTime = showMatchResultTime;
	}
	public long getIdleTime() {
		return idleTime;
	}
	public void setIdleTime(long idleTime) {
		this.idleTime = idleTime;
	}

}
