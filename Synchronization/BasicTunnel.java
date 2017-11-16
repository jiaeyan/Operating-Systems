package cs131.pa2.CarsTunnels;

import cs131.pa2.Abstract.Direction;
import cs131.pa2.Abstract.Tunnel;
import cs131.pa2.Abstract.Vehicle;

public class BasicTunnel extends Tunnel{
	private int num_car;
	private int num_sled;
	private Direction direction;
	
	public BasicTunnel(String name) {
		super(name);
		num_car = 0;
		num_sled = 0;
	}

	@Override
	public synchronized boolean tryToEnterInner(Vehicle vehicle) {
		if (vehicle instanceof Car) {
			if (num_car == 0 && num_sled == 0) {
				direction = vehicle.getDirection();
			}
			if (num_car < 3 && num_sled == 0 && vehicle.getDirection().equals(direction)) {
				num_car++;
				return true;
			}
		}
		if (vehicle instanceof Sled) {
			if (num_car == 0 && num_sled == 0) {
				direction = vehicle.getDirection();
				num_sled++;
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void exitTunnelInner(Vehicle vehicle) {
		if (vehicle instanceof Car) {num_car--;}
		if (vehicle instanceof Sled) {num_sled--;}
	}
	
}
