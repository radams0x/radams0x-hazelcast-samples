package pub.radams.idmpomsgr.datagrid;


import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Service("memoryClusterService")
public class MemoryClusterService {
	private HazelcastInstance memoryClusterNode;
	private String clusterNodeName;
	
	@PostConstruct
	public void init()
	{
		memoryClusterNode = Hazelcast.newHazelcastInstance();
		clusterNodeName = memoryClusterNode.getName();
	}
	public String getCLusterNodeName(){
		return clusterNodeName;
	}
	public HazelcastInstance getClusterNode(){
		return memoryClusterNode;
	}
	
}
