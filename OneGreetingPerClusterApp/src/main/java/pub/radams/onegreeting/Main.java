package pub.radams.onegreeting;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

public class Main {

	public static void main(String[] args) {
		HazelcastInstance memoryClusterNode = Hazelcast.newHazelcastInstance();
		IAtomicLong control = memoryClusterNode.getAtomicLong("control");
		if(control.compareAndSet(0, 1)){
			System.out.println("Hello!");
		}
		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
}