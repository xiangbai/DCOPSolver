package com.cqu.heuristics;
/*
 * 获取连接结点最少的结点
 * */
import com.cqu.core.Problem;
import com.cqu.util.StatisticUtil;
import com.cqu.varOrdering.DFS.DFSview;

public class LeastConnectedHeuristic implements ScoringHeuristic<Short>{
	private Problem problem;
	
	public LeastConnectedHeuristic(Problem problem){
		this.problem = problem;
	}
	@Override
	public int getScores(){ // 获取某个符合条件的结点
		// TODO Auto-generated method stub
		int minNeighbourCount=1000;
		int minNeighbourCountNodeId=-1;
		/*
		 * 寻找邻居结点最多的结点ID
		 */
		for(Integer nodeId : this.problem.neighbourAgents.keySet())
		{
			int temp=this.problem.neighbourAgents.get(nodeId).length;
			if(temp < minNeighbourCount)
			{
				minNeighbourCount=temp;
				minNeighbourCountNodeId=nodeId;
			}
		}
		//System.out.println("root: " + minNeighbourCountNodeId);
		return minNeighbourCountNodeId;
	}
	public int getScores (Integer nodeId, DFSview dfsview){
		int[] neighbours=dfsview.neighbourNodes.get(nodeId); //邻居结点的集合
		
		int[] counts=dfsview.neighbourCounts.get(nodeId); //每个邻居结点相邻的数目
		for(int i=0;i<counts.length;i++)
		{
			if(dfsview.nodeIterated.get(neighbours[i])==true) //是否遍历过
			{
				counts[i]= 1000; //将最少的结点数目变成最大
			}
		}
		int minIndex=StatisticUtil.min(counts); //得到集合中结点数目最多的下标
		
		if(counts[minIndex]== 1000)
		{
			return -1;
		}else
		{
			return neighbours[minIndex];
		}
	}
}
