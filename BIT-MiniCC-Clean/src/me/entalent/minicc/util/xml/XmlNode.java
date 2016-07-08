package me.entalent.minicc.util.xml;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
/**
 * 表示一个XML标签及其中的文本，属性以及所有子标签
 * 封装了将XML标签转换为字符串，以及直接打印到文件的方法
 * @author Wentian Zhao
 *
 */
public final class XmlNode {
	//标签名称
	String tagName;
	//标签的所有属性
	HashMap<String, String> attributes;
	//文本内容
	String textContent;
	//子标签
	ArrayList<XmlNode> children;
	
	//输出时的缩进空格数
	private static int indentNum = 4;
	
	/**
	 * 根据标签名构造
	 * @param tagName
	 */
	public XmlNode(String tagName) {
		this.tagName = tagName;
	}
	
	public static void setIndentNum(int indent) {
		indentNum = indent;
	}
	
	private static XmlNode fromNode(Node node) {
		XmlNode xmlNode = new XmlNode(node.getNodeName());
		xmlNode.textContent = node.getTextContent();
		NamedNodeMap map = node.getAttributes();
		for(int i = 0; i < map.getLength(); i++){
			xmlNode.attribute(map.item(i).getNodeName(), map.item(i).getNodeValue());
		}
		NodeList nodeList = node.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++){
			xmlNode.addChild(XmlNode.fromNode(nodeList.item(i)));
		}
		return xmlNode;
	}
	
	/**
	 * 设置开标签和闭标签之间的文本
	 * @param textContent 要设置的文本
	 * @return 返回自身
	 * eg. new XmlNode("tag").textContent("text");
	 */
	public XmlNode textContent(String textContent) {
		this.textContent = textContent;
		return this;
	}
	
	public String getTagName() {
		return this.tagName;
	}
	
	public String getTextContent() {
		return this.textContent;
	}
	
	public String getAttribute(String key) {
		return this.attributes.get(key);
	}
	
	public HashMap<String, String> getAllAttributes() {
		return this.attributes;
	}
	
	/**
	 * 给开标签增加一个属性
	 * @param key 属性名
	 * @param value 属性值
	 * @return 返回自身
	 * eg. new XmlNode("tag").attribute("key", "value");
	 */
	public XmlNode attribute(String key, String value) {
		if(this.attributes == null) 
			this.attributes = new HashMap<>();
		this.attributes.put(key, value);
		return this;
	}
	
	/**
	 * 添加一个子标签
	 * @param node 要添加的标签
	 * @return 返回是否添加成功
	 */
	public synchronized boolean addChild(XmlNode node) {
		if(children == null) 
			this.children = new ArrayList<>();
		return this.children.add(node);
	}
	
	/**
	 * 子标签数
	 * @return 子标签数（不包含子标签的子标签）
	 */
	public synchronized int getChildCount() {
		if(children == null) 
			return 0;
		return this.children.size();
	}
	
	/**
	 * 根据索引获取子节点
	 * @param index 索引
	 * @return 返回该位置的子节点。若没有孩子或索引非法，返回null
	 */
	public synchronized XmlNode getChild(int index) {
		if(children == null || index < 0 || index > children.size() - 1)
			return null;
		return this.children.get(index);
	}
	
	/**
	 * 根据标签名获取子节点
	 * @param tagName 标签名
	 * @return 返回包括所有标签名符合要求的子节点的List。
	 */
	public synchronized List<XmlNode> getChild(String tagName) {
		ArrayList<XmlNode> list = new ArrayList<>();
		if(children != null) {
			for(XmlNode i : children) {
				if(i.tagName.equals(tagName))
					list.add(i);
			}
		}
		return list;
	}

	/**
	 * 私有方法 递归将标签内容写入Document
	 * @param document 要写入的Document对象
	 * @param parent 父节点
	 */
	private void genXml(Document document, Element parent) {
		Element node = document.createElement(this.tagName);
		node.setTextContent(this.textContent);
		if(attributes != null)
			for(Entry<String, String> e : attributes.entrySet()) {
				node.setAttribute(e.getKey(), e.getValue());
			}
		if(parent != null) {
			parent.appendChild(node);
		} else {
			document.appendChild(node);
		}
		if(this.getChildCount() > 0) {
			for(XmlNode child : children)
				child.genXml(document, node);
		}
	}
	
	/**
	 * 从XML文件读取，并生成对应的XmlNode对象
	 * @param file 要读取的文件，必须指向一个存在且可读的文件
	 * @return 生成的XmlNode对象
	 * @throws Exception
	 */
	public static synchronized XmlNode readFromFile(File file) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		return XmlNode.fromNode(doc.getDocumentElement());
	}
	
	/**
	 * 将自身以及所有子节点转换为字符串
	 * @return XML标签的字符串表示
	 * @throws Exception
	 */
	public synchronized String toXmlString() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		genXml(document, null);
		OutputFormat format = new OutputFormat(document);
		format.setIndenting(true);
		format.setIndent(indentNum);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
		XMLSerializer serializer = new XMLSerializer(bos, format);
		serializer.serialize(document);
		return bos.toString();
	}
	
	/**
	 * 将自身以及所有子标签的内容写入文件
	 * @param file 要写入的文件，这个参数必须指向一个存在且可写的文件
	 * @throws Exception
	 */
	public synchronized void writeXmlToFile(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		genXml(document, null);
		OutputFormat format = new OutputFormat(document);
		format.setIndenting(true);
		format.setIndent(indentNum);
		Writer output = new BufferedWriter(new FileWriter(file));
		XMLSerializer serializer = new XMLSerializer(output, format);
		serializer.serialize(document);
	}
}
