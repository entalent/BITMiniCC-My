package me.entalent.minicc.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import bit.minisys.minicc.pp.IMiniCCPreProcessor;

public class MyPreProcessor implements IMiniCCPreProcessor {
	
	byte[] buffer = new byte[4096];
	
	Dfa dfa;

	@Override
	public void run(String iFilePath, String oFilePath) {
		try {
			File iFile = new File(iFilePath),
					oFile = new File(oFilePath);
			if(!oFile.exists()) {
				oFile.createNewFile();
			}
			execute(iFile, oFile);
		} catch (Exception e) {
			
		}
	}
	
	public void execute(File source, File product) throws Exception {
		initDfa();
		RandomAccessFile rs = new RandomAccessFile(source, "r");
		RandomAccessFile rs1 = new RandomAccessFile(product, "rw");
		long cursor = 0;
		//System.out.println(rs.length());
		while(cursor < rs.length()) {
			rs.seek(cursor);
			int prevState = dfa.getCurrentState();
			byte cb = rs.readByte(), nb;
			dfa.transfer(cb);
			boolean isInComment = false;
			
			if(dfa.currentState == 1) {
				nb = rs.readByte();
				int nextState = dfa.getNextState(nb);
				if(nextState != 0) {
					isInComment = true;
				}
			}
			else if(dfa.currentState == 0 && (prevState == 3)){
				isInComment = true;
			} else if(dfa.currentState != 0) {
				isInComment = true;
			}
			
			if(!isInComment) {
				if(!isIgnoreChar((char) cb))
					rs1.writeByte(cb);
				else
					rs1.writeByte(' ');
				//System.out.println(cb);
			}
			//System.out.println((char)cb + " " + isInComment);
			cursor++;
		}
		rs.close();
		rs1.close();
		
	}
	
	private boolean isIgnoreChar(char ch) {
		return ch == '\t' || ch == '\n' || ch == '\r';
	}
	
	private void initDfa() {
		HashMap<Byte, Integer>[] st = new HashMap[5];
		st[0] = new HashMap<>();
		st[0].put((byte)'/', 1);
		
		st[1] = new HashMap<>();
		st[1].put((byte)'*', 2);
		st[1].put((byte)'/', 4);
		byte b = 0;
		
		for(int i = 0; i < 256; i++){
			if(b == (byte)'*' || b == (byte)'/') continue;
			st[1].put(b, 0);
			b++;
		}
		
		st[2] = new HashMap<>();
		st[2].put((byte)'*', 3);
		
		st[3] = new HashMap<>();
		st[3].put((byte)'/', 0);
		
		st[4] = new HashMap<>();
		st[4].put((byte)'\r', 0);
		st[4].put((byte)'\n', 0);
		
		dfa = new Dfa(st);
		dfa.init();
	}
	
	/**
	 * added by entalent
	 * @param sourceFile
	 */
	private static void showSourceFile(String sourceFilePath) {
		//System.out.println(sourceFilePath);
		String cmd = "\"C:\\Program Files (x86)\\Notepad++\\notepad++.exe\" " + sourceFilePath;
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			System.out.println("open " + sourceFilePath + " failed");
		}
	}

}
