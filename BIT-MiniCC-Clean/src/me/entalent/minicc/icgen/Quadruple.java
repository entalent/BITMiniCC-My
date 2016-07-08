package me.entalent.minicc.icgen;

import me.entalent.minicc.util.xml.XmlNode;

public class Quadruple {
	//四元式标号
	public int index;
	//运算符，操作数1，操作数2，运算结果
	public String op, arg1, arg2, res;
	
	public Quadruple(int index, String op, String arg1, String arg2, String res) {
		super();
		this.index = index;
		this.op = op;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.res = res;
	}
	
	@Override
	public String toString() {
		String str = index + " (" + op + ", ";
		if(arg1 == null || arg1.isEmpty()) {
			str += "_";
		} else {
			str += arg1;
		}
		str += ", ";
		if(arg2 == null || arg2.isEmpty()) {
			str += "_";
		} else {
			str += arg2;
		}
		str += ", ";
		if(res == null || res.isEmpty()) {
			str += "_";
		} else {
			str += res;
		}
		str += ")";
		return str;
	}
	
	public XmlNode toXmlNode() {
		XmlNode node = new XmlNode("quaternion")
				.attribute("op", op)
				.attribute("arg1", (arg1 == null || arg1.isEmpty()) ? "_" : arg1)
				.attribute("arg2", (arg2 == null || arg2.isEmpty()) ? "_" : arg2)
				.attribute("res", (res == null || res.isEmpty()) ? "_" : res)
				.attribute("addr", this.index + "");
		return node;
	}
	
	
}
