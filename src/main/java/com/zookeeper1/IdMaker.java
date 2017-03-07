package com.zookeeper1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;

//生成id方法
public class IdMaker {
	private ZkClient client = null;
	private final String server;// 记录服务器的地址
	private final String root;// 记录父节点的路径
	private final String nodeName;// 节点的名称
	private volatile boolean running = false;   //服务器运行状态
	private ExecutorService cleanExector = null;

	// 删除节点的级别
	public enum RemoveMethod {
		NONE, IMMEDIATELY, DELAY

	}

	public IdMaker(String zkServer, String root, String nodeName) {

		this.root = root;
		this.server = zkServer;
		this.nodeName = nodeName;

	}
	
	//启动服务器
	public void start() throws Exception{
		if(running){
			throw new Exception("server has stated....");
		}
		running = true;
		init();
	}
	
	//停止服务器
	public void stop() throws Exception{
		if(!running){
			throw new Exception("Server has stopped....");
		}
		running = false;
		freeResource();
	}
	
	//初始化服务器
	private void init(){
		client = new ZkClient(server,5000,5000,new BytesPushThroughSerializer());
		cleanExector = Executors.newFixedThreadPool(10);
		try {
			client.createPersistent(root,true);
		} catch (ZkNodeExistsException e) {
			
		}
	}
	
	//释放服务资源
	private void freeResource(){
		cleanExector.shutdown();
		try {
			cleanExector.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			cleanExector = null;
		}
		
		if(client != null){
			client.close();
			client = null;
		}
	}
	
	//检测服务是否正在运行
	private void checkRunning() throws Exception{
		if(!running){
			throw new Exception("请先调用start");
		}
	}
	
	private String ExtractId(String str){
		int index = str.lastIndexOf(nodeName);
		if(index >= 0){
			index += nodeName.length();
			return index <= str.length()?str.substring(index):"";
		}
		return str;
	}
	
	public String generateId(RemoveMethod removeMethod) throws Exception{
		checkRunning();
		final String fullNodePath =root.concat("/").concat(nodeName);
		//返回创建的节点的名称
		final String ourPath = client.createPersistentSequential(fullNodePath, null);
		//创建节点后
		if(removeMethod.equals(RemoveMethod.IMMEDIATELY)){
			client.delete(ourPath);
		}else if(removeMethod.equals(RemoveMethod.DELAY)){
			cleanExector.execute(new Runnable(){
				public void run(){
					client.delete(ourPath);
				}
			});
		}
		return ExtractId(ourPath);
	}

}
