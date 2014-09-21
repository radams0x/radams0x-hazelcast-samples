package pub.radams.idmpomsgr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import pub.radams.idmpomsgr.biz.IdempotentBizTask;
import pub.radams.idmpomsgr.datagrid.MemoryClusterService;
import pub.radams.idmpomsgr.idempotentmessaging.IdempotentHandler;

import com.hazelcast.core.HazelcastInstance;

@Component
public class MsgrMain {
	
	private List<String> receivedMsgs;
	private List<String> completedMsgs;	
	private HazelcastInstance memoryClusterNode;
	private String clusterNodeName;
	
	@Inject
	private MemoryClusterService memoryClusterService;
	@Inject
	private IdempotentHandler idempotentHandler;
	@Inject
	private IdempotentBizTask idempotentBizTask;
	
	@PostConstruct
	public void init()
	{
		memoryClusterNode = memoryClusterService.getClusterNode();
		receivedMsgs = memoryClusterNode.getList("receivedMsgs");
		completedMsgs = memoryClusterService.getClusterNode().getList("completedMsgs");
		clusterNodeName = memoryClusterNode.getName();
	}
	

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MsgrMain.class.getPackage().getName());
		try{
			context.scan("pub.radams.idmpomsgr.*");
			MsgrMain p = context.getBean(MsgrMain.class);
	        p.start(args);
    	}
    	catch(Exception e){
			log("Exception in MsgrMain:'" + e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
    	}
		finally{
			context.close();
		}
	}
    private void start(String[] args) {
    	try{
			List<String> msgs = getIncoming();
			for(String msg : msgs)
			{
				idempotentHandler.processMessage(msg);
			}
			report();
    	}
    	catch(Exception e){
			log("Exception in MsgrMain:'" + e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
    	}

    }
    

	private List<String> getIncoming()
	{
		List<String> msgs = new ArrayList<String>();
	    Random randomGenerator = new Random();
	    int msgCount = randomGenerator.nextInt(10000) + 10000;
	    for (int idx = 1; idx <= msgCount; ++idx){
	      int randomInt = randomGenerator.nextInt(100);
	      String msg = "msg_" + new Integer(randomInt).toString();
	      msgs.add(msg);
	      log("Generated : " + msg);
	    }
	    return msgs;
	}
	
	private void report()
	{
		String rpt = "END PROCESSING on " + clusterNodeName + "\n" 
				+ "Results:" + "\n"
				+ "Received " + new Integer(receivedMsgs.size()).toString()  + "\n"
				+ "Completed " + new Integer(completedMsgs.size()).toString()  + "\n"
				+ "Concurrent Lock Acquisition Failures "
					+ new Integer(idempotentHandler.getConcurrentLockAcquisitionFailsCount()).toString()  + "\n"
				+ "Concurrent Race On Process Message " 
					+ new Integer(idempotentHandler.getConcurrentRaceOnProcessMessageCount()).toString()  + "\n"
				;
		Set<String> dupeCheck = new HashSet<String>();
		int dupeCount = 0;
		for(String msg : completedMsgs){
			if(!dupeCheck.add(msg)){
				dupeCount++;
			}
		}
		String dupeMsg = "No dupes found.";
		if(dupeCount > 0){
			dupeMsg = "FOUND " + (new Integer(dupeCount)).toString() + " DUPLICATE MESSAGES PROCESSED!!!";
		}

		log(rpt + dupeMsg);
	}

	
	private static void log(String aMessage)
	{
		System.out.println(aMessage);
	}
}
