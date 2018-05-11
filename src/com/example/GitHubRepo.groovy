package com.example

// Need to define a superclass that can save state with Serializable:
class GitHubRepo implements Serializable {
    def script
    def parameters

    /**
     * Constructor
     *
     * @param script Should be provided from the main pipeline as 'this'. This allow the methods to invoke steps.
     * @param parameters Repository parameters: name, .
     */
    // Need a constructor to init steps, env and parameters from pipeline to class:
    GitHubRepo(script, Map parameters) {
        this.script = script
        this.parameters = parameters
    }

    String name = parameters.name ?: 'coderbot'
    Boolean shallow = parameters.shallow ?: false
    Boolean rebase = parameters.rebase ?: false
    String targetBranch = parameters.targetBranch ?: 'master'
    Boolean changeLogs = parameters.changeLogs ?: true

    /**
     * Checkout branch.
     *
     * @param branch the name of branch to checkout from repo
     * @return scmVars the source control object containing the checked out version
     */
    def checkoutBranch(String branch = "master") {

        // Always clean workspace before checkout
        def extensions = [
                [$class: 'CleanBeforeCheckout']
        ]
        if(rebase) {
            extensions.add([$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: name, mergeStrategy: 'default', mergeTarget: targetBranch]])

            // Need to provide git with identity for the merge commit
            extensions.add([$class: 'UserIdentity', email: 'jenkins@sparkbeyond.com', name: 'jenkins'])
        }
        if(shallow) {
            extensions.add([$class: 'CloneOption', depth: 0, noTags: true, reference: ''])
        }

        def scmVars

        // Use custom retry mechanism because pipeline builtin "retry()" does not abort the loop with try-catch
        int maxTries = 10
        int count = 0

        while(true) {
            try {
                scmVars = script.checkout changelogs: changeLogs,
                        scm: [
                                $class           : 'GitSCM',
                                branches         : [[name: branch]],
                                extensions       : extensions,
                                userRemoteConfigs: [
                                        [
                                                name         : name,
                                                url          : "https://github.com/${name}"
                                        ]
                                ]
                        ]
            }
            catch(Throwable e) {
                script.println "ERROR checkout scm: " + e.getMessage()

                // Abort immediately if cannot rebase due to merge conflict
                if(rebase && e.getMessage().contains("CONFLICT (content): Merge conflict in")) {
                    throw e
                }
                // Retry for a maximum count of maxTries
                else if(++count == maxTries) {
                    throw e
                }
            }
            return scmVars
        }
    }
}
