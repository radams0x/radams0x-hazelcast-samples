package pub.radams.idmpomsgr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import pub.radams.idmpomsgr.IdempotentBizTask;
import pub.radams.idmpomsgr.datagrid.MemoryClusterService;
import pub.radams.idmpomsgr.IdempotentMessageHandler;

import com.hazelcast.core.HazelcastInstance;

@Component
public class MsgrMain {
	public static OptionSet commandArgs;
	private List<String> receivedMsgs;
	private List<String> completedMsgs;	
	private HazelcastInstance memoryClusterNode;
	private String clusterNodeName;
	
	@Inject
	private MemoryClusterService memoryClusterService;
	@Inject
	private IdempotentMessageHandler idempotentMessageHandler;
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
			commandArgs = parseArgs(args);
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
    		// command line opts to control test behavior:
    		// getIncoming() will generate a list of message ID strings; these are generated using a random int
    		// embedded in the string; count controls how many to generate; range and rangeStart control the min
    		// and max of the random int, and therefore roughly how many duplicate message ID's will be in the list;
    		// this allows us to test the once and only once message handling behavior, as well as to potentially 
    		// generate some cluster concurrent access on keys
    		int inboxCount = (int)commandArgs.valueOf("count");
    		int testMsgIdRandomRange = (int)commandArgs.valueOf("range");
    		int testMsgIdRandomRangeStart = (int)commandArgs.valueOf("rangestart");
    		// taskworktime sets how long biz logic message consumer will take to process, controlling contention
    		// window on locked map key
    		int taskWorkTime = (int)commandArgs.valueOf("taskworktime");
    		idempotentBizTask.setTestTaskWorkTimeMilliSecs(taskWorkTime);
    		
			List<String> msgs = getIncoming(inboxCount, testMsgIdRandomRange, testMsgIdRandomRangeStart);
			for(String msg : msgs)
			{
				idempotentMessageHandler.processMessage(msg);
			}
			report();
    	}
    	catch(Exception e){
			log("Exception in MsgrMain:'" + e.getMessage() + "\n" + e.toString() + "\n" + e.getStackTrace());
    	}

    }
    

	private List<String> getIncoming(int msgCount, int range, int rangeStart)
	{
		List<String> msgs = new ArrayList<String>();
	    Random randomGenerator = new Random();
	    for (int idx = 1; idx <= msgCount; ++idx){
	      int randomInt = randomGenerator.nextInt(range) + rangeStart;
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
					+ new Integer(idempotentMessageHandler.getConcurrentLockAcquisitionFailsCount()).toString()  + "\n"
				+ "Concurrent Race On Process Message " 
					+ new Integer(idempotentMessageHandler.getConcurrentRaceOnProcessMessageCount()).toString()  + "\n"
				;
		Set<String> dupeCheck = new HashSet<String>();
		int dupeCount = 0;
		for(String msg : completedMsgs){
			if(!dupeCheck.add(msg)){
				dupeCount++;
			}
		}
		String dupeMsg = "No accepted dupes found.";
		if(dupeCount > 0){
			dupeMsg = "FOUND " + (new Integer(dupeCount)).toString() + " DUPLICATE MESSAGES PROCESSED!!!";
		}

		log(rpt + dupeMsg);
	}

	
	private static void log(String aMessage)
	{
		System.out.println(aMessage);
	}
	static OptionSet parseArgs(String[] args) {
        OptionParser parser = new OptionParser();
        parser.accepts( "count" ).withRequiredArg().ofType(Integer.class);
        parser.accepts( "range" ).withRequiredArg().ofType(Integer.class);
        parser.accepts( "rangestart" ).withRequiredArg().ofType(Integer.class);
        parser.accepts( "taskworktime" ).withRequiredArg().ofType(Integer.class);
        
        OptionSet options = parser.parse(args );
        return options;
	}
}
