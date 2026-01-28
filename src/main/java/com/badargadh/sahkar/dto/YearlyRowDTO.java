package com.badargadh.sahkar.dto;

public class YearlyRowDTO {
	private String category;
	private Double jan = 0.0, feb = 0.0, mar = 0.0, apr = 0.0, may = 0.0, jun = 0.0;
	private Double jul = 0.0, aug = 0.0, sep = 0.0, oct = 0.0, nov = 0.0, dec = 0.0;
	private Double yearlyTotal = 0.0;
	private Boolean isSectionHeader = false; // Boolean with capital B for Jasper
	private Boolean isTotalRow = false;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Double getJan() {
		return jan;
	}

	public void setJan(Double jan) {
		this.jan = jan;
	}

	public Double getFeb() {
		return feb;
	}

	public void setFeb(Double feb) {
		this.feb = feb;
	}

	public Double getMar() {
		return mar;
	}

	public void setMar(Double mar) {
		this.mar = mar;
	}

	public Double getApr() {
		return apr;
	}

	public void setApr(Double apr) {
		this.apr = apr;
	}

	public Double getMay() {
		return may;
	}

	public void setMay(Double may) {
		this.may = may;
	}

	public Double getJun() {
		return jun;
	}

	public void setJun(Double jun) {
		this.jun = jun;
	}

	public Double getJul() {
		return jul;
	}

	public void setJul(Double jul) {
		this.jul = jul;
	}

	public Double getAug() {
		return aug;
	}

	public void setAug(Double aug) {
		this.aug = aug;
	}

	public Double getSep() {
		return sep;
	}

	public void setSep(Double sep) {
		this.sep = sep;
	}

	public Double getOct() {
		return oct;
	}

	public void setOct(Double oct) {
		this.oct = oct;
	}

	public Double getNov() {
		return nov;
	}

	public void setNov(Double nov) {
		this.nov = nov;
	}

	public Double getDec() {
		return dec;
	}

	public void setDec(Double dec) {
		this.dec = dec;
	}

	public Double getYearlyTotal() {
		return yearlyTotal;
	}

	public void setYearlyTotal(Double yearlyTotal) {
		this.yearlyTotal = yearlyTotal;
	}

	public Boolean getIsSectionHeader() {
		return isSectionHeader;
	}

	public void setIsSectionHeader(Boolean isSectionHeader) {
		this.isSectionHeader = isSectionHeader;
	}

	public Boolean getIsTotalRow() {
		return isTotalRow;
	}

	public void setIsTotalRow(Boolean isTotalRow) {
		this.isTotalRow = isTotalRow;
	}

}