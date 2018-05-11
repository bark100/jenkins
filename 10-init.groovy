import jenkins.model.*
import hudson.security.*

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.traits.BranchDiscoveryTrait
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import net.sf.json.JSONObject

import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

// Setup security
def env = System.getenv()

def jenkins = Jenkins.getInstance()
jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
jenkins.setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy())

def user = jenkins.getSecurityRealm().createAccount(env.JENKINS_USER, env.JENKINS_PASS)
user.save()

jenkins.getAuthorizationStrategy().add(Jenkins.ADMINISTER, env.JENKINS_USER)
jenkins.save()


// Setup number of executors
Jenkins.instance.setNumExecutors(100)


// Setup shared library
pipeline_shared_libraries = [
    'jenkins': [
        'defaultVersion': 'master',
        'implicit': false,
        'allowVersionOverride': true,
        'includeInChangesets': true,
        'scm': [
            'remote': 'https://github.com/bark100/jenkins.git'
        ]
    ]
]

if(!binding.hasVariable('pipeline_shared_libraries')) {
    pipeline_shared_libraries = [:]
}

if(!pipeline_shared_libraries in Map) {
    throw new Exception("pipeline_shared_libraries must be an instance of Map but instead is instance of: ${pipeline_shared_libraries.getClass()}")
}

pipeline_shared_libraries = pipeline_shared_libraries as JSONObject

List libraries = [] as ArrayList
pipeline_shared_libraries.each { name, config ->
    if(name && config && config in Map && 'scm' in config && config['scm'] in Map && 'remote' in config['scm'] && config['scm'].optString('remote')) {
        def scm = new GitSCMSource(config['scm'].optString('remote'))
        // scm.credentialsId = config['scm'].optString('credentialsId')
        scm.traits = [new BranchDiscoveryTrait()]
        def retriever = new SCMSourceRetriever(scm)
        def library = new LibraryConfiguration(name, retriever)
        library.defaultVersion = config.optString('defaultVersion')
        library.implicit = config.optBoolean('implicit', false)
        library.allowVersionOverride = config.optBoolean('allowVersionOverride', true)
        library.includeInChangesets = config.optBoolean('includeInChangesets', true)
        libraries << library
    }
}

def global_settings = Jenkins.instance.getExtensionList(GlobalLibraries.class)[0]
global_settings.libraries = libraries
global_settings.save()
println 'Configured Pipeline Global Shared Libraries:\n    ' + global_settings.libraries.collect { it.name }.join('\n    ')

// Create initial jobs
def jobDslScript = new File('/tmp/jobs.groovy')
def workspace = new File('/tmp')
def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)

// Run the jobs to create some test results
new DslScriptLoader(jobManagement).runScript(jobDslScript.text)

// Add GitHub credentials
//String keyfile = "/tmp/key"
//
//Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,java.util.UUID.randomUUID().toString(), "description", "user", "password")
//
//def ksm1 = new CertificateCredentialsImpl.FileOnMasterKeyStoreSource(keyfile)
//Credentials ck1 = new CertificateCredentialsImpl(CredentialsScope.GLOBAL,java.util.UUID.randomUUID().toString(), "description", "password", ksm1)
//
//def ksm2 = new CertificateCredentialsImpl.UploadedKeyStoreSource(keyfile)
//Credentials ck2 = new CertificateCredentialsImpl(CredentialsScope.GLOBAL,java.util.UUID.randomUUID().toString(), "description", "password", ksm2)
//
//SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
//SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), ck1)
//SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), ck2)
