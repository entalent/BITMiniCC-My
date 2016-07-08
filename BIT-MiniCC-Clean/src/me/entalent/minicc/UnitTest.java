package me.entalent.minicc;

import java.io.File;

import org.junit.Test;

import me.entalent.minicc.util.xml.XmlNode;

/**
 * 单元测试用的类，与编译框架无关
 * @author ental
 *
 */
public class UnitTest {
	@Test 
	public void test() throws Exception {
		XmlNode node = new XmlNode("root");
		XmlNode son = new XmlNode("son");
		node.addChild(son);
		son.addChild(new XmlNode("grandson"));
		System.out.println(node.toXmlString());
	}
}
