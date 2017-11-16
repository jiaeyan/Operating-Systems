package cs131.pa2.Test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Test;

import cs131.pa2.Abstract.Direction;
import cs131.pa2.Abstract.Tunnel;
import cs131.pa2.Abstract.Vehicle;
import cs131.pa2.Abstract.Log.Event;
import cs131.pa2.Abstract.Log.EventType;
import cs131.pa2.Abstract.Log.Log;

public class PrioritySchedulerTest {

    private final String prioritySchedulerName = "SCHEDULER";

    @Before
    public void setUp() {
        Tunnel.DEFAULT_LOG.clearLog();
    }

    @BeforeClass
    public static void broadcast() {
        System.out.printf("Running Priority Scheduler Tests using %s \n", TestUtilities.factory.getClass().getCanonicalName());
    }

    private Tunnel setupSimplePriorityScheduler(String name) {
        Collection<Tunnel> tunnels = new ArrayList<Tunnel>();
        tunnels.add(TestUtilities.factory.createNewBasicTunnel(name));
        return TestUtilities.factory.createNewPriorityScheduler(prioritySchedulerName, tunnels, new Log());
    }

//    @Test
    public void Car_Enter() {
        Vehicle car = TestUtilities.factory.createNewCar(TestUtilities.gbNames[0], Direction.random());
        Tunnel tunnel = setupSimplePriorityScheduler(TestUtilities.mrNames[0]);
        TestUtilities.VehicleEnters(car, tunnel);
    }

//    @Test
    public void Sled_Enter() {
    		Vehicle sled = TestUtilities.factory.createNewSled(TestUtilities.gbNames[0], Direction.random());
        Tunnel tunnel = setupSimplePriorityScheduler(TestUtilities.mrNames[0]);
        TestUtilities.VehicleEnters(sled, tunnel);
    }
    
    @Test
    public void Priority() {
    		List<Thread> vehicleThreads = new ArrayList<Thread>();
        Tunnel priorityScheduler = setupSimplePriorityScheduler(TestUtilities.mrNames[0]);
    		for (int i=0; i<7; i++) {
    			Vehicle car = TestUtilities.factory.createNewCar(Integer.toString(i), Direction.NORTH);
            car.addTunnel(priorityScheduler);
    			if (i<3) {
    				car.setPriority(4);
    			}
    			else {
    				car.setPriority(i-3);
    			}
    			Thread sharedThread = new Thread(car);
    			sharedThread.start();
    			vehicleThreads.add(sharedThread);
    		}
    		for (Thread t: vehicleThreads) {
    			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		}
    		Tunnel.DEFAULT_LOG.addToLog(EventType.END_TEST);
    		Log log = Tunnel.DEFAULT_LOG;
    		Event currentEvent;
		int i=0;
		Vehicle lastEnteredVehicle = null;
    		do {
    			currentEvent = log.get();
    			if(currentEvent.getEvent() == EventType.ENTER_SUCCESS) {
    				if(i++ > 2) {
    					if (lastEnteredVehicle == null) {
    						lastEnteredVehicle = currentEvent.getVehicle();
    					}
    					else if (currentEvent.getVehicle().getPriority() > lastEnteredVehicle.getPriority()){
    						assertTrue("Vehicle "+currentEvent.getVehicle() + " has higher priority than "+lastEnteredVehicle + " and should run before!", false);
    					}
    				}
    			}
    		} while (!currentEvent.getEvent().equals(EventType.END_TEST));    		
    }
    
}
