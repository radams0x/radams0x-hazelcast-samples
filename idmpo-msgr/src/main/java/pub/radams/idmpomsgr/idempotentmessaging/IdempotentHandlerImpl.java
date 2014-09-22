package pub.radams.idmpomsgr.idempotentmessaging;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import pub.radams.idmpomsgr.IdempotentMessageHandler;
import pub.radams.idmpomsgr.IdempotentBizTask;
import pub.radams.idmpomsgr.datagrid.MemoryClusterService;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;


@Service("idempotentMessageHandler")
public class IdempotentHandlerImpl implements IdempotentMessageHandler{

	
	private List<String> receivedMsgs;
	private HazelcastInstance memoryClusterNode;
	private IMap<String, String> processedMsgs;
	private String clusterNodeName;
	private int concurrentLockAcquisitionFailsCount = 0;
	private int concurrentRaceOnProcessMessageCount = 0;

	@Inject
	private MemoryClusterService memoryClusterService;
	@Inject
	private IdempotentBizTask idempotentBizTask;
	
	@PostConstruct
	public void init()
	{
		memoryClusterNode = memoryClusterService.getClusterNode();
		receivedMsgs = memoryClusterNode.getList("receivedMsgs");
		processedMsgs = memoryClusterNode.getMap("processedMsgs");
		clusterNodeName = memoryClusterNode.getName();
	}
	
	public void processMessage(String msg)
	{
		receivedMsgs.add(msg);
		if(processedMsgs.containsKey(msg))
			return;
		
		if(!processedMsgs.tryLock(msg))
		{
			concurrentLockAcquisitionFailsCount++;
			log("CONCURRENT RACE: LOCK AQUIRE FAIL '" + msg + "'");
			return;
		}
		try{
			if(processedMsgs.containsKey(msg)){
				concurrentRaceOnProcessMessageCount++;
				log("CONCURRENT RACE: " + msg + " ADDED FROM OTHER THREAD OR CLUSTER NODE");
				return;
			}
			idempotentBizTask.runIdempotentMessageTask(msg);
			// only add msg to processedMsgs map if runIdempotentMessageTask succeeds
			processedMsgs.put(msg, clusterNodeName);
			log(msg + " processed by " + clusterNodeName);		
		}
		catch(Exception e)
		{
			log("Exception in MsgrMain.processMessage:'" + msg + "'\n" 
			+ e.getMessage() + "\n" + e.toString()+ "\n" + e.getStackTrace()
		);
		}
		finally{
			processedMsgs.unlock(msg);
		}
	}
	
	
	public int getConcurrentLockAcquisitionFailsCount(){
		return concurrentLockAcquisitionFailsCount;
	}
	public int getConcurrentRaceOnProcessMessageCount(){
		return concurrentRaceOnProcessMessageCount;
	}
	
	private static void log(String aMessage)
	{
		System.out.println(aMessage);
	}
}
