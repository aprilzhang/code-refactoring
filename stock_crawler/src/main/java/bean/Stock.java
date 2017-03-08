package bean;

import java.math.BigDecimal;
import java.util.Map;

import com.alibaba.fastjson.JSON;

public class Stock {
	private String name;
	private String symbol;
	private BigDecimal price;
	private BigDecimal prev_weight;
	private BigDecimal target_weight;
	private String updateTime;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getPrev_weight() {
		return prev_weight;
	}

	public void setPrev_weight(BigDecimal prev_weight) {
		this.prev_weight = prev_weight;
	}

	public BigDecimal getTarget_weight() {
		return target_weight;
	}

	public void setTarget_weight(BigDecimal target_weight) {
		this.target_weight = target_weight;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String toString() {
		return "��Ʊ [����=" + name + ", ����=" + symbol + ", ����۸�="
				+ price + ", ԭ�ֲ�=" + prev_weight + ", Ŀǰ�ֲ�="
				+ target_weight + ", ��������=" + updateTime + "]";
	}
}
