package me.entalent.minicc.icgen;

import java.io.File;
import java.util.ArrayList;

import bit.minisys.minicc.icgen.IMiniCCICGen;
import me.entalent.minicc.parser.Parser;
import me.entalent.minicc.parser.SyntaxTree;
import me.entalent.minicc.util.Util;
import me.entalent.minicc.util.xml.XmlNode;

public class InternalCodeGen implements IMiniCCICGen {
	ArrayList<Quadruple> qList = new ArrayList<>();
	int tempIndex = 1;
	
	@Override
	public void run(String iFile, String oFile) throws Exception {
		System.out.println("icgen");
		gen(Parser.lastSyntaxTree);
		XmlNode functions = new XmlNode("functions");
		XmlNode function = new XmlNode("function");
		functions.addChild(function);
		for(Quadruple q : qList) {
			function.addChild(q.toXmlNode());
		}
		File out = new File(oFile);
		if(!out.exists()) {
			out.getParentFile().mkdirs();
			out.createNewFile();
		}
		functions.writeXmlToFile(out);
		printQuadruple();
	}
	
	public void gen(SyntaxTree tree) {
		if(tree.content.toString().equalsIgnoreCase("STMT")) {
			genStmt(tree);
		}
		if(tree.children != null) {
			for(int i = tree.children.size() - 1; i >= 0; i--){
				gen(tree.children.get(i));
			}
		} else {
			 
		}
	}
	
	public void genStmt(SyntaxTree tree) {
		if(tree.children != null) { //非叶子节点
			tree.prop = process(tree);
		} else { //叶子节点
			ArrayList<String> result = new ArrayList<>();
			if(tree.prop != null) {
				for(String i : tree.prop.values) {
					result.add(i);
				}
			}
			NodeProperty prop = new NodeProperty();
			prop.values = result;
			tree.prop = prop;
		}
	}
	
	private NodeProperty getAllChildNodeValue(SyntaxTree tree) {
		NodeProperty prop = new NodeProperty();
		prop.bid = Integer.MAX_VALUE;
		prop.eid = Integer.MIN_VALUE;
		ArrayList<String> result = new ArrayList<>();
		if(tree.children != null) {
			for(int i = tree.children.size() - 1; i >= 0; i--){
				genStmt(tree.children.get(i));
				ArrayList<String> list = tree.children.get(i).prop.values;
				if(list != null)
					result.addAll(list);
				int bid = tree.children.get(i).prop.bid,
						eid = tree.children.get(i).prop.eid;
				if(bid > 0 && eid > 0) {
					prop.bid = Math.min(prop.bid, bid);
					prop.eid = Math.max(prop.eid, eid);
				}
			}
		}
		prop.values = result;
		if(prop.bid == Integer.MAX_VALUE && prop.eid == Integer.MIN_VALUE) {
			prop.bid = 0; prop.eid = 0;
		}
		//System.out.println("getAllChild " + tree.content + " " + prop.bid + " " + prop.eid);
		return prop;
	}

	private NodeProperty process(SyntaxTree tree) {
		
		if(tree.processed) {
			return tree.prop;
		}
		tree.processed = true;
		
		NodeProperty resultProperty = new NodeProperty();
		ArrayList<String> childProp;
		switch(tree.content.toString()) {
		case "ETERM":
			resultProperty = getAllChildNodeValue(tree);
			childProp = resultProperty.values;
			if(childProp.size() == 3 && childProp.get(0).equals("(") && childProp.get(2).equals(")")) {
				ArrayList<String> result1 = new ArrayList<>();
				result1.add(childProp.get(1));
				resultProperty.values = result1;
				resultProperty.bid = tree.getChild("EXPR").get(0).prop.bid;
				break;
			} else {
				break;
			}
		case "ETLIST1_C":
		case "ETLIST2_C":
		case "ETLIST3_C":
		case "ETLIST4_C":
			resultProperty = getAllChildNodeValue(tree);
			break;
		case "ETLIST1":
		case "ETLIST2":
		case "ETLIST3":
		case "ETLIST4":
			resultProperty = getAllChildNodeValue(tree);
			//System.out.println(resultProperty.bid + " " + resultProperty.eid);
			
			childProp = resultProperty.values;
			if(childProp.size() >= 3) {
				int bid = Integer.MAX_VALUE, eid = Integer.MIN_VALUE;
				if(resultProperty.bid != 0 && resultProperty.eid != 0) {
					bid = resultProperty.bid;
					eid = resultProperty.eid;
				}
				String arg1, arg2, op, ret = childProp.get(0);
				for(int i = 0; (i + 1) < childProp.size(); i += 2) {
					arg1 = ret;
					op = childProp.get(i + 1);
					arg2 = childProp.get(i + 2);
					ret = newTemp();
					int index = newQuadruple(op, arg1, arg2, ret);
					bid = Math.min(bid, index);
					eid = Math.max(eid, index);
				}
				
				ArrayList<String> result1 = new ArrayList<>();
				result1.add(ret);
				resultProperty.values = result1;
				resultProperty.bid = bid;
				resultProperty.eid = eid;
				break;
			} else {
				break;
			}
		case "IF_STMT":
			SyntaxTree expr = tree.getChild("EXPR").get(0);
			genStmt(expr);
			int index = newQuadruple("jf", expr.prop.values.get(0), "", "");
			SyntaxTree block = tree.getChild("CODE_BLOCK").get(0);
			genStmt(block);
			qList.get(index - 1).res = Integer.toString(block.prop.eid + 1);
 			break;
		case "WHILE_STMT":
			SyntaxTree expr1 = tree.getChild("EXPR").get(0);
			genStmt(expr1);
			int indexWhileBegin = expr1.prop.bid;
			int indexConditionJudge = newQuadruple("jf", expr1.prop.values.get(0), "", "");
			SyntaxTree block1 = tree.getChild("WHILE_BODY").get(0);
			genStmt(block1);
			int indexLoopEnd = newQuadruple("j", "", "", Integer.toString(indexWhileBegin));
			qList.get(indexConditionJudge - 1).res = Integer.toString(indexLoopEnd + 1);
		case "FUNC_BODY":
		case "CODE_BLOCK":
			resultProperty = getAllChildNodeValue(tree);
			break;
		default:
			resultProperty = getAllChildNodeValue(tree);
			break;
		}
		//System.out.println(tree.content + " " + resultProperty);
		return resultProperty;
	}
	
	private String newTemp() {
		return "T" + (tempIndex++);
	}
	
	private int newQuadruple(String op, String arg1, String arg2, String res) {
		int index = qList.size() + 1;
		Quadruple q = new Quadruple(index, op, arg1, arg2, res);
		qList.add(q);
		return index;
	}
	
	public void printQuadruple() {
		for(Quadruple q : qList) {
			System.out.println(q.toString());
		}
	}
}
