package com.cqu.heuristics;

import com.cqu.core.Problem;
import com.cqu.varOrdering.DFS.DFSview;

public class SmallestDomainHeuristic implements ScoringHeuristic<Short>{
	private Problem problem;
	
	public SmallestDomainHeuristic (Problem problem)
	{
		this.problem = problem;
	}
	@Override
	public int getScores() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getScores(Integer nodeID, DFSview dfsview) {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
