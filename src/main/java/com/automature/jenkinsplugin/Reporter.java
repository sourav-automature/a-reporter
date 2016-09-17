package com.automature.jenkinsplugin;

import hudson.model.TaskListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.automature.spacetimeapiclient.SpacetimeClient;
import com.automature.spacetimeapiclient.exceptions.SpacetimeExecutionException;

public class Reporter {
	private SpacetimeClient spacetimeClient;
	private TaskListener listener;
	public Reporter(String url,String userName,String password,TaskListener listener) throws Exception {
	this.spacetimeClient=new SpacetimeClient(url, userName, password);
	this.listener=listener;
	}

	public String getTestPlanId(String testPlanPath) throws Exception
	{
		
		String product=testPlanPath.split("\\.")[0];
		String testplan=testPlanPath.split("\\.")[1];
		ArrayList<String> products=spacetimeClient.getProductList();
		for(String p:products){
			if(p.toLowerCase().startsWith(product.toLowerCase()+" ("))
			{
				product=p.substring(p.indexOf(" (")+2,p.indexOf(")"));
				break;
			}
		}

		ArrayList<String> testplans=spacetimeClient.getTestPlanListForProduct(product, "0");

		for(String t:testplans){
			if(t.toLowerCase().startsWith(testplan.toLowerCase()+" ("))
			{
				testplan=t.substring(t.indexOf(" (")+2,t.indexOf(")"));
				break;
			}
		}
		
		return testplan;
	}
	public String createTestCycle(String testplanId,String buildId) throws SpacetimeExecutionException
	{
			return spacetimeClient.testCycle_write(testplanId, "Created by Jenkins Build", new Date().toString(), new Date().toString(), "0", "0", buildId, "");
	}
	public void reportTestcase(String tcXMLPath,String testplanId,String buildId,String testCycleId)
	{
		try {
			listener.getLogger().println("Reporting testcase execution details");
	        listener.getLogger().println("TestCase XML Path : "+tcXMLPath);
	     	String topologySetId=spacetimeClient.getTopoSetsByTestPlanId(testplanId).get(0);
	     	listener.getLogger().println("Default Topology Set : "+topologySetId);
	     	String roleName=spacetimeClient.getRolesByTestPlanId(testplanId).get(0);
	     	listener.getLogger().println("Default TestSuite Role : "+roleName);
	     	roleName=roleName.substring(0, roleName.indexOf(" "));
	        if(tcXMLPath!=null)
	        {
	        File inputFile = new File(tcXMLPath);
	        DocumentBuilderFactory dbFactory 
	        = DocumentBuilderFactory.newInstance();
			    DocumentBuilder dBuilder;
					 dBuilder = dbFactory.newDocumentBuilder();
				     Document doc = dBuilder.parse(inputFile);
				     doc.getDocumentElement().normalize();
				     NodeList nList = doc.getElementsByTagName("testsuite");
				     for (int temp = 0; temp < nList.getLength(); temp++) {
				    	 Node nNode = nList.item(temp);
				    	 if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				    		 String testSuiteName=nNode.getNodeName();
				    		 String testCaseName="";
				    		 String description="";
				    		 String execTime="";
				    		 listener.getLogger().println("TestSuite Name :"+ nNode.getNodeName()+"\n\n");
				    		 
				    		 
				    		 NodeList children=nNode.getChildNodes();
				    		 for (int i = 0; i < children.getLength(); i++) {
				    			 Node child=children.item(i);
					    		 if (child.getNodeType() == Node.ELEMENT_NODE) {
					                 Element eElement = (Element) child;
					             
					                 
					    		 listener.getLogger().println("Reporting TestCase : "+eElement.getAttribute("classname"));
					    		 
					    		 //getting details of each testcase
					    		 testCaseName=eElement.getAttribute("classname");
					    		 description=eElement.getAttribute("name");
					    		 execTime=eElement.getAttribute("time");
					    		 if(execTime.equals(""))
					    			 execTime="0";

					    		 String status="pass";
					    		 
					    		 
					    		 String comment="";
					    		 NodeList childrenOfTc=child.getChildNodes();
					    		 for (int j = 0; j < childrenOfTc.getLength(); j++) {
						             try{ 
					    			 Node childOfTc=childrenOfTc.item(j);
					    			 Element eElement1 = (Element) childOfTc;
						    		 listener.getLogger().println("FailureMessage : "+eElement1.getTextContent());
						    		 comment=eElement1.getTextContent();
						    		 status="fail";
						             }catch(Exception e1){}
					    		 }
					    		 
					    		 try{
						    		 spacetimeClient.testExecutionDetails_write(testCaseName, description, testCycleId, status, buildId, execTime, new Date().toString(), testSuiteName, topologySetId.substring(topologySetId.indexOf("(")+1, topologySetId.indexOf(")")), roleName, comment.trim());
						    		 }catch(Exception e){listener.getLogger().println(e);}
							}
				    		 }
				    		 }
				    	 }
	        }
				} catch (Exception e1) {listener.getLogger().println(e1);
				}
	}
	
	public String reportBuild(String testplanId,String buildId) throws SpacetimeExecutionException, InterruptedException
	{
			return spacetimeClient.buildtag_write(testplanId,"jenkins_build_"+buildId);
	}
	
	public void checkVersion() throws Exception {
		spacetimeClient.checkVersion("1.1.6");
	}

}
