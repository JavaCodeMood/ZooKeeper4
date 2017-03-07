package com.zookeeper1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;

//����id����
public class IdMaker {
	private ZkClient client = null;
	private final String server;// ��¼�������ĵ�ַ
	private final String root;// ��¼���ڵ��·��
	private final String nodeName;// �ڵ������
	private volatile boolean running = false;   //����������״̬
	private ExecutorService cleanExector = null;

	// ɾ���ڵ�ļ���
	public enum RemoveMethod {
		NONE, IMMEDIATELY, DELAY

	}

	public IdMaker(String zkServer, String root, String nodeName) {

		this.root = root;
		this.server = zkServer;
		this.nodeName = nodeName;

	}
	
	//����������
	public void start() throws Exception{
		if(running){
			throw new Exception("server has stated....");
		}
		running = true;
		init();
	}
	
	//ֹͣ������
	public void stop() throws Exception{
		if(!running){
			throw new Exception("Server has stopped....");
		}
		running = false;
		freeResource();
	}
	
	//��ʼ��������
	private void init(){
		client = new ZkClient(server,5000,5000,new BytesPushThroughSerializer());
		cleanExector = Executors.newFixedThreadPool(10);
		try {
			client.createPersistent(root,true);
		} catch (ZkNodeExistsException e) {
			
		}
	}
	
	//�ͷŷ�����Դ
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
	
	//�������Ƿ���������
	private void checkRunning() throws Exception{
		if(!running){
			throw new Exception("���ȵ���start");
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
		//���ش����Ľڵ������
		final String ourPath = client.createPersistentSequential(fullNodePath, null);
		//�����ڵ��
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
