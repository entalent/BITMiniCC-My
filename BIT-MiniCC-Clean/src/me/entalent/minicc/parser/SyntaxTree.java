package me.entalent.minicc.parser;

import java.util.ArrayList;

import me.entalent.minicc.icgen.NodeProperty;
import me.entalent.minicc.scanner.Scanner.Token;

/**
 * 语法树
 * @author ental
 *
 */
public class SyntaxTree {
	//父节点，根节点的父节点为null
	public SyntaxTree parent;
	
	//节点上的内容
	public Object content;
	
	public NodeProperty prop;
	public boolean processed;
	
	//所有子树
	public ArrayList<SyntaxTree> children;
	
	public SyntaxTree(Object rootContent) {
		this.content = rootContent;
		this.parent = null;
		this.prop = new NodeProperty();
	}
	
	//添加子树
	public boolean addChild(SyntaxTree syntaxTree) {
		if(this.children == null)
			this.children = new ArrayList<>();
		syntaxTree.parent = this;
		return this.children.add(syntaxTree);
	}
	
	public boolean addChild(String childContent) {
		return addChild(new SyntaxTree(childContent));
	}
	
	public ArrayList<SyntaxTree> getChild(String key) {
		ArrayList<SyntaxTree> childs = new ArrayList<>();
		for(int i = this.children.size() - 1; i >= 0; i--) {
			if(children.get(i).content.equals(key)) {
				childs.add(this.children.get(i));
			}
		}
		return childs;
	}

	@Override
	public String toString() {
		if(this.children == null) {
			return (String) this.content + "\n";
		}
		String str = "<" + content.toString() + ">" + "\n";
		if(this.children != null) {
			for(int i = this.children.size() - 1; i >= 0; i--) {
				str += this.children.get(i).toString();
			}
		}
		str += "</" + content.toString() + ">\n";
		return str;
	}
	
	public static String getLeafNodeContent(String tagName, String value) {
		return "<" + tagName + ">" + value + "</" + tagName + ">";
	}
	
}
