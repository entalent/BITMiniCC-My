package me.entalent.minicc.icgen;

import java.util.ArrayList;

public class NodeProperty {
	public ArrayList<String> values;
	public int bid, eid;
	
	public NodeProperty() {
		this.bid = 0;
		this.eid = 0;
		this.values= new ArrayList<String>();
	}
	
	@Override
	public String toString() {
		return "(" + bid + ", " + eid + ") " + (values == null ? "null" : values.toString());
	}
	
	
}
