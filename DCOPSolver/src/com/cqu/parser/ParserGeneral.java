package com.cqu.parser;

/*
 * 问题解析类，对XML文件进行解析，得到每个结点，以及每个结点之间的约束关系
 * 
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;

import com.cqu.bfsdpop.CrossEdgeAllocator;
import com.cqu.heuristics.LeastConnectedHeuristic;
import com.cqu.heuristics.MostConnectedHeuristic;
import com.cqu.heuristics.RandScoringHeuristic;
import com.cqu.util.CollectionUtil;
import com.cqu.util.XmlUtil;
import com.cqu.varOrdering.DFS.DFSgeneration;
import org.jdom2.Element;

import com.cqu.util.CollectionUtil;

public class ParserGeneral extends ContentParser{

	public ParserGeneral(Element root, String problemType) {
		super(root, problemType);
		// TODO Auto-generated constructor stub
	}
	
	// 根据树的类型进行相应的解析操作
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Problem parse(String treeGeneratorType)
	{
		Problem problem=new Problem();
		
		Document doc=XmlUtil.openXmlDocument(this.xmlPath);
		
		String docURL=doc.getBaseURI();
		String[] docName=docURL.split("/");
		System.out.println(docName[docName.length-1]);
		
		Element root=doc.getRootElement();
		
		if(parsePresentation(root.getChild(PRESENTATION))==false)
		{
			this.printMessage("parsePresentation()=false");
			return null;
		}
		
		Map<String, Integer> agentNameIds=parseAgents(root.getChild(AGENTS), problem); // 获取Agents
		// TODO Auto-generated method stub
		Map<String, Integer> agentNameIds=parseAgents(root.getChild(AGENTS), problem);
		if(agentNameIds==null)
		{
			this.printMessage("parseAgents() fails!");
			return null;
		}
		
		if(parseDomains(root.getChild(DOMAINS), problem)==false) // 获取结点的域值
		{
			this.printMessage("parseDomains() fails!");
			return null;
		}
		
		Map<String, Integer> variableNameAgentIds=parseVariables(root.getChild(VARIABLES), problem, agentNameIds); // 获取每个变量
		if(variableNameAgentIds==null)
		{
			this.printMessage("parseVariables() fails!");
			return null;
		}
		
		if(parseRelations(root.getChild(RELATIONS), problem)==false) // 获取每个结点之间的约束代价
		{
			this.printMessage("parseRelations() fails!");
			return null;
		}
		
		if(parseConstraints(root.getChild(CONSTRAINTS), problem, variableNameAgentIds)==false) // 获取每个结点之间的约束关系
		{
			this.printMessage("parseConstraints() fails!");
			return null;
		}
		/*
		 * 获取邻接矩阵
		 */
		
		TreeGenerator treeGenerator;
		//构造树的结构
		if(treeGeneratorType.equals(TreeGenerator.TREE_GENERATOR_TYPE_DFS))
		{
			/*for(Map.Entry<Integer, int[]> entry : problem.neighbourAgents.entrySet()){
				System.out.println(entry.getKey());
				for(int i = 0; i < entry.getValue().length; i++){
					System.out.print(entry.getValue()[i] + "  ");
				}
				System.out.println();
			}
			for(Integer nodeId : problem.neighbourAgents.keySet())
			{
				System.out.println(nodeId);
			}*/
			//已获得了图的邻接矩阵，对图进行深度遍历
			//treeGenerator=new DFSTree(problem.neighbourAgents); //将每个结点的邻居关系结点传递过去
			
			treeGenerator = new DFSgeneration (problem.neighbourAgents); //DFS树的构造方法
		}else
		{
			treeGenerator=new BFSTree(problem.neighbourAgents);
		}
		
		//固定了树的生成策略，只是暂时这样，需要进一步的改进
		DFSgeneration.setRootHeuristics(new MostConnectedHeuristic(problem));
		DFSgeneration.setNextNodeHeuristics(new MostConnectedHeuristic(problem));
		
		treeGenerator.generate(); //开始构造树的结构
		
		//构造完结构之后可以得到相应的结点关系
		
		problem.agentLevels=treeGenerator.getNodeLevels();
		for(Integer level:problem.agentLevels.values())
			if(problem.treeDepth<(level+1))problem.treeDepth=level+1;
		problem.parentAgents=treeGenerator.getParentNode();
		
		problem.childAgents=treeGenerator.getChildrenNodes();
		
		Map[] allParentsAndChildren=treeGenerator.getAllChildrenAndParentNodes();
		problem.allParentAgents=allParentsAndChildren[0];
		problem.allChildrenAgents=allParentsAndChildren[1];
		
		if(treeGeneratorType.equals(TreeGenerator.TREE_GENERATOR_TYPE_BFS))
		{
			CrossEdgeAllocator allocator=new CrossEdgeAllocator(problem);
			allocator.allocate();
			problem.crossConstraintAllocation=allocator.getConsideredConstraint();
		}
		//返回问题本身中结点之间的关系
		
		
		return problem;
	}

	@Override
	protected Problem parseContent(Problem problem) {
// TODO Auto-generated method stub
		Map<String, Integer> agentNameIds=parseAgents(root.getChild(AGENTS), problem);
		if(agentNameIds==null)
		{
			this.printMessage("parseAgents() fails!");
			return null;
		}
		
		if(parseDomains(root.getChild(DOMAINS), problem)==false) // 获取结点的域值
		{
			this.printMessage("parseDomains() fails!");
			return null;
		}
		
		Map<String, Integer> variableNameAgentIds=parseVariables(root.getChild(VARIABLES), problem, agentNameIds); // 获取每个变量
		if(variableNameAgentIds==null)
		{
			this.printMessage("parseVariables() fails!");
			return null;
		}
		
		if(parseRelations(root.getChild(RELATIONS), problem)==false) // 获取每个结点之间的约束代价
		{
			this.printMessage("parseRelations() fails!");
			return null;
		}
		
		if(parseConstraints(root.getChild(CONSTRAINTS), problem, variableNameAgentIds)==false) // 获取每个结点之间的约束关系
		{
			this.printMessage("parseConstraints() fails!");
			return null;
		}
		return problem;
	}
	
	
	
	private boolean parsePresentation(Element element)
	{
		if(element==null)
		{
			return false;
		}
		problemType=element.getAttributeValue(TYPE);
		if(problemType.equals(TYPE_DCOP)==true||problemType.equals(TYPE_GRAPH_COLORING)==true)
		{
			return true;
		}
		return true;
	}
	
