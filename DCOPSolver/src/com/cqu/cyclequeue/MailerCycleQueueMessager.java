package com.cqu.cyclequeue;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.cqu.core.Message;
import com.cqu.core.ThreadEx;

public abstract class MailerCycleQueueMessager extends ThreadEx{
	
    private LinkedList<Message> msgQueue;
    
    protected AtomicBoolean cycleBegin;
    protected AtomicBoolean cycleEnd;
    protected AtomicInteger cycleEndCount;
    protected AtomicInteger totalAgentCount;
    
    protected int cycleCount;
	
	public MailerCycleQueueMessager(String threadName, int totalAgentCount) {
		super(threadName);
		// TODO Auto-generated constructor stub
		this.msgQueue=new LinkedList<Message>();
		this.totalAgentCount=new AtomicInteger(totalAgentCount);
		this.cycleBegin=new AtomicBoolean(false);
		this.cycleEnd=new AtomicBoolean(false);
		this.cycleEndCount=new AtomicInteger(this.totalAgentCount.get());
		
		this.cycleCount=0;
	}
	
	/**
	 * 添加消息至缓冲队列，若已满，则丢弃消息；
	 * 但是terminate消息不会被丢弃，而是丢弃队列尾的一个非terminate消息并把它加上;
	 * 如果队列尾为terminate消息，则前移，空出一个位置加上现在的terminate消息
	 * @param msg
	 */
	public void addMessage(Message msg)
	{
		synchronized (msgQueue) {
			msgQueue.add(msg);
		}
	}

	@Override
	protected void runProcess() {
		// TODO Auto-generated method stub
		initRun();
		while(isRunning==true)
		{
			//wait for all agents notify arrivals and then put all messages to agents
			synchronized (cycleEndCount) {
				while(cycleEndCount.get()<totalAgentCount.get())
				{
					try {
						cycleEndCount.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Thread.currentThread().interrupt();
						//当检测到中断消息时，认为是结束线程的通知，所以直接跳出循环
						break;
					}
				}
			}
			if(cycleEndCount.get()>=totalAgentCount.get())
			{
				cycleEndCount.set(0);
				
				cycleBegin.set(false);//close entrance
				synchronized (cycleEnd) {
					cycleEnd.set(true);//open exit
					cycleEnd.notifyAll();
				}
				
				while(msgQueue.isEmpty()==false)
				{
					Message msg=msgQueue.removeFirst();
					if(msg!=null)
					{
						disposeMessage(msg);
					}
				}
				
				cycleEnd.set(false);//close exit
				synchronized (cycleBegin) {
					cycleBegin.set(true);//open entrance
					cycleBegin.notifyAll();
				}
				cycleCount++;
				System.out.println("cycle: "+cycleCount);
			}
		}
		runFinished();
	}
	
	protected void initRun(){}
	
	protected void runFinished(){}
	
	protected abstract void disposeMessage(Message msg);
	
	protected abstract void messageLost(Message msg);

}
