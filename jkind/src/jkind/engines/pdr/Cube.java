package jkind.engines.pdr;

public abstract class Cube {
	private Cube next;
	
	public abstract boolean subsumes(Cube other);
	public abstract Cube removeLiteral(int i);
	public abstract int size();
	

	public void setNext(Cube next) {
		this.next = next;
	}

	public Cube getNext() {
		return next;
	}
}
