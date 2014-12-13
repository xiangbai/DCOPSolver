package com.cqu.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import com.cqu.bfsdpop.CrossEdgeAllocator;
import com.cqu.heuristics.MostConnectedHeuristic;
import com.cqu.main.DOTrenderer;
import com.cqu.util.CollectionUtil;
import com.cqu.util.XmlUtil;
import com.cqu.varOrdering.DFS.DFSgeneration;


/*
 * 将问题XML解析成.dot格式，调用DOTrenderer类，将其约束图展现出来
 */
public class XCSPparser {
	public static final String ID="id";
	public static final String NAME="name";
	public static final String TYPE="type";
	public static final String ARITY="arity";
	
	public static final String INSTANCE="instance";
	
	public static final String PRESENTATION="presentation";
	
	public static final String AGENTS="agents";
	public static final String NBAGENTS="nbAgents";
	public static final String AGENT="agent";
	
	public static final String DOMAINS="domains";
	public static final String DOMAIN="domain";
	public static final String NBDOMAINS="nbDomains";
	public static final String NBVALUES="nbValues";
	
	public static final String VARIABLES="variables";
	public static final String NBVARIABLES="nbVariables";
	
	public static final String CONSTRAINTS="constraints";
	public static final String NBCONSTRAINTS="nbConstraints";
	public static final String SCOPE="scope";
	public static final String REFERENCE="reference";
	
	public static final String RELATIONS="relations";
	public static final String NBRELATIONS="nbRelations";
	public static final String NBTUPLES="nbTuples";
	
	
	public static final String TYPE_DCOP="DCOP";
	public static final String TYPE_GRAPH_COLORING="DisCSP";
	
	
	private String problemType;
	private Problem problem;
	
	protected Element root;
	

	/** Construct function
	 * @param path    the XML file path
	 * @throws Exception 
	 */
	public XCSPparser (String path) throws Exception
	{
		this.problem = new Problem();
		Document doc=XmlUtil.openXmlDocument(path);
		
		String docURL=doc.getBaseURI();
		String[] docName=docURL.split("/");
		System.out.println(docName[docName.length-1]);
		
		
		this.root = doc.getRootElement();
	}
	/*
	 * 对文件解析出来，以确定每个结点之间的关系
	 */
	public void parse (String treeGeneratorType)
	{
		
		if(parsePresentation(this.root.getChild(PRESENTATION))==false)
		{
			this.printMessage("parsePresentation()=false");
			return;
		}
		Map<String, Integer> agentNameIds=parseAgents(this.root.getChild(AGENTS)); // 获取Agents
		if(agentNameIds==null)
		{
			this.printMessage("parseAgents() fails!");
			return;
		}
		
		if(parseDomains(this.root.getChild(DOMAINS))==false) // 获取结点的域值
		{
			this.printMessage("parseDomains() fails!");
			return;
		}
		
		Map<String, Integer> variableNameAgentIds=parseVariables(this.root.getChild(VARIABLES), agentNameIds); // 获取每个变量
		if(variableNameAgentIds==null)
		{
			this.printMessage("parseVariables() fails!");
			return;
		}
		
		if(parseRelations(this.root.getChild(RELATIONS))==false) // 获取每个结点之间的约束代价
		{
			this.printMessage("parseRelations() fails!");
			return;
		}
		
		if(parseConstraints(this.root.getChild(CONSTRAINTS), variableNameAgentIds)==false) // 获取每个结点之间的约束关系
		{
			this.printMessage("parseConstraints() fails!");
			return;
		}
		
		TreeGenerator treeGenerator;
		//构造树的结构
		if(treeGeneratorType.equals(TreeGenerator.TREE_GENERATOR_TYPE_DFS))
		{
			//已获得了图的邻接矩阵，对图进行深度遍历
			//treeGenerator=new DFSTree(problem.neighbourAgents); //将每个结点的邻居关系结点传递过去
			
			treeGenerator = new DFSgeneration (this.problem.neighbourAgents); //DFS树的构造方法
		}else
		{
			treeGenerator=new BFSTree(this.problem.neighbourAgents);
		}
		
		//固定了树的生成策略，只是暂时这样，需要进一步的改进
		DFSgeneration.setRootHeuristics(new MostConnectedHeuristic(this.problem));
		DFSgeneration.setNextNodeHeuristics(new MostConnectedHeuristic(this.problem));
		
		treeGenerator.generate(); //开始构造树的结构
		
		//构造完结构之后可以得到相应的结点关系
		
		this.problem.agentLevels=treeGenerator.getNodeLevels();
		for(Integer level:this.problem.agentLevels.values())
			if(this.problem.treeDepth<(level+1))this.problem.treeDepth=level+1;
		this.problem.parentAgents=treeGenerator.getParentNode();
		
		this.problem.childAgents=treeGenerator.getChildrenNodes();
		
		Map[] allParentsAndChildren=treeGenerator.getAllChildrenAndParentNodes();
		this.problem.allParentAgents=allParentsAndChildren[0];
		this.problem.allChildrenAgents=allParentsAndChildren[1];
		
		if(treeGeneratorType.equals(TreeGenerator.TREE_GENERATOR_TYPE_BFS))
		{
			CrossEdgeAllocator allocator=new CrossEdgeAllocator(this.problem);
			allocator.allocate();
			this.problem.crossConstraintAllocation=allocator.getConsideredConstraint();
		}
		//返回问题本身中结点之间的关系
		
		
	}
	
