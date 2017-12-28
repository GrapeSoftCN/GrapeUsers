package Test;

import common.java.httpServer.booter;
import common.java.nlogger.nlogger;

public class TestUser {
	public static void main(String[] args) {
		booter booter = new booter();
		try {
			System.out.println("GrapeUser1");
			System.setProperty("AppName", "GrapeUser1");
			booter.start(1006);
		} catch (Exception e) {
			nlogger.logout(e);
		}
	}
}
