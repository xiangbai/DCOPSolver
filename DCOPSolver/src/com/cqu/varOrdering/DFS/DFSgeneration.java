package com.cqu.varOrdering.DFS;
import java.util.*;

import com.cqu.core.TreeGenerator;
import com.cqu.heuristics.ScoringHeuristic;
import com.cqu.main.DOTrenderer;
import com.cqu.util.CollectionUtil;
//DFS树形成方式
public class DFSgeneration implements TreeGenerator {

	private DFSview dfsview ;
	
	/** The heuristic used to choose the root */
	private static ScoringHeuristic<Short> rootElectionHeuristic;
	/** The heuristic used to choose the next node */
	private static ScoringHeuristic<Short> NextNodeHeuristics ;
	/*Constructor
	 * @param neighbourNodes 	表示结点的邻接存储关系
	 * 为根结点选择策略
	 * (non-Javadoc)
	 * @see com.cqu.core.TreeGenerator#generate()
	 */
	public DFSgeneration (Map<Integer, int[]> neighbourNodes) 
	{
		dfsview = new DFSview (neighbourNodes);
	}
	
	//DFSTree generate,DFS遍历生成算法
	@Override
	public void generate() {
		// TODO Auto-generated method stub
		int iteratedCount=0;
		Integer curLevel=0;
		
		dfsview.rootId = DFSgeneration.rootElectionHeuristic.getScores();
		Integer curNodeId=dfsview.rootId;  //选择根节点
		dfsview.nodeIterated.put(curNodeId, true);
		iteratedCount++;//根节点已遍历
		dfsview.parentNode.put(curNodeId, -1); //<child, parent> 直接父子关系
		dfsview.nodeLevel.put(curNodeId, curLevel); //根结点是第0层  该结点在树中的层次关系
		
		int totalCount=dfsview.neighbourNodes.size();
		//DFS遍历算法
		while(iteratedCount < totalCount)
		{
			//下一个结点选择策略
			Integer nextNodeId=DFSgeneration.NextNodeHeuristics.getScores(curNodeId, dfsview); //下一个结点的选择是以连接边最多的为准
			
			if(nextNodeId==-1)
			{
				curLevel--;
				//回溯
				curNodeId=dfsview.parentNode.get(curNodeId);
			}else
			{
				curLevel++;
				
				dfsview.childrenNodes.get(curNodeId).add(nextNodeId); //将下一个结点作为其孩子结点
				dfsview.parentNode.put(nextNodeId, curNodeId);
				
				dfsview.nodeIterated.put(nextNodeId, true);
				dfsview.nodeLevel.put(nextNodeId, curLevel); //结点所在的层次
				iteratedCount++;
				
				curNodeId=nextNodeId;
			}
		}
		//display DFS tree
		new DOTrenderer ("DFS tree", this.dfsToString());
	}

	
	/*
	 * 选择根结点策略
	 */
	public static void setRootHeuristics (ScoringHeuristic<Short> rootHeuristic){
		DFSgeneration.rootElectionHeuristic = rootHeuristic;
	}
	/*
	 * 选择叶子结点策略
	 */
	public static void setNextNodeHeuristics (ScoringHeuristic<Short> nextNodeHeuristics) 
	{
		DFSgeneration.NextNodeHeuristics = nextNodeHeuristics ;
	}
	//直接返回这棵DFS伪树
	public DFSview getDFSview ()
	{
		return dfsview;
	}
	@Override
	public Map<Integer, int[]> getChildrenNodes() {
		// TODO Auto-generated method stub
		return CollectionUtil.transform(dfsview.childrenNodes);
	}

	@Override
	public Map<Integer, Integer> getParentNode() {
		// TODO Auto-generated method stub
		return dfsview.parentNode;
	}

	@Override
	public Map<Integer, Integer> getNodeLevels() {
		// TODO Auto-generated method stub
		return dfsview.nodeLevel;
	}

