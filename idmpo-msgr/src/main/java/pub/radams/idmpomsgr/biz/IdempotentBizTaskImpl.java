package pub.radams.idmpomsgr.biz;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import pub.radams.idmpomsgr.IdempotentBizTask;
import pub.radams.idmpomsgr.datagrid.MemoryClusterService;


@Service("idempotentBizTask")
public class IdempotentBizTaskImpl implements IdempotentBizTask{

	
	private List<String> completedMsgs;
	private int testTaskWorkTimeMilliSecs = 0;
	
	@Inject
	private MemoryClusterService memoryClusterService;

	@PostConstruct
	public void init()
	{
		completedMsgs = memoryClusterService.getClusterNode().getList("completedMsgs");
		try {
			Thread.sleep(testTaskWorkTimeMilliSecs);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void runIdempotentMessageTask(String msg)
	{
		completedMsgs.add(msg);
	}
	
	public void setTestTaskWorkTimeMilliSecs(int milliSecs){
		testTaskWorkTimeMilliSecs = milliSecs;
	}
}
