package com.cqu.bfsdpop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cqu.core.Agent;
import com.cqu.core.Infinity;
import com.cqu.core.Message;
import com.cqu.dpop.DPOPAgent;
import com.cqu.dpop.Dimension;
import com.cqu.dpop.MultiDimensionData;
import com.cqu.dpop.ReductDimensionResult;
import com.cqu.util.CollectionUtil;

public class BFSDPOPAgent extends Agent{
	
	public final static int TYPE_VALUE_MESSAGE=0;
	public final static int TYPE_UTIL_MESSAGE=1;
	
	public final static String KEY_TOTAL_COST="KEY_TOTAL_COST";
	
	private int disposedChildrenCount;
	private MultiDimensionData rawMDData;
	
	private Map<Integer, Integer> allParentValueIndexes;
	private int totalCost;
	
	private boolean[] isCrossNeighbours;
	{
		//表示消息不丢失
		QUEUE_CAPACITY=-1;
	}
	
	private Map<Integer, int[]> reductDimensionResultIndexList;
	private Map<Integer, List<Dimension>> dimensionList;
	
	public BFSDPOPAgent(int id, String name, int level, int[] domain) {
		super(id, name, level, domain);
		// TODO Auto-generated constructor stub
		disposedChildrenCount=0;
		allParentValueIndexes=new HashMap<Integer, Integer>();
		totalCost=0;
		reductDimensionResultIndexList=new HashMap<Integer, int[]>();
		dimensionList=new HashMap<Integer, List<Dimension>>();
	}
	
	@Override
	protected void initRun() {
		// TODO Auto-generated method stub
		super.initRun();
		
		this.isCrossNeighbours=new boolean[this.neighbours.length];
		for(int i=0;i<this.isCrossNeighbours.length;i++)
		{
			if(this.neighbours[i]==this.parent||CollectionUtil.indexOf(this.children, this.neighbours[i])!=-1)
			{
				this.isCrossNeighbours[i]=false;
			}else
			{
				this.isCrossNeighbours[i]=true;
			}
		}
		
		if(this.isRootAgent()==false)
		{
			rawMDData=this.computeLocalUtils();
		}
		if(this.isLeafAgent()==true)
		{
			if(rawMDData.isReductable(this.id+"")==true)
			{
				ReductDimensionResult result=rawMDData.reductDimension(this.id+"", ReductDimensionResult.REDUCT_DIMENSION_WITH_MIN);
				rawMDData=result.getMdData();
				dimensions=rawMDData.getDimensions();
				reductDimensionResultIndexList.put(this.id, result.getResultIndex());
			}
			sendUtilMessage(rawMDData);
		}
	}
	
	@Override
	protected void runFinished() {
		// TODO Auto-generated method stub
		super.runFinished();
		
		Map<String, Object> result=new HashMap<String, Object>();
		result.put(DPOPAgent.KEY_ID, this.id);
		result.put(DPOPAgent.KEY_NAME, this.name);
		result.put(DPOPAgent.KEY_VALUE, this.domain[valueIndex]);
		if(this.isRootAgent()==true)
		{
			result.put(DPOPAgent.KEY_TOTAL_COST, this.totalCost);
		}
		
		this.msgMailer.setResult(result);
		
		System.out.println("Agent "+this.name+" stopped!");
	}

	@Override
	public void printResults(List<Map<String, Object>> results) {
		// TODO Auto-generated method stub
		int totalCost=-1;
		Map<String, Object> result;
		for(int i=0;i<results.size();i++)
		{
			result=results.get(i);
			int id_=(Integer) result.get(DPOPAgent.KEY_ID);
			String name_=(String) result.get(DPOPAgent.KEY_NAME);
			int value_=(Integer) result.get(DPOPAgent.KEY_VALUE);
			if(result.containsKey(DPOPAgent.KEY_TOTAL_COST))
			{
				totalCost=(Integer) result.get(DPOPAgent.KEY_TOTAL_COST);
			}
			
			String displayStr="Agent "+name_+": id="+id_+" value="+value_;
			System.out.println(displayStr);
		}
		System.out.println("totalCost: "+Infinity.infinityEasy(totalCost));
	}

	@Override
	public String easyMessageContent(Message msg, Agent sender, Agent receiver) {
		// TODO Auto-generated method stub
		return "from "+sender.getName()+" to "+receiver.getName()+" type "+DPOPAgent.messageContent(msg);
	}
	
	public static String messageContent(Message msg)
	{
		switch (msg.getType()) {
		case DPOPAgent.TYPE_VALUE_MESSAGE:
		{
			int valueIndex=(Integer) msg.getValue();
			return "value["+valueIndex+"]";
		}
		case DPOPAgent.TYPE_UTIL_MESSAGE:
		{
			MultiDimensionData mdData=(MultiDimensionData) msg.getValue();
			return "util[dimensions="+mdData.getDimensions().toString()+"]";
		}
		default:
			return "unknown";
		}
	}

	@Override
	protected void disposeMessage(Message msg) {
		// TODO Auto-generated method stub
		int type=msg.getType();
		if(type==TYPE_VALUE_MESSAGE)
		{
			disposeValueMessage(msg);
		}else if(type==TYPE_UTIL_MESSAGE)
		{
			disposeUtilMessage(msg);
		}
	}

	@Override
	protected void messageLost(Message msg) {
		// TODO Auto-generated method stub
		System.out.println("Error occurs because no message can be lost!");
	}
	
	private void sendUtilMessage(MultiDimensionData multiDimentionalData)
	{
		if(this.isRootAgent()==true)
		{
			return;
		}
		Message utilMsg=new Message(this.id, this.parent, TYPE_UTIL_MESSAGE, multiDimentionalData);
		this.sendMessage(utilMsg);
	}
	
