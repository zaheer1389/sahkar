package com.badargadh.sahkar.dto;

public class EmiCounterDTO {

	private int feesCount;
	private int emi100;
	private int emi200;
	private int emi300;
	private int emi400;

	public int getEmi100() {
		return emi100;
	}

	public void setEmi100(int emi100) {
		this.emi100 = emi100;
	}

	public int getEmi200() {
		return emi200;
	}

	public void setEmi200(int emi200) {
		this.emi200 = emi200;
	}

	public int getEmi300() {
		return emi300;
	}

	public void setEmi300(int emi300) {
		this.emi300 = emi300;
	}

	public int getEmi400() {
		return emi400;
	}

	public void setEmi400(int emi400) {
		this.emi400 = emi400;
	}

	public int getFeesCount() {
		return feesCount;
	}

	public void setFeesCount(int feesCount) {
		this.feesCount = feesCount;
	}

	public double getTotal() {
		double emi100Total = emi100 * 100;
		double emi200Total = emi200 * 400;
		double emi300Total = emi300 * 300;
		double emi400Total = emi400 * 400;
		return emi100Total+emi200Total+emi300Total+emi400Total;
	}

}