	private boolean parsePresentation(Element element)
	{
		if(element==null)
		{
			return false;
		}
		problemType=element.getAttributeValue(TYPE);
		if(problemType == null)
		{
			problemType = TYPE_DCOP;
		}
		if(problemType.equals(TYPE_DCOP)==true||problemType.equals(TYPE_GRAPH_COLORING)==true)
		{
			return true;
		}
		
		return true;
	}
	private Map<String, Integer> parseAgents(Element element)
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
				this.problem.agentNames.put(id, name);
			}
		}catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return agentNameIds;
	}
	private boolean parseDomains(Element element)
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
			this.problem.domains.put(elementList.get(i).getAttributeValue(NAME), domain);
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
	
	private Map<String, Integer> parseVariables(Element element,Map<String, Integer> agentNameIds)
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
		if(nbVariables!=this.problem.agentNames.size())
		{
			printMessage("nbVariables!=problem.agentCount，要求每个agent中只包含一个variable");
			return null;
		}
		Map<String, Integer> variableNameAgentIds=new HashMap<String, Integer>();
		for(int i=0;i<nbVariables;i++)
		{
			int agentId=agentNameIds.get(elementList.get(i).getAttributeValue(AGENT));
			variableNameAgentIds.put(elementList.get(i).getAttributeValue(NAME), agentId);
			this.problem.agentDomains.put(agentId, elementList.get(i).getAttributeValue(DOMAIN));
			this.problem.variables.put(elementList.get(i).getAttributeValue(NAME), agentId);
		}
		return variableNameAgentIds;
	}
	
	private boolean parseRelations(Element element)
	{
		if(element == null)
		{
			return false ;
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
				cost=parseConstraintCostDisCSP(this.problem.domains.values().iterator().next(), elementList.get(i).getValue());
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
			this.problem.costs.put(elementList.get(i).getAttributeValue(NAME), cost);
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
	private boolean parseConstraints(Element element,Map<String, Integer> variableNameAgentIds)
	{
		if(element == null)
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
		for(Integer agentId : this.problem.agentNames.keySet())
		{
			List<Integer> temp=neighbourAgents.get(agentId);
			Integer[] buff=new Integer[temp.size()];
			this.problem.neighbourAgents.put(agentId, CollectionUtil.toInt(temp.toArray(buff)));
			
			String[] costNames=new String[temp.size()];
			for(int i=0;i<buff.length;i++)
			{
				costNames[i]=neighbourConstraintCosts.get(agentId).get(buff[i]);
			}
			this.problem.agentConstraintCosts.put(agentId, costNames);
		}
		
		return true;
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 1) {
			System.out.println("ERROR: Takes exactly one parameter: the path to the input XCSP file.");
			return;
		}
		new DOTrenderer ("Constraint graph", XCSPparser.toDOT(args[0]));  //获取约束图
	    System.out.println(XCSPparser.toDOT(args[0]));
	}

	
	public Problem getProblem()
	{
		return this.problem;
	}
	
	/** Returns the constraint graph in DOT format
	 * @param root 	the XCSP instance element, the problem 
	 * @return 		a String representation of the constraint graph in DOT format
	 * @throws Exception 
	 */
	public static String toDOT (String path) throws Exception {
		StringBuilder out = new StringBuilder ("graph {\n\tnode [shape = \"circle\"];\n");

		// Print the agents, with their respective variables
		// 获取XML文件
		// 构建解析类对象
		// 通过这个构造函数，最终将问题提取出来，从而获取约束图，以保存每个agent之间的约束关系
		//XCSPparser<AddableInteger, AddableInteger> parser = new XCSPparser<AddableInteger, AddableInteger> (null, root, false);
		
		/*
		 * 需要知道每个结点，以及每个结点直接的关系
		 * */
		
		
		// 需要将Agent与variable进关联起来，使得一个Agent控制一个variable或者多个Agent
		// 需更近一步来做代码的修改
		XCSPparser parser = new XCSPparser(path);
		
		for (String agent : parser.getAgents()) { // 获取Agent
			out.append("\tsubgraph cluster_" + agent + " {\n");
			out.append("\t\tlabel = " + agent + ";\n");
			
			// Add an invisible variable if the agent owns no variable, so that it is still displayed
			// 该agent所属的变量数目为空时
			//if (parser.getNbrVars() == 0) 
				//out.append("\t\t" + new Object().hashCode() + " [shape=\"none\", label=\"\"];\n");
			
			for (String var : parser.getVariables(agent)) { // 获取变量
				out.append("\t\t" + var);

				// If var if a facade variable, fill it
				/*for (String neigh : parser.getNeighborVars(var)) { // 获取邻居结点
					if (! agent.equals(parser.getOwner(neigh))) {  
						out.append(" [style=\"filled\"]");
						break;
					}
				}*/

				out.append(";\n");
			}
			out.append("\t}\n");
		}
		out.append("\n");

		// Print the variables with no specified owner
		/*for (String anonymVar : parser.getVariables(null)) // 获取变量名
			out.append("\t" + anonymVar + ";\n");
		out.append("\n");*/

		// Print the neighborhoods
		for (String var : parser.getVariables()) // variables with an owner
			for (String neighbor : parser.getNeighborVars(var)) 
				if (var.compareTo(neighbor) >= 0) 
					out.append("\t" + var + " -- " + neighbor + ";\n");
		
		/*for (String var : parser.getVariables(null)) // variables with no specified owner
			for (String neighbor : parser.getNeighborVars(var, true)) 
				if (var.compareTo(neighbor) >= 0) 
					out.append("\t" + var + " -- " + neighbor + ";\n");*/

		out.append("}\n");
		return out.toString();
	}
	/*
	 * 获取问题中结点之间的属性关系
	 * (non-Javadoc)
	 * @see com.cqu.core.DCOPProblemInterface#getAgent()
	 */
	public String getAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> getAgents() {
		// TODO Auto-generated method stub
		Set<String> agents = new HashSet<String> ();
		
		for (Element var : (List<Element>) this.root.getChild("agents").getChildren()) 
			agents.add(var.getAttributeValue("name"));
		
		return agents;
	}

	public Map<String, String> getOwners() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getOwner(String var) {
		// TODO Auto-generated method stub
		for (Element varElmt : (List<Element>) root.getChild("variables").getChildren()) 
			if (varElmt.getAttributeValue("name").equals(var)) 
				return varElmt.getAttributeValue("agent");

		// The variable was not found
		assert false : "Unknown variable '" + var + "'";
		return null;
	}

	public boolean setOwner(String var, String owner) {
		// TODO Auto-generated method stub
		for (Element varElmt : (List<Element>) root.getChild("variables").getChildren()) {
			if (varElmt.getAttributeValue("name").equals(var)) {
				varElmt.setAttribute("agent", owner);
				assert this.getAgents().contains(owner) : "Unknown agent " + owner;
				return true;
			}
		}
		
		return false;
	}

	public Set<String> getAllVars() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> getVariables() {
		// TODO Auto-generated method stub
		Set<String> out = new HashSet<String> ();

		for (Element varElmt : (List<Element>) root.getChild("variables").getChildren()) 
			if (! "random".equals(varElmt.getAttributeValue("type"))) // ignore random variables
				out.add(varElmt.getAttributeValue("name"));

		return out;
	}

	//确定每个agent所属的variable
	public Set<String> getVariables(String owner) {
		// TODO Auto-generated method stub
		Set<String> out = new HashSet<String> ();

		if (owner != null) {
			for (Element var : (List<Element>) this.root.getChild("variables").getChildren()) 
				if (owner.equals(var.getAttributeValue("agent"))) 
					out.add(var.getAttributeValue("name"));

		} else 
			for (Element var : (List<Element>) this.root.getChild("variables").getChildren()) 
				if (var.getAttributeValue("agent") == null) 
					out.add(var.getAttributeValue("name"));

		return out;
	}

	public HashSet<String> getNeighborVars(String var)
	{
		HashSet<String> out = new HashSet<String> ();

		LinkedList<String> pending = new LinkedList<String> (); // variable(s) whose direct neighbors will be returned
		pending.add(var);
		HashSet<String> done = new HashSet<String> ();
		do {
			// Retrieve the next pending variable
			String var2 = pending.poll();
			if (! done.add(var2)) // we have already processed this variable
				continue;

			// Go through the list of constraint scopes
			for (Element constraint : (List<Element>) root.getChild("constraints").getChildren()) {

				// Check if var2 is in the scope
				String[] scope = constraint.getAttributeValue("scope").trim().split("\\s+");
				Arrays.sort(scope);
				if (Arrays.binarySearch(scope, var2) >= 0) {

					// Go through the list of variables in the scope
					for (String neighbor : scope) {

						// Check if the neighbor is random
						if (! this.isRandom(neighbor)) // not random
							out.add(neighbor);

						
					}
				}
			}
		} while (! pending.isEmpty());

		// Remove the variable itself from its list of neighbors
		out.remove(var);

		return out;
	}
	/** Returns whether the input variable is defined as a random variable
	 * @param var 	the name of the variable
	 * @return 		\c true if the input variable is a random variable, \c false if not or if the variable is unknown
	 */
	public boolean isRandom (String var) {

		for (Element varElmt : (List<Element>) root.getChild("variables").getChildren()) 
			if (var.equals(varElmt.getAttributeValue("name"))) 
				return new String ("random").equals(varElmt.getAttributeValue("type"));

		// Variable not found
		return false;
	}
	private void printMessage(String msg)
	{
		System.out.println(msg);
	}
}
