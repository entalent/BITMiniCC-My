package me.entalent.minicc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import me.entalent.minicc.scanner.Scanner.Token;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Util {
	
	public static Document readXmlFile(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		org.w3c.dom.Document doc = db.parse(file);
		return doc;
	}
	
	public static ArrayList<Token> readTokensFromDocument(Document doc) {
		ArrayList<Token> tokenList = new ArrayList<Token>();
		NodeList nodeList = doc.getElementsByTagName("token");
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			Node child = node.getFirstChild();
			Token token = new Token();
			for(; child != null; child = child.getNextSibling()) {
				if(child.getNodeType() == Node.ELEMENT_NODE) {
					token.set(child.getNodeName(), child.getFirstChild().getNodeValue());
				}
			}
			tokenList.add(token);
		}
		Token token = new Token();
		token.mimeType = "#";
		token.value = "#";
		tokenList.add(token);
		return tokenList;
	}
	
	public static boolean createFile(File file) throws IOException {
		file.getParentFile().mkdirs();
		return file.createNewFile();
	}
	
	public static void writeStringToFile(File file, String content) throws IOException {
		if(!file.exists()) {
			createFile(file);
		}
		FileOutputStream out = new FileOutputStream(file);
		out.write(content.getBytes());
	}
}
