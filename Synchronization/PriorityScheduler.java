package cs131.pa2.CarsTunnels;

import java.util.Collection;
import cs131.pa2.Abstract.Tunnel;
import cs131.pa2.Abstract.Vehicle;
import cs131.pa2.Abstract.Log.Log;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityScheduler extends Tunnel{
	private Collection<Tunnel> tunnels;
	private Queue<Vehicle> waitQueue;
	private Map<Vehicle, Tunnel> record = new HashMap<>();  //a record to track vehicle and its tunnel
	final Lock lock = new ReentrantLock();
	final Condition enter = lock.newCondition();

	public PriorityScheduler(String name, Collection<Tunnel> tunnels, Log log) {
		super(name, log);
		this.tunnels = tunnels;
		this.waitQueue = new PriorityQueue<>(new Comparator<Vehicle>() {
			public int compare(Vehicle v1, Vehicle v2) {return v2.getPriority() - v1.getPriority();}
		});
	}
	
	@Override
	public boolean tryToEnterInner(Vehicle vehicle) {
		lock.lock();
		try {
			waitQueue.add(vehicle);                                   //add the thread to queue immediately
			while (!priorityWin(vehicle) || !enterAllowed(vehicle)) { //if failed to enter, wait
				enter.await();
			}
			waitQueue.poll(); //if succeeded to enter, it means the thread is already at head, poll it out
			return true;      //so the vehicle can cross the tunnel, doWhileInLoop()
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return false;
	}
	
	public boolean enterAllowed(Vehicle vehicle) {  //return if the vehicle is allowed to enter
		for (Tunnel tunnel:tunnels) {
			if (tunnel.tryToEnterInner(vehicle)) {
				record.put(vehicle, tunnel);        //record the vehicle and its tunnel if it gets in
				return true;
			}
		}
		return false;
	}
	
	public boolean priorityWin(Vehicle vehicle) {   //if the vehicle is the head, it has the highest priority
		return vehicle.equals(this.waitQueue.peek());
	}

	@Override
	public void exitTunnelInner(Vehicle vehicle) {
		lock.lock();
		try {
			this.record.get(vehicle).exitTunnelInner(vehicle);
			enter.signalAll();  //when a vehicle leaves, wake up all threads so they can compete again
		} finally {
			lock.unlock();
		}	
	}
	
}
