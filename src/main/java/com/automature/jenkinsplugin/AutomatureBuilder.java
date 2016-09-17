package com.automature.jenkinsplugin;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.PluginWrapper;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.automature.spacetimeapiclient.SpacetimeClient;
import com.automature.spacetimeapiclient.exceptions.SpacetimeExecutionException;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutomatureBuilder extends Builder implements SimpleBuildStep {


    private final String testPlanPath;
    private final String tcPath;
    @DataBoundConstructor
    public AutomatureBuilder(String tcPath,String testPlanPath) {
        
        this.tcPath=tcPath;
        this.testPlanPath=testPlanPath;
    }
    
    public String getTestPlanPath() {
		return testPlanPath;
	}
    
    public String getTcPath() {
		return tcPath;
	}
    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

     try {
		Reporter r=new Reporter(getDescriptor().getUrl(), getDescriptor().getUserName(), getDescriptor().getPassword(), listener);
		
		String testplan=r.getTestPlanId(testPlanPath);
		String buildId=r.reportBuild(testplan, build.getId());
		String testCycleId=r.createTestCycle(testplan,buildId);
		r.reportTestcase(workspace.toURI().getPath()+tcPath,testplan,buildId,testCycleId);
		
	} catch (Exception e) {
		e.printStackTrace();
		listener.getLogger().println("Erroe while invoking Automature Reporting : ["+e.getMessage()+"] :: Check build steps for TestPlan Path or TestCase XML file path.");
	}
    }
   
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private  String url;
        
        private  String userName;
        
        private  String password;

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
//            if (value.length() == 0)
//                return FormValidation.error("Please set a name");
//            if (value.length() < 4)
//                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Invoke Automature Reporting";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        	if(!formData.getString("url").startsWith("http://"))
            	url = "http://"+formData.getString("url");
            else
            	url = formData.getString("url");
            userName=formData.getString("userName");
            password=formData.getString("password");;
            save();
            return super.configure(req,formData);
        }

        public String getUrl() {
			return url;
		}
        
        public String getUserName() {
			return userName;
		}
        public String getPassword() {
			return password;
		}
        @Override
        protected PluginWrapper getPlugin() {
        	return super.getPlugin();
        }
        public FormValidation doTestConnection(@QueryParameter("url") final String url,@QueryParameter("userName") final String userName,@QueryParameter("password") final String password)
        {
        	try{
        		new Reporter(url, userName, password, null).checkVersion();
        		
        		return FormValidation.ok("Successfully connected to server");
        	}
        	catch(Exception e){
//        		if(e.getMessage().toLowerCase().equals("Spacetime-client ERROR in Response from Spacetime Server.".toLowerCase()))
//        			return FormValidation.error("Error while connecting to server : check credentials ");
//        		else
        			return FormValidation.error("Error while connecting to server : "+e.getMessage());
        	}
        }
    }
}
