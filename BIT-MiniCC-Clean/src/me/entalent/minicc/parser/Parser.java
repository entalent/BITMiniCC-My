package me.entalent.minicc.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import me.entalent.minicc.parser.Syntax.Rule;
import me.entalent.minicc.parser.Syntax.SymbolType;
import me.entalent.minicc.scanner.Scanner.Token;
import me.entalent.minicc.util.Util;
import bit.minisys.minicc.parser.IMiniCCParser;

public class Parser implements IMiniCCParser {
	
	private static final boolean DEBUG = false;
	
	/**
	 * 表示LL(1)分析表的一个入口
	 * @author ental
	 *
	 */
	public static class TableEntry {
		//非终结符，终结符
		public String A, a;

		public TableEntry(String _a, String _a2) {
			A = _a;
			a = _a2;
		}
		
		public String toString() {
			return "[" + A + ", " + a + "]";
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof TableEntry)) {
				return super.equals(obj);
			} else if(this == obj){
				return true;
			} else {
				TableEntry entry = (TableEntry) obj;
				return A.equals(entry.A) && a.equals(entry.a);
			}
		}

		//若在HashMap中使用必须重载hashCode方法
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
	}
	
	//文法
	Syntax syntax = new Syntax();
	
	//所有FIRST与FOLLOW集
	Map<String, HashSet<String>> allFirst = new HashMap<>(),
			allFollow = new HashMap<>();
	
	//LL(1)分析表
	Map<TableEntry, Rule> table;
			
	/**
	 * 求一个非终结符号的FIRST集
	 * @param nonterm
	 */
	Map<String, Boolean> vis = new HashMap<>();
	
	void getFirstCollectionOfNonTerminator(String nonterm) {
		Boolean flag = vis.get(nonterm);
		if(flag != null && flag) {
			return;
		}
		vis.put(nonterm, true);
		
		HashSet<String> first = allFirst.get(nonterm);
		if(first == null) {
			first = new HashSet<>();
		}
		Rule thisRule = syntax.getSyntaxRule(nonterm);
		for(String rightItem : thisRule.right){
			String[] symArr = Rule.getSymbolsAsArray(rightItem);
			String firstSym = symArr[0];
			boolean containsEmpty = true;
			for(String sym : symArr) {
				//遇到终结符
				if(syntax.getSymbolType(sym).equals(SymbolType.TERMINATOR)) {
					first.add(sym);
					containsEmpty = false;
					break;
				} else {
					if(!nonterm.equals(sym)) {
						getFirstCollectionOfNonTerminator(sym);
						boolean symContainsEmpty = false;
						for(String i : allFirst.get(sym)) {
							first.add(i);
							if(i.equals(Syntax.EMPTY_STRING))
								symContainsEmpty = true;
						}
						//非终结符的FIRST不含空串
						if(!symContainsEmpty) {
							containsEmpty = false;
							break;
						}
					}
				}
			}
			if(containsEmpty) {
				first.add(Syntax.EMPTY_STRING);
			}
		}
		
		allFirst.put(nonterm, first);
	}

	/**
	 * 求一个串的FIRST集
	 * @param str
	 */
	HashSet<String> getFirstCollectionOfString(String str) {
		if(str.length() == 0) {
			HashSet<String> first = new HashSet<>();
			first.add(Syntax.EMPTY_STRING);
			return first;
		}
		String[] syms = Syntax.Rule.getSymbolsAsArray(str);
		HashSet<String> first = new HashSet<>();
		for(int i = 0; i < syms.length; i++) {
			if(syntax.getSymbolType(syms[i]).equals(SymbolType.TERMINATOR)) {
				first.add(syms[i]);
				return first;
			} else {
				HashSet<String> firstNonTerm = allFirst.get(syms[i]);
				if(firstNonTerm.contains(Syntax.EMPTY_STRING)) {
					first.addAll(firstNonTerm);
					first.remove(Syntax.EMPTY_STRING);
				} else {
					first.addAll(firstNonTerm);
					return first;
				}
			}
		}
		first.add(Syntax.EMPTY_STRING);
		return first;
	}
	
	Map<String, HashSet<String>> graph;
	
	
	/**
	 * 求所有FIRST集
	 */
	void getAllFirstCollection() {
		for(String nonterm : syntax.vn) {
			getFirstCollectionOfNonTerminator(nonterm);
		}
		if(DEBUG) {
			for(String nonterm : syntax.vn) {
				HashSet<String> first = allFirst.get(nonterm);
				String str = "FIRST(" + nonterm + "): ";
				for(String sym : first) {
					str += sym + " ";
				}
				System.out.println(str);
			}
		}
	}
	
	/**
	 * 求所有FOLLOW集
	 */
	void getAllFollowCollection() {
		for(String nonterm : syntax.vn){
			allFollow.put(nonterm, new HashSet<String>());
		}
		allFollow.get(syntax.beginSym).add(syntax.END_CHAR);
		while(true) {
			int ret = getAllFollowCollectionExec();
			if(ret == 0) break;
		}
		if(DEBUG) {
			for(String nonterm : syntax.vn) {
				String str = "FOLLOW(" + nonterm + "):";
				for(String i : allFollow.get(nonterm)) {
					str += " " + i;
				}
				System.out.println(str);
			}
		}
	}
	
	int getAllFollowCollectionExec() {
		int ret = 0;
		
		Set<Entry<String, Rule>> entrySet = syntax.rules.entrySet();
		for(Entry<String, Rule> entry : entrySet) {
			Rule rule = entry.getValue();
			String B = rule.left;
			for(String right : rule.right) {
				String[] syms = Rule.getSymbolsAsArray(right);
				for(int i = 0; i < syms.length; i++) {
					String A = syms[i];
					if(syntax.getSymbolType(A).equals(SymbolType.TERMINATOR)) continue;
					String b = "";
					for(int j = i + 1; j < syms.length; j++) b += ("." + syms[j]);
					
					Set<String> firstB = getFirstCollectionOfString(b);
					for(String s : firstB) {
						if(s.equals(Syntax.EMPTY_STRING)) {
							//FOLLOW(B)属于FOLLOW(A)
							Set<String> followB = allFollow.get(B);
							Set<String> followA = allFollow.get(A);
							for(String str : followB) {
								if(!followA.contains(str)){
									followA.add(str);
									ret++;
								}
							}
						} else {
							if(!allFollow.get(A).contains(s)){
								allFollow.get(A).add(s);
								ret++;
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * 求LL(1)分析表
	 */
	void buildLL1Table() {
		//求LL(1)分析表
		table = new HashMap<TableEntry, Rule>();
		for(Iterator<Entry<String, Rule>> it = syntax.rules.entrySet().iterator(); it.hasNext(); ) {
			Entry<String, Rule> entry = it.next();
			String A = entry.getKey();
			ArrayList<String> right = entry.getValue().right;
			boolean hasEmptyString = false;
			for(String rightItem : right) {
				if(DEBUG)
					System.out.println(A + "->" + rightItem);
				Set<String> fs = getFirstCollectionOfString(rightItem);
				for(String s : fs) {
					TableEntry tableEntry;
					Rule newRule;
					if(s.equals(Syntax.EMPTY_STRING)) {
						tableEntry = new TableEntry(A, Syntax.END_CHAR);
						newRule = new Rule(A, rightItem);
						hasEmptyString = true;
					} else {
						tableEntry = new TableEntry(A, s);
						newRule = new Rule(A, rightItem);
						if(table.containsKey(tableEntry) && !table.get(tableEntry).equals(newRule)) {
							if(DEBUG)
								System.err.println("collision at " + tableEntry.toString() + " old rule: " + table.get(tableEntry) + " new rule: " + newRule);
						} else {
							table.put(tableEntry, newRule);
							if(DEBUG)
								System.out.println(tableEntry.toString() + newRule.toString());
						}
					}
				}
				if(hasEmptyString) {
					Set<String> followA = allFollow.get(A);
					for(String s : followA) {
						TableEntry tableEntry = new TableEntry(A, s);
						Rule newRule = new Rule(A, Syntax.EMPTY_STRING);
						if(table.containsKey(tableEntry) && !table.get(tableEntry).equals(newRule)) {
							if(DEBUG)
								System.err.println("collision at " + tableEntry.toString() + " old rule: " + table.get(tableEntry) + " new rule: " + newRule);
						} else {
							table.put(tableEntry, newRule);
							if(DEBUG)
								System.out.println(tableEntry.toString() + newRule.toString());
						}
					}
				}
			}
		}
	}
	
	void init() throws Exception {
		syntax.read();
		getAllFirstCollection();
		getAllFollowCollection();
		buildLL1Table();
	}
	
	void printStack(Stack<String> s) {
		if(!DEBUG) return;
		for(int i = 0; i < s.size(); i++){
			System.out.print(s.get(i) + " ");
		}
		System.out.println("");
	}
	
	public static SyntaxTree lastSyntaxTree;
	
	@Override
	public void run(String iFile, String oFile) {
		System.out.println("parse");
		
		ArrayList<Token> tokens;
		
		try {
			Document doc = Util.readXmlFile(new File(iFile));
			tokens = Util.readTokensFromDocument(doc);
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			System.out.println("failed to load token file");
			e1.printStackTrace();
			return;
		}
		
		//String[] input = null;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Stack<String> stack = new Stack<String>();
		Stack<SyntaxTree> trees = new Stack<SyntaxTree>();
		stack.push(Syntax.END_CHAR);
		stack.push(syntax.beginSym);
		SyntaxTree root = new SyntaxTree(syntax.beginSym);
		trees.push(new SyntaxTree(""));
		trees.push(root);
		int p = 0;
		boolean successFlag = false;
		while(!stack.isEmpty()) {
			printStack(stack);
			if(DEBUG)
				System.out.println(tokens.get(p).mimeType);
			String top = stack.peek();
			Token token = tokens.get(p);
			String a = token.mimeType;
			//栈顶是终结符
			if(top.equals(Syntax.END_CHAR) || syntax.getSymbolType(top).equals(SymbolType.TERMINATOR)) {
				if(top.equals(Syntax.END_CHAR) && a.equals(Syntax.END_CHAR)) {
					//分析成功
					successFlag = true;
					break;
				}
				if(top.equals(a)) {
					p++;
					stack.pop();
					SyntaxTree tree = trees.pop();
					tree.content = SyntaxTree.getLeafNodeContent(a, token.value);
					
					tree.prop.values.add(token.value);
					//System.out.println(tree.content + " " + tree.value.length);
				} else {
					//出错
					throw new RuntimeException();
				}
			} else {
				TableEntry entry = new TableEntry(top, a);
				Rule rule = table.get(entry);
				if(rule != null) {
					String right = rule.right.get(0);
					stack.pop();
					SyntaxTree rt = trees.pop();
					if(!right.equals(Syntax.EMPTY_STRING)) {
						String[] syms = Rule.getSymbolsAsArray(right);
						for(int i = syms.length - 1; i >= 0; i--) {
							stack.push(syms[i]);
							SyntaxTree child = new SyntaxTree(syms[i]);
							rt.addChild(child);
							trees.push(child);
						}
					} else {
						rt.addChild("");
					}
				} else {
					//出错
					throw new RuntimeException();
				}
			}
		}
		if(DEBUG)
			System.out.println(root);
		this.lastSyntaxTree = root;
		try {
			Util.writeStringToFile(new File(oFile), root.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
