package pub.radams.idmpomsgr;

public interface IdempotentMessageHandler {

	public void processMessage(String msg);
	
	public int getConcurrentLockAcquisitionFailsCount();
	
	public int getConcurrentRaceOnProcessMessageCount();
}
