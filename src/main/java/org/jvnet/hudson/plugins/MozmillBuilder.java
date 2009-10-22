package org.jvnet.hudson.plugins;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link MozmillBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 */
public class MozmillBuilder extends Builder {

    private final String tests;
    private final String wrapper;
    private final String logfile;
    private final boolean showall;
    private final boolean showerrors;
    private final String port;

    @DataBoundConstructor
    public MozmillBuilder(String tests, String wrapper, String logfile, boolean showall, boolean showerrors, String port) {
        this.tests = tests;
        this.wrapper = wrapper;
        this.logfile = logfile;
        this.showall = showall;
        this.showerrors = showerrors;
        this.port = port;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getTests() {
        return tests;
    }
    public String getWrapper() {
        return wrapper;
    }
    public String getLogfile() {
        return logfile;
    }

    public boolean getShowall() {
        return showall;
    }

    public boolean getShowerrors(){
        return showerrors;
    }

    public String getPort(){
        return port;
    }

    public String buildCommand(){
        String command = "";

        // use the wrapper script instead of mozmill if it exists
        if (!this.getWrapper().isEmpty()){
            command += this.getWrapper();
        } else {
            command += "mozmill";
        }

        // add arguments if they exist
        if (!this.getLogfile().isEmpty()){
            command += " --logfile " + this.getLogfile();
        }

        if (!this.getPort().isEmpty()){
            command += " --port="+this.getPort();
        }

        // add the tests
        command += " -t " + this.getTests();

        if (this.getShowall()){
            command += " --showall";
        }
        if (this.getShowerrors()){
            command += " --show-errors";
        }


        return command;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // this is where you 'build' the project
        // since this is a dummy, we just say 'hello world' and call that a build
        int exitCode = 0;

        if (this.getTests().equals("")){
            listener.error("Mozmill cannot run without any tests specified.");
        }
        else {
            Map<String,String> envVars = build.getEnvironment(listener);
            envVars.putAll(build.getBuildVariables());
            
            String cmd = this.buildCommand();

            exitCode = launcher.launch().cmds(Util.tokenize(cmd)).envs(envVars).stdout(listener).pwd(build.getWorkspace()).join();
        }

        if (exitCode != 0){
             build.setResult(Result.UNSTABLE);
        }

        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link MozmillBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Deprecated private transient boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a name");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Mozmill Test";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req,o);
        }
    }
}

