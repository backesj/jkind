package jkind.engines.pdr;

import java.util.HashSet;
import java.util.Set;

public abstract class Frame {
	protected final Set<Cube> cubes = new HashSet<>();

	public void add(Cube c) {
		cubes.add(c);
	}

	public Set<Cube> getCubes() {
		return cubes;
	}

	public boolean isEmpty() {
		return cubes.isEmpty();
	}

	@Override
	public String toString() {
			return cubes.toString();
	}
}
