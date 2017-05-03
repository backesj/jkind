package jkind.engines.pdr;

import java.util.List;

import jkind.engines.pdr.PdrSmtInterpol.Option;
import jkind.lustre.Expr;

public interface PdrSmt {
	public Cube getBadCube();
	public boolean isInitial(Cube cube);
	public TCube solveRelative(TCube s, Option option);
	public TCube solveRelative(TCube s);
	public boolean isBlocked(TCube s);
	public Frame createInitialFrame();
	public void refine(List<Cube> cubes);
	public void comment(String comment);
	public Expr getInvariant(Cube cube);
}