=======
		
		return problem;
	}

	private Map<String, Integer> parseAgents(Element element, Problem problem)
	{
		if(element==null)
		{
			return null;
		}
		int nbAgents=-1;
		try {
			nbAgents=Integer.parseInt(element.getAttributeValue(NBAGENTS));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		List<Element> elementList=element.getChildren();
		if(nbAgents!=elementList.size())
		{
			printMessage("nbAgents!=elementList.size()");
			return null;
		}
		Map<String, Integer> agentNameIds=new HashMap<String, Integer>();
		try{
			for(int i=0;i<nbAgents;i++)
			{
				int id=Integer.parseInt(elementList.get(i).getAttributeValue(ID));
				String name=elementList.get(i).getAttributeValue(NAME);
				
				
				agentNameIds.put(name, id);
				problem.agentNames.put(id, name);
			}
		}catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return agentNameIds;
	}
	
	private boolean parseDomains(Element element, Problem problem)
	{
		if(element==null)
		{
			return false;
		}
		int nbDomains=-1;
		try {
			nbDomains=Integer.parseInt(element.getAttributeValue(NBDOMAINS));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		List<Element> elementList=element.getChildren();
		if(nbDomains!=elementList.size())
		{
			printMessage("nbDomains!=elementList.size()");
			return false;
		}
		
		for(int i=0;i<nbDomains;i++)
		{
			int[] domain=parseFromTo(elementList.get(i).getValue());
			int nbValues=Integer.parseInt(elementList.get(i).getAttributeValue(NBVALUES));
			if(nbValues!=domain.length)
			{
				printMessage("nbValues!=domain.length");
				return false;
			}
			problem.domains.put(elementList.get(i).getAttributeValue(NAME), domain);
		}
		
		return true;
	}
	
	private int[] parseFromTo(String fromToStr)
	{
		int from=-1;
		int to=-1;
		String separator="..";
		
		fromToStr=fromToStr.trim();
		int sepIndex=fromToStr.indexOf(separator);
		from=Integer.parseInt(fromToStr.substring(0, sepIndex));
		to=Integer.parseInt(fromToStr.substring(sepIndex+separator.length()));
		
		int[] ret=new int[to-from+1];
		for(int i=0;i<ret.length;i++)
		{
			ret[i]=i+from;
		}
		
		return ret;
	}
	
	private Map<String, Integer> parseVariables(Element element, Problem problem, Map<String, Integer> agentNameIds)
	{
		if(element==null)
		{
			return null;
		}
		int nbVariables=-1;
		try {
			nbVariables=Integer.parseInt(element.getAttributeValue(NBVARIABLES));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		List<Element> elementList=element.getChildren();
		if(nbVariables!=elementList.size())
		{
			printMessage("nbVariables!=elementList.size()");
			return null;
		}
		if(nbVariables!=problem.agentNames.size())
		{
			printMessage("nbVariables!=problem.agentCount，要求每个agent中只包含一个variable");
			return null;
		}
		Map<String, Integer> variableNameAgentIds=new HashMap<String, Integer>();
		for(int i=0;i<nbVariables;i++)
		{
			int agentId=agentNameIds.get(elementList.get(i).getAttributeValue(AGENT));
			variableNameAgentIds.put(elementList.get(i).getAttributeValue(NAME), agentId);
			problem.agentDomains.put(agentId, elementList.get(i).getAttributeValue(DOMAIN));
			problem.variables.put(elementList.get(i).getAttributeValue(NAME), agentId);
		}
		return variableNameAgentIds;
	}
	
	private boolean parseRelations(Element element, Problem problem)
	{
		if(element==null)
		{
			return false;
		}
		int nbRelations=-1;
		try {
			nbRelations=Integer.parseInt(element.getAttributeValue(NBRELATIONS));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		List<Element> elementList=element.getChildren();
		if(nbRelations!=elementList.size())
		{
			printMessage("nbRelations!=elementList.size()");
			return false;
		}
		
		for(int i=0;i<nbRelations;i++)
		{
			int arity=Integer.parseInt(elementList.get(i).getAttributeValue(ARITY));
			if(arity!=2)
			{
				printMessage("arity!=2");
				return false;
			}
			int[] cost=null;
			if(this.problemType.equals(TYPE_GRAPH_COLORING))
			{
				cost=parseConstraintCostDisCSP(problem.domains.values().iterator().next(), elementList.get(i).getValue());
			}else
			{
				cost=parseConstraintCost(elementList.get(i).getValue());
				int nbTuples=Integer.parseInt(elementList.get(i).getAttributeValue(NBTUPLES));
				if(nbTuples!=cost.length)
				{
					printMessage("nbValues!=cost length");
					return false;
				}
			}
			problem.costs.put(elementList.get(i).getAttributeValue(NAME), cost);
		}
		return true;
	}
	
	private int[] parseConstraintCost(String costStr)
	{
		String[] items=costStr.split("\\|");
		String[] costParts=new String[items.length];
		Map<String, Integer> valuePairParts=new HashMap<String, Integer>();
		int index=0;
		for(int i=0;i<items.length;i++)
		{
			index=items[i].indexOf(':');
			costParts[i]=items[i].substring(0, index);
			valuePairParts.put(items[i].substring(index+1), i);
		}
		Object[] valuePairPartsKeyArray=valuePairParts.keySet().toArray();
		Arrays.sort(valuePairPartsKeyArray);
		
		int[] costs=new int[items.length];
		for(int i=0;i<items.length;i++)
		{
			costs[i]=Integer.parseInt(costParts[valuePairParts.get(valuePairPartsKeyArray[i])]);
		}
		return costs;
	}
	
	private int[] parseConstraintCostDisCSP(int[] domain, String costStr)
	{
		String[] items=costStr.split("\\|");
		int[] costs=new int[domain.length*domain.length];
		for(int i=0;i<domain.length;i++)
		{
			for(int j=0;j<domain.length;j++)
			{
				if(CollectionUtil.indexOf(items, domain[i]+" "+domain[j])!=-1)
				{
					costs[i*domain.length+j]=1;
				}else
				{
					costs[i*domain.length+j]=0;
				}
			}
		}
		return costs;
	}
	
	//解析每个结点之间的约束关系
	
	private boolean parseConstraints(Element element, Problem problem, Map<String, Integer> variableNameAgentIds)
	{
		if(element==null)
		{
			return false;
		}
		int nbConstraints=-1;
		try {
			nbConstraints=Integer.parseInt(element.getAttributeValue(NBCONSTRAINTS));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		List<Element> elementList=element.getChildren();
		if(nbConstraints!=elementList.size())
		{
			printMessage("nbConstraints!=elementList.size()");
			return false;
		}
		
		//图的邻接表存储结构
		Map<Integer, List<Integer>> neighbourAgents=new HashMap<Integer, List<Integer>>();
		Map<Integer, Map<Integer, String>> neighbourConstraintCosts=new HashMap<Integer, Map<Integer, String>>();
		for(Integer agentId : problem.agentNames.keySet())
		{
			neighbourAgents.put(agentId, new ArrayList<Integer>());
			neighbourConstraintCosts.put(agentId, new HashMap<Integer, String>());
		}
		
		for(int i=0;i<nbConstraints;i++)
		{
			int arity=Integer.parseInt(elementList.get(i).getAttributeValue(ARITY));
			if(arity!=2)
			{
				printMessage("arity!=2");
				return false;
			}
			String[] constraintedParts=elementList.get(i).getAttributeValue(SCOPE).split(" ");
			int leftAgentId=variableNameAgentIds.get(constraintedParts[0]);
			int rightAgentId=variableNameAgentIds.get(constraintedParts[1]);
			if(leftAgentId>rightAgentId)//保证id小的agent放在行的位置，id大的放在列的位置
			{
				int temp=leftAgentId;
				leftAgentId=rightAgentId;
				rightAgentId=temp;
			}
			neighbourAgents.get(leftAgentId).add(rightAgentId);
			neighbourAgents.get(rightAgentId).add(leftAgentId);
			neighbourConstraintCosts.get(leftAgentId).put(rightAgentId, elementList.get(i).getAttributeValue(REFERENCE));
			neighbourConstraintCosts.get(rightAgentId).put(leftAgentId, elementList.get(i).getAttributeValue(REFERENCE));
		}
		for(Integer agentId : problem.agentNames.keySet())
		{
			List<Integer> temp=neighbourAgents.get(agentId);
			Integer[] buff=new Integer[temp.size()];
			problem.neighbourAgents.put(agentId, CollectionUtil.toInt(temp.toArray(buff)));
			
			String[] costNames=new String[temp.size()];
			for(int i=0;i<buff.length;i++)
			{
				costNames[i]=neighbourConstraintCosts.get(agentId).get(buff[i]);
			}
			problem.agentConstraintCosts.put(agentId, costNames);
		}
		
		return true;
	}
}
