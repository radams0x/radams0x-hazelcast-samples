package pub.radams.idmpomsgr;

public interface IdempotentBizTask {
	
	public void runIdempotentMessageTask(String msg);
	
	public void setTestTaskWorkTimeMilliSecs(int milliSecs);
}
