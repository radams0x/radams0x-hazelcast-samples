package pub.radams.idmpomsgr.biz;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import pub.radams.idmpomsgr.datagrid.MemoryClusterService;


@Service("idempotentBizTask")
public class IdempotentBizTask {

	
	private List<String> completedMsgs;
	
	@Inject
	private MemoryClusterService memoryClusterService;

	@PostConstruct
	public void init()
	{
		completedMsgs = memoryClusterService.getClusterNode().getList("completedMsgs");
	}
	
	public void runIdempotentMessageTask(String msg)
	{
		completedMsgs.add(msg);
	}
}
