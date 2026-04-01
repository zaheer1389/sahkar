package com.badargadh.sahkar.dto;

public class YearlyRowDTO {
	private String category;
	private String jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec;
	private String yearlyTotal;
	private Boolean isSectionHeader = false;
	private Boolean isTotalRow = false;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getJan() {
		return jan;
	}

	public void setJan(String jan) {
		this.jan = jan;
	}

	public String getFeb() {
		return feb;
	}

	public void setFeb(String feb) {
		this.feb = feb;
	}

	public String getMar() {
		return mar;
	}

	public void setMar(String mar) {
		this.mar = mar;
	}

	public String getApr() {
		return apr;
	}

	public void setApr(String apr) {
		this.apr = apr;
	}

	public String getMay() {
		return may;
	}

	public void setMay(String may) {
		this.may = may;
	}

	public String getJun() {
		return jun;
	}

	public void setJun(String jun) {
		this.jun = jun;
	}

	public String getJul() {
		return jul;
	}

	public void setJul(String jul) {
		this.jul = jul;
	}

	public String getAug() {
		return aug;
	}

	public void setAug(String aug) {
		this.aug = aug;
	}

	public String getSep() {
		return sep;
	}

	public void setSep(String sep) {
		this.sep = sep;
	}

	public String getOct() {
		return oct;
	}

	public void setOct(String oct) {
		this.oct = oct;
	}

	public String getNov() {
		return nov;
	}

	public void setNov(String nov) {
		this.nov = nov;
	}

	public String getDec() {
		return dec;
	}

	public void setDec(String dec) {
		this.dec = dec;
	}

	public String getYearlyTotal() {
		return yearlyTotal;
	}

	public void setYearlyTotal(String yearlyTotal) {
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