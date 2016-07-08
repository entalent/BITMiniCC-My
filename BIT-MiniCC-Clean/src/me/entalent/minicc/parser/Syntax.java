package me.entalent.minicc.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * @author ental
 * 表示一个上下文无关语法
 */
public class Syntax {
	
	enum SymbolType {
		TERMINATOR,
		NONTERMINATOR;
	}
	
	/**
	 * 
	 * @author ental
	 * 表示左部 为某个非终结符号的一组产生式
	 */
	public static class Rule {
		String left;
		ArrayList<String> right;
		
		public Rule(String lstr, String rstr) {
			this.left = lstr;
			this.right = new ArrayList<String>();
			Set<String> strs = new HashSet<>();
			String[] rules = rstr.split("[|]");
			for(String i : rules) {
				strs.add(i);
			}
			right.addAll(strs);
		}
		
		/**
		 * 获取产生式右部的所有符号
		 * @param rightItem 产生式右部
		 * @return
		 */
		public static String[] getSymbolsAsArray(String rightItem) {
			if(rightItem.startsWith(".")) {
				rightItem = rightItem.substring(1);
			} 
			if(rightItem.endsWith(".")) {
				rightItem = rightItem.substring(0, rightItem.length() - 1);
			}
			/*
			if(rightItem.equals(Syntax.EMPTY_STRING)) {
				return new String[0];
			}
			*/
			return rightItem.split("[.]");
		}
		
		public static String getStringFromList(ArrayList<String> list) {
			if(list.size() == 0) {
				return "";
			}
			String str = list.get(0);
			for(int i = 1; i < list.size(); i++)
				str += "." + list.get(i);
			return str;
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Rule)) {
				return false;
			}
			Rule rule = (Rule) obj;
			if((!(left.equals(rule.left))) || (right.size() != rule.right.size())) {
				return false;
			}
			Collections.sort(right);
			Collections.sort(rule.right);
			for(int i = 0; i < right.size(); i++){
				if(!right.get(i).equals(rule.right.get(i))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			String str = left + " -> " + right.get(0);
			for(int i = 1; i < right.size(); i++){
				str += "|" + right.get(i);
			}
			return str;
		}
		
		
	}
	
	//空串，ε
	public static final String EMPTY_STRING = "$";
	//字符串结束标志
	public static final String END_CHAR = "#";

	JsonParser parser = new JsonParser();
	//终结符
	public Map<String, String> vt = new HashMap<>();
	//非终结符
	public HashSet<String> vn = new HashSet<>();
	//产生式
	public Map<String, Rule> rules = new HashMap<>();
	//开始符号
	public String beginSym;
		
	Map<String, SymbolType> allSymbolType = new HashMap<>();
	
	SymbolType getSymbolType(String sym) {
		if(!allSymbolType.containsKey(sym)) {
			throw new RuntimeException("symbol does not exist: " + sym);
		}
		return allSymbolType.get(sym);
	}
	
	/**
	 * 读取配置文件中所有的文法规则
	 * @throws Exception
	 */
	void read() throws Exception {
		JsonObject obj = (JsonObject) parser.parse(new InputStreamReader(new FileInputStream(new File(".\\syntax.json"))));
		allSymbolType.put(EMPTY_STRING, SymbolType.TERMINATOR);
		//所有终结符
		JsonObject terminators = (JsonObject) obj.get("terminator");
		Set<Entry<String, JsonElement>> term = terminators.entrySet();
		for(Entry<String, JsonElement> iter : term) {
			String key = iter.getKey(), value = iter.getValue().getAsString();
			vt.put(key, value);
			allSymbolType.put(key, SymbolType.TERMINATOR);
		}
		//所有非终结符
		JsonArray arr = (JsonArray) obj.get("non-terminator");
		for(int i = 0; i < arr.size(); i++){
			String sym = arr.get(i).getAsString();
			vn.add(sym);
			allSymbolType.put(sym, SymbolType.NONTERMINATOR);
		}
		//所有产生式
		JsonObject productions = (JsonObject) obj.get("grammar-production");
		Set<Entry<String, JsonElement>> prod = productions.entrySet();
		for(Entry<String, JsonElement> iter : prod) {
			String left = iter.getKey(), right = iter.getValue().getAsString();
			rules.put(left, new Rule(left, right));
		}
		//开始符号
		beginSym = obj.get("begin-symbol").getAsString();
	}
	
	/**
	 * 获取一个非终结符的产生式
	 * @param nonterm
	 * @return
	 */
	public Rule getSyntaxRule(String nonterm) {
		return rules.get(nonterm);
	}
}
