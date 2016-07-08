package me.entalent.minicc.scanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import bit.minisys.minicc.scanner.IMiniCCScanner;
import me.entalent.minicc.util.xml.XmlNode;

public class Scanner implements IMiniCCScanner {
	
	static {
		initDfa();
	}
	
	/**
	 * DFA的所有状态
	 */
	private static final int BEGIN = 0,
			NUMBER_DEC = 1,
			NUMBER_OCTORHEX = 2,
			NUMBER_HEX = 3,
			NUMBER_OCT = 4,
			NUMBER_REAL_POST_DOT = 5,
			NUMBER_REAL_PRE_DOT = 6,
			NUMBER_REAL_PRE = 7,
			NUMBER_REAL_E = 8,
			NUMBER_REAL_EXP_SIGN = 9,
			NUMBER_REAL_EXP = 10,
			NUMBER_REAL_FL = 11, 
			NUMBER_REAL_ACCEPT = 12, //ACCEPT
			NUMBER_INT_U = 13,
			NUMBER_INT_L = 14,
			NUMBER_INT_ACCEPT = 15, //ACCEPT
			NUMBER_ERROR = 16, //ERROR
			
			IDENTIFIER = 17,
			IDENTIFIER_ACCEPT = 18, //ACCEPT
			IDENTIFIER_ERROR = 19, //ERROR
			
			SEPARATOR = 20, 
			SEPARATOR_ACCEPT = 21, //ACCEPT
			
			OPERATOR_1 = 22,
			OPERATOR_2 = 23,
			OPERATOR_3 = 24,
			OPERATOR_4 = 25,
			OPERATOR_5 = 26,
			OPERATOR_6 = 27,
			OPERATOR_7 = 28,
			OPERATOR_8 = 29,
			OPERATOR_9 = 30,
			OPERATOR_ACCEPT = 31, //ACCEPT
			OPERATOR_ERROR = 32, //ERROR
			
			CHAR_BEGIN = 33, //1
			CHAR_NORMAL = 34,//2
			CHAR_SLASH = 35, //3
			CHAR_HEX_1 = 36, //4
			CHAR_HEX_2 = 37, //6
			CHAR_OCT_1 = 38, //5
			CHAR_OCT_2 = 39, //7
			CHAR_ACCEPT = 41, //ACCEPT
			CHAR_ERROR = 42, //ERROR
	
			STRING_BEGIN = 43,
			STRING_SLASH = 45,
			STRING_HEX_1 = 46,
			STRING_HEX_2 = 47,
			STRING_OCT_1 = 48,
			STRING_OCT_2 = 49,
			STRING_ACCEPT = 50, //ACCEPT
			STRING_ERROR = 51; //ERROR
	
	/**
	 * token类型枚举
	 * @author entalent
	 */
	public enum TokenType {
		KEYWORD, IDENTIFIER, SEPARATOR, CONST_INTEGER, CONST_REAL, CONST_CHAR, CONST_STRING, OPERATOR
	}
	
	/**
	 * 关键字
	 */
	public static final String[] keywords = {
		"auto", "short", "int", "long", "float", "double", "char", "struct", "union", "enum", "typedef", "const",
		"unsigned", "signed", "extern", "register", "static", "volatile", "void", "if", "else", "switch", "case",
		"for", "do", "while", "goto", "continue", "break", "default", "sizeof", "return",
	};
	
	/**
	 * 关键字对应的终结符
	 */
	public static final String[] keywordType = {
			"auto", "short", "TKN_INT", "long", "TKN_FLOAT", "double", "char", "struct", "union", "enum", "typedef", "const",
			"unsigned", "signed", "extern", "register", "static", "volatile", "void", "TKN_IF", "TKN_ELSE", "switch", "case",
			"for", "do", "TKN_WHILE", "goto", "TKN_CONTINUE", "TKN_BREAK", "default", "sizeof", "TKN_RET",
	};
	
	/**
	 * 运算符
	 */
	public static final String[] operators = {
		"(", ")", "{", "}", "[", "]", ",", ";", "=", "+", "-", "*", "/", "==", "!=", ">", "<", ">=", "<="
	};
	