	private MultiDimensionData computeLocalUtils()
	{
		int dataLength=1;
		List<Dimension> dimensions=new ArrayList<Dimension>();
		List<Integer> relatedNodes=new ArrayList<Integer>();
		for(int i=0;i<neighbours.length;i++)
		{
			if(this.isCrossNeighbours[i]==false)
			{
				int parentId=neighbours[i];
				int dimensionSize=neighbourDomains.get(parentId).length;
				dimensions.add(new Dimension(parentId+"", dimensionSize, neighbourLevels.get(parentId)));
				dataLength=dataLength*dimensionSize;
				relatedNodes.add(parentId);
			}else
			{
				int crossNeighbourId=neighbours[i];
				//for crossing constraint edges, current agent just consider those whose ids are larger than its.
				if(this.id<crossNeighbourId)
				{
					int dimensionSize=neighbourDomains.get(crossNeighbourId).length;
					dimensions.add(new Dimension(crossNeighbourId+"", dimensionSize, neighbourLevels.get(crossNeighbourId), Integer.MAX_VALUE, 1));
					dataLength=dataLength*dimensionSize;
					relatedNodes.add(crossNeighbourId);
				}
			}
		}
		dimensions.add(new Dimension(this.id+"", this.domain.length, this.level, this.neighbours.length, relatedNodes.size()));
		dataLength=dataLength*this.domain.length;
		//set data
		int[] agentValueIndexes=new int[relatedNodes.size()+1];
		int[] data=new int[dataLength];
		int dataIndex=0;
		int curDimention=agentValueIndexes.length-1;
		while(dataIndex<data.length)
		{
			int costSum=0;
			for(int i=0;i<relatedNodes.size();i++)
			{
				//保证id小的为行，id大的为列
				if(this.id<relatedNodes.get(i))
				{
					costSum+=this.constraintCosts.get(relatedNodes.get(i))[agentValueIndexes[agentValueIndexes.length-1]][agentValueIndexes[i]];
				}else
				{
					costSum+=this.constraintCosts.get(relatedNodes.get(i))[agentValueIndexes[i]][agentValueIndexes[agentValueIndexes.length-1]];
				}
			}
			data[dataIndex]=costSum;
			
			agentValueIndexes[curDimention]+=1;
			while(agentValueIndexes[curDimention]>=dimensions.get(curDimention).getSize())
			{
				agentValueIndexes[curDimention]=0;
				curDimention-=1;
				if(curDimention==-1)
				{
					//all data has been set
					break;
				}
				agentValueIndexes[curDimention]+=1;
			}
			curDimention=agentValueIndexes.length-1;
			dataIndex++;
		}
		
		return new MultiDimensionData(dimensions, data);
	}
	
	private void disposeUtilMessage(Message msg)
	{
		if(this.isRootAgent()==true&&rawMDData==null)
		{
			rawMDData=(MultiDimensionData) msg.getValue();
		}else
		{
			rawMDData=rawMDData.mergeDimension((MultiDimensionData) msg.getValue());
		}
		rawMDData.getDimensions().get(rawMDData.indexOf(this.id+"")).setConstraintCountTotal(this.neighbours.length);
		
		disposedChildrenCount++;
		if(disposedChildrenCount>=this.children.length)
		{
			//所有子节点(包括伪子节点)的UtilMessage都已收集完毕，
			//则可以进行针对本节点的降维，将最终得到的UtilMessage再往父节点发送
			List<Dimension> dimenList=rawMDData.getDimensions();
			for(Dimension dimen : dimenList)
			{
				if(rawMDData.isReductable(dimen.getName())==true)
				{
					int agentId=Integer.parseInt(dimen.getName());
					ReductDimensionResult result=rawMDData.reductDimension(dimen.getName(), ReductDimensionResult.REDUCT_DIMENSION_WITH_MIN);
					reductDimensionResultIndexList.put(agentId, result.getResultIndex());
					rawMDData=result.getMdData();
					dimensionList.put(agentId, rawMDData.getDimensions());
				}
			}
			
			if(this.isRootAgent()==true)
			{
				this.totalCost=rawMDData.getData()[0];
				this.valueIndex=reductDimensionResultIndexList.get(this.id)[0];
				sendValueMessage(this.valueIndex);
				
				this.stopRunning();
			}else
			{
				this.sendUtilMessage(rawMDData);
			}
		}
	}
	
	private void sendValueMessage(HashMap<Integer, Integer> valueIndexes)
	{
		if(this.isLeafAgent()==true)
		{
			return;
		}
		for(int i=0;i<this.allChildren.length;i++)
		{
			Message valueMsg=new Message(this.id, this.allChildren[i], TYPE_VALUE_MESSAGE, valueIndexes);
			this.sendMessage(valueMsg);
		}
	}
	
	private void disposeValueMessage(Message msg)
	{
		allParentValueIndexes.put(msg.getIdSender(), (Integer) msg.getValue());
		if(allParentValueIndexes.size()>=this.allParents.length)
		{
			int[] periods=new int[dimensions.size()];
			for(int i=0;i<dimensions.size();i++)
			{
				int temp=1;
				for(int j=i+1;j<dimensions.size();j++)
				{
					temp*=dimensions.get(j).getSize();
				}
				periods[i]=temp;
			}
			int index=0;
			for(int i=0;i<periods.length;i++)
			{
				index+=allParentValueIndexes.get(Integer.parseInt(dimensions.get(i).getName()))*periods[i];
			}
			this.valueIndex=this.reductDimensionResultIndexes[index];
			this.sendValueMessage(this.valueIndex);
			
			this.stopRunning();
		}
	}
}
