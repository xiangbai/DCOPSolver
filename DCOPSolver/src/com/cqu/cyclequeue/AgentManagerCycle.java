package com.cqu.cyclequeue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cqu.adopt.AdoptAgentCycle;
import com.cqu.adopt.AdoptAgentCycle_2;
import com.cqu.bnbadopt.ADOPT_K;
import com.cqu.bnbadopt.BnBAdoptAgent;
import com.cqu.bnbmergeadopt.AgentModel;
import com.cqu.core.Message;
import com.cqu.parser.Problem;
import com.cqu.settings.Settings;
import com.cqu.util.CollectionUtil;
import com.cqu.util.FileUtil;

public class AgentManagerCycle {

    private Map<Integer, AgentCycle> agents;
	
	public AgentManagerCycle(Problem problem, String agentType) {
		// TODO Auto-generated constructor stub
		
		agents=new HashMap<Integer, AgentCycle>();
		for(Integer agentId : problem.agentNames.keySet())
		{
			AgentCycle agent=null;;
			if(agentType.equals("DPOP"))
			{
			}else if(agentType.equals("BNBADOPT"))
			{
				agent=new BnBAdoptAgent(agentId, problem.agentNames.get(agentId), problem.agentLevels.get(agentId), 
						problem.domains.get(problem.agentDomains.get(agentId)));
			}else if(agentType.equals("BDADOPT"))
			{
				agent=new AgentModel(agentId, problem.agentNames.get(agentId), problem.agentLevels.get(agentId), 
						problem.domains.get(problem.agentDomains.get(agentId)),problem.treeDepth);
			}else if(agentType.equals("ADOPT_K"))
			{
				agent=new ADOPT_K(agentId, problem.agentNames.get(agentId), problem.agentLevels.get(agentId), 
						problem.domains.get(problem.agentDomains.get(agentId)),Settings.settings.getADOPT_K());
			}else if(agentType.equals("SynAdopt2"))
			{
				agent=new AdoptAgentCycle_2(agentId, problem.agentNames.get(agentId), problem.agentLevels.get(agentId), 
						problem.domains.get(problem.agentDomains.get(agentId)));
			}else
			{
				agent=new AdoptAgentCycle(agentId, problem.agentNames.get(agentId), problem.agentLevels.get(agentId), 
						problem.domains.get(problem.agentDomains.get(agentId)));
			}
			Map<Integer, int[]> neighbourDomains=new HashMap<Integer, int[]>();
			Map<Integer, int[][]> constraintCosts=new HashMap<Integer, int[][]>();
			int[] neighbourAgentIds=problem.neighbourAgents.get(agentId);
			Map<Integer, Integer> neighbourLevels=new HashMap<Integer, Integer>();
			for(int i=0;i<neighbourAgentIds.length;i++)
			{
				neighbourDomains.put(neighbourAgentIds[i], problem.domains.get(problem.agentDomains.get(neighbourAgentIds[i])));
				neighbourLevels.put(neighbourAgentIds[i], problem.agentLevels.get(neighbourAgentIds[i]));
			}
			String[] neighbourAgentCostNames=problem.agentConstraintCosts.get(agentId);
			for(int i=0;i<neighbourAgentCostNames.length;i++)
			{
				constraintCosts.put(neighbourAgentIds[i], 
						CollectionUtil.toTwoDimension(problem.costs.get(neighbourAgentCostNames[i]), 
								problem.domains.get(problem.agentDomains.get(agentId)).length, 
								problem.domains.get(problem.agentDomains.get(neighbourAgentIds[i])).length));
			}
			
			agent.setNeibours(problem.neighbourAgents.get(agentId), problem.parentAgents.get(agentId), 
					problem.childAgents.get(agentId), problem.allParentAgents.get(agentId), 
					problem.allChildrenAgents.get(agentId), neighbourDomains, constraintCosts, neighbourLevels);
			
			agents.put(agent.getId(), agent);
			
			{
				String str="-----------"+agent.name+"-----------\n";
				str+="Parent: "+agent.parent+"\n";
				str+="Children: "+CollectionUtil.arrayToString(agent.children)+"\n";
				str+="AllParents: "+CollectionUtil.arrayToString(agent.allParents)+"\n";
				str+="AllChildren: "+CollectionUtil.arrayToString(agent.allChildren)+"\n";
				FileUtil.writeStringAppend(str, "dfsTree.txt");
			}
		}
	}
	
	public AgentCycle getAgent(int agentId)
	{
		if(agents.containsKey(agentId))
		{
			return agents.get(agentId);
		}else
		{
			return null;
		}
	}
	
	public Map<Integer, Integer> getAgentValues()
	{
		Map<Integer, Integer> agentValues=new HashMap<Integer, Integer>();
		for(AgentCycle agent : agents.values())
		{
			agentValues.put(agent.getId(), agent.getValue());
		}
		return agentValues;
	}
	
	public Map<Integer, AgentCycle> getAgents()
	{
		return this.agents;
	}
	
	public int getAgentCount()
	{
		return this.agents.size();
	}
	
	public void startAgents(MessageMailerCycle msgMailer)
	{
		for(AgentCycle agent : agents.values())
		{
			agent.setMessageMailer(msgMailer);
			agent.start();
		}
	}
	
	public void stopAgents()
	{
		for(AgentCycle agent : agents.values())
		{
			agent.stopRunning();
		}
	}
	
	public Object printResults(List<Map<String, Object>> results)
	{
		if(results.size()>0)
		{
			AgentCycle agent=null;
			for(Integer agentId : this.agents.keySet())
			{
				agent=this.agents.get(agentId);
				break;
			}
			return agent.printResults(results);
		}
		return null;
	}
	
	public String easyMessageContent(Message msg)
	{
		AgentCycle senderAgent=this.getAgent(msg.getIdSender());
		AgentCycle receiverAgent=this.getAgent(msg.getIdReceiver());
		return senderAgent.easyMessageContent(msg, senderAgent, receiverAgent);
	}
}