	public static final String[] operatorType = {
		"TKN_LP", "TKN_RP", "TKN_LB", "TKN_RB", "TKN_LB2", "TKN_RB2", "TKN_COMMA", "TKN_SEMI", "TKN_ASIGN", "TKN_ADD", "TKN_SUB", "TKN_MUL", "TKN_DIV", "TKN_EQU", "TKN_NEQU", "TKN_G", "TKN_L", "TKN_GE", "TKN_LE"
	};
	
	/**
	 * 存储单个token的类
	 * @author entalent
	 *
	 */
	public static class Token {
		public int number;
		public String value;
		public TokenType type;
		public int line;
		public boolean valid;
		public String mimeType;
		
		public void set(String field, String value) {
			switch(field) {
			case "number":
				this.number = Integer.parseInt(value);
				break;
			case "value":
				this.value = (String) value;
				break;
			case "type":
				//this.type = (TokenType) value;
				break;
			case "line":
				this.line = Integer.parseInt(value);
				break;
			case "valid":
				this.valid = Boolean.parseBoolean(value);
				break;
			case "mimetype":
				this.mimeType = (String) value;
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		
		public void setMimeType() {
			switch(type) {
			case KEYWORD:
				for(int i = 0; i < keywords.length; i++){
					if(keywords[i].equals(this.value)){
						this.mimeType = keywordType[i];
						break;
					}
				}
				break;
			case OPERATOR:
			case SEPARATOR:
				for(int i = 0; i < operators.length; i++){
					if(operators[i].equals(this.value)) {
						this.mimeType = operatorType[i];
						break;
					}
				}
			
			break;
			case IDENTIFIER:
				this.mimeType = "TKN_ID";
				break;
			case CONST_INTEGER:
				this.mimeType = "TKN_CONSTI";
				break;
			case CONST_REAL:
				this.mimeType = "TKN_CONSTF";
				break;
			}
		}
	}
			
	/**
	 * 词法分析要用的自动机
	 * 用Integer表示状态， Map存储状态转换矩阵
	 * @author ental
	 *
	 */
	public static class Dfa {
		int currentState;
		
		HashMap<Byte, Integer>[] stateTrans;
		
		public Dfa(HashMap<Byte, Integer>[] stateTrans) {
			this.stateTrans = stateTrans;
		}
		
		public void init() {
			this.currentState = 0;
		}
		
		public void transfer(byte b) {
			try {
				if(stateTrans[currentState].containsKey(b))
					this.currentState = stateTrans[currentState].get(b);
				else {
					System.out.println("no key for state " + currentState + " " + (char)b + "(" + b + ")");
				}
			} catch(Exception e) {
				System.out.println("byte = " + (char)b + " " + dfa.currentState);
				e.printStackTrace();
			}
		}
		
		public int getCurrentState() {
			return currentState;
		}
		
		public int getNextState(byte b) {
			int nextState = currentState;
			if(stateTrans[currentState].containsKey(b)) {
				nextState = stateTrans[currentState].get(b);
			}
			return nextState;
		}
	}
			
	/**
	 * 用于词法分析的DFA
	 */
	static Dfa dfa;
	
	
	
	/**
	 * 生成XML文件的方法
	 * @param tokenList
	 * @param outFile
	 */
	private static void generateXmlFile(ArrayList<Token> tokenList, File outFile) {
		XmlNode project = new XmlNode("project").attribute("name", "test.c");
		XmlNode rootNode = new XmlNode("tokens");
		project.addChild(rootNode);
		for(Token t : tokenList) {
			XmlNode node = new XmlNode("token");
			node.addChild(new XmlNode("number").textContent(t.number + ""));
			node.addChild(new XmlNode("value").textContent(t.value));
			node.addChild(new XmlNode("type").textContent(tokenTypeToString(t.type)));
			node.addChild(new XmlNode("line").textContent("1"));
			node.addChild(new XmlNode("valid").textContent("true"));
			node.addChild(new XmlNode("mimetype").textContent(t.mimeType));
			rootNode.addChild(node);
		}
		try {
			if(!outFile.exists()) {
				outFile.getParentFile().mkdirs();
				outFile.createNewFile();
			}
			project.writeXmlToFile(outFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 判断当前是否为接受状态
	 * @param state
	 * @return
	 */
	private static int isAcceptState(int state) {
		if( state == NUMBER_REAL_ACCEPT
				|| state == NUMBER_INT_ACCEPT
				|| state == IDENTIFIER_ACCEPT
				|| state == SEPARATOR_ACCEPT
				|| state == OPERATOR_ACCEPT)
			return 1;
		else if(state == CHAR_ACCEPT || state == STRING_ACCEPT)
			return 0;
		else
			return -1;
	}
	
	/**
	 * 判断是否为产生错误的状态
	 * @param state
	 * @return
	 */
	private static boolean isErrorState(int state) {
		return state == NUMBER_ERROR
				|| state == IDENTIFIER_ERROR
				|| state == OPERATOR_ERROR;
	}

	@Override
	public void run(String iFile, String oFile) throws IOException {
		System.out.println("scan");
		BufferedReader reader = new BufferedReader(new FileReader(iFile));
		String line = "";
		StringBuilder sb = new StringBuilder("");
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		
		ArrayList<Token> tokens = new ArrayList<Token>();
		int tokenNumberCounter = 1;
		
		String str = sb.toString();
		int beginIndex = 0, endIndex = 0;
		
		Map<Integer, TokenType> stateTypeMap = new HashMap<>();
		stateTypeMap.put(IDENTIFIER_ACCEPT, TokenType.IDENTIFIER);
		stateTypeMap.put(NUMBER_INT_ACCEPT, TokenType.CONST_INTEGER);
		stateTypeMap.put(NUMBER_REAL_ACCEPT, TokenType.CONST_REAL);
		stateTypeMap.put(CHAR_ACCEPT, TokenType.CONST_CHAR);
		stateTypeMap.put(STRING_ACCEPT, TokenType.CONST_STRING);
		stateTypeMap.put(SEPARATOR_ACCEPT, TokenType.SEPARATOR);
		stateTypeMap.put(OPERATOR_ACCEPT, TokenType.OPERATOR);
		
		
		while(beginIndex < str.length()) {
			while(beginIndex < str.length() && Character.isWhitespace(str.charAt(beginIndex))) 
				beginIndex++;
			if(beginIndex >= str.length())
				break;
			dfa.init();
			endIndex = beginIndex;
			boolean errFlag = false;
			int accFlag = -1;
			while(-1 == (accFlag = isAcceptState(dfa.getCurrentState()))) {
				if(endIndex >= str.length()) {
					dfa.transfer((byte)-1);
					if(-1 == isAcceptState(dfa.getCurrentState())) {
						System.out.println("error at index " + endIndex + " " + dfa.getCurrentState());
						errFlag = true;
					}
					break;
				} else {
					dfa.transfer((byte) str.charAt(endIndex));
					if(isErrorState(dfa.getCurrentState())) {
						System.out.println("error at index " + endIndex + " " + dfa.getCurrentState());
						errFlag = true;
						break;
					}
				}
				endIndex++;
			}
			if(errFlag) {
				break;
			}
			endIndex -= accFlag;
			String tokenStr = str.substring(beginIndex, Math.min(endIndex, str.length()));
			
			Token token = new Token();
			token.line = 1;
			token.number = tokenNumberCounter++;
			token.value = tokenStr;
			token.valid = true;
			
			token.type = stateTypeMap.get(dfa.getCurrentState());
			if(token.type == TokenType.IDENTIFIER) {
				if(isKeyword(token.value)) {
					token.type = TokenType.KEYWORD;
				}
			}
			token.setMimeType();
			//System.out.println("token: " + tokenStr + " state: " + dfa.getCurrentState() + " " + tokenTypeToString(token.type));
			tokens.add(token);
			beginIndex = endIndex;
		}
		
		generateXmlFile(tokens, new File(oFile));
		reader.close();
	}

	/**
	 * 初始化词法分析的DFA
	 */
	public static void initDfa() {
		//十六进制
		char[] hexChars = "0123456789ABCDEFabcdef".toCharArray();
		//十进制
		char[] decChars = "0123456789".toCharArray();
		//八进制
		char[] octChars = "01234567".toCharArray();
		//转义字符
		char[] escapeChars = "0abfnrtv\\?\'\"".toCharArray();
		
		final byte EOF = (byte)0xff;
		
		HashMap<Byte, Integer>[] st = new HashMap[52];
		HashMap<Byte, Integer> map;
		
		//////////////BEGIN///////////////////
		st[BEGIN] = new HashMap<>();
		map = st[BEGIN];
		for(byte b = '1'; b <= '9'; b++){
			map.put(b, NUMBER_DEC);
		}
		map.put((byte)'0', NUMBER_OCTORHEX);
		map.put((byte)'.', NUMBER_REAL_PRE_DOT);
		map.put((byte)'_', IDENTIFIER);
		for(byte b = 'a'; b <= 'z'; b++){
			map.put(b, IDENTIFIER);
			map.put((byte)(b + ('A' - 'a')), IDENTIFIER);
		}
		map.put((byte)';', SEPARATOR);
		map.put((byte)',', SEPARATOR);
		map.put((byte)'(', SEPARATOR);
		map.put((byte)')', SEPARATOR);
		map.put((byte)'{', SEPARATOR);
		map.put((byte)'}', SEPARATOR);
		map.put((byte)'[', SEPARATOR);
		map.put((byte)']', SEPARATOR);
		map.put((byte)'+', OPERATOR_1);
		map.put((byte)'-', OPERATOR_2);
		char[] op3chars = new char[]{'*', '/', '%', '=', '!', '^'};
		for(char ch : op3chars) {
			map.put((byte)ch, OPERATOR_3);
		}
		map.put((byte)'&', OPERATOR_4);
		map.put((byte)'|', OPERATOR_5);
		map.put((byte)'>', OPERATOR_6);
		map.put((byte)'<', OPERATOR_7);
		map.put((byte)'~', OPERATOR_9);
		map.put((byte)'?', OPERATOR_9);
		map.put((byte)':', OPERATOR_9);
		map.put((byte)' ', BEGIN);
		map.put((byte)'\t', BEGIN);
		map.put((byte)'\r', BEGIN);
		map.put((byte)'\n', BEGIN);
		map.put((byte)'\'', CHAR_BEGIN);
		map.put((byte)'\"', STRING_BEGIN);
		for(byte b = 0x00; b != (byte)0xff; b++){
			if(map.containsKey(b)) continue;
			map.put(b, IDENTIFIER_ERROR);
		}
		
		//////////////NUMBER_DEC///////////////
		st[NUMBER_DEC] = new HashMap<>();
		map = st[NUMBER_DEC];
		for(byte b = '0'; b <= '9'; b++){
			map.put(b, NUMBER_DEC);
		}
		map.put((byte)'.', NUMBER_REAL_POST_DOT);
		map.put((byte)'E', NUMBER_REAL_E);
		map.put((byte)'e', NUMBER_REAL_E);
		map.put((byte)'U', NUMBER_INT_U);
		map.put((byte)'u', NUMBER_INT_U);
		map.put((byte)'L', NUMBER_INT_L);
		map.put((byte)'l', NUMBER_INT_L);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		//////////////NUMBER_OCTORHEX////////////////
		st[NUMBER_OCTORHEX] = new HashMap<>();
		map = st[NUMBER_OCTORHEX];
		map.put((byte)'x', NUMBER_HEX);
		map.put((byte)'X', NUMBER_HEX);
		for(byte b = '0'; b <= '7'; b++){
			map.put(b, NUMBER_OCT);
		}
		for(byte b = '8'; b <= '9'; b++){
			map.put(b, NUMBER_REAL_PRE);
		}
		map.put((byte)'.', NUMBER_REAL_POST_DOT);
		map.put((byte)'E', NUMBER_REAL_E);
		map.put((byte)'e', NUMBER_REAL_E);
		map.put((byte)'U', NUMBER_INT_U);
		map.put((byte)'u', NUMBER_INT_U);
		map.put((byte)'L', NUMBER_INT_L);
		map.put((byte)'l', NUMBER_INT_L);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		//////////////NUMBER_HEX////////////////
		st[NUMBER_HEX] = new HashMap<>();
		map = st[NUMBER_HEX];
		for(char c : hexChars) {
			map.put((byte)c, NUMBER_HEX);
		}
		map.put((byte)'U', NUMBER_INT_U);
		map.put((byte)'u', NUMBER_INT_U);
		map.put((byte)'L', NUMBER_INT_L);
		map.put((byte)'l', NUMBER_INT_L);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		//////////////NUMBER_OCT//////////////////		
		st[NUMBER_OCT] = new HashMap<>();
		map = st[NUMBER_OCT];
		for(byte b = '0'; b <= '7'; b++){
			map.put(b, NUMBER_OCT);
		}
		for(byte b = '8'; b <= '9'; b++){
			map.put(b, NUMBER_REAL_PRE);
		}
		map.put((byte)'.', NUMBER_REAL_POST_DOT);
		map.put((byte)'E', NUMBER_REAL_E);
		map.put((byte)'e', NUMBER_REAL_E);
		map.put((byte)'U', NUMBER_INT_U);
		map.put((byte)'u', NUMBER_INT_U);
		map.put((byte)'L', NUMBER_INT_L);
		map.put((byte)'l', NUMBER_INT_L);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		//////////////NUMBER_REAL_POST_DOT/////////////////
		st[NUMBER_REAL_POST_DOT] = new HashMap<>();
		map = st[NUMBER_REAL_POST_DOT];
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_POST_DOT);
		}
		map.put((byte)'E', NUMBER_REAL_E);
		map.put((byte)'e', NUMBER_REAL_E);
		map.put((byte)'F', NUMBER_REAL_FL);
		map.put((byte)'f', NUMBER_REAL_FL);
		map.put((byte)'L', NUMBER_REAL_FL);
		map.put((byte)'l', NUMBER_REAL_FL);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_REAL_ACCEPT);
			}
		}
		
