package me.entalent.minicc.scanner;

import java.util.HashMap;

public class Dfa {
	int currentState;
	
	HashMap<Byte, Integer>[] stateTrans;
	
	public Dfa(HashMap<Byte, Integer>[] stateTrans) {
		this.stateTrans = stateTrans;
	}
	
	public void init() {
		this.currentState = 0;
	}
	
	public void transfer(byte b) {
		if(stateTrans[currentState].containsKey(b))
			this.currentState = stateTrans[currentState].get(b);
	}
	
	public int getCurrentState() {
		return currentState;
	}
	
	public int getNextState(byte b) {
		int nextState = currentState;
		if(stateTrans[currentState].containsKey(b))
			nextState = stateTrans[currentState].get(b);
		return nextState;
	}
}
