package com.lhyone.nn.entity;
import java.io.Serializable;


/**
 * 
 * 
 * 
 **/
@SuppressWarnings("serial")
public class NnApplyLandlord implements Serializable {

	/**房间id**/
	private Long roomId;

	/**房间编号**/
	private String roomNo;

	/**房间id**/
	private Long userId;

	/**状态[1.同意 2.拒绝 3.已坐庄]**/
	private Integer status;

	/**创建时间**/
	private java.util.Date createDate;



	public void setRoomId(Long roomId){
		this.roomId = roomId;
	}

	public Long getRoomId(){
		return this.roomId;
	}

	public void setRoomNo(String roomNo){
		this.roomNo = roomNo;
	}

	public String getRoomNo(){
		return this.roomNo;
	}

	public void setUserId(Long userId){
		this.userId = userId;
	}

	public Long getUserId(){
		return this.userId;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setCreateDate(java.util.Date createDate){
		this.createDate = createDate;
	}

	public java.util.Date getCreateDate(){
		return this.createDate;
	}

}
