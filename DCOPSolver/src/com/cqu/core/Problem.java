package com.cqu.core;
/*
 * 通过约束图获取相结点之间彼此的关系，从而记录它们相对应的关系
 * */
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Element;

public class Problem {
	
	public final static String KEY_PARENT="parent";
	public final static String KEY_PSEUDO_PARENT="pseudo_parent";
	public final static String KEY_CHILDREN="children";
	public final static String KEY_NEIGHBOUR="neighbour";
	
	public Problem() {
		// TODO Auto-generated constructor stub
		domains=new HashMap<String, int[]>();
		costs=new HashMap<String, int[]>();
		agentNames=new HashMap<Integer, String>();
		
		variables = new HashMap<String, Integer>();
		
		agentLevels=new HashMap<Integer, Integer>();
		treeDepth=0;
		agentDomains=new HashMap<Integer, String>();
		neighbourAgents=new HashMap<Integer, int[]>();
		
		
		parentAgents=new HashMap<Integer, Integer>();
		allParentAgents=new HashMap<Integer, int[]>();
		childAgents=new HashMap<Integer, int[]>();
		allChildrenAgents=new HashMap<Integer, int[]>();
		agentConstraintCosts=new HashMap<Integer, String[]>();
	}
	
	//all possible domains and costs
	public Map<String, int[]> domains;
	public Map<String, int[]> costs;
	
	//for each agent
	public Map<Integer, String> agentNames;
	public Map<String, Integer> variables;
	public Map<Integer, Integer> agentLevels;
	public long treeDepth;
	public Map<Integer,String> agentDomains;
	public Map<Integer, int[]> neighbourAgents;
	
	
	public Map<Integer, Integer> parentAgents;
	public Map<Integer, int[]> allParentAgents;
	public Map<Integer, int[]> childAgents;
	public Map<Integer, int[]> allChildrenAgents;
	
	public Map<Integer, String[]> agentConstraintCosts;
	
	public Map<Integer, boolean[]> crossConstraintAllocation;
	
	/*
	 * 只处理一个agent对应一个变量的情况
	 */
	/*public Set<String> getAgentNames(){
		Set<String> agents = new HashSet<String> ();
		
		Iterator<Entry<Integer, String>> iter = this.agentNames.entrySet().iterator();
		while (iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			agents.add((String) entry.getValue());
		}
		return agents;
	}*/
													
	
	/** Extracts the number of variables in the problem
	 * @return 	the number of variables in the problem
	 * @warning Ignores variables with no specified owner. 
	 */
	/*public int getNbrVars () {
		return this.getVariables().size();
	}*/
	/*public Set<String> getVariables()
	{
		Set<String> out = new HashSet<String> ();
		out = this.variables.keySet();
		return out;
	}*/
	/*public HashSet<String> getNeighborVars(String var)
	{
		HashSet<String> out = new HashSet<String> ();
		Set<String> variables = this.getVariables(); // 获取的是变量key的集合,一对一的关系
		int varId = this.variables.get(var);
		
		
		int[] neigh = this.neighbourAgents.get(varId); //它是value
		for(int i = 0; i < neigh.length; i++){
			
			Iterator<String> itor = variables.iterator();
			while (itor.hasNext())
			{
				String key = itor.next();
				if (this.variables.get(key).equals(neigh[i])) // 通过value获取key
				{
					out.add(key);
				}
			}
		}
		
		return out;
	}*/
}