	@Override
	public Map[] getAllChildrenAndParentNodes() {
		// TODO Auto-generated method stub
				Map<Integer, List<Integer>> allChildren=new HashMap<Integer, List<Integer>>();
				Map<Integer, List<Integer>> allParents=new HashMap<Integer, List<Integer>>();
				for(Integer nodeId : dfsview.neighbourNodes.keySet())
				{
					int[] neighbours=dfsview.neighbourNodes.get(nodeId);
					List<Integer> children=dfsview.childrenNodes.get(nodeId);
					Integer parent=dfsview.parentNode.get(nodeId);
					Integer level=dfsview.nodeLevel.get(nodeId);
					if(parent==-1)
					{
						//根节点
						List<Integer> allChildrenList=new ArrayList<Integer>();
						for(int i=0;i<neighbours.length;i++)
						{
							allChildrenList.add(neighbours[i]);
						}
						allParents.put(nodeId, new ArrayList<Integer>());
						allChildren.put(nodeId, allChildrenList);
					}else if(children.size()==0)
					{
						//叶子节点
						List<Integer> allParentList=new ArrayList<Integer>();
						for(int i=0;i<neighbours.length;i++)
						{
							allParentList.add(neighbours[i]);
						}
						allParents.put(nodeId, allParentList);
						allChildren.put(nodeId, new ArrayList<Integer>());
					}else
					{
						//中间节点
						List<Integer> allParentList=new ArrayList<Integer>();
						List<Integer> allChildrenList=new ArrayList<Integer>();
						for(int i=0;i<neighbours.length;i++)
						{
							if(neighbours[i]==parent)
							{
								allParentList.add(neighbours[i]);
							}else if(CollectionUtil.exists(children, neighbours[i])!=-1)
							{
								allChildrenList.add(neighbours[i]);
							}else
							{
								if(level<dfsview.nodeLevel.get(neighbours[i]))
								{
									//在本节点之下
									allChildrenList.add(neighbours[i]);
								}else
								{
									//在本节点之上
									allParentList.add(neighbours[i]);
								}
							}
						}
						allParents.put(nodeId, allParentList);
						allChildren.put(nodeId, allChildrenList);
					}
				}
				return new Map[]{CollectionUtil.transform(allParents), CollectionUtil.transform(allChildren)};
			}

	//绘制DFS伪树
	public static String toTreeString(Map<Integer, String> agentNames, Map<Integer, Integer> parentAgents, Map<Integer, int[]> childAgents)
	{
		Integer rootId=-1;
		for(Integer nodeId : parentAgents.keySet())
		{
			if(parentAgents.get(nodeId)==-1)
			{
				rootId=nodeId;
				break;
			}
		}
		String treeString=getNodeString(rootId, agentNames, childAgents);
		return "["+treeString+"]";
	}
	
	private static String getNodeString(Integer nodeId, Map<Integer, String> agentNames, Map<Integer, int[]> childAgents)
	{
		int[] children=childAgents.get(nodeId);
		if(children!=null&&children.length>0)
		{
			String str="{";
			str+=agentNames.get(nodeId);
			str+="; ";
			for(int i=0;i<children.length-1;i++)
			{
				str+=getNodeString(children[i], agentNames, childAgents);
				str+="; ";
			}
			str+=getNodeString(children[children.length-1], agentNames, childAgents);
			str+="}";
			return str;
		}else
		{
			return "{"+agentNames.get(nodeId)+"}";
		}
	}
	
	
	/** @return a DOT-formated representation of the dfs */
	public String dfsToString() {
		return dfsToString(this.dfsview);
	}
	/** Prints the input dfs in DOT format
	 * @param dfs for each variable, a map associating a list of neighbors to each type of relationship
	 * @return a String representation of the DFS in DOT format
	 */
	public String dfsToString(DFSview dfsview) {
		StringBuilder out = new StringBuilder ("digraph {\n\tnode [shape = \"circle\"];\n\n");
		//获取每个变量结点
		for(Map.Entry<Integer, int[]> entry : dfsview.neighbourNodes.entrySet())
		{
			int nodeId = entry.getKey(); //获取变量的ID号，现在需要根据遍历的情况来确定该结点的父亲与伪父亲
			// First print the variable
			String var = "X" + nodeId;
			out.append("\t" + var + " [style=\"filled\"];\n");
			
			
			// Print the edge with the parent, if any
			int parentId = dfsview.parentNode.get(nodeId); // 获取该变量的父亲结点的ID，得到直接父亲
			if (parentId != -1) //不是根结点
			{
				String parent = "X" + parentId;
				out.append("\t" + parent + " -> " + var + ";\n");
				
				
			}
			if (dfsview.getPseudoParents(nodeId).size() == 0)
			{
				continue; // 没有伪父母的情况
			}
			Iterator<String> iter = dfsview.getPseudoParents(nodeId).iterator();
			while (iter.hasNext()) {
				String pseudo = iter.next();
				out.append("\t" + pseudo + " -> " + var + " [style = \"dashed\" arrowhead = \"none\" weight=\"0.5\"];\n");
				
			}
		}
		out.append("}");
		return out.toString();
	}
}


