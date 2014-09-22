
idmpo-msgr.MsgrMain is a stand-alone Java command line app. 
It generates random test message ID keys, and then sends the list of messages to IdempotentMessageHandler to process.
IdempotentMessageHandlerImpl uses an injected MemoryClusterService (which encapsulates Hazelcast) to maintain a
com.hazelcast.core.IMap which it uses to ensure that each message key is processed once and only once. When a new unique key
is found it sends it to IdempotentBizTask to process. IdempotentBizTaskImpl records each processed key in a 
completedMsgs clustered List, which should contain no dupes.
IdempotentMessageHandlerImpl also records each message before processing in a receivedMsgs clustered List, which therefore 
may contain dupes.

At the end, MsgrMain.report inspects completedMsgs for dupes, and also reports on lock contention. Each MsgrMain instance that 
reports will include stats for the whole cluster at the time that the given instance reported.


MsgrMain.getIncoming() will generate a list of message ID strings; these are generated using a random int
embedded in the string; 

Command line opts to control test behavior:
    		
   Sample command line:
   --count=1000 --range=1000 --rangestart=1 --taskworktime=40
   

	count  - controls how many to generate; 
	range, rangeStart  - control the min and max of the random int, 
					and therefore roughly how many duplicate message ID's will be in the list;
					this allows us to test the once and only once message handling behavior,
					as well as to potentially generate some cluster concurrent access on keys
					
	taskworktime  - sets how long biz logic message consumer will take to process, 
					controlling contention window on locked map key