		//////////////NUMBER_REAL_PRE_DOT//////////////
		st[NUMBER_REAL_PRE_DOT] = new HashMap<>();
		map = st[NUMBER_REAL_PRE_DOT];
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_POST_DOT);
		}
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, SEPARATOR_ACCEPT);
		}
		map.put(EOF, SEPARATOR_ACCEPT);
		
		//////////////SEPARATOR/////////////////
		st[SEPARATOR] = new HashMap<>();
		map = st[SEPARATOR];
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, SEPARATOR_ACCEPT);
		}
		
		//////////////NUMBER_REAL_PRE////////////
		st[NUMBER_REAL_PRE] = new HashMap<>();
		map = st[NUMBER_REAL_PRE];
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_PRE);
		}
		map.put((byte)'.', NUMBER_REAL_POST_DOT);
		map.put((byte)'E', NUMBER_REAL_E);
		map.put((byte)'e', NUMBER_REAL_E);
		map.put((byte)'F', NUMBER_REAL_FL);
		map.put((byte)'f', NUMBER_REAL_FL);
		for(byte b = 0x00; b != (byte)0xff; b++){
			if(map.containsKey(b)) continue;
			map.put(b, NUMBER_ERROR);
		}
		
		//////////////NUMBER_REAL_E/////////////
		st[NUMBER_REAL_E] = new HashMap<>();
		map = st[NUMBER_REAL_E];
		map.put((byte)'+', NUMBER_REAL_EXP_SIGN);
		map.put((byte)'-', NUMBER_REAL_EXP_SIGN);
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_EXP);
		}
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, NUMBER_ERROR);
		}
		
		//////////////NUMBER_REAL_EXP_SIGN///////////
		st[NUMBER_REAL_EXP_SIGN] = new HashMap<>();
		map = st[NUMBER_REAL_EXP_SIGN];
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_EXP);
		}
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, NUMBER_ERROR);
		}
		
		//////////////NUMBER_REAL_EXP///////////////
		st[NUMBER_REAL_EXP] = new HashMap<>();
		map = st[NUMBER_REAL_EXP];
		for(char c : decChars){
			map.put((byte)c, NUMBER_REAL_EXP);
		}
		map.put((byte)'F', NUMBER_REAL_FL);
		map.put((byte)'f', NUMBER_REAL_FL);
		map.put((byte)'L', NUMBER_REAL_FL);
		map.put((byte)'l', NUMBER_REAL_FL);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_REAL_ACCEPT);
			}
		}
		
		st[NUMBER_REAL_FL] = new HashMap<>();
		map = st[NUMBER_REAL_FL];
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_REAL_ACCEPT);
			}
		}
		
		st[NUMBER_INT_U] = new HashMap<>();
		map = st[NUMBER_INT_U];
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		st[NUMBER_INT_L] = new HashMap<>();
		map = st[NUMBER_INT_L];
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b)) {
				map.put(b, NUMBER_ERROR);
			} else {
				map.put(b, NUMBER_INT_ACCEPT);
			}
		}
		
		st[IDENTIFIER] = new HashMap<>();
		map = st[IDENTIFIER];
		map.put((byte)'_', IDENTIFIER);
		for(byte b = 'a'; b <= 'z'; b++){
			map.put(b, IDENTIFIER);
			map.put((byte)(b + ('A' - 'a')), IDENTIFIER);
		}
		for(char c : decChars){
			map.put((byte)c, IDENTIFIER);
		}
		//FIXME:???
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, IDENTIFIER_ACCEPT);
		}
		
		String operatorPost = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890_ \n\r\t";
		
		st[OPERATOR_1] = new HashMap<>();
		map = st[OPERATOR_1];
		map.put((byte)'+', OPERATOR_9);
		map.put((byte)'=', OPERATOR_9);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_2] = new HashMap<>();
		map = st[OPERATOR_2];
		map.put((byte)'-', OPERATOR_9);
		map.put((byte)'=', OPERATOR_9);
		map.put((byte)'>', OPERATOR_9);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_3] = new HashMap<>();
		map = st[OPERATOR_3];
		map.put((byte)'=', OPERATOR_9);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_4] = new HashMap<>();
		map = st[OPERATOR_4];
		map.put((byte)'=', OPERATOR_9);
		map.put((byte)'&', OPERATOR_9);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_5] = new HashMap<>();
		map = st[OPERATOR_5];
		map.put((byte)'=', OPERATOR_9);
		map.put((byte)'|', OPERATOR_9);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_6] = new HashMap<>();
		map = st[OPERATOR_6];
		map.put((byte)'>', OPERATOR_8);
		map.put((byte)'=', OPERATOR_8);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_7] = new HashMap<>();
		map = st[OPERATOR_7];
		map.put((byte)'<', OPERATOR_8);
		map.put((byte)'=', OPERATOR_8);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_8] = new HashMap<>();
		map = st[OPERATOR_8];
		map.put((byte)'=', OPERATOR_8);
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			if(Character.isAlphabetic(b) || Character.isDigit(b) || b == '_' || Character.isWhitespace(b)) 
				map.put(b, OPERATOR_ACCEPT);
			else
				map.put(b, OPERATOR_ERROR);
		}
		
		st[OPERATOR_9] = new HashMap<>();
		map = st[OPERATOR_9];
		for(int i = 0; i < 256; i++){
			byte b = (byte)i;
			if(map.containsKey(b)) continue;
			map.put(b, OPERATOR_ACCEPT);
		}
		
		st[CHAR_BEGIN] = new HashMap<>();
		map = st[CHAR_BEGIN];
		for(byte b = 0x00; b != (byte)0xff; b++){
			if(b == '\'' || b == '\"' || b == '\\') continue;
			map.put(b, CHAR_NORMAL);
		}
		map.put((byte)'\\', CHAR_SLASH);
		for(byte b = 0x00; b != (byte)0xff; b++){
			if(map.containsKey(b)) continue;
			map.put(b, CHAR_NORMAL);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_NORMAL] = new HashMap<>();
		map = st[CHAR_NORMAL];
		map.put((byte)'\'', CHAR_ACCEPT);
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_SLASH] = new HashMap<>();
		map = st[CHAR_SLASH];
		for(char c : escapeChars) {
			map.put((byte)c, CHAR_NORMAL);
		}
		map.put((byte)'x', CHAR_HEX_1);
		for(char c = '1'; c <= '7'; c++){
			map.put((byte)c, CHAR_OCT_1);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_HEX_1] = new HashMap<>();
		map = st[CHAR_HEX_1];
		for(char c : hexChars) {
			map.put((byte)c, CHAR_HEX_2);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_HEX_2] = new HashMap<>();
		map = st[CHAR_HEX_2];
		for(char c : hexChars) {
			map.put((byte)c, CHAR_NORMAL);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_OCT_1] = new HashMap<>();
		map = st[CHAR_OCT_1];
		for(char c : octChars){
			map.put((byte)c, CHAR_OCT_2);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[CHAR_OCT_2] = new HashMap<>();
		map = st[CHAR_OCT_2];
		for(char c : octChars) {
			map.put((byte)c, CHAR_NORMAL);
		}
		map.put(EOF, CHAR_ERROR);
		
		st[STRING_BEGIN] = new HashMap<>();
		map = st[STRING_BEGIN];
		map.put((byte)'\"', STRING_ACCEPT);
		map.put((byte)'\\', STRING_SLASH);
		for(byte b = 0x00; b != (byte)0xff; b++){
			if(b == '\'' || b == '\"' || b == '\\') continue;
			if(map.containsKey(b)) continue;
			map.put(b, STRING_BEGIN);
		}
		map.put(EOF, STRING_ERROR);
		
		st[STRING_SLASH] = new HashMap<>();
		map = st[STRING_SLASH];
		for(char c : escapeChars) {
			map.put((byte)c, STRING_BEGIN);
		}
		map.put((byte)'x', STRING_HEX_1);
		for(char c = '1'; c <= '7'; c++){
			map.put((byte)c, STRING_OCT_1);
		}
		map.put(EOF, STRING_ERROR);
		
		st[STRING_OCT_1] = new HashMap<>();
		map = st[STRING_OCT_1];
		for(char c : octChars) {
			map.put((byte)c, STRING_OCT_2);
		}
		map.put(EOF, STRING_ERROR);
		
		st[STRING_OCT_2] = new HashMap<>();
		map = st[STRING_OCT_2];
		for(char c : octChars) {
			map.put((byte)c, STRING_BEGIN);
		}
		map.put(EOF, STRING_ERROR);
		
		st[STRING_HEX_1] = new HashMap<>();
		map = st[STRING_HEX_1];
		for(char c : octChars) {
			map.put((byte)c, STRING_HEX_2);
		}
		map.put(EOF, STRING_ERROR);
		
		st[STRING_HEX_2] = new HashMap<>();
		map = st[STRING_HEX_2];
		for(char c : octChars) {
			map.put((byte)c, STRING_BEGIN);
		}
		map.put(EOF, STRING_ERROR);
		
		dfa = new Dfa(st);
		dfa.init();
	}

	/**
	 * token类型转换为字符串
	 * @param type
	 * @return
	 */
	private static String tokenTypeToString(TokenType type) {
		String[] strs = new String[]{"keyword", "identifier", "separator", "const_i", "const_f", "const_char", "const_string", "operator"};
		return strs[type.ordinal()];
	}
	
	private static boolean isKeyword(String identifier) {
		for(String k : keywords) {
			if(identifier.equals(k)) {
				return true;
			}
		}
		return false;
	}
	

}

