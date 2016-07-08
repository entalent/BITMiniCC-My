package bit.minisys.minicc;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;


import me.entalent.minicc.MiniCCompiler2;

import org.xml.sax.SAXException;

public class BITMiniCC {
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 1){
			usage();
			return;
		}
		String file = args[0];
		if(file.indexOf(".c") < 0){
			System.out.println("Incorrect input file:" + file);
			return;
		}
		System.out.println("Start to compile ...");
		new MiniCCompiler2().run(file);
		System.out.println("Compiling completed!");
	}
	
	public static void usage(){
		System.out.println("USAGE: BITMiniCC FILE_NAME.c");
	}
}
