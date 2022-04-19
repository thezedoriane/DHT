package helloWorld;

import peersim.edsim.*;

public class Message {

    public final static int HELLOWORLD = 0;

    private int type;
    private Object content;
    private String typem;

    Message(int type, Object content, String typem) {
	this.type = type;
	this.content = content;
	this.typem=typem;
    }

    public Object getContent() {
	return this.content;
    }

    public int getType() {
	return this.type;
    }
    
    public String getTypeM() {
    	return this.typem;
        }
}